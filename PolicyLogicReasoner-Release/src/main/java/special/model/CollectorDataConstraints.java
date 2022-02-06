/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model;

import java.util.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;

/**
 *
 * @author Luca Ioffredo
 */
public class CollectorDataConstraints implements OWLClassExpressionVisitorEx<Map<OWLDataProperty, Set<Integer[]>>> {

    Map<OWLDataProperty, Set<Integer[]>> intervals = new HashMap<>();

    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLClass ce) {
        return this.intervals;
    }

    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLDataAllValuesFrom ce) {
        Integer[] interval = new Integer[2];
        interval[0] = interval[1] = 0;
        OWLDataProperty p = ce.getProperty().asOWLDataProperty();
        OWLDatatypeRestriction restriction = (OWLDatatypeRestriction) ce.getFiller();
        for (OWLFacetRestriction facetRestrinction : restriction.facetRestrictionsAsList()) {
            OWLFacet facet = facetRestrinction.getFacet();
            if (facet.equals(OWLFacet.MIN_INCLUSIVE)) {
                interval[0] = facetRestrinction.getFacetValue().parseInteger();
            } else if (facet.equals(OWLFacet.MAX_INCLUSIVE)) {
                interval[1] = facetRestrinction.getFacetValue().parseInteger();
            } else if (facet.equals(OWLFacet.MIN_EXCLUSIVE)) {
                interval[0] = facetRestrinction.getFacetValue().parseInteger() + 1;
            } else if (facet.equals(OWLFacet.MAX_EXCLUSIVE)) {
                interval[1] = facetRestrinction.getFacetValue().parseInteger() - 1;
            }
        }
        Set<Integer[]> tmp = null;
        if (intervals.containsKey(p.getIRI())) {
            tmp = intervals.get(p.getIRI());
        } else {
            tmp = new LinkedHashSet<>();
        }
        tmp.add(interval);
        intervals.put(p, tmp);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLDataExactCardinality ce) {
        Integer[] interval = new Integer[2];
        interval[0] = ce.getCardinality();
        interval[1] = interval[0];
        Set<Integer[]> tmp = null;
        IRI iri = ce.getProperty().asOWLDataProperty().getIRI();
        if (intervals.containsKey(iri)) {
            tmp = intervals.get(iri);
        } else {
            tmp = new LinkedHashSet<>();
        }
        tmp.add(interval);
        intervals.put(ce.getProperty().asOWLDataProperty(), tmp);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLDataHasValue ce) {
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLDataMaxCardinality ce) {
        Integer[] interval = new Integer[2];
        interval[0] = ce.getCardinality();
        interval[1] = Integer.MAX_VALUE;
        Set<Integer[]> tmp = null;
        IRI iri = ce.getProperty().asOWLDataProperty().getIRI();
        if (intervals.containsKey(iri)) {
            tmp = intervals.get(iri);
        } else {
            tmp = new LinkedHashSet<>();
        }
        tmp.add(interval);
        intervals.put(ce.getProperty().asOWLDataProperty(), tmp);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLDataMinCardinality ce) {
        Integer[] interval = new Integer[2];
        interval[0] = 0;
        interval[1] = ce.getCardinality();
        Set<Integer[]> tmp = null;
        IRI iri = ce.getProperty().asOWLDataProperty().getIRI();
        if (intervals.containsKey(iri)) {
            tmp = intervals.get(iri);
        } else {
            tmp = new LinkedHashSet<>();
        }
        tmp.add(interval);
        intervals.put(ce.getProperty().asOWLDataProperty(), tmp);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLDataSomeValuesFrom ce) {
        Integer[] interval = new Integer[2];
        interval[0] = interval[1] = 0;
        OWLDataProperty p = ce.getProperty().asOWLDataProperty();
        OWLDatatypeRestriction restriction = (OWLDatatypeRestriction) ce.getFiller();
        for (OWLFacetRestriction facetRestrinction : restriction.facetRestrictionsAsList()) {
            OWLFacet facet = facetRestrinction.getFacet();
            if (facet.equals(OWLFacet.MIN_INCLUSIVE)) {
                interval[0] = facetRestrinction.getFacetValue().parseInteger();
            } else if (facet.equals(OWLFacet.MAX_INCLUSIVE)) {
                interval[1] = facetRestrinction.getFacetValue().parseInteger();
            } else if (facet.equals(OWLFacet.MIN_EXCLUSIVE)) {
                interval[0] = facetRestrinction.getFacetValue().parseInteger() + 1;
            } else if (facet.equals(OWLFacet.MAX_EXCLUSIVE)) {
                interval[1] = facetRestrinction.getFacetValue().parseInteger() - 1;
            }
        }
        Set<Integer[]> tmp = null;
        IRI iri = ce.getProperty().asOWLDataProperty().getIRI();
        if (intervals.containsKey(iri)) {
            tmp = intervals.get(iri);
        } else {
            tmp = new LinkedHashSet<>();
        }
        tmp.add(interval);
        intervals.put(p, tmp);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectAllValuesFrom ce) {
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectComplementOf ce) {
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectExactCardinality ce) {
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectHasSelf ce) {
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectHasValue ce) {
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectIntersectionOf ce) {
        for (OWLClassExpression conjunct : ce.asConjunctSet()) {
            conjunct.accept(this);
        }
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectMaxCardinality ce) {
        ce.getFiller().accept(this);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectMinCardinality ce) {
        ce.getFiller().accept(this);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectSomeValuesFrom ce) {
        ce.getFiller().accept(this);
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectUnionOf ce) {
        for (OWLClassExpression disjunct : ce.asDisjunctSet()) {
            disjunct.accept(this);
        }
        return this.intervals;
    }

    @Override
    public Map<OWLDataProperty, Set<Integer[]>> visit(OWLObjectOneOf ce) {
        return this.intervals;
    }
}
