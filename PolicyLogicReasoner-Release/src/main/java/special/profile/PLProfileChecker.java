/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.profile;

import java.util.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.*;
import org.semanticweb.owlapi.profiles.violations.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.vocab.*;

/**
 *
 * @author Luca Ioffredo
 */
public class PLProfileChecker extends OWLOntologyWalkerVisitorEx {

    private final Set<IRI> allowedDatatypes = new HashSet<>();
    private final Set<OWLProfileViolation> profileViolations = new HashSet<>();
    private final PLSubPropertyExpressionChecker subPropertyExpressionChecker = new PLSubPropertyExpressionChecker();
    private final PLSuperPropertyExpressionChecker superObjectPropertyExpressionChecker = new PLSuperPropertyExpressionChecker();
    private final PLSubClassExpressionChecker subClassExpressionChecker = new PLSubClassExpressionChecker();

    public PLProfileChecker(OWLOntologyWalker walker) {
        super(walker);
        allowedDatatypes.add(XSDVocabulary.POSITIVE_INTEGER.getIRI());
        allowedDatatypes.add(XSDVocabulary.NON_NEGATIVE_INTEGER.getIRI());
        allowedDatatypes.add(OWLRDFVocabulary.OWL_ANNOTATION.getIRI());
        allowedDatatypes.add(OWLRDFVocabulary.OWL_ANNOTATION_PROPERTY.getIRI());
        allowedDatatypes.add(OWLRDFVocabulary.OWL_ANNOTATED_PROPERTY.getIRI());
        allowedDatatypes.add(XSDVocabulary.STRING.getIRI());
        /* About annotation axiom, a comment is a string */
 /* OWL2Datatype.XSD_INTEGER.getIRI() maybe in future? */
    }

    public Set<OWLProfileViolation> getProfileViolations() {
        return new HashSet<>(profileViolations);
    }

    private boolean isPLSubObjectPropertyExpression(OWLObjectPropertyExpression ope) {
        return ope.accept(subPropertyExpressionChecker);
    }

    private boolean isPLSuperObjectPropertyExpression(OWLObjectPropertyExpression ope) {
        return ope.accept(superObjectPropertyExpressionChecker);
    }

    private boolean isPLSubClassExpression(OWLClassExpression ce) {
        return ce.accept(subClassExpressionChecker);
    }

    private boolean isPLSuperClassExpression(OWLClassExpression ce) {
        return ce.accept(subClassExpressionChecker);
    }

    private boolean isPLSubDataPropertyExpression(OWLDataPropertyExpression property) {
        return property.accept(subPropertyExpressionChecker);
    }

    @Override
    public Object visit(OWLDisjointClassesAxiom axiom) {
        for (OWLClassExpression ce : axiom.getOperandsAsList()) {
            if (!ce.isOWLClass()) {
                profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
            }
        }
        return null;
    }

    @Override
    public Object visit(OWLClassAssertionAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(OWLDataPropertyDomainAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        /* Maybe in future? 
        OWLDataPropertyExpression property = axiom.getProperty();
        if (!isPLSubDataPropertyExpression(property)) {
            profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        }
        OWLClassExpression domain = axiom.getDomain();
        if (!isPLSuperClassExpression(domain)) {
            profileViolations.add(new UseOfNonSuperClassExpression(getCurrentOntology(), axiom, domain));

        }
         */
        return null;
    }

    @Override
    public Object visit(OWLDisjointDataPropertiesAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(OWLDisjointUnionAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(OWLEquivalentClassesAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(OWLEquivalentDataPropertiesAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(OWLFunctionalDataPropertyAxiom axiom) {
        OWLDataPropertyExpression property = axiom.getProperty();
        if (!isPLSubDataPropertyExpression(property)) {
            profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        }
        return null;
    }

    @Override
    public Object visit(OWLHasKeyAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(OWLObjectPropertyDomainAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        /* Maybe in future? 
        OWLObjectPropertyExpression property = axiom.getProperty();
        if (!isPLSubObjectPropertyExpression(property)) {
            profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        }
        OWLClassExpression domain = axiom.getDomain();
        if (!isPLSuperClassExpression(domain)) {
            profileViolations.add(new UseOfNonSuperClassExpression(getCurrentOntology(), axiom, domain));
        }
         */
        return null;
    }

    @Override
    public Object visit(OWLObjectPropertyRangeAxiom axiom) {
        OWLObjectPropertyExpression property = axiom.getProperty();
        if (!isPLSubObjectPropertyExpression(property)) {
            profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        }
        OWLClassExpression range = axiom.getRange();
        if (!isPLSuperClassExpression(range)) {
            profileViolations.add(new UseOfNonSuperClassExpression(getCurrentOntology(), axiom, range));
        }
        return null;
    }

    @Override
    public Object visit(OWLFunctionalObjectPropertyAxiom axiom) {
        OWLObjectPropertyExpression property = axiom.getProperty();
        if (!isPLSubObjectPropertyExpression(property)) {
            profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        }
        return null;
    }

    @Override
    public Object visit(OWLSubClassOfAxiom axiom) {
        if (!isPLSubClassExpression(axiom.getSubClass())) {
            profileViolations.add(new UseOfNonSubClassExpression(getCurrentOntology(), axiom, axiom.getSubClass()));
        }
        if (!isPLSuperClassExpression(axiom.getSuperClass())) {
            profileViolations.add(new UseOfNonSuperClassExpression(getCurrentOntology(), axiom, axiom.getSuperClass()));
        }
        return null;
    }

    @Override
    public Object visit(OWLSubObjectPropertyOfAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(OWLSubDataPropertyOfAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), axiom));
        return null;
    }

    @Override
    public Object visit(SWRLRule rule) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), rule));
        return super.visit(rule);
    }

    @Override
    public Object visit(OWLDataComplementOf node) {
        profileViolations.add(new UseOfIllegalDataRange(getCurrentOntology(), getCurrentAxiom(), node));
        return null;
    }

    @Override
    public Object visit(OWLDataIntersectionOf node) {
        profileViolations.add(new UseOfIllegalDataRange(getCurrentOntology(), getCurrentAxiom(), node));
        return null;
    }

    @Override
    public Object visit(OWLDataOneOf node) {
        profileViolations.add(new UseOfIllegalDataRange(getCurrentOntology(), getCurrentAxiom(), node));
        return null;
    }

    @Override
    public Object visit(OWLDatatype node) {
        if (!allowedDatatypes.contains(node.getIRI())) {
            profileViolations.add(new UseOfIllegalDataRange(getCurrentOntology(), getCurrentAxiom(), node));
        }
        return null;
    }

    @Override
    public Object visit(OWLDatatypeRestriction node) {
        profileViolations.add(new UseOfIllegalDataRange(getCurrentOntology(), getCurrentAxiom(), node));
        return null;
    }

    @Override
    public Object visit(OWLDataUnionOf node) {
        profileViolations.add(new UseOfIllegalDataRange(getCurrentOntology(), getCurrentAxiom(), node));
        return null;
    }

    @Override
    public Object visit(OWLDatatypeDefinitionAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), getCurrentAxiom()));
        return null;
    }

    @Override
    public Object visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), getCurrentAxiom()));
        return null;
    }

    @Override
    public Object visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        profileViolations.add(new UseOfIllegalAxiom(getCurrentOntology(), getCurrentAxiom()));
        return null;
    }

    @Override
    public Object visit(OWLAnnotationAssertionAxiom axiom) {
        return null;
    }

    @Override
    public Object visit(OWLAnnotationPropertyDomainAxiom axiom) {
        return null;
    }

    @Override
    public Object visit(OWLAnnotationPropertyRangeAxiom axiom) {
        return null;
    }

    @Override
    public Object visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        return null;
    }
}
