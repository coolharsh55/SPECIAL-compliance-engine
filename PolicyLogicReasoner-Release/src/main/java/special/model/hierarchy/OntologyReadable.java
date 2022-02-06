/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model.hierarchy;

import java.util.Set;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 *
 * @author Luca Ioffredo
 * @param <T> Subclass of OWLObject as OWLClass, OWLDataProperty or OWLObjectProperty
 */
public interface OntologyReadable<T extends OWLObject> {

    Set<T> getParentsInRawHierarchy(T child);

    Set<T> getChildrenInRawHierarchy(T parent);

    Set<T> getDisjointsInRawHierarchy(T el);

    boolean isFunctional(T el);

    public OWLOntology getRootOntology();

    public OWLDataFactory getDataFactory();
}
