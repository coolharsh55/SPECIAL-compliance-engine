/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.profile;

import java.util.*;
import java.util.stream.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.*;
import org.semanticweb.owlapi.util.*;

/**
 *
 * @author Luca Ioffredo
 */
public class PLProfile implements OWLProfile {
    private static final IRI OWL2_PolicyLogic = IRI.create("http://www.w3.org/ns/owl-profile/", "PolicyLogic");
    
    @Override
    public String getName() {
        return "Policy Logic";
    }

    /**
     * Checks an ontology and its import closure to see if it is within this
     * profile.
     *
     * @param ontology The ontology to be checked.
     * @return An <code>OWLProfileReport</code> that describes whether or not
     * the ontology is within this profile.
     */
    @Override
    public OWLProfileReport checkOntology(OWLOntology ontology) {
        Set<OWLProfileViolation> violations = new HashSet<>();
        OWLOntologyWalker walker = new OWLOntologyWalker(ontology.importsClosure().collect(Collectors.toSet()));
        PLProfileChecker visitor = new PLProfileChecker(walker);
        walker.walkStructure(visitor);
        violations.addAll(visitor.getProfileViolations());
        return new OWLProfileReport(this, violations);
    }

    @Override
    public IRI getIRI() {
        return OWL2_PolicyLogic;
    }

}
