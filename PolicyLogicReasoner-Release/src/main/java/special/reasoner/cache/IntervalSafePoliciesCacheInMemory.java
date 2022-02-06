/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import special.model.ANDNODE;
import special.model.ORNODE;

/**
 * Cache to save each couple of normalized interval safe policies.A policy
 interval-safe is mapped with two policies as a key.
 *
 * @author Luca Ioffredo
 * @param <T1> First key's type
 * @param <T2> Second key's type
 */
public class IntervalSafePoliciesCacheInMemory<T1, T2> implements DoubleKeyCache<T1, T2, ANDNODE> {

    private final Map<DoubleKey<T1,T2>, ORNODE> map;

    public IntervalSafePoliciesCacheInMemory() {
        this.map = new HashMap<>();
    }

    public IntervalSafePoliciesCacheInMemory(int size) {
        if (size <= 0) {
            size = 32;
        }
        this.map = new HashMap<>(size);
    }

    public boolean put(T1 key1, T2 key2, ANDNODE value) {
        DoubleKey<T1,T2> key = new DoubleKey(key1, key2);
        ORNODE node = map.computeIfAbsent(key, k -> new ORNODE());
        return node.addTree(value);
    }

    public boolean put(T1 key1, T2 key2, Collection<? extends ANDNODE> trees) {
        DoubleKey<T1,T2> key = new DoubleKey(key1, key2);
        ORNODE node = map.computeIfAbsent(key, k -> new ORNODE());
        return node.addTrees(trees);
    }

    public boolean put(T1 key1, T2 key2, ORNODE disjuncts) {
        DoubleKey<T1,T2> key = new DoubleKey(key1, key2);
        ORNODE node = map.get(key);
        if (node == null) {
            map.put(key, disjuncts);
            return true;
        } else {
            return node.addTrees(disjuncts.getDisjuncts());
        }
    }

    public ORNODE remove(T1 key1, T2 key2) {
        DoubleKey<T1,T2> key = new DoubleKey(key1, key2);
        return map.remove(key);
    }

    public ORNODE get(T1 key1, T2 key2) {
        DoubleKey<T1,T2> key = new DoubleKey(key1, key2);
        return map.get(key);
    }

    public boolean containsKey(T1 key1, T2 key2) {
        DoubleKey<T1,T2> key = new DoubleKey(key1, key2);
        return map.containsKey(key);
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        this.map.clear();
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
