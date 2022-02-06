/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model;

import java.util.Optional;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.impl.DefaultNode;

/**
 *
 * @author Luca Ioffredo
 * @param <E> SubClass of OWLObject as OWLClass
 */
public class EntityIntersectionNode<E extends OWLObject> extends DefaultNode<E> { 

    @Override
    protected Optional<E> getTopEntity() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Optional<E> getBottomEntity() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
