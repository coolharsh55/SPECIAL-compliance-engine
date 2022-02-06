/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.profile;

import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.*;

/**
 *
 * @author Luca Ioffredo
 */
public class PLBaseClassExpressionChecker implements OWLClassExpressionVisitorEx<Boolean> {

    @Override
    public Boolean visit(OWLClass ce) {
        return true;
    }

    @Override
    public Boolean visit(OWLObjectIntersectionOf ce) {
        for (OWLClassExpression op : ce.operands().collect(Collectors.toList())) {
            if (!(op.accept(this))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean visit(OWLObjectUnionOf ce) {
        for (OWLClassExpression op : ce.operands().collect(Collectors.toList())) {
            if (!(op.accept(this))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean visit(OWLObjectComplementOf ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectSomeValuesFrom ce) {
        OWLClassExpression filler = ce.getFiller();
        return filler.accept(this);
    }

    @Override
    public Boolean visit(OWLObjectAllValuesFrom ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectHasValue ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectMinCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectExactCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectMaxCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectHasSelf ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectOneOf ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataSomeValuesFrom ce) {
        return true;
    }

    @Override
    public Boolean visit(OWLDataAllValuesFrom ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataHasValue ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataMinCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataExactCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataMaxCardinality ce) {
        return false;
    }
}
