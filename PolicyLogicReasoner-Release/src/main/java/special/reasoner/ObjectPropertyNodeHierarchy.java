/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import java.util.Collection;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.reasoner.impl.OWLObjectPropertyNode;

/**
 * Node OWLObjectPropertyExpression of roles's hierarchy
 *
 * @author Luca Ioffredo
 */
public class ObjectPropertyNodeHierarchy extends PropertyNodeHierarchy<OWLObjectPropertyExpression, OWLClass> {

    ObjectPropertyNodeHierarchy(OWLObjectPropertyExpression elem) {
        this.node = new OWLObjectPropertyNode(elem);
    }

    ObjectPropertyNodeHierarchy(Collection<OWLObjectPropertyExpression> elements) {
        this.node = new OWLObjectPropertyNode(elements);
    }

    @Override
    NodeHierarchy<OWLObjectPropertyExpression> getNode(OWLObjectPropertyExpression element) {
        return new ObjectPropertyNodeHierarchy(element);
    }

}
