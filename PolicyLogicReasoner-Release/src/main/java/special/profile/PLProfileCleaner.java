/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.profile;

import java.util.*;
import javax.annotation.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.*;

/**
 * Remove all axioms NO-PL from an ontology
 *
 * @author Luca Ioffredo
 */
public class PLProfileCleaner {

    public Set<OWLOntology> clearOntology(@Nonnull Set<OWLOntology> ontologies) {
        OWLProfile profile = new PLProfile();
        for (OWLOntology o : ontologies) {
            OWLProfileReport report = profile.checkOntology(o);
            if (!report.isInProfile()) {
                for (final OWLProfileViolation violation : report.getViolations()) {
                    System.out.println("Violation: " + violation.getAxiom());
                    o.remove(violation.getAxiom());
                }
            }
        }
        return ontologies;
    }

}
