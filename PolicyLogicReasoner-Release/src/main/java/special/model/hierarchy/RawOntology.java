/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model.hierarchy;

import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;

/**
 *
 * @author Luca Ioffredo
 */
public abstract class RawOntology<T extends OWLObject> implements OntologyReadable<T> {

    private final OWLOntology rootOntology;
    private final OWLDataFactory dataFactory;

    RawOntology(@Nonnull OWLOntology o, @Nonnull OWLDataFactory f) {
        this.rootOntology = checkNotNull(o, "ontology cannot be null");;
        this.dataFactory = checkNotNull(f, "data factory cannot be null");
    }

    public OWLOntology getRootOntology() {
        return rootOntology;
    }
    
    public OWLDataFactory getDataFactory() {
        return dataFactory;
    }
}
