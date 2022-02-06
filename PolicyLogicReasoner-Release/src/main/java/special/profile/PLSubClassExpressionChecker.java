/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.profile;

import org.semanticweb.owlapi.model.*;

/**
 *
 * @author Luca Ioffredo
 */
public class PLSubClassExpressionChecker implements OWLClassExpressionVisitorEx<Boolean> {

    public Boolean visit(OWLClass desc) {
        return true;
    }

    public Boolean visit(OWLObjectIntersectionOf desc) {
        return false;
    }

    public Boolean visit(OWLObjectUnionOf desc) {
        return true;
    }

    public Boolean visit(OWLObjectComplementOf desc) {
        return false;
    }

    public Boolean visit(OWLObjectSomeValuesFrom desc) {
        return false;
    }

    public Boolean visit(OWLObjectAllValuesFrom desc) {
        return false;
    }

    public Boolean visit(OWLObjectHasValue desc) {
        return false;
    }

    public Boolean visit(OWLObjectMinCardinality desc) {
        return false;
    }

    public Boolean visit(OWLObjectExactCardinality desc) {
        return false;
    }

    public Boolean visit(OWLObjectMaxCardinality desc) {
        return false;
    }

    public Boolean visit(OWLObjectHasSelf desc) {
        return false;
    }

    public Boolean visit(OWLObjectOneOf desc) {
        return false;
    }

    public Boolean visit(OWLDataSomeValuesFrom desc) {
        return false;
    }

    public Boolean visit(OWLDataAllValuesFrom desc) {
        return false;
    }

    public Boolean visit(OWLDataHasValue desc) {
        return false;
    }

    public Boolean visit(OWLDataMinCardinality desc) {
        return false;
    }

    public Boolean visit(OWLDataExactCardinality desc) {
        return false;
    }

    public Boolean visit(OWLDataMaxCardinality desc) {
        return false;
    }
}
