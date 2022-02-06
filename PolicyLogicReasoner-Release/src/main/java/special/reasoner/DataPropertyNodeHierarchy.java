/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import java.util.Collection;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.reasoner.impl.OWLDataPropertyNode;

/**
 * Node OWLDataProperty of constraints's hierarchy
 *
 * @author Luca Ioffredo
 *
 */
public class DataPropertyNodeHierarchy extends PropertyNodeHierarchy<OWLDataProperty, OWLClass> {

    DataPropertyNodeHierarchy(OWLDataProperty elem) {
        this.node = new OWLDataPropertyNode(elem);
    }

    DataPropertyNodeHierarchy(Collection<OWLDataProperty> elements) {
        this.node = new OWLDataPropertyNode(elements);
    }

    @Override
    NodeHierarchy<OWLDataProperty> getNode(OWLDataProperty element) {
        return new DataPropertyNodeHierarchy(element);
    }

}
