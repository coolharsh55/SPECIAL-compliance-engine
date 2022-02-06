package special.model;

import java.io.Serializable;
import java.util.*;
import java.util.stream.*;
import javax.annotation.*;
import org.semanticweb.owlapi.model.*;

/**
 * AND Node of a Policy Logic Tree
 *
 * @author Luca Ioffredo
 */
public class ANDNODE implements Serializable {

    private static final short SIZE_CLASS_DATA_STRUCTURE = 32;
    private static final short SIZE_DATA_PROPERTY_DATA_STRUCTURE = 8;
    private static final short SIZE_OBJECT_PROPERTY_DATA_STRUCTURE = 32;
    private static final float LOAD_FACTOR_DATA_STRUCTURE = 0.75f;
    private static final Set EMPTY_SET = Collections.emptySet();
    private static final Map EMPTY_MAP = Collections.emptyMap();
    private static final List EMPTY_LIST = Collections.emptyList();
    private static final Deque EMPTY_DEQUE = new ArrayDeque(0);

    private Set<OWLClass> conceptNames = null; //Concept Names
    private Map<OWLDataProperty, List<IntRange>> dataConstraints = null; //Data Contraint
    private Map<OWLObjectProperty, List<ANDNODE>> children = null;
    private Deque<ORNODE> orNodes = null;

    public ANDNODE() {
        this.orNodes = new ArrayDeque<>();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ANDNODE)) {
            return false;
        }
        boolean cn = false;
        boolean dc = false;
        boolean ex = false;
        boolean or = false;
        ANDNODE cc = (ANDNODE) o;
        cn = (cc.conceptNames == null
                ? this.conceptNames == null
                : (this.conceptNames != null && cc.conceptNames.size() == this.conceptNames.size() && cc.conceptNames.containsAll(this.conceptNames)));
        if (!cn) {
            return false;
        }
        dc = (cc.dataConstraints == null || cc.dataConstraints.isEmpty()
                ? this.dataConstraints == null || this.dataConstraints.isEmpty()
                : (this.dataConstraints != null && cc.dataConstraints.size() == this.dataConstraints.size()
                && cc.dataConstraints.entrySet().stream()
                        .allMatch(entry -> {
                            List<IntRange> tmp = this.dataConstraints.get(entry.getKey());
                            return tmp != null && entry.getValue().containsAll(tmp);
                        })));
        if (!dc) {
            return false;
        }
        ex = areEqualChildren(cc.children, this.children);
        if (!ex) {
            return false;
        }
        or = (cc.orNodes == null
                ? this.orNodes == null
                : (this.orNodes != null && cc.orNodes.size() == this.orNodes.size() && cc.orNodes.containsAll(this.orNodes)));
        return cn && dc && ex && or;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        if (this.conceptNames != null) {
            for (OWLClass clazz : this.conceptNames) {
                hashCode += clazz.hashCode();
            }
        }
        if (this.orNodes != null) {
            for (ORNODE ornode : this.orNodes) {
                hashCode += ornode.hashCode();
            }
        }
        if (this.dataConstraints != null) {
            int hashKeys = 1, hashValues = 1;
            for (Map.Entry<OWLDataProperty, List<IntRange>> entry : this.dataConstraints.entrySet()) {
                hashKeys = 31 * hashKeys + entry.getKey().hashCode();
                for (IntRange interval : entry.getValue()) {
                    hashValues = 13 * hashValues + interval.hashCode();
                }
            }
            hashCode = hashCode + hashValues + hashKeys;
        }
        if (this.children != null) {
            int hashKeys = 1, hashValues = 1;
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : this.children.entrySet()) {
                hashKeys = 31 * hashKeys + entry.getKey().hashCode();
                for (ANDNODE node : entry.getValue()) {
                    hashValues = 13 * hashValues + node.hashCode();
                }
            }
            hashCode = hashCode + hashValues + hashKeys;
        }
        return 17 * 3 + hashCode;
//        return 17 * 3 + Objects.hash(this.conceptNames, this.dataConstraints, this.children, this.orNodes);
    }

    private boolean areEqualChildren(Map<OWLObjectProperty, List<ANDNODE>> map1, Map<OWLObjectProperty, List<ANDNODE>> map2) {
        boolean result = false;
        if (map1 == null || map1.isEmpty()) {
            return map2 == null || map2.isEmpty();
        } else if (map2 != null && !map2.isEmpty() && map1.size() == map2.size()) {
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : map2.entrySet()) {
                List<ANDNODE> tmp2 = entry.getValue();
                List<ANDNODE> tmp1 = map1.get(entry.getKey());
                if (tmp1 == null || tmp1.isEmpty()) {
                    result = tmp2 == null || tmp2.isEmpty();
                } else {
                    result = tmp2 != null && tmp1.size() == tmp2.size() && tmp2.containsAll(tmp1);
                }
                if (!result) {
                    break;
                }
            }
        }
        return result;
    }

    private void createORNodesStructure() {
        if (this.orNodes == null) {
            this.orNodes = new ArrayDeque<>();
        }
    }

    private void createConceptNamesStructure() {
        if (this.conceptNames == null) {
            this.conceptNames = new HashSet<>(SIZE_CLASS_DATA_STRUCTURE, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createConceptNamesStructure(@Nonnull Collection<OWLClass> c) {
        if (this.conceptNames == null) {
            this.conceptNames = new HashSet<>(c.size() + 1, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createDataConstraintStructure() {
        if (this.dataConstraints == null) {
            this.dataConstraints = new HashMap<>(SIZE_DATA_PROPERTY_DATA_STRUCTURE, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createChildrenStructure() {
        if (this.children == null) {
            this.children = new HashMap<>(SIZE_OBJECT_PROPERTY_DATA_STRUCTURE, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    /* Concept Names ----------------------------------------------------------------------------------------------------------------------------- */
    /**
     * Check if Concept Name Set (CN) contains the input class
     *
     * @param clazz OWLClassExpression as OWLClass
     * @return true if CN contains the input class, false otherwise
     */
    public boolean containsConceptName(OWLClassExpression clazz) {
        return clazz != null && this.conceptNames != null && clazz.isOWLClass() && this.conceptNames.contains(clazz.asOWLClass());
    }

    /**
     * Add the input class in the Concept Name Set (CN)
     *
     * @param clazz OWLClassExpression as OWLClass
     * @return true if this set did not already contain the specified element
     */
    public boolean addConceptName(OWLClass clazz) {
        if (clazz != null) {
            createConceptNamesStructure();
            return this.conceptNames.add(clazz);
        } else {
            return false;
        }
    }

    /**
     * Add the input set in the Concept Name Set (CN)
     *
     * @param c Collection of OWLClassExpression
     * @return true if this collection changed as a result of the call
     */
    public boolean addConceptName(Collection<OWLClass> c) {
        if (c != null && !c.isEmpty()) {
            createConceptNamesStructure(c);
            return this.conceptNames.addAll(c);
        }
        return false;
    }

    public Set<OWLClass> getConceptNames() {
        if (this.conceptNames == null) {
            return EMPTY_SET;
        }
        return this.conceptNames;
    }

    public Stream<OWLClass> conceptNames() {
        if (this.conceptNames == null) {
            return EMPTY_SET.stream();
        }
        return this.conceptNames.stream();
    }

    public boolean hasConceptNames() {
        return this.conceptNames != null && !this.conceptNames.isEmpty();
    }

    public void clearConceptNames() {
        if (this.conceptNames != null) {
            this.conceptNames.clear();
        }
    }

    /* Data Constraints ----------------------------------------------------------------------------------------------------------------------------- */
    /**
     * Add the interval to the specified IRI
     *
     * @param p OWLDataProperty to add
     * @param interval Array of the interval. Size = 2
     */
    public void addDataProperty(OWLDataProperty p, IntRange interval) {
        if (p != null && interval != null) {
            createDataConstraintStructure();
            List<IntRange> intervals = this.dataConstraints.get(p);
            if (intervals == null) {
                intervals = new LinkedList<>();
                this.dataConstraints.put(p, intervals);
            }
            intervals.add(interval);
        }
    }

    /**
     * Add the intervals's set to the specified IRI
     *
     * @param p OWLDataProperty to add
     * @param intervals Set of intervals. Each interval has size of 2
     */
    public void addDataProperty(OWLDataProperty p, Collection<IntRange> intervals) {
        if (p != null && intervals != null && !intervals.isEmpty()) {
            createDataConstraintStructure();
            List<IntRange> tmp = this.dataConstraints.get(p);
            if (tmp == null) {
                tmp = new LinkedList<>();
                this.dataConstraints.put(p, tmp);
            }
            tmp.addAll(intervals);
        }
    }

    public void makeSingletonDataProperty(OWLDataProperty p, IntRange interval) {
        if (p != null && interval != null) {
            createDataConstraintStructure();
            List<IntRange> tmp = new LinkedList<>();
            tmp.add(interval);
            this.dataConstraints.put(p, tmp);
        }
    }

    /**
     * Check if Data Constraint Set (DC) contains the constraint specified
     *
     * @param <T> Subclass of OWLPropertyExpression as
     * OWLObjectPropertyExpression or OWLDataPropertyExpression
     * @param dataConstraint A subtype of OWLPropertyExpression
     * @return true if DC containts che constraint specified, false otherwise
     */
    public <T extends OWLPropertyExpression> boolean containsDataProperty(T dataConstraint) {
        if (dataConstraint == null) {
            return false;
        }
        OWLDataProperty p = (OWLDataProperty) dataConstraint;
        return this.dataConstraints != null && this.dataConstraints.containsKey(p.getIRI());
    }

    public boolean containsDataProperty(OWLDataProperty property) {
        return this.dataConstraints != null && property != null && this.dataConstraints.containsKey(property);
    }

    public Map<OWLDataProperty, List<IntRange>> getDataProperty() {
        if (this.dataConstraints == null) {
            return EMPTY_MAP;
        }
        return this.dataConstraints;
    }

    public List<IntRange> removeDataProperty(OWLDataProperty property) {
        if (this.dataConstraints == null || property == null) {
            return EMPTY_LIST;
        }
        return this.dataConstraints.remove(property);
    }

    public void clearDataConstraints() {
        if (this.dataConstraints != null) {
            this.dataConstraints.clear();
        }
    }

    public List<IntRange> getDataProperty(OWLDataProperty property) {
        if (this.dataConstraints == null || property == null || !this.dataConstraints.containsKey(property)) {
            return EMPTY_LIST;
        }
        return this.dataConstraints.get(property);
    }

    public Set<Map.Entry<OWLDataProperty, List<IntRange>>> getDataPropertyEntrySet() {
        if (this.dataConstraints == null) {
            return EMPTY_SET;
        }
        return this.dataConstraints.entrySet();
    }

    public Set<OWLDataProperty> getDataPropertyKeySet() {
        if (this.dataConstraints == null) {
            return EMPTY_SET;
        }
        return this.dataConstraints.keySet();
    }

    public Collection<List<IntRange>> getDataPropertyValuesSet() {
        if (this.dataConstraints == null) {
            return EMPTY_SET;
        }
        return this.dataConstraints.values();
    }

    public boolean hasDataProperties() {
        return this.dataConstraints != null && !this.dataConstraints.isEmpty();
    }

    public void removeIfEmpty(OWLDataProperty property) {
        if (property != null && this.dataConstraints != null && this.dataConstraints.containsKey(property) && this.dataConstraints.get(property).isEmpty()) {
            this.dataConstraints.remove(property);
        }
    }

    public boolean existsButIsEmpty(OWLDataProperty property) {
        return property != null && this.dataConstraints != null && this.dataConstraints.containsKey(property) && this.dataConstraints.get(property).isEmpty();
    }

    /* ORnodes - alias Disjuncts -------------------------------------------------------------------------------------------------------------------- */
    public boolean addDisjuncts(Collection<ANDNODE> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        createORNodesStructure();
        return this.orNodes.add(new ORNODE(nodes));
    }

    public boolean hasORNodes() {
        return this.orNodes != null && !this.orNodes.isEmpty();
    }

    public boolean hasORNodesInAllTree() {
        if (hasORNodes()) {
            return true;
        } else {
            if (this.children != null) {
                for (List<ANDNODE> childrenNode : this.children.values()) {
                    for (ANDNODE child : childrenNode) {
                        if (child.hasORNodesInAllTree()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean addORnode(ORNODE node) {
        if (node == null) {
            return false;
        }
        createORNodesStructure();
        return this.orNodes.add(node);
    }

    public boolean addORnodes(Collection<ORNODE> nodes) {
        if (nodes != null && !nodes.isEmpty()) {
            createORNodesStructure();
            return this.orNodes.addAll(nodes);
        }
        return false;
    }

    public Deque<ORNODE> getORNodes() {
        if (this.orNodes == null) {
            return EMPTY_DEQUE;
        }
        return this.orNodes;
    }

    public int getSizeOfORNodes() {
        if (this.orNodes != null) {
            return this.orNodes.size();
        }
        return 0;
    }

    public void clearORNodes() {
        if (this.orNodes != null) {
            this.orNodes.clear();
        }
    }

    /* children - alias Roles or Existential Quantifiers -------------------------------------------------------------------------------------------- */
    /**
     * Add the existential in input to the collection of children
     *
     * @param existential OWLObjectSomeValuesFrom to add
     * @return ConcepTree node child created by the call
     */
    public ANDNODE addChild(@Nonnull OWLObjectSomeValuesFrom existential) {
        createChildrenStructure();
        ANDNODE child = new ANDNODE();
        OWLObjectPropertyExpression p = existential.getProperty();
        OWLObjectProperty property = p.getNamedProperty();
        return this.addChild(property, child);
    }

    /**
     * Add the child node in input to the children set with the IRI of
     * OWLObjectSomeValuesFrom specified
     *
     * @param existential OWLObjectSomeValuesFrom
     * @param child ANDNODE node of the child
     * @return ConcepTree node child created by the call
     */
    public ANDNODE addChild(@Nonnull OWLObjectSomeValuesFrom existential, @Nonnull ANDNODE child) {
        createChildrenStructure();
        OWLObjectPropertyExpression p = existential.getProperty();
        OWLObjectProperty property = p.getNamedProperty();
        return this.addChild(property, child);
    }

    /**
     * Add the child node in input to the children set with the IRI specified
     *
     * @param property Role to add
     * @param child ANDNODE node of the child
     * @return ConcepTree node child created by the call
     */
    public ANDNODE addChild(@Nonnull OWLObjectProperty property, @Nonnull ANDNODE child) {
        createChildrenStructure();
        List<ANDNODE> tmp = this.children.get(property);
        if (tmp == null) {
            tmp = new LinkedList<>();
        }
        tmp.add(child);
        this.children.put(property, tmp);
        return child;
    }

    public void addChild(OWLObjectProperty property, Collection<ANDNODE> newChildren) {
        if (property != null && newChildren != null && !newChildren.isEmpty()) {
            createChildrenStructure();
            List<ANDNODE> tmp = this.children.get(property);
            if (tmp == null) {
                tmp = new LinkedList<>();
            }
            tmp.addAll(newChildren);
            this.children.put(property, tmp);
        }
    }

    public void makeSingletonObjectProperty(OWLObjectProperty p, ANDNODE child) {
        if (p != null && child != null) {
            createChildrenStructure();
            List<ANDNODE> tmp = new ArrayList<>(1);
            tmp.add(child);
            this.children.put(p, tmp);
        }
    }

    public Map<OWLObjectProperty, List<ANDNODE>> getChildren() {
        if (this.children == null) {
            return EMPTY_MAP;
        }
        return this.children;
    }

    public boolean containsChildren(OWLObjectProperty property) {
        return this.children != null && property != null && this.children.containsKey(property);
    }

    public void clearChildren() {
        if (this.children != null) {
            this.children.clear();
        }
    }

    public void setChildren(Map<OWLObjectProperty, List<ANDNODE>> newChildren) {
        this.children = newChildren;
    }

    public Set<Map.Entry<OWLObjectProperty, List<ANDNODE>>> getChildrenEntrySet() {
        if (this.children == null) {
            return EMPTY_SET;
        }
        return this.children.entrySet();
    }

    public Collection<List<ANDNODE>> getChildrenValuesSet() {
        if (this.children == null) {
            return EMPTY_SET;
        }
        return this.children.values();
    }

    public Set<OWLObjectProperty> getChildrenKeySet() {
        if (this.children == null) {
            return EMPTY_SET;
        }
        return this.children.keySet();
    }

    public List<ANDNODE> getChildren(OWLObjectProperty property) {
        if (this.children == null || property == null || !this.children.containsKey(property)) {
            return EMPTY_LIST;
        }
        return this.children.get(property);
    }

    public List<ANDNODE> removeChildren(OWLObjectProperty property) {
        if (this.children == null || property == null) {
            return EMPTY_LIST;
        }
        return this.children.remove(property);
    }

    public void removeIfEmpty(OWLObjectProperty property) {
        if (this.children != null && property != null && this.children.containsKey(property) && this.children.get(property).isEmpty()) {
            this.children.remove(property);
        }
    }

    public boolean existsButIsEmpty(OWLObjectProperty property) {
        if (this.children != null && property != null) {
            List<ANDNODE> tmp = this.children.get(property);
            return tmp != null && tmp.isEmpty();
        }
        return false;
    }

    public void clearData() {
        this.children = null;
        this.conceptNames = null;
        this.dataConstraints = null;
        this.orNodes = null;
    }

    public void makeBottomNode(OWLClass entity) {
        if (entity != null) {
            clearData();
            this.conceptNames = Collections.singleton(entity);
        }
    }

    public ANDNODE copy() {
        ANDNODE newNode = new ANDNODE();
        if (this.conceptNames != null) {
            newNode.addConceptName(this.conceptNames);
        }
        if (this.dataConstraints != null) {
            for (Map.Entry<OWLDataProperty, List<IntRange>> entry : this.dataConstraints.entrySet()) {
                newNode.addDataProperty(entry.getKey(), entry.getValue());
            }
        }
        if (this.orNodes != null) {
            for (ORNODE node : this.orNodes) {
                ORNODE newORNode = new ORNODE();
                for (ANDNODE tree : node) {
                    newORNode.addTree(tree.copy());
                }
                newNode.addORnode(newORNode);
            }
        }
        if (this.children != null) {
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : this.children.entrySet()) {
                OWLObjectProperty property = entry.getKey();
                for (ANDNODE child : entry.getValue()) {
                    newNode.addChild(property, child.copy());
                }
            }
        }
        return newNode;
    }

    public boolean replaceConceptNames(@Nonnull Set<OWLClass> newSet) {
        if (this.conceptNames == null && !newSet.isEmpty()) {
            this.conceptNames = newSet;
            return true;
        }
        return this.conceptNames != null && this.conceptNames.equals(newSet);
    }

    public boolean replaceDataProperties(@Nonnull Map<OWLDataProperty, List<IntRange>> newProperties) {
        if (this.dataConstraints == null && !newProperties.isEmpty()) {
            this.dataConstraints = newProperties;
            return true;
        }
        return this.dataConstraints != null && this.dataConstraints.equals(newProperties);
    }

    public boolean replaceChildren(@Nonnull Map<OWLObjectProperty, List<ANDNODE>> newProperties) {
        if (this.children == null && !newProperties.isEmpty()) {
            this.children = newProperties;
            return true;
        }
        return this.children != null && this.children.equals(newProperties);
    }

    public boolean replaceNodeOfChild(@Nonnull OWLObjectProperty property, @Nonnull Collection<ANDNODE> newChildren) {
        if (!containsChildren(property)) {
            addChild(property, newChildren);
            return true;
        }
        return false;
    }
}
