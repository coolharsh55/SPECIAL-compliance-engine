/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model;

import java.io.Serializable;
import java.util.*;

/**
 * OR Node of a Policy Logic Tree
 *
 * @author Luca Ioffredo
 */
public class ORNODE implements Iterable<ANDNODE>, Deque<ANDNODE>, Serializable {

    private Deque<ANDNODE> disjuncts;

    public ORNODE() {
        this.disjuncts = new LinkedList<>();
    }

    public ORNODE(int size) {
        if (size <= 0) {
            size = 32;
        }
        this.disjuncts = new ArrayDeque<>(size);
    }

    public ORNODE(Collection<? extends ANDNODE> nodes) {
        this(nodes.size() + 1);
        this.disjuncts.addAll(nodes);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ORNODE)) {
            return false;
        }
        ORNODE cc = (ORNODE) o;
        return (cc.disjuncts == null ? this.disjuncts == null : this.disjuncts != null && cc.disjuncts.containsAll(this.disjuncts));
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for(ANDNODE node : this.disjuncts) {
            hashCode += node.hashCode();
        }
        return 53 * 3 + hashCode;
//        return 53 * 3 + Objects.hashCode(this.disjuncts);
    }

    public boolean isEmpty() {
        return this.disjuncts.isEmpty();
    }

    public int size() {
        return this.disjuncts.size();
    }

    public void clear() {
        this.disjuncts.clear();
    }

    public Deque<ANDNODE> getDisjuncts() {
        return this.disjuncts;
    }

    public boolean addTree(ANDNODE tree) {
        return tree != null && this.disjuncts.add(tree);
    }

    public boolean addTrees(Collection<? extends ANDNODE> trees) {
        return trees != null && this.disjuncts.addAll(trees);
    }

    @Override
    public Iterator<ANDNODE> iterator() {
        return this.disjuncts.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.disjuncts.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.disjuncts.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.disjuncts.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends ANDNODE> c) {
        return this.disjuncts.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.disjuncts.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.disjuncts.retainAll(c);
    }

    @Override
    public void addFirst(ANDNODE e) {
        this.disjuncts.addFirst(e);
    }

    @Override
    public void addLast(ANDNODE e) {
        this.disjuncts.addLast(e);
    }

    @Override
    public boolean offerFirst(ANDNODE e) {
        return this.disjuncts.offerFirst(e);
    }

    @Override
    public boolean offerLast(ANDNODE e) {
        return this.disjuncts.offerLast(e);
    }

    @Override
    public ANDNODE removeFirst() {
        return this.disjuncts.removeFirst();
    }

    @Override
    public ANDNODE removeLast() {
        return this.disjuncts.removeLast();
    }

    @Override
    public ANDNODE pollFirst() {
        return this.disjuncts.pollFirst();
    }

    @Override
    public ANDNODE pollLast() {
        return this.disjuncts.pollLast();
    }

    @Override
    public ANDNODE getFirst() {
        return this.disjuncts.getFirst();
    }

    @Override
    public ANDNODE getLast() {
        return this.disjuncts.getLast();
    }

    @Override
    public ANDNODE peekFirst() {
        return this.disjuncts.peekFirst();
    }

    @Override
    public ANDNODE peekLast() {
        return this.disjuncts.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return this.disjuncts.removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return this.disjuncts.removeLastOccurrence(o);
    }

    @Override
    public boolean add(ANDNODE e) {
        return this.disjuncts.add(e);
    }

    @Override
    public boolean offer(ANDNODE e) {
        return this.disjuncts.offer(e);
    }

    @Override
    public ANDNODE remove() {
        return this.disjuncts.remove();
    }

    @Override
    public ANDNODE poll() {
        return this.disjuncts.poll();
    }

    @Override
    public ANDNODE element() {
        return this.disjuncts.element();
    }

    @Override
    public ANDNODE peek() {
        return this.disjuncts.peek();
    }

    @Override
    public void push(ANDNODE e) {
        this.disjuncts.push(e);
    }

    @Override
    public ANDNODE pop() {
        return this.disjuncts.pop();
    }

    @Override
    public boolean remove(Object o) {
        return this.disjuncts.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return this.disjuncts.contains(o);
    }

    @Override
    public Iterator<ANDNODE> descendingIterator() {
        return this.disjuncts.descendingIterator();
    }
}
