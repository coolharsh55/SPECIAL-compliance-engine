/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.profile;

import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpressionVisitorEx;

/**
 *
 * @author Luca Ioffredo
 */
public class PLSubPropertyExpressionChecker implements OWLPropertyExpressionVisitorEx<Boolean> {

    @Override
    public Boolean visit(OWLObjectProperty property) {
        return true;
    }

    @Override
    public Boolean visit(OWLObjectInverseOf property) {
        OWLObjectPropertyExpression inverse = property.getInverse();
        return inverse.accept(this);
    }

    @Override
    public Boolean visit(OWLDataProperty property) {
        return true;
    }
}
