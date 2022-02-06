/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

/**
 *
 * @author Luca Ioffredo
 */
public class PLConfiguration implements OWLReasonerConfiguration {

    public static final boolean DEFAULT_CACHE_DISJOINTS_TRUE = false;
    public static final boolean DEFAULT_CACHE_DISJOINTS_FALSE = false;
    public static final boolean DEFAULT_FULL_TREE_CACHE = true;
    public static final boolean DEFAULT_FULL_INTERVAL_SAFE_CACHE = true;
    public static final boolean DEFAULT_SIMPLE_TREE_CACHE = true;
    public static final boolean DEFAULT_SIMPLE_INTERVAL_SAFE_CACHE = true;
    public static final boolean DEFAULT_NORMALIZE_SUPER_CLASS_CONCEPT = false;
    public static final boolean DEFAULT_CHECK_PL_PROFILE = false;
    
    private boolean cacheDisjointsTrueBetweenClasses = DEFAULT_CACHE_DISJOINTS_FALSE;
    private boolean cacheDisjointsFalseBetweenClasses = DEFAULT_CACHE_DISJOINTS_TRUE;
    private boolean fullTreeCache = DEFAULT_FULL_TREE_CACHE;
    private boolean fullIntervalSafeCache = DEFAULT_FULL_INTERVAL_SAFE_CACHE;
    private boolean simpleTreeCache = DEFAULT_SIMPLE_TREE_CACHE;
    private boolean simpleIntervalSafeCache = DEFAULT_SIMPLE_INTERVAL_SAFE_CACHE;
    private boolean normalizeSuperClassConcept = DEFAULT_NORMALIZE_SUPER_CLASS_CONCEPT;
    private ReasonerProgressMonitor progressMonitor = new NullReasonerProgressMonitor();
    private FreshEntityPolicy freshEntityPolicy = FreshEntityPolicy.ALLOW;
    private long timeOut = 1200000;

    /* in ms. 60000 = 1 minute; 1200000 = 20 minutes*/

    public PLConfiguration() {
        super();
    }

    public PLConfiguration(boolean activeCacheDisjointTrue, boolean activeCacheDisjointFalse) {
        super();
        this.cacheDisjointsTrueBetweenClasses = activeCacheDisjointTrue;
        this.cacheDisjointsFalseBetweenClasses = activeCacheDisjointFalse;
    }

    public PLConfiguration(boolean activeCacheDisjointTrue, boolean activeCacheDisjointFalse, long timeOut) {
        super();
        this.cacheDisjointsTrueBetweenClasses = activeCacheDisjointTrue;
        this.cacheDisjointsFalseBetweenClasses = activeCacheDisjointFalse;
        this.timeOut = timeOut;
    }

    /**
     * @param progressMonitor the progress monitor to use
     */
    public PLConfiguration(ReasonerProgressMonitor progressMonitor) {
        this(false, false);
        this.progressMonitor = progressMonitor;
    }

    /**
     * @param progressMonitor the progress monitor to use
     * @param timeOut the timeout in milliseconds
     */
    public PLConfiguration(ReasonerProgressMonitor progressMonitor, long timeOut) {
        this(false, false);
        this.progressMonitor = progressMonitor;
        this.timeOut = timeOut;
    }

    /**
     * @param progressMonitor the progress monitor to use
     * @param freshEntityPolicy the policy for fresh entities
     * @param timeOut the timeout in milliseconds
     * @param individualNodeSetPolicy the policy for individual nodes
     */
    public PLConfiguration(ReasonerProgressMonitor progressMonitor,
            FreshEntityPolicy freshEntityPolicy,
            long timeOut, IndividualNodeSetPolicy individualNodeSetPolicy) {
        this(false, false);
        this.progressMonitor = progressMonitor;
        this.freshEntityPolicy = freshEntityPolicy;
        this.timeOut = timeOut;
    }

    /**
     * @param freshEntityPolicy the policy for fresh entities
     * @param timeOut the timeout in milliseconds
     */
    public PLConfiguration(FreshEntityPolicy freshEntityPolicy, long timeOut) {
        this(false, false);
        this.freshEntityPolicy = freshEntityPolicy;
        this.timeOut = timeOut;
    }

    /**
     * @param timeOut the timeout in milliseconds
     */
    public PLConfiguration(long timeOut) {
        this(false, false);
        this.timeOut = timeOut;
    }

    @Override
    public ReasonerProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    @Override
    public long getTimeOut() {
        return timeOut;
    }

    @Override
    public FreshEntityPolicy getFreshEntityPolicy() {
        return freshEntityPolicy;
    }

    @Override
    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    public PLConfiguration setCacheDisjointTrueClasses(boolean v) {
        this.cacheDisjointsTrueBetweenClasses = v;
        return this;
    }

    public PLConfiguration setCacheDisjointFalseClasses(boolean v) {
        this.cacheDisjointsFalseBetweenClasses = v;
        return this;
    }

    public PLConfiguration setFullConceptCache(boolean treeCache, boolean intervalSafeCache) {
        this.fullTreeCache = treeCache;
        this.fullIntervalSafeCache = intervalSafeCache;
        return this;
    }

    public PLConfiguration setSimpleConceptCache(boolean treeCache, boolean intervalSafeCache) {
        this.simpleTreeCache = treeCache;
        this.simpleIntervalSafeCache = intervalSafeCache;
        return this;
    }
    
    public PLConfiguration setNormalizeSuperClassConcept(boolean state) {
        this.normalizeSuperClassConcept = state;
        return this;
    }
    
    public boolean hasNormalizationSuperClassConcept() {
        return this.normalizeSuperClassConcept;
    }

    public boolean hasCacheDisjointTrueClasses() {
        return this.cacheDisjointsTrueBetweenClasses;
    }

    public boolean hasCacheDisjointFalseClasses() {
        return this.cacheDisjointsFalseBetweenClasses;
    }
    
    public boolean hasCacheFullConcept() {
        return this.fullTreeCache;
    }
    
    public boolean hasCacheFullConceptIntervalSafe() {
        return this.fullIntervalSafeCache;
    }
    
    public boolean hasCacheSimpleConcept() {
        return this.simpleTreeCache;
    }
    
    public boolean hasCacheSimpleConceptIntervalSafe() {
        return this.simpleIntervalSafeCache;
    }
}
