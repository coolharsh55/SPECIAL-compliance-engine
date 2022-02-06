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
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.*;

/**
 *
 * @author Luca Ioffredo
 */
public class ClassInOntology extends RawOntology<OWLClass> {

    public ClassInOntology(@Nonnull OWLOntology o, @Nonnull OWLDataFactory f) {
        super(o, f);
    }

    @Override
    public Set<OWLClass> getParentsInRawHierarchy(@Nonnull OWLClass child) {
        Set<OWLClass> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLSubClassOfAxiom subClassOf : ont.subClassAxiomsForSubClass(child).collect(Collectors.toList())) {
                if (!subClassOf.getSuperClass().isAnonymous()) {
                    OWLClass superClass = (OWLClass) subClassOf.getSuperClass();
                    result.add(superClass);
                }
            }
        }
        return result;
    }

    @Override
    public Set<OWLClass> getChildrenInRawHierarchy(@Nonnull OWLClass parent) {
        Set<OWLClass> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLSubClassOfAxiom subClassOf : ont.subClassAxiomsForSuperClass(parent).collect(Collectors.toList())) {
                if (!subClassOf.getSubClass().isAnonymous()) {
                    OWLClass subClass = (OWLClass) subClassOf.getSubClass();
                    result.add(subClass);
                }
            }
        }
        return result;
    }

    @Override
    public Set<OWLClass> getDisjointsInRawHierarchy(@Nonnull OWLClass el) {
        Set<OWLClass> result = new HashSet<>();
        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
            for (OWLDisjointClassesAxiom disjointClassesAxiom : ont.disjointClassesAxioms(el).collect(Collectors.toList())) {
                for (OWLClass disjoint : disjointClassesAxiom.classesInSignature().collect(Collectors.toList())) {
                    if (!disjoint.equals(el)) {
                        result.add(disjoint);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean isFunctional(@Nonnull OWLClass el) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
