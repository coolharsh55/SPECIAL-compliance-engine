/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
public class IntervalSafePoliciesCacheInMemoryLimited<T1, T2> implements DoubleKeyCache<T1, T2, ANDNODE> {

    private static final long CLEAN_UP_PERIOD_IN_SEC = 5;
    private int maxCapacity = 1024;
    private final Map<DoubleKey<T1,T2>, SoftReference<ORNODE>> map;
    private final Deque<DoubleKey<T1,T2>> queueKey;

    public IntervalSafePoliciesCacheInMemoryLimited() {
        this.map = new ConcurrentHashMap<>();
        this.queueKey = new ConcurrentLinkedDeque<>();
        this.maxCapacity = Integer.MAX_VALUE;
    }

    public IntervalSafePoliciesCacheInMemoryLimited(int size) {
        if (size <= 0) {
            size = 1024;
        }
        this.map = new ConcurrentHashMap<>(size);
        this.queueKey = new ConcurrentLinkedDeque<>();
        this.maxCapacity = size;
        Thread cleanerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(CLEAN_UP_PERIOD_IN_SEC * 1000);
                        removeOldestKey();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    private void removeOldestKey() {
        if (map.size() >= maxCapacity && !queueKey.isEmpty()) {
            for (float i = (maxCapacity * 0.75f); i < maxCapacity && !queueKey.isEmpty(); i++) {
                map.remove(queueKey.removeLast());
            }
        }
    }

    @Override
    public boolean put(T1 key1, T2 key2, ANDNODE value) {
        DoubleKey<T1, T2> key = new DoubleKey(key1, key2);
        ORNODE node = Optional
                .ofNullable(map.get(key))
                .map(SoftReference::get)
                .orElse(null);
        if (node == null) {
            removeOldestKey();
            node = new ORNODE();
            map.put(key, new SoftReference<>(node));
        }
        return node.addTree(value);
    }

    @Override
    public boolean put(T1 key1, T2 key2, Collection<? extends ANDNODE> trees) {
        DoubleKey<T1, T2> key = new DoubleKey(key1, key2);
        ORNODE node = Optional
                .ofNullable(map.get(key))
                .map(SoftReference::get)
                .orElse(null);
        if (node == null) {
            removeOldestKey();
            node = new ORNODE();
            map.put(key, new SoftReference<>(node));
        }
        return node.addTrees(trees);
    }

    @Override
    public boolean put(T1 key1, T2 key2, ORNODE disjuncts) {
        DoubleKey<T1, T2> key = new DoubleKey(key1, key2);
        ORNODE node = Optional
                .ofNullable(map.get(key))
                .map(SoftReference::get)
                .orElse(null);
        if (node == null) {
            removeOldestKey();
            map.put(key, new SoftReference<>(disjuncts));
            return true;
        } else {
            return node.addTrees(disjuncts.getDisjuncts());
        }
    }

    @Override
    public ORNODE remove(T1 key1, T2 key2) {
        DoubleKey<T1, T2> key = new DoubleKey(key1, key2);
        queueKey.remove(key);
        return Optional
                .ofNullable(map.remove(key))
                .map(SoftReference::get)
                .orElse(null);
    }

    @Override
    public ORNODE get(T1 key1, T2 key2) {
        DoubleKey<T1, T2> key = new DoubleKey(key1, key2);
        return Optional
                .ofNullable(map.get(key))
                .map(SoftReference::get)
                .orElse(null);
    }

    @Override
    public boolean containsKey(T1 key1, T2 key2) {
        DoubleKey<T1, T2> key = new DoubleKey(key1, key2);
        return map.containsKey(key);
    }

    @Override
    public boolean isFull() {
        return map.size() >= maxCapacity;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void close() {
        clear();
    }

    @Override
    public void clear() {
        this.map.clear();
        this.queueKey.clear();
    }

    @Override
    public void destroy() {
        this.map.clear();
        this.queueKey.clear();
    }
}
