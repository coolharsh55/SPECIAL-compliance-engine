/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import special.reasoner.cache.DoubleKeyCache;
import special.reasoner.cache.SingleKeyCache;

/**
 *
 * @author Luca Ioffredo
 */
public class PLReasonerFactory implements OWLReasonerFactory {

    @Override
    public String getReasonerName() {
        return PLReasoner.REASONER_NAME;
    }

    @Override
    public OWLReasoner createNonBufferingReasoner(@Nonnull OWLOntology ontology) {
        return createNonBufferingReasoner(ontology, new PLConfiguration());
    }

    @Override
    public OWLReasoner createReasoner(@Nonnull OWLOntology ontology) {
        return createReasoner(ontology, new PLConfiguration());
    }

    @Override
    public OWLReasoner createNonBufferingReasoner(@Nonnull OWLOntology ontology, @Nonnull OWLReasonerConfiguration config) {
        return new PLReasoner(ontology, config, BufferingMode.NON_BUFFERING);
    }

    @Override
    public OWLReasoner createReasoner(@Nonnull OWLOntology ontology, @Nonnull OWLReasonerConfiguration config) {
        return createPolicyLogicReasoner(ontology, config, BufferingMode.BUFFERING);
    }

    public OWLReasoner createPolicyLogicReasoner(@Nonnull OWLOntology ontology, @Nonnull OWLReasonerConfiguration config, @Nonnull BufferingMode bufferingMode,
            @Nonnull SingleKeyCache singleKeyCache, @Nonnull DoubleKeyCache doubleKeyCache) {
        return new PLReasoner(ontology, config, bufferingMode, singleKeyCache, doubleKeyCache);
    }

    private OWLReasoner createPolicyLogicReasoner(OWLOntology ontology, OWLReasonerConfiguration config, BufferingMode bufferingMode) {
        return new PLReasoner(ontology, config, bufferingMode);
    }
}
