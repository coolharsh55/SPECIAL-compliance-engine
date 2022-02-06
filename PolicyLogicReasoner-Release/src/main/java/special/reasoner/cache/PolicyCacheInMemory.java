/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import special.model.ANDNODE;
import special.model.ORNODE;

/**
 * Cache to save the normalized policies. A policy is mapped with a tree.
 *
 * @author Luca Ioffredo
 */
public class PolicyCacheInMemory<T> implements SingleKeyCache<T> {

    private final Map<T, ORNODE> map;

    public PolicyCacheInMemory() {
        this.map = new HashMap<>();
    }

    public PolicyCacheInMemory(int size) {
        if (size <= 0) {
            size = 32;
        }
        this.map = new HashMap<>(size);
    }

    public boolean put(T key, ANDNODE value) {
        ORNODE node = map.get(key);
        if (node == null) {
            node = new ORNODE(16);
            map.put(key, node);
        }
        return node.addTree(value);
    }

    public boolean put(T key, Collection<? extends ANDNODE> trees) {
        ORNODE node = map.get(key);
        if (node == null) {
            node = new ORNODE(trees.size());
            map.put(key, node);
        }
        return node.addTrees(trees);
    }

    public boolean put(T key, ORNODE disjuncts) {
        ORNODE node = map.get(key);
        if (node == null) {
            map.put(key, disjuncts);
            return true;
        } else {
            return node.addTrees(disjuncts.getDisjuncts());
        }
    }

    public ORNODE remove(T key) {
        return map.remove(key);
    }

    public ORNODE get(T key) {
        return map.get(key);
    }

    public boolean containsKey(T key) {
        return map.containsKey(key);
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        this.map.clear();
    }

    @Override
    public ORNODE computeIfAbsent(T key, Function<T, ORNODE> mappingFunction) {
        return this.map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public void destroy() {
        this.map.clear();
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public void close() {
        clear();
    }
}
