/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import java.util.Collection;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;

/**
 * OWLClass node of concept's hierarchy
 *
 * @author Luca Ioffredo
 */
public class ClassNodeHierarchy extends NodeHierarchy<OWLClass> {

    ClassNodeHierarchy(OWLClass clazz) {
        this.node = new OWLClassNode(clazz);
    }

    ClassNodeHierarchy(Collection<OWLClass> elements) {
        this.node = new OWLClassNode(elements);
    }

    @Override
    NodeHierarchy<OWLClass> getNode(OWLClass element) {
        return new ClassNodeHierarchy(element);
    }
}
