/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.profile;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

/**
 *
 * @author Luca Ioffredo
 */
public class OWLEquivalentClassesAxiomVisitor extends OWLOntologyWalkerVisitor {

    Set<OWLEquivalentClassesAxiom> axioms;

    public OWLEquivalentClassesAxiomVisitor(OWLOntologyWalker walker) {
        super(walker);
        axioms = new HashSet<>();
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        if (axiom.getAxiomType().equals(AxiomType.EQUIVALENT_CLASSES)) {
            axioms.add(axiom);
        }
    }

    public Set<OWLEquivalentClassesAxiom> getOWLEquivalentClassesAxiomVisitor() {
        return axioms;
    }

}
