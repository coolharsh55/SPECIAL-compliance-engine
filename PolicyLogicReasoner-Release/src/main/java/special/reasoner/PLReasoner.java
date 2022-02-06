/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import java.util.*;
import java.util.stream.*;
import javax.annotation.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.vocab.*;
import special.model.*;
import special.model.exception.*;
import special.model.hierarchy.*;
import special.reasoner.cache.*;
import special.profile.*;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.*;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.*;

/**
 *
 * @author Luca Ioffredo
 */
public class PLReasoner implements OWLReasoner {

    public static final String REASONER_NAME = "PolicyLogicReasoner";
    private static final Version VERSION = new Version(1, 5, 3, 0);
    private static final Set<AxiomType> SUPPORTED_AXIOMS = new HashSet<>(Arrays.asList(AxiomType.SUBCLASS_OF, AxiomType.FUNCTIONAL_DATA_PROPERTY, AxiomType.DISJOINT_CLASSES, AxiomType.FUNCTIONAL_OBJECT_PROPERTY, AxiomType.OBJECT_PROPERTY_RANGE));
    private static final Set<InferenceType> PRECOMPUTED_INFERENCE_TYPES = new HashSet<>(Arrays.asList(InferenceType.CLASS_HIERARCHY, InferenceType.DISJOINT_CLASSES));
    private final SingleKeyCache<OWLClassExpression> fullSubClassConceptCache;
    private final SingleKeyCache<OWLClassExpression> fullSuperClassConceptCache;
    private final DoubleKeyCache<OWLClassExpression, OWLClassExpression, ANDNODE> fullConceptlIntervalSafeCache;
    private final SingleKeyCache<ANDNODE> simpleConceptCache;
    private final DoubleKeyCache<ANDNODE, ORNODE, ANDNODE> simpleConceptIntervalSafeCache;
    private final OWLReasonerConfiguration configuration;
    private final OWLOntologyManager manager;
    private final OWLOntology rootOntology; // Ontology 
    private final BufferingMode bufferingMode; // Mode BUFFERING - NON_BUFFERING. Keeps any changes in memory or use them at runtime.
    private final OWLDataFactory dataFactory;
    private final List<OWLOntologyChange> pendingChanges; // Keeps pending changes in ontology - Only with BUFFERING 
    private final RawOntologyChangeListener ontologyChangeListener; // Listener that catch changes in ontology
    private final OWLProfile profile;
    private final HierarchyGraph<OWLClass> classHierarchy;
    private final HierarchyGraph<OWLDataProperty> dataPropertyHierarchy;
    private final HierarchyGraph<OWLObjectPropertyExpression> objectPropertyHierarchy;
    private final OWLClass bottomEntity;
    private final OWLClass topEntity;
    private final boolean checkNotSupportedAxioms = false;
    private Cache<OWLClass, OWLClass> trueDisjointsCache;
    private Cache<OWLClass, OWLClass> falseDisjointsCache;
    private boolean reasonerIsConsistent = false; //There isn't ABOX, so always consistent
    private boolean fullTreeCache = true; // Cache to store concepts at runtime. If false, any concept is processed at each call
    private boolean fullIntervalSafeCache = true; // Cache to store concepts interval-safety normalized at runtime. If false, any concept is processed at each call
    private boolean simpleTreeCache = true;
    private boolean simpleIntervalSafeCache = true;
    private boolean cacheDisjointsTrueBetweenClasses = false;
    private boolean cacheDisjointsFalseBetweenClasses = false;
    private boolean normalizeSuperClassConcept = false;
    private boolean interrupted = false;
//    public int queryFullCache = 0, foundFullCache = 0, querySimpleCache = 0, foundSimpleCache = 0;

    public PLReasoner(OWLOntology rootOntology, OWLReasonerConfiguration configuration,
            BufferingMode bufferingMode, SingleKeyCache singleKeyCache, DoubleKeyCache doubleKeyCache) {
        this.rootOntology = checkNotNull(rootOntology, "rootOntology cannot be null");
        this.bufferingMode = checkNotNull(bufferingMode, "bufferingMode cannot be null");
        this.configuration = checkNotNull(configuration, "configuration cannot be null");
        this.fullSubClassConceptCache = checkNotNull(singleKeyCache, "singleKeyCache cannot be null");
        this.fullConceptlIntervalSafeCache = checkNotNull(doubleKeyCache, "doubleKeyCache cannot be null");

        this.fullSuperClassConceptCache = new PolicyCacheInMemory<OWLClassExpression>(8192);
        this.simpleConceptCache = new PolicyCacheInMemory<ANDNODE>(8192);
        this.simpleConceptIntervalSafeCache = new IntervalSafePoliciesCacheInMemory<ANDNODE, ORNODE>(8192);

        this.profile = new PLProfile();
        OWLProfileReport report = this.profile.checkOntology(this.rootOntology);
        if (checkNotSupportedAxioms && !report.isInProfile()) {
            System.err.println("Violation in ontology: " + this.rootOntology.getOntologyID());
            for (final OWLProfileViolation violation : report.getViolations()) {
                System.err.println("Violation: " + violation.getAxiom());
                if (false) {
                    throw new AxiomNotInProfileException(violation.getAxiom(), profile.getIRI());
                }
            }
        }

        this.manager = this.rootOntology.getOWLOntologyManager();
        this.dataFactory = this.manager.getOWLDataFactory();
        this.pendingChanges = new LinkedList<>();

        this.ontologyChangeListener = new RawOntologyChangeListener();
        this.manager.addOntologyChangeListener(this.ontologyChangeListener);
        this.bottomEntity = this.dataFactory.getOWLNothing();
        this.topEntity = this.dataFactory.getOWLThing();
        this.classHierarchy
                = new ClassHierarchyInOntology(
                        this.topEntity,
                        this.bottomEntity,
                        new ClassInOntology(this.rootOntology, this.dataFactory));
        this.dataPropertyHierarchy
                = new DataPropertyHierarchyInOntology(
                        this.dataFactory.getOWLTopDataProperty(),
                        this.dataFactory.getOWLBottomDataProperty(),
                        new DataPropertyInOntology(this.rootOntology, this.dataFactory));
        this.objectPropertyHierarchy
                = new ObjectPropertyHierarchyInOntology(
                        this.dataFactory.getOWLTopObjectProperty(),
                        this.dataFactory.getOWLBottomObjectProperty(),
                        new ObjectPropertyInOntology(this.rootOntology, this.dataFactory));
        this.prepareHierarchy();

        if (configuration.getClass().equals(PLConfiguration.class)) {
            PLConfiguration conf = (PLConfiguration) configuration;
            if (conf.hasCacheDisjointTrueClasses()) {
                this.cacheDisjointsTrueBetweenClasses = true;
                trueDisjointsCache = new SoftReferenceCache(4096);
            }
            if (conf.hasCacheDisjointFalseClasses()) {
                this.cacheDisjointsFalseBetweenClasses = true;
                falseDisjointsCache = new SoftReferenceCache(4096);
            }
            this.fullTreeCache = conf.hasCacheFullConcept();
            this.fullIntervalSafeCache = conf.hasCacheFullConceptIntervalSafe();
            this.simpleTreeCache = conf.hasCacheSimpleConcept();
            this.simpleIntervalSafeCache = conf.hasCacheSimpleConceptIntervalSafe();
            this.normalizeSuperClassConcept = conf.hasNormalizationSuperClassConcept();
        } else {
            this.fullTreeCache = PLConfiguration.DEFAULT_FULL_TREE_CACHE;
            this.fullIntervalSafeCache = PLConfiguration.DEFAULT_FULL_INTERVAL_SAFE_CACHE;
            this.simpleTreeCache = PLConfiguration.DEFAULT_SIMPLE_TREE_CACHE;
            this.simpleIntervalSafeCache = PLConfiguration.DEFAULT_SIMPLE_INTERVAL_SAFE_CACHE;
            this.normalizeSuperClassConcept = PLConfiguration.DEFAULT_NORMALIZE_SUPER_CLASS_CONCEPT;
        }

//        ((ClassHierarchyInOntology) classHierarchy).printInfo();
    }

    public PLReasoner(OWLOntology rootOntology, OWLReasonerConfiguration configuration,
            BufferingMode bufferingMode) {
        this(rootOntology,
                configuration,
                bufferingMode,
                new PolicyCacheInMemory<OWLClassExpression>(8192),
                new IntervalSafePoliciesCacheInMemory<OWLClassExpression, OWLClassExpression>(8192));
    }

    /**
     * Compute the hierarchy's classification
     */
    public final void prepareHierarchy() {
        this.classHierarchy.compute();
        this.dataPropertyHierarchy.compute();
        this.objectPropertyHierarchy.compute();
    }

    /**
     * Enable or disable the in memory cache.
     *
     * @deprecated This method is no longer used from newer verswion.
     * <p>
     * Use
     * {@link PolicyLogicReasoner#setFullConceptCache(boolean treeCache, boolean intervalSafety)}
     * and use
     * {@link PolicyLogicReasoner#setSimpleConceptCache(boolean treeCache, boolean intervalSafety)}
     * instead.
     *
     * @param treeCache
     * @param intervalSafety
     */
    @Deprecated
    public final void setCache(boolean treeCache, boolean intervalSafety) {
        setFullConceptCache(treeCache, intervalSafety);
        setSimpleConceptCache(treeCache, intervalSafety);
    }

    /**
     * Enable or disable the in memory cache for Full Concept
     *
     * @param treeCache Cache to preserve a tree after normalization of 7 rules
     * @param intervalSafety Preserves a tree after interval safety
     * normalization
     */
    public final void setFullConceptCache(boolean treeCache, boolean intervalSafety) {
        this.fullTreeCache = treeCache;
        this.fullIntervalSafeCache = intervalSafety;
    }

    /**
     * Enable or disable the in memory cache for Simple Concept
     *
     * @param treeCache Cache to preserve a tree after normalization of 7 rules
     * @param intervalSafety Preserves a tree after interval safety
     * normalization
     */
    public final void setSimpleConceptCache(boolean treeCache, boolean intervalSafety) {
        this.simpleTreeCache = treeCache;
        this.simpleIntervalSafeCache = intervalSafety;
    }

    /**
     * Clear internal caches
     */
    public void clearCache() {
        if (this.fullSubClassConceptCache != null) {
            this.fullSubClassConceptCache.clear();
        }
        if (this.fullSuperClassConceptCache != null) {
            this.fullSuperClassConceptCache.clear();
        }
        if (this.fullConceptlIntervalSafeCache != null) {
            this.fullConceptlIntervalSafeCache.clear();
        }
        if (this.simpleConceptIntervalSafeCache != null) {
            this.simpleConceptIntervalSafeCache.clear();
        }
        if (this.simpleConceptCache != null) {
            this.simpleConceptCache.clear();
        }
    }

    /**
     *
     * @return true if in memory caching of full tree parsing is enable, false
     * otherwise
     */
    public boolean isFullCacheEnabled() {
        return this.fullTreeCache;
    }

    /**
     *
     * @return true if in memory caching of full interval safety is enable,
     * false otherwise
     */
    public boolean isFullIntervalSafeCacheEnabled() {
        return this.fullIntervalSafeCache;
    }

    /**
     *
     * @return true if in memory caching of simple tree parsing is enable, false
     * otherwise
     */
    public boolean isSimpleCacheEnabled() {
        return this.simpleTreeCache;
    }

    /**
     *
     * @return true if in memory caching of simple interval safety is enable,
     * false otherwise
     */
    public boolean isSimpleIntervalSafeCacheEnabled() {
        return this.simpleIntervalSafeCache;
    }

    /**
     * Return the OWLDataFactory
     *
     * @return OWLDataFactory
     */
    public OWLDataFactory getOWLDataFactory() {
        return rootOntology.getOWLOntologyManager().getOWLDataFactory();
    }

    /**
     * Return the reasoner name
     *
     * @return String
     */
    @Override
    public String getReasonerName() {
        return REASONER_NAME;
    }

    /**
     * Return the reasoner version
     *
     * @return Version
     */
    @Override
    public Version getReasonerVersion() {
        return VERSION;
    }

    /**
     * Return the root ontology used by the reasoner
     *
     * @return String
     */
    @Override
    public OWLOntology getRootOntology() {
        return rootOntology;
    }

    /**
     * Check if expression specified is an Policy Logic expression
     *
     * @param ce OWLClassExpression
     * @return true or false
     */
    public boolean PLChecking(OWLClassExpression ce) {
        return ce != null && (ce instanceof OWLClass || ce instanceof OWLDataSomeValuesFrom
                || ce instanceof OWLObjectSomeValuesFrom
                || ce instanceof OWLObjectUnionOf
                || ce instanceof OWLObjectIntersectionOf);
    }

    /**
     * Build a ANDNODE tree parsing the concept specified
     *
     * @param inputG OWLClassExpression of the concept specified
     * @return root node ANDNODE
     * @throws special.model.exception.IllegalPolicyLogicExpressionException if
     * the specified concept is not a PL concept
     */
    public ANDNODE buildTree(@Nonnull OWLClassExpression inputG) throws IllegalPolicyLogicExpressionException {
        ANDNODE root = new ANDNODE();
        for (OWLClassExpression conjunct : inputG.asConjunctSet()) {
            switch (conjunct.getClassExpressionType()) {
                case OWL_CLASS:
                    root.addConceptName(conjunct.asOWLClass());
                    break;
                case DATA_SOME_VALUES_FROM:
                    int minCard = 0;
                    int maxCard = Integer.MAX_VALUE;
                    OWLDataSomeValuesFrom dataConstraint = (OWLDataSomeValuesFrom) conjunct;
                    OWLDataProperty p = dataConstraint.getProperty().asOWLDataProperty();     //data constraint
                    OWLDatatypeRestriction restriction = (OWLDatatypeRestriction) dataConstraint.getFiller();
                    for (OWLFacetRestriction facetRestrinction : restriction.facetRestrictionsAsList()) {
                        OWLFacet facet = facetRestrinction.getFacet();
                        switch (facet) {
                            case MIN_INCLUSIVE:
                                minCard = facetRestrinction.getFacetValue().parseInteger();
                                break;
                            case MAX_INCLUSIVE:
                                maxCard = facetRestrinction.getFacetValue().parseInteger();
                                break;
                            case MIN_EXCLUSIVE:
                                minCard = facetRestrinction.getFacetValue().parseInteger() + 1;
                                break;
                            case MAX_EXCLUSIVE:
                                maxCard = facetRestrinction.getFacetValue().parseInteger() - 1;
                                break;
                            default:
                                throw new IllegalPolicyLogicExpressionException("Facet type non allowed:" + facet + "\nType found: " + conjunct.getNNF());
                        }
                    }
                    root.addDataProperty(p, new IntRange(minCard, maxCard));
                    break;
                case OBJECT_SOME_VALUES_FROM:
                    OWLObjectSomeValuesFrom existential = (OWLObjectSomeValuesFrom) conjunct;
                    OWLClassExpression fillerC = existential.getFiller();
                    ANDNODE child = buildTree(fillerC);
                    root.addChild(existential, child);
                    break;
                case OBJECT_UNION_OF:
                    Set<OWLClassExpression> disjuncts = conjunct.asDisjunctSet();
                    Deque<ANDNODE> nodes = new ArrayDeque<>(disjuncts.size());
                    for (OWLClassExpression disjunct : disjuncts) {
                        ANDNODE node = buildTree(disjunct);
                        nodes.add(node);
                    }
                    root.addDisjuncts(nodes);
                    break;
                default:
                    throw new IllegalPolicyLogicExpressionException("Type found: " + conjunct.getNNF());
            }
        }
        return root;
    }

    /**
     * Normalize the tree moving disjuncts at top level of the tree (in root
     * node). The result is a DNF concept.
     *
     * @param root ANDNODE
     * @return ORNODE with a tree for each disjunct normalized
     */
    public ORNODE normalizeUnion(final @Nonnull ANDNODE root) {
        ORNODE node = null;
        if (root.hasORNodes()) {
            Deque<ORNODE> rootOrNodes = root.getORNodes();
            Deque<ORNODE> orNodes = new ArrayDeque<>(rootOrNodes.size());
            while (!rootOrNodes.isEmpty()) {
                ORNODE orNode = rootOrNodes.pollFirst();
                node = new ORNODE();
                while (!orNode.isEmpty()) {
                    for (ANDNODE subDisjunct : normalizeUnion(orNode.pollFirst())) {
                        node.addTree(subDisjunct);
                    }
                }
                orNodes.add(node);
            }
            root.clearORNodes();
            root.addORnodes(orNodes);
        }
//        boolean childrenModified = false;

//        int childrenCount = root.getChildren().size();
//        childrenCount = (int) (childrenCount + (childrenCount * 0.4) + 1);
//        Map<OWLObjectProperty, List<ANDNODE>> propertyToPreserve = new HashMap<>(childrenCount);
//        for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : root.getChildrenEntrySet()) {
//            OWLObjectProperty property = entry.getKey();
//            List<ANDNODE> children = entry.getValue();
//            List<ANDNODE> treesToPreserve = new ArrayList<>(children.size());
//            for (ANDNODE child : children) {
//                ORNODE disjunctsSubTree = normalizeUnion(child);
//                if (disjunctsSubTree.size() > 1) {
//                    ORNODE tmp = new ORNODE(disjunctsSubTree.size());
//                    while (!disjunctsSubTree.isEmpty()) {
//                        ANDNODE rootSubTree = new ANDNODE();
//                        rootSubTree.addChild(property, disjunctsSubTree.pollFirst());
//                        tmp.addTree(rootSubTree);
//                    }
//                    root.addORnode(tmp);
//                } else {
//                    treesToPreserve.add(child);
//                }
//            }
//            if (!treesToPreserve.isEmpty()) {
//                propertyToPreserve.put(property, treesToPreserve);
//            }
//        }
//        root.setChildren(propertyToPreserve);
//        propertyToPreserve = null;
        for (Iterator<Map.Entry<OWLObjectProperty, List<ANDNODE>>> childrenKeySetIterator = root.getChildrenEntrySet().iterator(); childrenKeySetIterator.hasNext();) {
            Map.Entry<OWLObjectProperty, List<ANDNODE>> entry = childrenKeySetIterator.next();
            OWLObjectProperty property = entry.getKey();
            for (Iterator<ANDNODE> children = entry.getValue().iterator(); children.hasNext();) {
                ORNODE disjunctsSubTree = normalizeUnion(children.next());
                if (disjunctsSubTree.size() > 1) {
                    ORNODE tmp = new ORNODE(disjunctsSubTree.size());
                    while (!disjunctsSubTree.isEmpty()) {
                        ANDNODE rootSubTree = new ANDNODE();
                        rootSubTree.addChild(property, disjunctsSubTree.pollFirst());
                        tmp.addTree(rootSubTree);
                    }
//                    childrenModified = true;
                    root.addORnode(tmp);
                    children.remove();
                }
            }
            if (root.existsButIsEmpty(property)) {
                childrenKeySetIterator.remove();
            }
        }
//        node = new ORNODE();
        if (root.hasORNodes()) {
            Deque<ORNODE> disjuncts = root.getORNodes();
            if (disjuncts.size() > 1) {
                Collection<ANDNODE> combinations = applyDistributivityIterative(disjuncts);
                node = new ORNODE(combinations.size());
                node.addAll(combinations);
//                node.addTrees(applyDistributivityIterative(disjuncts));
            } else {
                node = new ORNODE();
                node.addTrees(disjuncts.pollFirst());
            }
            root.clearORNodes();
        }
        if (node != null) {
            for (ANDNODE disjunct : node) {
//                if (!disjunct.replaceConceptNames(root.getConceptNames())) {
                disjunct.addConceptName(root.getConceptNames());
//                }
//                if (!disjunct.replaceDataProperties(root.getDataProperty())) {
                for (Map.Entry<OWLDataProperty, List<IntRange>> entry : root.getDataPropertyEntrySet()) {
                    disjunct.addDataProperty(entry.getKey(), entry.getValue());
                }
//                }
//                if (childrenModified || !disjunct.replaceChildren(root.getChildren())) {
                for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : root.getChildrenEntrySet()) {
//                        if (childrenModified || !disjunct.replaceNodeOfChild(entry.getKey(), entry.getValue())) {
                    disjunct.addChild(entry.getKey(), entry.getValue());
//                        }
                }
//                }
            }
//            root.clearData();
        } else {
            node = new ORNODE();
            node.addTree(root);
        }
        return node;
    }

    /**
     * Apply distributivity on orNodes in input. Iterative version.
     *
     * @param orNodes Data structure created by normalizeUnion function
     * @param conjuncts Support set with combinations of elements.
     * @return List<ANDNODE> with applied distributivity
     */
    private Collection<ANDNODE> applyDistributivityIterative(final @Nonnull Deque<ORNODE> orNodes) {
        if (orNodes == null || orNodes.isEmpty()) {
            return Collections.emptyList();
        }
        Deque<Deque<ANDNODE>> combinations = new ArrayDeque<>(orNodes.getFirst().size());
        Deque<Deque<ANDNODE>> newCombinations;
        for (ANDNODE i : orNodes.pollFirst()) {
            Deque<ANDNODE> newList = new ArrayDeque<>(1);
            newList.add(i);
            combinations.add(newList);
        }
        while (!orNodes.isEmpty()) {
            Deque<ANDNODE> nextList = orNodes.pollFirst();
            newCombinations = new ArrayDeque<>(combinations.size() * nextList.size());
            for (Deque<ANDNODE> first : combinations) {
                for (ANDNODE second : nextList) {
                    Deque<ANDNODE> tmp = new ArrayDeque<>(first.size() + 1);
                    tmp.addAll(first);
                    tmp.add(second);
                    newCombinations.add(tmp);
                }
            }
            combinations = newCombinations;
        }
        newCombinations = null;
        Collection<ANDNODE> newOrNodes = new ArrayList<>(combinations.size());
        for (Collection<ANDNODE> combination : combinations) {
            newOrNodes.add(mergeANDNODEs(combination));
        }
        return newOrNodes;
    }

    /**
     * Merge the set of trees in an unique tree
     *
     * @param trees Deque of trees
     * @return ANDNODE merged
     */
    private ANDNODE mergeANDNODEs(final @Nonnull Collection<ANDNODE> trees) {
        ANDNODE root = null;
        if (!trees.isEmpty()) {
            root = new ANDNODE();
            for (ANDNODE tree : trees) {
                root.addConceptName(tree.getConceptNames());
                for (Map.Entry<OWLDataProperty, List<IntRange>> entry : tree.getDataPropertyEntrySet()) {
                    root.addDataProperty(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : tree.getChildrenEntrySet()) {
                    root.addChild(entry.getKey(), entry.getValue());
                }
                if (tree.hasORNodes()) {
                    root.addORnodes(tree.getORNodes());
                }
            }
        }
        return root;
    }

    /**
     * Merge each functional property applying the policy logic's rules. This
     * function apply rules #4, #5 and #6
     *
     * @param root ANDNODE root node of a tree
     * @param wrapper Support's object to help with normalization of union
     */
    protected void mergeFunctionals(@Nonnull ANDNODE root, final @Nonnull WrapperBoolean wrapper) {
        Deque<ANDNODE> queueDown = new LinkedList<>();
        queueDown.add(root);
        while (!queueDown.isEmpty()) {
            root = queueDown.pollFirst();
            mergeConstraints(root);
            mergeExistentials(root);
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : root.getChildrenEntrySet()) {
                Set<EntityIntersectionNode<OWLClass>> setRanges = objectPropertyHierarchy.getPropertyRange(entry.getKey());
                for (ANDNODE child : entry.getValue()) {
                    if (!child.hasORNodes()) {
                        if (setRanges.size() > 1) {
                            /* The range is a disjunction */
                            boolean foundDisjunct = false;
                            for (EntityIntersectionNode<OWLClass> intersectionNode : setRanges) {
                                boolean foundConjunct = true;
                                for (OWLClass A : intersectionNode) {
                                    if (!child.containsConceptName(A)) {
                                        foundConjunct = false;
                                        break;
                                    }
                                }
                                if (intersectionNode.getSize() > 0 && foundConjunct) {
                                    foundDisjunct = true;
                                    break;
                                }
                            }
                            if (!foundDisjunct) {
                                List<ANDNODE> disjunctions = new ArrayList<>(setRanges.size());
                                for (EntityIntersectionNode<OWLClass> intersectionNode : setRanges) {
                                    ANDNODE conjunct = new ANDNODE();
                                    for (OWLClass A : intersectionNode) {
                                        if (!child.containsConceptName(A)) {
                                            conjunct.addConceptName(A);
                                        }
                                    }
                                    disjunctions.add(conjunct);
                                }
                                child.addDisjuncts(disjunctions);
                                wrapper.setValue(true);
                            }
                        } else {
                            for (EntityIntersectionNode<OWLClass> intersectionNode : setRanges) {
                                for (OWLClass A : intersectionNode) {
                                    child.addConceptName(A);
                                }
                            }
                        }
                    }
                    queueDown.addFirst(child);
                }
            }
        }
    }

    /**
     * Merge each Role Property functional applying the policy logic's rules.
     * Apply rule #4
     *
     * @param root ANDNODE root node of a tree
     */
    protected void mergeExistentials(@Nonnull ANDNODE root) {
        for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : root.getChildrenEntrySet()) {
            OWLObjectProperty property = entry.getKey();
            if (objectPropertyHierarchy.isFunctional(property)) {
                root.makeSingletonObjectProperty(property, mergeANDNODEs(entry.getValue()));
            }
        }
    }

    /**
     * Merge each Data Constraint functional applying the policy logic's rules.
     * Apply rule #5
     *
     * @param root ANDNODE root node of a tree
     */
    protected void mergeConstraints(@Nonnull ANDNODE root) {
        for (Map.Entry<OWLDataProperty, List<IntRange>> entry : root.getDataPropertyEntrySet()) {
            OWLDataProperty property = entry.getKey();
            if (dataPropertyHierarchy.isFunctional(property)) {
                int minCard = 0;
                int maxCard = Integer.MAX_VALUE;
                for (IntRange interval : entry.getValue()) {
                    if (minCard < interval.getMin()) {
                        minCard = interval.getMin();
                    }
                    if (maxCard > interval.getMax()) {
                        maxCard = interval.getMax();
                    }
                }
                root.makeSingletonDataProperty(property, new IntRange(minCard, maxCard));
            }
        }
    }

    /**
     * Check the tree's consistency applying the policy logic's rules. Apply
     * rule #1, #2, #3 and #7
     *
     * @param root ANDNODE root node of a tree
     * @return root ANDNODE node of the normalized tree
     */
    protected ANDNODE consistencyTree(@Nonnull ANDNODE root) {
        boolean isBottom = false;
        for (List<ANDNODE> children : root.getChildrenValuesSet()) {
            if (isBottom) {
                break;
            }
            for (ANDNODE child : children) {
                consistencyTree(child);
                if (child.containsConceptName(this.bottomEntity)) {
                    isBottom = true;
                    break;
                }
            }
        }
        if (!isBottom && root.containsConceptName(this.bottomEntity)) {
            isBottom = true;
        }
        if (!isBottom) {
            isBottom = hasDisjunction(root.getConceptNames());
//            Set<OWLClass> cn = root.getConceptNames();
//            for (OWLClass Ai : cn) {
//                if (this.cacheDisjointsTrueBetweenClasses || this.cacheDisjointsFalseBetweenClasses) {
//                    if (hasDisjunctionCache(Ai, cn)) {
//                        isBottom = true;
//                    }
//                } else if (hasDisjunction(Ai, cn)) {
//                    isBottom = true;
//                }
//                if (isBottom) {
//                    break;
//                }
//            }
        }
        for (List<IntRange> ranges : root.getDataPropertyValuesSet()) {
            if (isBottom) {
                break;
            }
            for (IntRange interval : ranges) {
                if (interval.getMin() > interval.getMax()) {
                    isBottom = true;
                    break;
                }
            }
        }
        if (isBottom) {
            root.makeBottomNode(this.bottomEntity);
        }
        return root;
    }

    public boolean hasDisjunction(final @Nonnull Collection<OWLClass> entitySet) {
        if (entitySet.size() <= 1) {
            return false;
        }
        final Set<OWLClass> visited = new HashSet<>();
        final Deque<OWLClass> queue = new ArrayDeque<>(entitySet.size() * 2);
        queue.addAll(entitySet);
        while (!queue.isEmpty()) {
            OWLClass A = queue.pop();
            if (!visited.contains(A)) {
                for (OWLClass B : classHierarchy.getDisjunctNodes(A)) {
                    if (visited.contains(B)) {
                        return true;
                    }
                }
                visited.add(A);
                for (OWLClass y : classHierarchy.getParentNodes(A)) {
                    if (!y.isOWLThing()) {
                        queue.addLast(y);
                    }
                }
            }
        }
        return false;
    }

    /**
     *
     * @param entity Concept Name from which to start the dijkstra visit
     * @param entitySet Concept Names Set of a root node ANDNODE
     * @return true if exists a disjoint in K between a concept from B and a
     * concept of CN, false otherwise
     */
    public boolean hasDisjunction(final @Nonnull OWLClass entity, final @Nonnull Collection<OWLClass> entitySet) {
        final Deque<OWLClass> queueUp = new ArrayDeque<>();
        final Deque<OWLClass> queueDown = new ArrayDeque<>();
        final Set<OWLClass> alreadyCheckedUP = new HashSet<>();
        final Set<OWLClass> alreadyCheckedDOWN = new HashSet<>();
        queueUp.add(entity);
        while (!queueUp.isEmpty()) {
            OWLClass u = queueUp.pollFirst();
            for (OWLClass x : classHierarchy.getDisjunctNodes(u)) {
                queueDown.add(x);
                while (!queueDown.isEmpty()) {
                    OWLClass d = queueDown.pollFirst();
                    if (entitySet.contains(d)) {
                        return true;
                    }
                    alreadyCheckedDOWN.add(d);
                    if (!d.isOWLNothing()) {
                        for (OWLClass y : classHierarchy.getChildNodes(d)) {
                            if (!y.isOWLNothing() && !alreadyCheckedDOWN.contains(y)) {
                                queueDown.add(y);
                            }
                        }
                    }
                }
            }
            alreadyCheckedUP.add(u);
            if (!u.isOWLThing()) {
                for (OWLClass y : classHierarchy.getParentNodes(u)) {
                    if (!y.isOWLThing() && !alreadyCheckedUP.contains(y)) {
                        queueUp.add(y);
                    }
                }
            }
        }
        return false;
    }

    /**
     * This version use a cache limited by size.
     *
     * @param entity Concept Name from which to start the dijkstra visit
     * @param entitySet Concept Names Set of a root node ANDNODE
     * @return True if exists a disjoint in K between a concept from B and a
     * concept of CN
     */
    public boolean hasDisjunctionNoDagCache(final @Nonnull OWLClass entity, @Nonnull Set<OWLClass> entitySet) {
        final Deque<OWLClass> queueUp = new ArrayDeque<>();
        final Deque<OWLClass> queueDown = new ArrayDeque<>();
        final Set<OWLClass> alreadyCheckedUP = new HashSet<>();
        final Set<OWLClass> alreadyCheckedDOWN = new HashSet<>();
        final Deque<OWLClass> superClassesOfEntity = new LinkedList<>();
        queueUp.add(entity);
        while (!queueUp.isEmpty()) {
            OWLClass u = queueUp.pollFirst();
            if (cacheDisjointsTrueBetweenClasses && !this.trueDisjointsCache.isEmptyIntersection(entitySet, u)) {
                return true;
            }
            superClassesOfEntity.add(u);
            if (cacheDisjointsFalseBetweenClasses) {
                entitySet = this.falseDisjointsCache.removeCacheValuesFromSet(entitySet, u);
            }
            if (!entitySet.isEmpty()) {
                for (OWLClass x : classHierarchy.getDisjunctNodes(u)) {
                    Deque<OWLClass> maybeDisjoints = new LinkedList<>();
                    queueDown.add(x);
                    while (!queueDown.isEmpty()) {
                        OWLClass d = queueDown.pollFirst();
                        maybeDisjoints.add(d);
                        if (entitySet.contains(d)) {
                            if (cacheDisjointsTrueBetweenClasses && !superClassesOfEntity.isEmpty()) {
                                this.trueDisjointsCache.add(superClassesOfEntity, maybeDisjoints);
                            }
                            return true;
                        }
                        alreadyCheckedDOWN.add(d);
                        if (!d.isOWLNothing()) {
                            for (OWLClass y : classHierarchy.getChildNodes(d)) {
                                if (!y.isOWLNothing() && !alreadyCheckedDOWN.contains(y)) {
                                    queueDown.add(y);
                                }
                            }
                        }
                    }
                }
            }
            alreadyCheckedUP.add(u);
            if (!u.isOWLThing()) {
                for (OWLClass y : classHierarchy.getParentNodes(u)) {
                    if (!y.isOWLThing() && !alreadyCheckedUP.contains(y)) {
                        queueUp.add(y);
                    }
                }
            }
        }
        if (cacheDisjointsFalseBetweenClasses && !superClassesOfEntity.isEmpty() && !entitySet.isEmpty()) {
            this.falseDisjointsCache.add(superClassesOfEntity, entitySet);
        }
        return false;
    }

    public boolean hasDisjunctionCache(final @Nonnull OWLClass entity, @Nonnull Set<OWLClass> entitySet) {
        final Deque<OWLClass> queueUp = new ArrayDeque<>();
        final Deque<OWLClass> queueDown = new ArrayDeque<>();
        final Deque<Integer> upCount = new ArrayDeque<>();
        final Set<OWLClass> alreadyCheckedUP = new HashSet<>();
        final Set<OWLClass> alreadyCheckedDOWN = new HashSet<>();
        final Deque<OWLClass> superClassesOfEntity = new LinkedList<>();
        queueUp.add(entity);
        boolean up = false, down = false;
        while (!queueUp.isEmpty()) {
            OWLClass u = queueUp.pollFirst();
            if (cacheDisjointsTrueBetweenClasses && !this.trueDisjointsCache.isEmptyIntersection(entitySet, u)) {
                return true;
            }
            up = false;
            down = false;
            superClassesOfEntity.add(u);
            if (cacheDisjointsFalseBetweenClasses) {
                entitySet = this.falseDisjointsCache.removeCacheValuesFromSet(entitySet, u);
            }
            if (!entitySet.isEmpty()) {
                for (OWLClass x : classHierarchy.getDisjunctNodes(u)) {
                    down = true;
                    Deque<OWLClass> maybeDisjoints = new LinkedList<>();
                    queueDown.add(x);
                    while (!queueDown.isEmpty()) {
                        OWLClass d = queueDown.pollFirst();
                        maybeDisjoints.add(d);
                        if (entitySet.contains(d)) {
                            if (cacheDisjointsTrueBetweenClasses) {
                                this.trueDisjointsCache.add(superClassesOfEntity, maybeDisjoints);
                            }
                            return true;
                        }
                        alreadyCheckedDOWN.add(d);
                        if (!d.isOWLNothing()) {
                            for (OWLClass y : classHierarchy.getChildNodes(d)) {
                                if (!y.isOWLNothing() && !alreadyCheckedDOWN.contains(y)) {
                                    queueDown.add(y);
                                }
                            }
                        }
                    }
                }
            }
            alreadyCheckedUP.add(u);
            if (!u.isOWLThing()) {
                int sizeQueueUp = queueUp.size();
                for (OWLClass y : classHierarchy.getParentNodes(u)) {
                    if (!y.isOWLThing() && !alreadyCheckedUP.contains(y)) {
                        queueUp.add(y);
                    }
                }
                sizeQueueUp = sizeQueueUp - queueUp.size();
                upCount.add(sizeQueueUp);
                if (sizeQueueUp > 0) {
                    up = true;
                }
            }
            if (!up) {
                if (down && cacheDisjointsFalseBetweenClasses) {
                    OWLClass last = superClassesOfEntity.removeLast();
                    this.falseDisjointsCache.add(superClassesOfEntity, entitySet);
                    superClassesOfEntity.addLast(last);
                }
                superClassesOfEntity.poll();
                if (!upCount.isEmpty()) {
                    int sizeQueueUp = upCount.poll();
                    while (!upCount.isEmpty() && sizeQueueUp <= 1) {
                        superClassesOfEntity.poll();
                        sizeQueueUp = upCount.poll();
                    }
                    if (sizeQueueUp > 1) {
                        sizeQueueUp--;
                        upCount.push(sizeQueueUp);
                    }
                }
            }
        }
        if (cacheDisjointsFalseBetweenClasses && !superClassesOfEntity.isEmpty()) {
            this.falseDisjointsCache.add(superClassesOfEntity, entitySet);
        }
        return false;
    }

    /**
     * Inverse parsing of a Tree.
     *
     * @param root ANDNODE root node of a tree
     * @return OWLClassExpression of the concept's tree
     */
    public OWLClassExpression buildConcept(@Nonnull ANDNODE root) {
        final OWLDatatype dataType = dataFactory.getOWLDatatype(XSDVocabulary.POSITIVE_INTEGER);
        final Deque<OWLClassExpression> concept = new LinkedList<>();
        concept.addAll(root.getConceptNames());
        for (OWLDataProperty property : root.getDataPropertyKeySet()) {
            for (IntRange interval : root.getDataProperty(property)) {
                List<OWLFacetRestriction> cardinalities = new ArrayList<>(2);
                cardinalities.add(dataFactory.getOWLFacetRestriction(OWLFacet.MIN_INCLUSIVE, interval.getMin()));
                cardinalities.add(dataFactory.getOWLFacetRestriction(OWLFacet.MAX_INCLUSIVE, interval.getMax()));
                OWLDatatypeRestriction restriction = dataFactory.getOWLDatatypeRestriction(dataType, cardinalities);
                OWLDataSomeValuesFrom constraint = dataFactory.getOWLDataSomeValuesFrom(property, restriction);
                concept.add(constraint);
            }
        }
        for (OWLObjectProperty property : root.getChildrenKeySet()) {
            for (ANDNODE child : root.getChildren(property)) {
                OWLClassExpression ex = dataFactory.getOWLObjectSomeValuesFrom(property, buildConcept(child));
                concept.add(ex);
            }
        }
        for (ORNODE disjuncts : root.getORNodes()) {
            Deque<OWLClassExpression> union = new ArrayDeque<>(disjuncts.size());
            for (ANDNODE node : disjuncts.getDisjuncts()) {
                union.add(buildConcept(node));
            }
            if (union.size() == 1) {
                concept.add(union.pollFirst());
            } else {
                concept.add(dataFactory.getOWLObjectUnionOf(union));
            }
        }
        if (concept.size() == 1) {
            return concept.pollFirst();
        } else {
            return dataFactory.getOWLObjectIntersectionOf(concept);
        }
    }

    /**
     * Inverse parsing of a Trees's Set.
     *
     * @param trees ANDNODE set
     * @return OWLClassExpression as UNION of each tree's concept
     */
    public OWLClassExpression buildConcept(final @Nonnull Collection<ANDNODE> trees) {
        Set<OWLClassExpression> concept = new HashSet<>();
        for (ANDNODE root : trees) {
            concept.add(buildConcept(root));
        }
        if (concept.size() == 1) {
            return concept.iterator().next();
        }
        return dataFactory.getOWLObjectUnionOf(concept);
    }

    /**
     * Inverse parsing of a Trees's Set.
     *
     * @param node ORNODE containing a set of trees
     * @return OWLClassExpression as UNION of each tree's concept
     */
    public OWLClassExpression buildConcept(final @Nonnull ORNODE node) {
        return buildConcept(node.getDisjuncts());
    }

    /**
     * Retrieve a set containing each data property's interval specified by the
     * property
     *
     * @param tree ANDNODE root of the tree to analyze
     * @param property Data property specified
     * @return A set of Integer array with size = 2
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    protected Deque<IntRange> getIntervalsOfCostraint(@Nonnull ANDNODE tree, final @Nonnull OWLDataProperty property) throws UnionNotNormalizedException {
        Deque<ANDNODE> queueDown = new LinkedList<>();
        Deque<IntRange> intervals = new LinkedList<>();
        queueDown.add(tree);
        while (!queueDown.isEmpty()) {
            tree = queueDown.pollFirst();
            if (tree.hasORNodes()) {
                throw new UnionNotNormalizedException("Subtree has disjuncts in some nodes. Normalize them in DNF before structural subsumption.");
            }
            if (tree.containsDataProperty(property)) {
                intervals.addAll(tree.getDataProperty(property));
            }
            for (List<ANDNODE> trees : tree.getChildrenValuesSet()) {
                queueDown.addAll(trees);
            }
        }
        return intervals;
    }

    /**
     * Retrieve a set containing each data property's interval specified by the
     * property
     *
     * @param treesD List of ANDNODE to analyze
     * @param property Data property specified
     * @return A set of Integer array with size = 2
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    protected Set<IntRange> getIntervalsOfCostraint(final @Nonnull Collection<ANDNODE> treesD, final @Nonnull OWLDataProperty property) throws UnionNotNormalizedException {
        Set<IntRange> intervals = new HashSet<>(32);
        for (ANDNODE D : treesD) {
            Deque<ANDNODE> queueDown = new LinkedList<>();
            queueDown.add(D);
            while (!queueDown.isEmpty()) {
                ANDNODE tree = queueDown.pollFirst();
                if (tree.hasORNodes()) {
                    throw new UnionNotNormalizedException("Subtree has disjuncts in some nodes. Normalize them in DNF before structural subsumption.");
                }
                if (tree.containsDataProperty(property)) {
                    intervals.addAll(tree.getDataProperty(property));
                }
                for (List<ANDNODE> trees : tree.getChildrenValuesSet()) {
                    queueDown.addAll(trees);
                }
            }
        }
        return intervals;
    }

    /**
     * Normalize interval safety a list of disjuncts trees C respect to another
     * list of disjuncts D
     *
     * @param treesC Disjuncts to normalize
     * @param treesD Disjuncts to consider
     * @return ORNODE with a tree for each disjunct normalized. It's a copy so
     * the original tree isn't touched
     */
    public ORNODE normalizeIntervalSafety(final @Nonnull Collection<ANDNODE> treesC, final @Nonnull ORNODE treesD) {
        final ORNODE disjunctOfC = new ORNODE();
        final WrapperBoolean wrapper = new WrapperBoolean(false);
        /* Normalizzazione di ogni albero di C - C potrebbe avere l'unione, quindi sarebbe una moltitudine di alberi */
        for (ANDNODE C : treesC) {
            wrapper.setValue(false);
            ANDNODE cCopy = applyIntervalSafe(C.copy(), treesD, wrapper);
            if (wrapper.getValue()) {
                ORNODE disjunction = normalizeUnion(cCopy);
                disjunctOfC.addTrees(disjunction.getDisjuncts());
            } else {
                disjunctOfC.addTree(cCopy);
            }
        }
        return disjunctOfC;
    }

    public ORNODE normalizeIntervalSafetyCache(final @Nonnull Collection<ANDNODE> treesC, final @Nonnull ORNODE treesD) {
        final ORNODE disjunctOfC = new ORNODE();
        final WrapperBoolean wrapper = new WrapperBoolean(false);
        /* Normalizzazione di ogni albero di C - C potrebbe avere l'unione, quindi sarebbe una moltitudine di alberi */
        for (ANDNODE C : treesC) {
            wrapper.setValue(false);
            ORNODE normalized = this.simpleConceptIntervalSafeCache.get(C, treesD);
            if (normalized == null) {
                ANDNODE cCopy = applyIntervalSafe(C.copy(), treesD, wrapper);
                if (wrapper.getValue()) {
                    ORNODE disjunction = normalizeUnion(cCopy);
                    disjunctOfC.addTrees(disjunction.getDisjuncts());
                    this.simpleConceptIntervalSafeCache.put(C, treesD, disjunction);
                } else {
                    disjunctOfC.addTree(cCopy);
                    this.simpleConceptIntervalSafeCache.put(C, treesD, cCopy);
                }
            } else {
                disjunctOfC.addTrees(normalized);
            }
        }
        return disjunctOfC;
    }

    /**
     * Normalize a tree C with interval safety respect to a disjuncts's list D
     *
     * @param treeC Tree to normalize
     * @param treesD Disjuncsts to consider
     * @param wrapper This is used to check if a normalization on union is
     * necessary.
     * @return ANDNODE normalized respect to interval safety
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    protected ANDNODE applyIntervalSafe(final @Nonnull ANDNODE treeC, final @Nonnull Collection<ANDNODE> treesD, final WrapperBoolean wrapper) throws UnionNotNormalizedException {
        if (treeC.hasORNodes()) {
            throw new UnionNotNormalizedException("Subtree at left has disjuncts in some nodes. Normalize them in DNF before structural subsumption.");
        }
        Deque<ANDNODE> queueDown = new LinkedList<>();
        Map<OWLDataProperty, Set<IntRange>> propertiesInD = new HashMap<>();
        queueDown.add(treeC);
        while (!queueDown.isEmpty()) {
            ANDNODE tree = queueDown.pollFirst();
            for (Iterator<Map.Entry<OWLDataProperty, List<IntRange>>> propertyIterator = tree.getDataPropertyEntrySet().iterator(); propertyIterator.hasNext();) {
                Map.Entry<OWLDataProperty, List<IntRange>> entry = propertyIterator.next();
                OWLDataProperty property = entry.getKey();
                Set<IntRange> intervalsInD
                        = propertiesInD
                                .computeIfAbsent(property, p -> getIntervalsOfCostraint(treesD, p));
                if (!intervalsInD.isEmpty()) {
                    final int extremesSize = intervalsInD.size() * 2 + 2;
                    for (ListIterator<IntRange> intervalInCiterator = entry.getValue().listIterator(); intervalInCiterator.hasNext();) {
                        IntRange intervalC = intervalInCiterator.next();
                        final int min = intervalC.getMin();
                        final int max = intervalC.getMax();
                        final Set<Integer> left = new HashSet<>(extremesSize);
                        final Set<Integer> right = new HashSet<>(extremesSize);
                        final Set<Integer> extremesSet = new HashSet<>(extremesSize);
                        left.add(min);
                        right.add(max);
                        extremesSet.add(min);
                        extremesSet.add(max);
                        for (IntRange intervalD : intervalsInD) {
                            int minD = intervalD.getMin();
                            int maxD = intervalD.getMax();
                            if (minD >= min && minD <= max) {
                                left.add(minD);
                                extremesSet.add(minD);
                            }
                            if (maxD >= min && maxD <= max) {
                                right.add(maxD);
                                extremesSet.add(maxD);
                            }
                        }
                        final int[] extremes = extremesSet.stream().mapToInt(x -> x).toArray();
                        Arrays.sort(extremes);
                        if (extremes.length == 1) {
                            intervalInCiterator.set(new IntRange(extremes[0], extremes[0]));
                        } else {
                            final Deque<IntRange> rangesCreated = new ArrayDeque<>(max - min + 1);
                            for (int x = 0; x < extremes.length - 1; x++) {
                                int current = extremes[x];
                                int next = extremes[x + 1];
                                if (left.contains(current) && right.contains(current)) {
                                    rangesCreated.add(new IntRange(current, current));
                                }
                                if (right.contains(current)) {
                                    current += 1;
                                }
                                if (left.contains(next)) {
                                    next -= 1;
                                }
                                if (current <= next) {
                                    rangesCreated.add(new IntRange(current, next));
                                }
                            }
                            int last = extremes[extremes.length - 1];
                            if (left.contains(last) && right.contains(last)) {
                                rangesCreated.add(new IntRange(last, last));
                            }
                            if (rangesCreated.size() <= 1) {
                                intervalInCiterator.set(rangesCreated.getFirst());
                            } else {
                                ORNODE orNodes = new ORNODE(rangesCreated.size());
                                for (IntRange interval : rangesCreated) {
                                    ANDNODE disjunct = new ANDNODE();
                                    disjunct.addDataProperty(property, interval);
                                    orNodes.addTree(disjunct);
                                }
                                tree.addORnode(orNodes);
                                wrapper.setValue(true);
                                intervalInCiterator.remove();
                                if (tree.existsButIsEmpty(property)) {
                                    propertyIterator.remove();
                                }
                            }
                        }
                    }
                }
            }
            for (List<ANDNODE> children : tree.getChildrenValuesSet()) {
                queueDown.addAll(children);
            }
        }
        return treeC;
    }

    /**
     * Check if a class A is subclass of another class B
     *
     * @param A First class, it rapresents the child
     * @param B Second Class, it rapresents the parent
     * @return true if A is a subclass of B, false otherwise
     */
    public boolean isSubClassOf(@Nonnull OWLClass A, @Nonnull OWLClass B) {
        if (A.equals(B)) {
            return true;
        }
        final Set<OWLClass> visited = new HashSet<>(512);
        final Deque<OWLClass> queueUp = new ArrayDeque<>(512);
        queueUp.add(A);
        while (!queueUp.isEmpty()) {
            OWLClass u = queueUp.pollFirst();
            Iterable<OWLClass> parents = null;
            if (classHierarchy.containsEntity(u)) {
                parents = classHierarchy.getParentNodes(u);
            } else {
                parents = classHierarchy.getTopNode();
            }
            for (OWLClass parent : parents) {
                if (parent.equals(B)) {
                    return true;
                }
                if (!parent.isOWLThing() && !visited.contains(parent)) {
                    queueUp.addLast(parent);
                    visited.add(parent);
                }
            }
        }
        return false;
    }

    public boolean isSubClassOf_OLD(@Nonnull OWLClass A, @Nonnull OWLClass B) {
        if (A.equals(B)) {
            return true;
        }
        final Deque<OWLClass> queueUp = new ArrayDeque<>();
        queueUp.add(A);
        while (!queueUp.isEmpty()) {
            OWLClass u = queueUp.pollFirst();
            for (OWLClass parent : classHierarchy.getParentNodes(u)) {
                if (parent.equals(B)) {
                    return true;
                }
                if (!parent.isOWLThing()) {
                    queueUp.add(parent);
                }
            }
        }
        return false;
    }

    /**
     * Check if the tree C is subsumed by the tree D. STS algorithm
     *
     * @param C Simple PL Concept to check respect to treesD
     * @param treesD Full PL Concept, it rapresents the consent policy
     * @return true if C is subsumed by D, false otherwise
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    protected boolean structuralSubsumption(@Nonnull ANDNODE C, @Nonnull ORNODE treesD) throws UnionNotNormalizedException {
        boolean result = false;
        for (ANDNODE D : treesD) {
            checkIfInterrupted();
            result = structuralSubsumption(C, D);
            if (result) {
                break;
            }
        }
        return result;
    }

    /**
     * Check if the tree C is subsumed by the tree D. STS algorithm
     *
     * @param disjunctOfC Full PL Concept
     * @param treesD Full PL Concept
     * @return true if C is subsumed by D, false otherwise
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    protected boolean structuralSubsumption(@Nonnull ORNODE disjunctOfC, @Nonnull ORNODE treesD) throws UnionNotNormalizedException {
        boolean result = true;
        for (ANDNODE C : disjunctOfC) {
            result = structuralSubsumption(C, treesD);
            if (!result) {
                break;
            }
        }
        return result;
    }

    /**
     * Check if the tree C is subsumed by the tree D. STS algorithm
     *
     * @param C Simple PL Concept
     * @param D Simple PL Concept
     * @return true if C is subsumed by D, false otherwise
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    protected boolean structuralSubsumption(@Nonnull ANDNODE C, @Nonnull ANDNODE D) throws UnionNotNormalizedException {
        if (C.hasORNodes()) {
            throw new UnionNotNormalizedException("Subtree at left has disjuncts in some nodes. Normalize them before structural subsumption.");
        } else if (D.hasORNodes()) {
            throw new UnionNotNormalizedException("Subtree at right has disjuncts in some nodes. Normalize them before structural subsumption.");
        }
        if (C.containsConceptName(this.bottomEntity)) {
            return true;
        }
        boolean result = true;
        for (OWLClass B : D.getConceptNames()) {
            if (!result) {
                break;
            }
            result = false;
            for (OWLClass A : C.getConceptNames()) {
                if (isSubClassOf(A, B)) {
                    result = true;
                    break;
                }
            }
        }
        for (Map.Entry<OWLDataProperty, List<IntRange>> constraint : D.getDataPropertyEntrySet()) {
            if (!result) {
                break;
            }
            OWLDataProperty property = constraint.getKey();
            for (IntRange intervalD : constraint.getValue()) {
                if (!result) {
                    break;
                }
                result = false;
                for (IntRange intervalC : C.getDataProperty(property)) {
                    if (intervalC.isIncluseIn(intervalD)) {
                        result = true;
                        break;
                    }
                }
            }
        }
        for (Map.Entry<OWLObjectProperty, List<ANDNODE>> child : D.getChildrenEntrySet()) {
            if (!result) {
                break;
            }
            OWLObjectProperty property = child.getKey();
            for (ANDNODE childD : child.getValue()) {
                if (!result) {
                    break;
                }
                result = false;
                for (ANDNODE childC : C.getChildren(property)) {
                    result = structuralSubsumption(childC, childD);
                    if (result) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Take an OWLClassExpression to normalize and build a tree
     *
     * @param ce FULL PL Concept to normalize respect to 7 rules
     * @return ORNODE with a tree for each disjunct normalized
     */
    public ORNODE normalizeSatisfiability(@Nonnull OWLClassExpression ce) {
        final ORNODE trees = normalizeUnion(buildTree(ce));
        final ORNODE results = new ORNODE(trees.size() + 1);
        final WrapperBoolean wrapper = new WrapperBoolean(false);
        while (!trees.isEmpty()) {
            ANDNODE tree = trees.pollFirst();
            wrapper.setValue(false);
            mergeFunctionals(tree, wrapper);
            if (wrapper.getValue()) {
                /* The Rule #6 with Union is applied. The tree output is a Complex PL concept. */
                ORNODE treeToNormalize = normalizeUnion(tree);
                while (!treeToNormalize.isEmpty()) {
                    ANDNODE treeDNF = treeToNormalize.pollFirst();
                    consistencyTree(treeDNF);
                    results.addTree(treeDNF);
                }
            } else {
                /* The Rule #6 with Union was not applied. The tree output is a Simple PL concept. */
                consistencyTree(tree);
                results.addTree(tree);
            }
        }
        return results;
    }

    public ORNODE normalizeSatisfiabilityCache(@Nonnull OWLClassExpression ce) {
        final ORNODE trees = normalizeUnion(buildTree(ce));
        final ORNODE results = new ORNODE(trees.size() + 1);
        final WrapperBoolean wrapper = new WrapperBoolean(false);
        while (!trees.isEmpty()) {
            ANDNODE tree = trees.pollFirst();
            ORNODE normalized = this.simpleConceptCache.get(tree);
//            querySimpleCache++;
            if (normalized == null) {
                ANDNODE keyCache = tree.copy();
                wrapper.setValue(false);
                mergeFunctionals(tree, wrapper);
                if (wrapper.getValue()) {
                    /* The Rule #6 with Union is applied. The tree output is a Complex PL concept. */
                    ORNODE treeToNormalize = normalizeUnion(tree);
                    while (!treeToNormalize.isEmpty()) {
                        ANDNODE treeDNF = treeToNormalize.pollFirst();
                        consistencyTree(treeDNF);
                        results.addTree(treeDNF);
                        this.simpleConceptCache.put(keyCache, treeDNF);
                    }
                } else {
                    /* The Rule #6 with Union was not applied. The tree output is a Simple PL concept. */
                    consistencyTree(tree);
                    results.addTree(tree);
                    this.simpleConceptCache.put(keyCache, tree);
                }
            } else {
//                foundSimpleCache++;
                for (ANDNODE treeNormalized : normalized) {
                    results.addTree(treeNormalized);
                }
            }
        }
        return results;
    }

    /**
     * Return a NodeSet of OWLClass subclasses of the specified concept.
     *
     * @param ce The specified class. If anonymouse then the result is empty
     * @param direct true to direct subclasses or false to all subclasses
     * @return NodeSet
     */
    @Override
    public NodeSet<OWLClass> getSubClasses(@Nonnull OWLClassExpression ce, boolean direct) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            if (classHierarchy.containsEntity(ce.asOWLClass())) {
                return classHierarchy.getChildNodes(ce.asOWLClass(), direct, ns);
            } else {
                ns.addNode(classHierarchy.getBottomNode());
            }
        }
        return ns;
    }

    /**
     * Return a NodeSet of OWLClass superclasses of the specified concept.
     *
     * @param ce The specified class. If anonymouse then the result is empty
     * @param direct true to direct superclasses or false to all superclasses
     * @return NodeSet
     */
    @Override
    public NodeSet<OWLClass> getSuperClasses(@Nonnull OWLClassExpression ce, boolean direct) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            if (classHierarchy.containsEntity(ce.asOWLClass())) {
                return classHierarchy.getParentNodes(ce.asOWLClass(), direct, ns);
            } else {
                ns.addNode(classHierarchy.getTopNode());
            }
        }
        return ns;
    }

    /**
     * Check if a OWLClassExpression is already preprocessed and in cache,
     *
     * @param ce Policy to check
     * @return true if preprocessed, otherwise false
     */
    public boolean isPreprocessed(@Nonnull OWLClassExpression ce) {
        return this.fullTreeCache && fullSubClassConceptCache.containsKey(ce);
    }

    /**
     * Check if a OWLAxiom is already preprocessed and in cache,
     *
     * @param axiom Axiom to check
     * @return true if preprocessed, otherwise false
     */
    public boolean isPreprocessed(@Nonnull OWLAxiom axiom) {
        if (!this.fullIntervalSafeCache || !(axiom instanceof OWLSubClassOfAxiom)) {
            return false;
        }
        OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;
        OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
        OWLClassExpression subClass = subClassOfAxiom.getSubClass();
        return fullConceptlIntervalSafeCache.containsKey(subClass, superClass);
    }

    /**
     * Preprocess a concept OWLClassExpression (a policy) respect to the
     * normalization with seven rules. A concept preprocessed is a tree with
     * ANDNODE and ORNODE as nodes. This method cache the result in memory.
     *
     * @param ce Full PL Concept to normalize respect to 7 rules
     */
    public void preProcess(@Nonnull OWLClassExpression ce) {
        if (this.fullTreeCache) {
//            checkEntityInClassificationGraph(ce);
            ORNODE treesNormalized = fullSubClassConceptCache.get(ce);
            if (treesNormalized == null) {
                if (this.simpleTreeCache) {
                    treesNormalized = normalizeSatisfiabilityCache(ce);
                } else {
                    treesNormalized = normalizeSatisfiability(ce);
                }
                fullSubClassConceptCache.put(ce, treesNormalized);
            }
        }
    }

    public void preProcess(@Nonnull Collection<OWLClassExpression> exs) {
        if (this.fullTreeCache) {
            for (OWLClassExpression ce : exs) {
                preProcess(ce);
            }
        }
    }

    /**
     * Preprocess a SubClassOf axiom and save it in a cache in memory.
     * Preprocess each axiom's concept as a tree with ANDNODE and ORNODE nodes.
     *
     * @param axiom Query to preprocess respect each rules in the reasoner
     */
    public void preProcessIntervalSafety(@Nonnull OWLAxiom axiom) {
        if (!(axiom instanceof OWLSubClassOfAxiom)) {
            throw new UnsupportedOperationException("Expected to be encoded as OWLSubClassOfAxioms.");
        }
        if (fullConceptlIntervalSafeCache != null && !fullConceptlIntervalSafeCache.isFull()) {
            OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;
            OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
            OWLClassExpression subClass = subClassOfAxiom.getSubClass();

            /* 
            Check if the entities in thr expression there are already in the 
            classification graph.
            If not, then this add to the DAG
             */
//            checkEntityInClassificationGraph(superClass);
//            checkEntityInClassificationGraph(subClass);
            ORNODE treesC = fullSubClassConceptCache.get(subClass);
            if (treesC == null) {
                if (this.simpleTreeCache) {
                    treesC = normalizeSatisfiabilityCache(subClass);
                } else {
                    treesC = normalizeSatisfiability(subClass);
                }
                if (this.fullTreeCache) {
                    fullSubClassConceptCache.put(subClass, treesC);
                }
            }
            ORNODE treesD = fullSuperClassConceptCache.get(superClass);
            if (treesD == null) {
                if (this.normalizeSuperClassConcept) {
                    if (this.simpleTreeCache) {
                        treesD = normalizeSatisfiabilityCache(superClass);
                    } else {
                        treesD = normalizeSatisfiability(superClass);
                    }
                } else {
                    treesD = normalizeUnion(buildTree(superClass));
                }
                if (this.fullTreeCache) {
                    fullSuperClassConceptCache.put(superClass, treesD);
                }
            }
            if (!this.fullIntervalSafeCache) {
                return;
            }
            checkIfInterrupted();
            ORNODE disjunctOfC = fullConceptlIntervalSafeCache.get(subClass, superClass);
            if (disjunctOfC == null) {
                if (this.simpleIntervalSafeCache) {
                    disjunctOfC = normalizeIntervalSafetyCache(treesC, treesD);
                } else {
                    disjunctOfC = normalizeIntervalSafety(treesC, treesD);
                }
                fullConceptlIntervalSafeCache.put(subClass, superClass, disjunctOfC);
            }
        }
    }

    /**
     * Preprocess a collection of SubClassOf axioms and save it in a cache in
     * memory.
     *
     * @param axioms Collection of axioms to preprocess.
     */
    public void preProcessIntervalSafety(@Nonnull Collection<OWLAxiom> axioms) {
        for (OWLAxiom axiom : axioms) {
            preProcessIntervalSafety(axiom);
        }
    }

    @Override
    public boolean isSatisfiable(@Nonnull OWLClassExpression ce) {
        Timer timer = new Timer(this.getTimeOut());
        ORNODE treesNormalized = null;
//        checkEntityInClassificationGraph(ce);
        if (this.fullTreeCache) {
            treesNormalized = fullSubClassConceptCache.computeIfAbsent(ce, k -> normalizeSatisfiability(ce));
        } else {
            treesNormalized = normalizeSatisfiability(ce);
        }
        for (ANDNODE tree : treesNormalized) {
            timer.checkTime();
            checkIfInterrupted();
            if (!tree.containsConceptName(this.bottomEntity)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEntailed(@Nonnull OWLDisjointClassesAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        Collection<OWLClass> queue = axiom.classesInSignature().collect(Collectors.toCollection(LinkedList::new));
        for (OWLClass clazz : queue) {
//            checkEntityInClassificationGraph(clazz);
            NodeSet<OWLClass> disjoints = getDisjointClasses(clazz);
            for (OWLClass cl : queue) {
                if (cl.equals(clazz)) {
                    continue;
                }
                result = disjoints.containsEntity(cl);
                if (!result) {
                    break;
                }
            }
            timer.checkTime();
            if (!result) {
                break;
            }
        }
        return result;
    }

    public boolean isEntailed(@Nonnull OWLFunctionalObjectPropertyAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        for (OWLObjectProperty property : axiom.objectPropertiesInSignature().collect(Collectors.toCollection(LinkedList::new))) {
//            checkEntityInClassificationGraph(property);
            result = this.objectPropertyHierarchy.isFunctional(property);
            if (!result) {
                break;
            }
            timer.checkTime();
        }
        return result;
    }

    public boolean isEntailed(@Nonnull OWLFunctionalDataPropertyAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        for (OWLDataProperty property : axiom.dataPropertiesInSignature().collect(Collectors.toCollection(LinkedList::new))) {
//            checkEntityInClassificationGraph(property);
            result = this.dataPropertyHierarchy.isFunctional(property);
            if (!result) {
                break;
            }
            timer.checkTime();
        }
        return result;
    }

    public boolean isEntailed(@Nonnull OWLObjectPropertyRangeAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        OWLObjectPropertyExpression property = axiom.getProperty();
//        checkEntityInClassificationGraph(property.asOWLObjectProperty());

        Set<OWLClassExpression> supposedRanges = axiom.getRange().asDisjunctSet();
        NodeSet<OWLClass> ranges = getObjectPropertyRanges(property);
        for (OWLClassExpression ce : supposedRanges) {
            result = false;
            if (!ce.isAnonymous()) {
//                checkEntityInClassificationGraph(ce);
                result = ranges.containsEntity(ce.asOWLClass());
            }
            if (!result) {
                break;
            }
            timer.checkTime();
        }
        return result;
    }

    public boolean isEntailed(@Nonnull OWLSubClassOfAxiom axiom) {
        final Timer timer = new Timer(this.getTimeOut());
        final OWLClassExpression superClass = axiom.getSuperClass();
        final OWLClassExpression subClass = axiom.getSubClass();

        /* 
            Check if the entities in thr expression there are already in the 
            classification graph.
            If not, then this add to the DAG
         */
//        checkEntityInClassificationGraph(superClass);
//        checkEntityInClassificationGraph(subClass);
        ORNODE treesC, treesD, disjunctOfC;
        /* SubClass Expression - C - Business Policy */
        if (this.fullTreeCache && this.simpleTreeCache) {
            treesC = fullSubClassConceptCache.computeIfAbsent(subClass, k -> normalizeSatisfiabilityCache(subClass));
        } else if (this.fullTreeCache) {
            treesC = fullSubClassConceptCache.computeIfAbsent(subClass, k -> normalizeSatisfiability(subClass));
        } else if (this.simpleTreeCache) {
            treesC = normalizeSatisfiabilityCache(subClass);
        } else {
            treesC = normalizeSatisfiability(subClass);
        }
        /* SuperClass Expression - D - Consent Policy */
        if (this.normalizeSuperClassConcept) {
            if (this.fullTreeCache && this.simpleTreeCache) {
                treesD = fullSuperClassConceptCache.computeIfAbsent(superClass, k -> normalizeSatisfiabilityCache(superClass));
            } else if (this.fullTreeCache) {
                treesD = fullSuperClassConceptCache.computeIfAbsent(superClass, k -> normalizeSatisfiability(superClass));
            } else if (this.simpleTreeCache) {
                treesD = normalizeSatisfiabilityCache(superClass);
            } else {
                treesD = normalizeSatisfiability(superClass);
            }
        } else {
            if (this.fullTreeCache) {
                treesD = fullSuperClassConceptCache.computeIfAbsent(superClass, k -> normalizeUnion(buildTree(superClass)));
            } else {
                treesD = normalizeUnion(buildTree(superClass));
            }
        }
        /* Interval Safety */
        if (this.fullIntervalSafeCache) {
            /* Normalize each subTree of C - C can have a disjuntion, so C'll be a group of trees */
            disjunctOfC = fullConceptlIntervalSafeCache.get(subClass, superClass);
            if (disjunctOfC == null) {
                if (this.simpleIntervalSafeCache) {
                    disjunctOfC = normalizeIntervalSafetyCache(treesC, treesD);
                } else {
                    disjunctOfC = normalizeIntervalSafety(treesC, treesD);
                }
                fullConceptlIntervalSafeCache.put(subClass, superClass, disjunctOfC);
            }
        } else {
            if (this.simpleIntervalSafeCache) {
                disjunctOfC = normalizeIntervalSafetyCache(treesC, treesD);
            } else {
                disjunctOfC = normalizeIntervalSafety(treesC, treesD);
            }
        }
        treesC = null;
        checkIfInterrupted();
        timer.checkTime();
        return structuralSubsumption(disjunctOfC, treesD);
    }

    /**
     * Check if the specified axiom is entailed respect to current ontology
     *
     * @param axiom Only SubClassOf axiom
     * @return True if it is entailed, false otherwise
     */
    @Override
    public boolean isEntailed(@Nonnull OWLAxiom axiom) {
        AxiomType type = axiom.getAxiomType();
        if (type.equals(AxiomType.SUBCLASS_OF)) { //STS
            return isEntailed((OWLSubClassOfAxiom) axiom);
        } else if (type.equals(AxiomType.DISJOINT_CLASSES)) {
            return isEntailed((OWLDisjointClassesAxiom) axiom);
        } else if (type.equals(AxiomType.FUNCTIONAL_OBJECT_PROPERTY)) {
            return isEntailed((OWLFunctionalObjectPropertyAxiom) axiom);
        } else if (type.equals(AxiomType.FUNCTIONAL_DATA_PROPERTY)) {
            return isEntailed((OWLFunctionalDataPropertyAxiom) axiom);
        } else if (type.equals(AxiomType.OBJECT_PROPERTY_RANGE)) {
            return isEntailed((OWLObjectPropertyRangeAxiom) axiom);
        } else {
            throw new UnsupportedOperationException("Expected to be encoded as one of " + SUPPORTED_AXIOMS.stream().map(x -> x.getName()).collect(Collectors.toList()));
        }
    }

    @Override
    public boolean isEntailed(@Nonnull Set<? extends OWLAxiom> axioms) {
        for (OWLAxiom ax : axioms) {
            if (!isEntailed(ax)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BufferingMode getBufferingMode() {
        return bufferingMode;
    }

    @Override
    public List<OWLOntologyChange> getPendingChanges() {
        return new ArrayList<>(pendingChanges);
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomAdditions() {
        Set<OWLAxiom> added = new HashSet<>(pendingChanges.size());
        for (OWLOntologyChange change : pendingChanges) {
            if (change.isAddAxiom()) {
                added.add(change.getAxiom());
            }
        }
        return added;
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomRemovals() {
        Set<OWLAxiom> removed = new HashSet<>(pendingChanges.size());
        for (OWLOntologyChange change : pendingChanges) {
            if (change.isRemoveAxiom()) {
                removed.add(change.getAxiom());
            }
        }
        return removed;
    }

    /**
     * Disposes of this reasoner. This frees up any resources used by the
     * reasoner and detaches the reasoner as an OWLOntologyChangeListener from
     * the OWLOntologyManager that manages the ontologies contained within the
     * reasoner.
     */
    @Override
    public void dispose() {
        manager.removeOntologyChangeListener(ontologyChangeListener);
        pendingChanges.clear();
        clearCache();
        prepareHierarchy();
    }

    /**
     * Flushes any changes stored in the buffer, which causes the reasoner to
     * take into consideration the changes the current root ontology specified
     * by the changes. If the reasoner buffering mode is
     * BufferingMode.NON_BUFFERING then this method will have no effect.
     */
    @Override
    public void flush() {
        clearCache();
        pendingChanges.clear();
        prepareHierarchy();
    }

    @Override
    public void interrupt() {
        interrupted = true;
    }

    private void checkIfInterrupted() {
        if (interrupted) {
            interrupted = false;
            throw new ReasonerInterruptedException(this.getReasonerName() + " is interrupted!");
        }
    }

    @Override
    public void precomputeInferences(@Nonnull InferenceType... inferenceTypes) {
        Set<InferenceType> requiredInferences = new HashSet<>(Arrays.asList(inferenceTypes));
        if (requiredInferences.contains(InferenceType.CLASS_HIERARCHY)) {
            prepareHierarchy();
        }
    }

    @Override
    public boolean isPrecomputed(@Nonnull InferenceType inferenceType) {
        return PRECOMPUTED_INFERENCE_TYPES.contains(inferenceType);
    }

    @Override
    public Set<InferenceType> getPrecomputableInferenceTypes() {
        return PRECOMPUTED_INFERENCE_TYPES;
    }

    @Override
    public boolean isConsistent() {
        if (this.bufferingMode.equals(BufferingMode.NON_BUFFERING)) {
            this.flush();
        }
        this.reasonerIsConsistent = true;
        return this.reasonerIsConsistent;
    }

    @Override
    public Node<OWLClass> getUnsatisfiableClasses() {
        return this.classHierarchy.getBottomNode();
    }

    @Override
    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        return axiomType.equals(AxiomType.SUBCLASS_OF);
    }

    @Override
    public Node<OWLClass> getTopClassNode() {
        return this.classHierarchy.getTopNode();
    }

    @Override
    public Node<OWLClass> getBottomClassNode() {
        return this.classHierarchy.getBottomNode();
    }

    @Override
    public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce) {
        if (!ce.isAnonymous()) {
            return classHierarchy.getEquivalentEntity(ce.asOWLClass());
        } else {
            return new OWLClassNode();
        }
    }

    @Override
    public NodeSet<OWLClass> getDisjointClasses(@Nonnull OWLClassExpression ce) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            ns.addAllNodes(
                    classHierarchy.getDisjunctNodes(ce.asOWLClass(), false, new HashSet<>())
                            .stream()
                            .map(x -> new OWLClassNode(x))
            );
        }
        return ns;
    }

    @Override
    public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        //Per estensioni future la gerarchia già è rappresentata: return objectPropertyHierarchy.getTopNode();
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        //Per estensioni future la gerarchia già è rappresentata: return objectPropertyHierarchy.getBottomNode();
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(OWLObjectPropertyExpression pe, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(OWLObjectPropertyExpression pe, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getInverseObjectProperties(OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyDomains(OWLObjectPropertyExpression pe, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyRanges(OWLObjectPropertyExpression pe, boolean direct) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        for (EntityIntersectionNode<OWLClass> entityIntersection : objectPropertyHierarchy.getPropertyRange(pe, direct, new HashSet<>())) {
            if (entityIntersection.getSize() <= 1) {
                for (OWLClass clazz : entityIntersection) {
                    ns.addNode(classHierarchy.getEquivalentEntity(clazz));
                }
            }
        }
        if (!direct || ns.isEmpty()) {
            ns.addNode(classHierarchy.getTopNode());
        }
        return ns;
    }

    @Override
    public Node<OWLDataProperty> getTopDataPropertyNode() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Node<OWLDataProperty> getBottomDataPropertyNode() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLDataProperty> getDisjointDataProperties(OWLDataPropertyExpression pe) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce, boolean direct) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(OWLNamedIndividual ind, OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind, OWLDataProperty pe) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(OWLNamedIndividual ind) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public long getTimeOut() {
        return configuration.getTimeOut();
    }

    @Override
    public FreshEntityPolicy getFreshEntityPolicy() {
        return configuration.getFreshEntityPolicy();
    }

    @Override
    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        throw new UnsupportedOperationException("Not supported.");
    }

    private void checkEntityInClassificationGraph(OWLDataProperty entity) {
        this.dataPropertyHierarchy.checkEntityInClassification(entity);
    }

    private void checkEntityInClassificationGraph(OWLObjectProperty entity) {
        this.objectPropertyHierarchy.checkEntityInClassification(entity);
    }

    private void checkEntityInClassificationGraph(OWLClassExpression entity) {
        for (OWLClassExpression conjunct : entity.asConjunctSet()) {
            switch (conjunct.getClassExpressionType()) {
                case OWL_CLASS:
                    this.classHierarchy.checkEntityInClassification((OWLClass) conjunct);
                    break;
                case DATA_SOME_VALUES_FROM:
                    OWLDataSomeValuesFrom dataConstraint = (OWLDataSomeValuesFrom) conjunct;
                    OWLDataProperty p = dataConstraint.getProperty().asOWLDataProperty();     //data constraint
                    this.dataPropertyHierarchy.checkEntityInClassification(p);
                    break;
                case OBJECT_SOME_VALUES_FROM:
                    OWLObjectSomeValuesFrom existential = (OWLObjectSomeValuesFrom) conjunct;
                    OWLObjectProperty role = existential.getProperty().asOWLObjectProperty();     //object property
                    this.objectPropertyHierarchy.checkEntityInClassification(role);
                    checkEntityInClassificationGraph(existential.getFiller());
                    break;
                case OBJECT_UNION_OF:
                    for (OWLClassExpression disjunct : conjunct.asDisjunctSet()) {
                        checkEntityInClassificationGraph(disjunct);
                    }
                    break;
                default:
                    throw new IllegalPolicyLogicExpressionException("Type found: " + conjunct.getNNF());
            }
        }
    }

    /**
     * Listener about changes in the ontology. For each change, check and save
     * it in a list, so we can elaborate it later.
     */
    protected class RawOntologyChangeListener implements OWLOntologyChangeListener {

        @Override
        public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {
            for (OWLOntologyChange change : changes) {
                if (!(change instanceof AnnotationChange
                        || change instanceof RemoveOntologyAnnotation
                        || change instanceof AddOntologyAnnotation)) {
                    pendingChanges.add(change);
                }
            }
            if (bufferingMode.equals(BufferingMode.NON_BUFFERING)) {
                flush();
            }
        }
    }

    /**
     * Generic class that rapresent the hierarchy of a type T
     *
     * @param <T>
     */
    private abstract class HierarchyGraph<T extends OWLObject> {

        protected final OntologyReadable<T> entityInHierarchyReadable;
        private final Set<NodeHierarchy<T>> directChildrenOfTopNode = new HashSet<>();
        private final Set<NodeHierarchy<T>> directParentsOfBottomNode = new HashSet<>();
        private final Map<T, NodeHierarchy<T>> mapHierarchy = new HashMap<>();
        private final T topEntity;
        private final T bottomEntity;
        @Nullable
        protected NodeHierarchy<T> topNode;
        @Nullable
        protected NodeHierarchy<T> bottomNode;

        HierarchyGraph(T topEntity, T bottomEntity, OntologyReadable<T> entityInHierarchyReadable) {
            this.topEntity = topEntity;
            this.bottomEntity = bottomEntity;
            this.entityInHierarchyReadable = entityInHierarchyReadable;
        }

        protected void clearHierarchy() {
            this.directChildrenOfTopNode.clear();
            this.directParentsOfBottomNode.clear();
            this.mapHierarchy.clear();
            this.topNode = null;
            this.bottomNode = null;
        }

        protected int getSize() {
            return this.mapHierarchy.size();
        }

        protected boolean containsEntity(T e) {
            return mapHierarchy.containsKey(e);
        }

        protected NodeHierarchy<T> getTopNodeHierarchy() {
            return this.topNode;
        }

        protected NodeHierarchy<T> getBottomNodeHierarchy() {
            return this.bottomNode;
        }

        public Node<T> getTopNode() {
            if (topNode != null) {
                return topNode.node;
            }
            return null;
        }

        public Node<T> getBottomNode() {
            if (bottomNode != null) {
                return bottomNode.node;
            }
            return null;
        }

        public Set<T> getParentNodes(T child) {
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(child);
            if (nodeHierarchy == null || !nodeHierarchy.hasParentsNodes()) {
                return Collections.emptySet();
            }
            Set<T> ns = new HashSet<>();
            for (NodeHierarchy<T> parentNode : nodeHierarchy.getParentsNodes()) {
                parentNode.getValue()
                        .entities()
                        .forEach(parentEntity -> ns.add(parentEntity));
            }
            return ns;
        }

        public NodeSet<T> getParentNodes(T child, boolean direct, DefaultNodeSet<T> ns) {
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(child);
            if (nodeHierarchy == null) {
                return ns;
            } else {
                Node<T> node = nodeHierarchy.getValue();
                if (node == null/* || node.isBottomNode() */) {
                    return ns;
                }
                Set<Node<T>> directParentsNodes = new HashSet<>();
                Set<T> directParentsEntity = new HashSet<>();
                if (nodeHierarchy.hasParentsNodes()) {
                    for (NodeHierarchy<T> parentNode : nodeHierarchy.getParentsNodes()) {
                        directParentsNodes.add(parentNode.getValue());
                        if (!direct) {
                            for (T equivParent : parentNode.getValue()) {
                                directParentsEntity.add(equivParent);
                            }
                        }
                    }
                }
                if (node.isBottomNode()) {
                    for (NodeHierarchy<T> parentNode : directParentsOfBottomNode) {
                        directParentsNodes.add(parentNode.getValue());
                        if (!direct) {
                            for (T equivParent : parentNode.getValue()) {
                                directParentsEntity.add(equivParent);
                            }
                        }
                    }
                }
                for (Node<T> parentNode : directParentsNodes) {
                    ns.addNode(parentNode);
                }
                if (!direct) {
                    for (T parent : directParentsEntity) {
                        getParentNodes(parent, direct, ns);
                    }
                }
            }
            return ns;
        }

        public Set<T> getChildNodes(T parent) {
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(parent);
            if (nodeHierarchy == null || !nodeHierarchy.hasChildrenNodes()) {
                return Collections.emptySet();
            }
            Set<T> ns = new HashSet<>();
            for (NodeHierarchy<T> child : nodeHierarchy.getChildrenNodes()) {
                child.getValue()
                        .entities()
                        .forEach(childEntity -> ns.add(childEntity));
            }
            return ns;
        }

        public NodeSet<T> getChildNodes(T parent, boolean direct, DefaultNodeSet<T> ns) {
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(parent);
            if (nodeHierarchy == null) {
                return ns;
            } else {
                Node<T> node = nodeHierarchy.getValue();
                if (node == null || node.isBottomNode()) {
                    return ns;
                }
                Set<Node<T>> directChildrenNodes = new HashSet<>();
                Set<T> directChildrenEntity = new HashSet<>();
                if (nodeHierarchy.hasChildrenNodes()) {
                    for (NodeHierarchy<T> child : nodeHierarchy.getChildrenNodes()) {
                        directChildrenNodes.add(child.getValue());
                        if (!direct) {
                            for (T equivChild : child.getValue()) {
                                directChildrenEntity.add(equivChild);
                            }
                        }
                    }
                }
                if (node.isTopNode()) {
                    for (NodeHierarchy<T> child : directChildrenOfTopNode) {
                        directChildrenNodes.add(child.getValue());
                        if (!direct) {
                            for (T equivChild : child.getValue()) {
                                directChildrenEntity.add(equivChild);
                            }
                        }
                    }
                }
                for (Node<T> childNode : directChildrenNodes) {
                    ns.addNode(childNode);
                }
                if (!direct) {
                    for (T child : directChildrenEntity) {
                        getChildNodes(child, direct, ns);
                    }
                }
            }
            return ns;
        }

        /**
         * Retrieve only LOCAL disjuncts in node
         *
         * @param entity
         * @return
         */
        public Set<T> getDisjunctNodes(T entity) {
            Set<T> ns = new HashSet<>();
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(entity);
            if (nodeHierarchy == null) {
                return ns;
            }
            if (nodeHierarchy.hasDisjuncts()) {
                ns.addAll(nodeHierarchy.getDisjuncts());
            }
            if (ns.isEmpty()) {
                ns.add(bottomEntity);
            }
            return ns;
        }

        /**
         * Retrieve ALL disjuncts in the hierarchy
         *
         * @param entity
         * @param direct
         * @param ns
         * @return
         */
        public Set<T> getDisjunctNodes(T entity, boolean direct, Set<T> ns) {
            if (entity.isBottomEntity()) {
                ns.addAll(mapHierarchy.keySet());
            } else {
                NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(entity);
                if (nodeHierarchy != null) {
                    Deque<T> disjointsClasses = new LinkedList<>();
                    Deque<NodeHierarchy<T>> queueUp = new LinkedList<>();
                    queueUp.add(nodeHierarchy);
                    while (!queueUp.isEmpty()) {
                        NodeHierarchy<T> node = queueUp.pollFirst();
                        if (node.hasDisjuncts()) {
                            disjointsClasses.addAll(node.getDisjuncts());
                            ns.addAll(node.getDisjuncts());
                        }
                        if (node.hasParentsNodes()) {
                            queueUp.addAll(node.getParentsNodes());
                        }
                    }
                    if (!direct) {
                        for (T djn : disjointsClasses) {
                            NodeHierarchy<T> node = mapHierarchy.get(djn);
                            if (node != null && node.hasChildrenNodes()) {
                                Deque<NodeHierarchy<T>> queueDown = new LinkedList<>();
                                queueDown.add(node);
                                while (!queueDown.isEmpty()) {
                                    NodeHierarchy<T> el = queueDown.pollFirst();
                                    el.getValue().forEach(x -> ns.add(x));
                                    if (el.hasChildrenNodes()) {
                                        queueDown.addAll(el.getChildrenNodes());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (ns.isEmpty()) {
                ns.add(bottomEntity);
            }
            return ns;
        }

        public boolean isFunctional(T el) {
            PropertyNodeHierarchy<T, OWLClass> node = (PropertyNodeHierarchy) getNodeFromHierarchy(el);
            if (node != null) {
                return node.isFunctional();
            }
            return false;
        }

        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(T entity) {
            PropertyNodeHierarchy<T, OWLClass> nodeHierarchy = (PropertyNodeHierarchy) mapHierarchy.get(entity);
            if (nodeHierarchy != null && nodeHierarchy.hasRanges()) {
                return new HashSet<>(nodeHierarchy.getRanges());
            }
            return Collections.emptySet();
        }

        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(T entity, boolean direct, Set<EntityIntersectionNode<OWLClass>> ns) {
            PropertyNodeHierarchy<T, OWLClass> nodeHierarchy = (PropertyNodeHierarchy) mapHierarchy.get(entity);
            if (nodeHierarchy != null) {
                if (nodeHierarchy.hasRanges()) {
                    ns.addAll(nodeHierarchy.getRanges());
                }
                if (!direct && nodeHierarchy.hasParentsNodes()) {
                    for (NodeHierarchy<T> parent : nodeHierarchy.getParentsNodes()) {
                        for (T ent : parent.getValue().entities().collect(Collectors.toList())) {
                            ns.addAll(getPropertyRange(ent, direct, ns));
                        }
                    }
                }
            }
            return ns;
        }

        protected Set<OWLClass> getPropertyRangeClasses(T entity) {
            Set<OWLClass> ranges = new HashSet<>();
            PropertyNodeHierarchy<T, OWLClass> nodeHierarchy = (PropertyNodeHierarchy) mapHierarchy.get(entity);
            if (nodeHierarchy != null && nodeHierarchy.hasRanges()) {
                Set<OWLClass> classes = nodeHierarchy.getRangesClassOnly();
                if (classes != null) {
                    ranges.addAll(classes);
                }
            }
            return ranges;
        }

        protected Set<OWLClass> getPropertyRangeClasses(T entity, boolean direct, Set<OWLClass> ns) {
            PropertyNodeHierarchy<T, OWLClass> nodeHierarchy = (PropertyNodeHierarchy) mapHierarchy.get(entity);
            if (nodeHierarchy != null) {
                if (nodeHierarchy.hasRanges()) {
                    Set<OWLClass> classes = nodeHierarchy.getRangesClassOnly();
                    if (classes != null) {
                        ns.addAll(classes);
                    }
                }
                if (!direct && nodeHierarchy.hasParentsNodes()) {
                    for (NodeHierarchy<T> parent : nodeHierarchy.getParentsNodes()) {
                        for (T ent : parent.getValue().entities().collect(Collectors.toList())) {
                            ns.addAll(getPropertyRangeClasses(ent, direct, ns));
                        }
                    }
                }
            }
            return ns;
        }

        protected void compute() {
            clearHierarchy();
            topNode = mapHierarchy.computeIfAbsent(topEntity,
                    e -> getNodeHierarchy(e)
            );
            bottomNode = mapHierarchy.computeIfAbsent(bottomEntity,
                    e -> getNodeHierarchy(e)
            );
            for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
                for (T entity : getEntityInSignature(ont).collect(Collectors.toList())) {
                    checkIfInterrupted();
                    NodeHierarchy<T> node = mapHierarchy.computeIfAbsent(entity,
                            e -> getNodeHierarchy(e)
                    );
                    Set<T> disjoints = entityInHierarchyReadable.getDisjointsInRawHierarchy(entity);
                    if (!disjoints.isEmpty()) {
                        node.addDisjuncts(disjoints);
                    }
                    Set<T> parents = entityInHierarchyReadable.getParentsInRawHierarchy(entity);
                    Set<T> children = entityInHierarchyReadable.getChildrenInRawHierarchy(entity);
                    if (!entity.equals(topEntity)) {
                        if (parents.isEmpty() || parents.contains(topEntity)) {
                            directChildrenOfTopNode.add(node);
                        } else {
                            for (T parent : parents) {
                                NodeHierarchy<T> parentNode = mapHierarchy.get(parent);
                                if (parentNode == null) {
                                    parentNode = node.addParent(parent);
                                    mapHierarchy.put(parent, parentNode);
                                } else {
                                    node.addParentNode(parentNode);
                                }
                            }
                        }
                    }
                    if (!entity.equals(bottomEntity)) {
                        if (children.isEmpty() || children.contains(bottomEntity)) {
                            directParentsOfBottomNode.add(node);
                        } else {
                            for (T child : children) {
                                NodeHierarchy<T> childNode = mapHierarchy.get(child);
                                if (childNode == null) {
                                    childNode = node.addChild(child);
                                    mapHierarchy.put(child, childNode);
                                } else {
                                    node.addChildNode(childNode);
                                }
                            }
                        }
                    }
                }
            }
            if (!directChildrenOfTopNode.isEmpty()) {
                topNode.addChildrenNode(directChildrenOfTopNode);
            }
            if (!directParentsOfBottomNode.isEmpty()) {
                bottomNode.addParentsNode(directParentsOfBottomNode);
            }
            checkCycles();
        }

        protected abstract Stream<T> getEntityInSignature(OWLOntology o);

        protected abstract NodeHierarchy<T> getNodeHierarchy(T e);

        protected abstract NodeHierarchy<T> getNodeHierarchy(Collection<T> e);

        protected void checkEntityInClassification(T entity) {
            if (!mapHierarchy.containsKey(entity)) {
                NodeHierarchy<T> node = mapHierarchy.computeIfAbsent(entity, e -> getNodeHierarchy(e));
                directChildrenOfTopNode.add(node);
                topNode.addChildNode(node);
                directParentsOfBottomNode.add(node);
                bottomNode.addParentNode(node);
            }
        }

        protected NodeHierarchy<T> getNodeFromHierarchy(T e) {
            return this.mapHierarchy.get(e);
        }

        protected Node<T> getEquivalentEntity(T e) {
            NodeHierarchy<T> node = this.mapHierarchy.get(e);
            if (node != null) {
                return node.getValue();
            }
            return null;
        }

        private void checkCycles() {
            Set<Set<NodeHierarchy<T>>> setSCCs = new HashSet<>();
            Map<NodeHierarchy<T>, Integer> processedNodes = new HashMap<>();
            for (NodeHierarchy<T> node : this.mapHierarchy.values().stream().collect(Collectors.toCollection(HashSet::new))) {
                if (!processedNodes.containsKey(node)) {
                    tarjanSCC(node, 0, new LinkedList<>(), new HashSet<>(), setSCCs, new HashMap<>(), processedNodes);
                }
            }
            for (Set<NodeHierarchy<T>> scc : setSCCs) {
                checkIfInterrupted();
                Set<T> allEntities = new HashSet<>();
                for (NodeHierarchy<T> node : scc) {
                    allEntities.addAll(node.getValue().entities().collect(Collectors.toList()));
                }
                NodeHierarchy<T> mergedNode = getNodeHierarchy(allEntities);
                for (T entity : allEntities) {
                    this.mapHierarchy.put(entity, mergedNode);
                }
                for (NodeHierarchy<T> node : scc) {
                    if (node.hasParentsNodes()) {
                        for (NodeHierarchy parent : new HashSet<>(node.getParentsNodes())) {
                            if (!scc.contains(parent)) {
                                parent.addChildNode(mergedNode);
                            }
                            parent.removeChildNode(node);
                        }
                    }
                    if (node.hasChildrenNodes()) {
                        for (NodeHierarchy child : new HashSet<>(node.getChildrenNodes())) {
                            if (!scc.contains(child)) {
                                child.addParentNode(mergedNode);
                            }
                            child.removeParentNode(node);
                        }
                    }
                    if (node.hasDisjuncts()) {
                        for (T entity : node.getDisjuncts()) {
                            mergedNode.addDisjunct(entity);
                            NodeHierarchy<T> nodeDisjunct = this.mapHierarchy.get(entity);
                            if (nodeDisjunct != null) {
                                nodeDisjunct.addDisjuncts(allEntities);
                            }
                        }
                    }
                }
                if (!mergedNode.hasParentsNodes()) {
                    topNode.addChildNode(mergedNode);
                }
                if (!mergedNode.hasChildrenNodes()) {
                    bottomNode.addParentNode(mergedNode);
                }
            }
        }

        private void tarjanSCC(NodeHierarchy<T> node,
                Integer index,
                Deque<NodeHierarchy<T>> queue,
                Set<NodeHierarchy<T>> queueSet,
                Set<Set<NodeHierarchy<T>>> setSCCs,
                Map<NodeHierarchy<T>, Integer> lowLinkNodes,
                Map<NodeHierarchy<T>, Integer> indexNodes) {
            indexNodes.put(node, index);
            lowLinkNodes.put(node, index);
            index++;
            queue.push(node);
            queueSet.add(node);
            if (node.hasParentsNodes()) {
                for (NodeHierarchy<T> parent : node.getParentsNodes()) {
                    if (!indexNodes.containsKey(parent)) {
                        tarjanSCC(parent, index, queue, queueSet, setSCCs, lowLinkNodes, indexNodes);
                        Integer min = Math.min(lowLinkNodes.get(node), lowLinkNodes.get(parent));
                        lowLinkNodes.put(node, min);
                    } else if (queueSet.contains(parent)) {
                        Integer min = Math.min(lowLinkNodes.get(node), lowLinkNodes.get(parent));
                        lowLinkNodes.put(node, min);
                    }
                }
            }
            if (indexNodes.get(node).equals(lowLinkNodes.get(node))) { //Set of scc, node is the root of the scc
                Set<NodeHierarchy<T>> scc = new HashSet<>();
                NodeHierarchy<T> el = null;
                do {
                    el = queue.pop();
                    queueSet.remove(el);
                    scc.add(el);
                } while (!el.equals(node));
                if (scc.size() > 1) {
                    setSCCs.add(scc);
                }
            }
            checkIfInterrupted();
        }
    }

    /**
     * Class that rapresent the hierarchy of OWLClass in the imports closure's
     * signature.
     */
    private class ClassHierarchyInOntology extends HierarchyGraph<OWLClass> {

        ClassHierarchyInOntology(OWLClass topEntity, OWLClass bottomEntity, OntologyReadable<OWLClass> classInHierarchyReadable) {
            super(topEntity, bottomEntity, classInHierarchyReadable);
        }

        @Override
        public boolean isFunctional(OWLClass el) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLClass entity) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLClass entity, boolean direct, Set<EntityIntersectionNode<OWLClass>> ns) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Stream<OWLClass> getEntityInSignature(OWLOntology o) {
            return o.classesInSignature();
        }

        @Override
        protected NodeHierarchy<OWLClass> getNodeHierarchy(OWLClass e) {
            return new ClassNodeHierarchy(e);
        }

        @Override
        protected NodeHierarchy<OWLClass> getNodeHierarchy(Collection<OWLClass> e) {
            return new ClassNodeHierarchy(e);
        }
    }

    /**
     * Class that rapresent the hierarchy of OWLDataProperty in the imports
     * closure's signature.
     */
    private class DataPropertyHierarchyInOntology extends HierarchyGraph<OWLDataProperty> {

        DataPropertyHierarchyInOntology(OWLDataProperty topEntity, OWLDataProperty bottomEntity, OntologyReadable<OWLDataProperty> dataPropertyInHierarchyReadable) {
            super(topEntity, bottomEntity, dataPropertyInHierarchyReadable);
        }

        @Override
        protected void compute() {
            clearHierarchy();
            super.compute();
            NodeHierarchy<OWLDataProperty> top = getTopNodeHierarchy();
            if (top != null) {
                Deque<NodeHierarchy<OWLDataProperty>> queueDown = new LinkedList<>();
                queueDown.add(top);
                while (!queueDown.isEmpty()) {
                    PropertyNodeHierarchy<OWLDataProperty, OWLClass> node = (PropertyNodeHierarchy) queueDown.pollFirst();
                    for (OWLDataProperty el : node.getValue()) {
                        if (entityInHierarchyReadable.isFunctional(el)) {
                            node.setFunctional(true);
                        }
                    }
                    if (node.hasChildrenNodes()) {
                        queueDown.addAll(node.getChildrenNodes());
                    }
                }
            }
        }

        @Override
        protected Stream<OWLDataProperty> getEntityInSignature(OWLOntology o) {
            return o.dataPropertiesInSignature();
        }

        @Override
        protected NodeHierarchy<OWLDataProperty> getNodeHierarchy(OWLDataProperty e) {
            return new DataPropertyNodeHierarchy(e);
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLDataProperty entity) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLDataProperty entity, boolean direct, Set<EntityIntersectionNode<OWLClass>> ns) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected NodeHierarchy<OWLDataProperty> getNodeHierarchy(Collection<OWLDataProperty> e) {
            return new DataPropertyNodeHierarchy(e);
        }
    }

    /**
     * Class that rapresent the hierarchy of OWLObjectPropertyExpression in the
     * imports closure's signature.
     */
    private class ObjectPropertyHierarchyInOntology extends HierarchyGraph<OWLObjectPropertyExpression> {

        ObjectPropertyHierarchyInOntology(OWLObjectPropertyExpression topEntity, OWLObjectPropertyExpression bottomEntity, OntologyReadable<OWLObjectPropertyExpression> objectPropertyInHierarchyReadable) {
            super(topEntity, bottomEntity, objectPropertyInHierarchyReadable);
        }

        @Override
        protected void compute() {
            clearHierarchy();
            super.compute();
            NodeHierarchy<OWLObjectPropertyExpression> top = getTopNodeHierarchy();
            if (top != null) {
                Deque<NodeHierarchy<OWLObjectPropertyExpression>> queueDown = new LinkedList<>();
                queueDown.add(top);
                while (!queueDown.isEmpty()) {
                    PropertyNodeHierarchy<OWLObjectPropertyExpression, OWLClass> node = (PropertyNodeHierarchy) queueDown.pollFirst();
                    for (OWLObjectPropertyExpression el : node.getValue()) {
                        if (entityInHierarchyReadable.isFunctional(el)) {
                            node.setFunctional(true);
                        }
                        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
                            List<OWLClassExpression> ranges
                                    = ont.objectPropertyRangeAxioms(el)
                                            .map(x -> x.getRange())
                                            .filter(
                                                    x -> x.isOWLClass()
                                                    || x instanceof OWLObjectIntersectionOf
                                                    || x instanceof OWLObjectUnionOf)
                                            .collect(Collectors.toCollection(LinkedList::new));
                            OWLObjectIntersectionOf intersection = entityInHierarchyReadable.getDataFactory().getOWLObjectIntersectionOf(ranges);
                            for (OWLClassExpression disjunct : getDNF(intersection).asDisjunctSet()) {
                                EntityIntersectionNode<OWLClass> nodeRange = new EntityIntersectionNode<>();
                                for (OWLClass range : disjunct.asConjunctSet()
                                        .stream()
                                        .filter(x -> x.isOWLClass())
                                        .map(x -> x.asOWLClass())
                                        .collect(Collectors.toCollection(LinkedList::new))) {
                                    nodeRange.add(range);
                                }
                                node.addRange(nodeRange);
                            }
                            ont.objectPropertyRangeAxioms(el)
                                    .map(x -> x.getRange())
                                    .filter(x -> x.isOWLClass())
                                    .forEach(clazz -> {
                                        node.addRangeClassOnly(clazz.asOWLClass());
                                    });
                        }
                    }
                    if (node.hasChildrenNodes()) {
                        queueDown.addAll(node.getChildrenNodes());
                    }
                }
            }
        }

        private OWLClassExpression getDNF(@Nonnull OWLClassExpression ce) {
            Deque<OWLClassExpression> results = new LinkedList<>();
            Deque<OWLClassExpression> conjuncts = new LinkedList<>();
            Deque<OWLClassExpression> unionOfQueue = new LinkedList<>();
            for (OWLClassExpression conjunct : ce.asConjunctSet()) {
                if (conjunct instanceof OWLObjectUnionOf) {
                    unionOfQueue.add(conjunct);
                } else if (conjunct.isOWLClass()) {
                    conjuncts.add(conjunct);
                }
            }
            getDNF(unionOfQueue, conjuncts, results);
            if (results.size() == 1) {
                return results.getFirst();
            }
            return entityInHierarchyReadable.getDataFactory().getOWLObjectUnionOf(results);
        }

        private void getDNF(@Nonnull Deque<OWLClassExpression> unionOfQueue, Deque<OWLClassExpression> conjuncts, Deque<OWLClassExpression> results) {
            if (!unionOfQueue.isEmpty()) {
                OWLClassExpression unionOf = unionOfQueue.pollFirst();
                for (OWLClassExpression disjunct : unionOf.asDisjunctSet()) {
                    conjuncts.addFirst(disjunct);
                    getDNF(unionOfQueue, conjuncts, results);
                    conjuncts.pollFirst();
                }
                unionOfQueue.addFirst(unionOf);
            } else {
                OWLObjectIntersectionOf intersection = entityInHierarchyReadable.getDataFactory().getOWLObjectIntersectionOf(conjuncts);
                results.add(intersection);
            }
        }

        @Override
        protected Stream<OWLObjectPropertyExpression> getEntityInSignature(OWLOntology o) {
            return o
                    .objectPropertiesInSignature()
                    .map(x -> x.asObjectPropertyExpression());
        }

        @Override
        protected NodeHierarchy<OWLObjectPropertyExpression> getNodeHierarchy(OWLObjectPropertyExpression e) {
            return new ObjectPropertyNodeHierarchy(e);
        }

        @Override
        protected NodeHierarchy<OWLObjectPropertyExpression> getNodeHierarchy(Collection<OWLObjectPropertyExpression> e) {
            return new ObjectPropertyNodeHierarchy(e);
        }
    }

    private class Timer {

        private final long start;
        private final long max;

        Timer(long max) {
            this.start = System.currentTimeMillis();
            this.max = max;
        }

        void checkTime() {
            long current = System.currentTimeMillis();
            if (this.max < (current - this.start)) {
                throw new TimeOutException("Timeout occurred while reasoning! Time: " + (current - this.start) + " ms");
            }
        }
    }
}
