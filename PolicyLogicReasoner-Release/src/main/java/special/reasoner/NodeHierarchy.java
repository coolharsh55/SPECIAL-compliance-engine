/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.Node;

/**
 * Generic node of concepts's hierarchy
 *
 * @author Luca Ioffredo
 * @param <T> Where T is a subclass of OWLObject as OWLClass, OWLObjectPropery and OWLDataProperty
 */
public abstract class NodeHierarchy<T extends OWLObject> {

    protected Node<T> node;
    protected Map<T, NodeHierarchy<T>> processed;
    protected Set<T> disjuncts;
    protected Set<NodeHierarchy<T>> parents;
    protected Set<NodeHierarchy<T>> children;

    abstract NodeHierarchy<T> getNode(T element);

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NodeHierarchy)) {
            return false;
        }
        return this.node.equals(((NodeHierarchy) obj).node);
    }

    @Override
    public int hashCode() {
        return this.node.hashCode();
    }

    public boolean isPropertyNode() {
        return false;
    }
    
    NodeHierarchy<T> addParent(T element) {
        if (this.parents == null) {
            this.parents = new HashSet<>();
        }
        NodeHierarchy<T> n = getProcessed(element);
        if (n == null) {
            n = getNode(element);
            this.parents.add(n);
            n.attachChild(this);
            addToProcessed(element, n);
        }
        return n;
    }

    Set<NodeHierarchy<T>> addParents(Set<T> elements) {
        Set<NodeHierarchy<T>> res = new HashSet<>();
        if (this.parents == null) {
            this.parents = new HashSet<>();
        }
        for (T element : elements) {
            NodeHierarchy<T> n = getProcessed(element);
            if (n == null) {
                n = getNode(element);
                this.parents.add(n);
                n.attachChild(this);
                addToProcessed(element, n);
            }
            res.add(n);
        }
        return res;
    }

    void addParentNode(NodeHierarchy<T> element) {
        element.attachChild(this);
        attachParent(element);
    }

    void addParentsNode(Set<NodeHierarchy<T>> elements) {
        elements.forEach(element -> {
            element.attachChild(this);
            attachParent(element);
        });
    }

    NodeHierarchy<T> addChild(T element) {
        if (this.children == null) {
            this.children = new HashSet<>();
        }
        NodeHierarchy<T> n = getProcessed(element);
        if (n == null) {
            n = getNode(element);
            this.children.add(n);
            n.attachParent(this);
            addToProcessed(element, n);
        }
        return n;
    }

    Set<NodeHierarchy<T>> addChildren(Set<T> elements) {
        Set<NodeHierarchy<T>> res = new HashSet<>();
        if (this.children == null) {
            this.children = new HashSet<>();
        }
        for (T element : elements) {
            NodeHierarchy<T> n = getProcessed(element);
            if (n == null) {
                n = getNode(element);
                this.children.add(n);
                n.attachParent(this);
                addToProcessed(element, n);
            }
            res.add(n);
        }
        return res;
    }

    void addChildNode(NodeHierarchy<T> element) {
        element.attachParent(this);
        attachChild(element);
    }

    void addChildrenNode(Set<NodeHierarchy<T>> elements) {
        elements.forEach(element -> {
            element.attachParent(this);
            attachChild(element);
        });
    }

    void removeChildNode(NodeHierarchy<T> element) {
        element.removeParent(this);
        removeChild(element);
    }

    void removeParentNode(NodeHierarchy<T> element) {
        element.removeChild(this);
        removeParent(element);
    }

    protected boolean hasDisjuncts() {
        return this.disjuncts != null && !this.disjuncts.isEmpty();
    }

    protected boolean isDisjunct(T n) {
        return this.disjuncts != null && this.disjuncts.contains(n);
    }

    protected Set<T> getDisjuncts() {
        if(this.disjuncts == null) {
            return Collections.emptySet();
        }
        return this.disjuncts;
    }

    protected boolean addDisjunct(T n) {
        if (this.disjuncts == null) {
            this.disjuncts = new HashSet<>();
        }
        return this.disjuncts.add(n);
    }

    protected boolean addDisjuncts(Set<T> elements) {
        if (this.disjuncts == null) {
            this.disjuncts = new HashSet<>();
        }
        return this.disjuncts.addAll(elements);
    }

    protected boolean isProcessed(T n) {
        return this.processed != null && this.processed.containsKey(n);
    }

    protected NodeHierarchy<T> getProcessed(T n) {
        if (this.processed != null) {
            return this.processed.get(n);
        }
        return null;
    }

    protected void addToProcessed(T n, NodeHierarchy<T> node) {
        if (this.processed == null) {
            this.processed = new HashMap<>();
        }
        if (!isProcessed(n)) {
            this.processed.put(n, node);
        }
    }

    protected Node<T> getValue() {
        return this.node;
    }

    protected boolean hasParentsNodes() {
        return this.parents != null && !this.parents.isEmpty();
    }

    protected boolean hasChildrenNodes() {
        return this.children != null && !this.children.isEmpty();
    }

    protected Set<NodeHierarchy<T>> getParentsNodes() {
        if(this.parents == null) {
            return Collections.emptySet();
        }
        return this.parents;
    }

    protected Set<NodeHierarchy<T>> getChildrenNodes() {
        if(this.children == null) {
            return Collections.emptySet();
        }
        return this.children;
    }

    protected void attachParent(NodeHierarchy<T> node) {
        if (this.parents == null) {
            this.parents = new HashSet<>();
        }
        this.parents.add(node);
        for (T entity : node.node) {
            addToProcessed(entity, this);
        }
    }

    protected void removeParent(NodeHierarchy<T> node) {
        if (this.parents != null) {
            this.parents.remove(node);
        }
    }

    protected void attachChild(NodeHierarchy<T> node) {
        if (this.children == null) {
            this.children = new HashSet<>();
        }
        this.children.add(node);
        for (T entity : node.node) {
            addToProcessed(entity, node);
        }
    }

    protected void removeChild(NodeHierarchy<T> node) {
        if (this.children != null) {
            this.children.remove(node);
        }
    }
}
