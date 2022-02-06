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
import java.util.function.Function;
import special.model.ANDNODE;
import special.model.ORNODE;

/**
 * Cache to save the normalized policies. A policy is mapped with a tree.
 *
 * @author Luca Ioffredo
 */
public class PolicyCacheInMemoryLimited<T> implements SingleKeyCache<T> {

    private static final long CLEAN_UP_PERIOD_IN_SEC = 5;
    private int maxCapacity = 1024;
    private final Map<T, SoftReference<ORNODE>> map;
    private final Deque<T> queueKey;

    public PolicyCacheInMemoryLimited() {
        this.maxCapacity = Integer.MAX_VALUE;
        this.map = new ConcurrentHashMap<>();
        this.queueKey = new ConcurrentLinkedDeque<>();
    }

    public PolicyCacheInMemoryLimited(int size) {
        if (size <= 0) {
            size = 1024;
        }
        this.maxCapacity = size;
        this.map = new ConcurrentHashMap<>(size);
        this.queueKey = new ConcurrentLinkedDeque<>();
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
    public boolean put(T key, ANDNODE value) {
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
    public boolean put(T key, Collection<? extends ANDNODE> trees) {
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
    public boolean put(T key, ORNODE disjuncts) {
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
    public ORNODE remove(T key) {
        this.queueKey.remove(key);
        return Optional
                .ofNullable(map.remove(key))
                .map(SoftReference::get)
                .orElse(null);
    }

    @Override
    public ORNODE get(T key) {
        return Optional
                .ofNullable(map.get(key))
                .map(SoftReference::get)
                .orElse(null);
    }

    @Override
    public boolean containsKey(T key) {
        return map.containsKey(key);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void clear() {
        this.queueKey.clear();
        this.map.clear();
    }

    @Override
    public ORNODE computeIfAbsent(T key, Function<T, ORNODE> mappingFunction) {
        ORNODE node = Optional
                .ofNullable(map.get(key))
                .map(SoftReference::get)
                .orElse(null);
        if (node == null) {
            node = mappingFunction.apply(key);
            this.map.put(key, new SoftReference<>(node));
        }
        return node;
    }

    @Override
    public void close() {
        clear();
    }

    @Override
    public void destroy() {
        this.queueKey.clear();
        this.map.clear();
    }

    @Override
    public boolean isFull() {
        return this.map.size() >= this.maxCapacity;
    }
}
