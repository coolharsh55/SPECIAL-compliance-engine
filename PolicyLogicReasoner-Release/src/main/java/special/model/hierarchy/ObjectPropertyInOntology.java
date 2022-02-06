/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model.hierarchy;

import java.util.*;
import java.util.stream.*;
import javax.annotation.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.*;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.*;

/**
 *
 * @author Luca Ioffredo
 */
public class ObjectPropertyInOntology extends RawOntology<OWLObjectPropertyExpression> {

    public ObjectPropertyInOntology(@Nonnull OWLOntology o, @Nonnull OWLDataFactory f) {
        super(o, f);
    }

    @Override
    public Set<OWLObjectPropertyExpression> getParentsInRawHierarchy(OWLObjectPropertyExpression child) {
        Set<OWLObjectPropertyExpression> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLSubObjectPropertyOfAxiom subPropertyOf : ont.objectSubPropertyAxiomsForSubProperty(child).collect(Collectors.toList())) {
                if (!subPropertyOf.getSuperProperty().isAnonymous()) {
                    result.add(subPropertyOf.getSuperProperty());
                }
            }
        }
        return result;
    }

    @Override
    public Set<OWLObjectPropertyExpression> getChildrenInRawHierarchy(OWLObjectPropertyExpression parent) {
        Set<OWLObjectPropertyExpression> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLSubObjectPropertyOfAxiom superPropertyOf : ont.objectSubPropertyAxiomsForSuperProperty(parent).collect(Collectors.toList())) {
                if (!superPropertyOf.getSubProperty().isAnonymous()) {
                    result.add(superPropertyOf.getSubProperty());
                }
            }
        }
        return result;
    }

    @Override
    public Set<OWLObjectPropertyExpression> getDisjointsInRawHierarchy(OWLObjectPropertyExpression el) {
        Set<OWLObjectPropertyExpression> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLDisjointObjectPropertiesAxiom disjointObjectPropertiesAxiom : ont.disjointObjectPropertiesAxioms(el).collect(Collectors.toList())) {
                for (OWLObjectPropertyExpression disjoint : disjointObjectPropertiesAxiom.properties().collect(Collectors.toList())) {
                    if (!disjoint.isAnonymous() && !disjoint.equals(el)) {
                        result.add(disjoint);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean isFunctional(OWLObjectPropertyExpression el) {
        return EntitySearcher.isFunctional(el, getRootOntology());
    }
}
