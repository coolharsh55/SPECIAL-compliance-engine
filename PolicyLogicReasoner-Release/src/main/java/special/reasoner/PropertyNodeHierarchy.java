/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import java.util.*;
import org.semanticweb.owlapi.model.*;
import special.model.EntityIntersectionNode;

/**
 *
 * @author Luca Ioffredo
 * @param <T> Subclass of OWLObject as OWLDataProperty or OWLObjectProperty
 * @param <V> Subclass of OWLClassExpression as OWLUnionOf
 */
public abstract class PropertyNodeHierarchy<T extends OWLObject, V extends OWLClassExpression> extends NodeHierarchy<T> {

    protected boolean functional = false;
    /* Each EntityIntersectionNode is a disjunct of a disjunction.
    Example 1: 
    range(R, A or B or F)
    Node A or Node B or Node F
    
    Example 2:
    range(R, C)
    range(R, A or B)
    Node(C,A) or Node(C,B)
     */
    private List<EntityIntersectionNode<V>> ranges;
    /**
     * This set contains only ranges defined with pure class only. Example:
     * range(R,A) range(R,B or C) rangesAsOWLClass contains only A
     */
    private Set<OWLClass> rangesAsOWLClass;

    boolean isFunctional() {
        return this.functional;
    }

    void setFunctional(boolean v) {
        this.functional = v;
    }

    boolean addRange(EntityIntersectionNode<V> element) {
        if (this.ranges == null) {
            this.ranges = new LinkedList<>();
        }
        return this.ranges.add(element);
    }

    boolean addRangeClassOnly(OWLClass element) {
        if (this.rangesAsOWLClass == null) {
            this.rangesAsOWLClass = new LinkedHashSet<>();
        }
        return this.rangesAsOWLClass.add(element);
    }

    boolean addRangesClassOnly(Collection<OWLClass> elements) {
        if (this.rangesAsOWLClass == null) {
            this.rangesAsOWLClass = new LinkedHashSet<>();
        }
        return this.rangesAsOWLClass.addAll(elements);
    }

    boolean hasRanges() {
        return this.ranges != null && !this.ranges.isEmpty();
    }

    List<EntityIntersectionNode<V>> getRanges() {
        return this.ranges;
    }

    Set<OWLClass> getRangesClassOnly() {
        return this.rangesAsOWLClass;
    }
    
    @Override
    public boolean isPropertyNode() {
        return true;
    }
}
