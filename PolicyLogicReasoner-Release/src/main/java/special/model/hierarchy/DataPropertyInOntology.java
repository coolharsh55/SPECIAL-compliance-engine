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
import org.semanticweb.owlapi.search.EntitySearcher;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.*;

/**
 *
 * @author Luca Ioffredo
 */
public class DataPropertyInOntology extends RawOntology<OWLDataProperty> {

    public DataPropertyInOntology(@Nonnull OWLOntology o, @Nonnull OWLDataFactory f) {
        super(o, f);
    }

    @Override
    public Set<OWLDataProperty> getParentsInRawHierarchy(OWLDataProperty child) {
        Set<OWLDataProperty> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLSubDataPropertyOfAxiom subPropertyOf : ont.dataSubPropertyAxiomsForSubProperty(child).collect(Collectors.toList())) {
                if (!subPropertyOf.getSuperProperty().isAnonymous()) {
                    OWLDataProperty superProperty = (OWLDataProperty) subPropertyOf.getSuperProperty();
                    result.add(superProperty);
                }
            }
        }
        return result;
    }

    @Override
    public Set<OWLDataProperty> getChildrenInRawHierarchy(OWLDataProperty parent) {
        Set<OWLDataProperty> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLSubDataPropertyOfAxiom superPropertyOf : ont.dataSubPropertyAxiomsForSuperProperty(parent).collect(Collectors.toList())) {
                if (!superPropertyOf.getSubProperty().isAnonymous()) {
                    OWLDataProperty subProperty = (OWLDataProperty) superPropertyOf.getSubProperty();
                    result.add(subProperty);
                }
            }
        }
        return result;
    }

    @Override
    public Set<OWLDataProperty> getDisjointsInRawHierarchy(OWLDataProperty el) {
        Set<OWLDataProperty> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLDisjointDataPropertiesAxiom disjointDataPropertiesAxiom : ont.disjointDataPropertiesAxioms(el).collect(Collectors.toList())) {
                for (OWLDataPropertyExpression disjoint : disjointDataPropertiesAxiom.properties().collect(Collectors.toList())) {
                    if (!disjoint.isAnonymous() && !disjoint.equals(el)) {
                        result.add(disjoint.asOWLDataProperty());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean isFunctional(OWLDataProperty el) {
        return EntitySearcher.isFunctional(el, getRootOntology());
    }
}
