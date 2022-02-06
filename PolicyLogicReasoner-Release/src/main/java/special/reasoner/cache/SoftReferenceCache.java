/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLClass;

/**
 *
 * @author Luca Ioffredo
 */
public class SoftReferenceCache<E> implements Cache<E, E> {

    private static final long CLEAN_UP_PERIOD_IN_SEC = 5;
    private int maxCapacity = 1024;
    private final Map<E, SoftReference<Set<E>>> cache;
    private final Deque<E> queueKey;
    private final Map<E, SoftReference<Deque<E>>> mapWithQueueValues;

    public SoftReferenceCache() {
        this(1024);
    }

    public SoftReferenceCache(int capacity) {
        if (capacity <= 0) {
            capacity = 1024;
        }
        maxCapacity = capacity;
        cache = new ConcurrentHashMap<>(maxCapacity + 1, 1f);
        mapWithQueueValues = new ConcurrentHashMap<>(maxCapacity + 1, 1f);
        queueKey = new ConcurrentLinkedDeque<>();
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
        if (cache.size() >= maxCapacity && !queueKey.isEmpty()) {
            for (float i = (maxCapacity * 0.75f); i < maxCapacity && !queueKey.isEmpty(); i++) {
                E key = queueKey.removeLast();
                cache.remove(key);
                mapWithQueueValues.remove(key);
            }
        }
    }

    private void removeOldestElementFromKeyValues(@Nonnull E key, Set<E> values) {
        if (values.size() >= maxCapacity) {
            Deque<E> queue
                    = Optional
                            .ofNullable(mapWithQueueValues.get(key))
                            .map(SoftReference::get)
                            .orElse(null);
            for (float i = (maxCapacity * 0.75f); i < maxCapacity && queue != null && !queue.isEmpty(); i++) {
                values.remove(queue.removeLast());
            }
        }
    }

    @Override
    public void add(@Nonnull E key, @Nonnull E value) {
        Set<E> set
                = Optional
                        .ofNullable(cache.get(key))
                        .map(SoftReference::get)
                        .orElse(null);
        if (set != null) {
            removeOldestElementFromKeyValues(key, set);
            if (set.add(value)) {
                Optional.ofNullable(mapWithQueueValues.get(key))
                        .map(SoftReference::get)
                        .ifPresent(queue -> queue.addFirst(value));
            }
        } else {
            removeOldestKey();
            set = new LinkedHashSet<>();
            set.add(value);
            cache.put(key, new SoftReference<>(set));
            Deque<E> queue = new LinkedList<>();
            queue.addFirst(value);
            queueKey.addFirst(key);
            mapWithQueueValues.put(key, new SoftReference<>(queue));
        }
    }

    @Override
    public void add(@Nonnull E key, @Nonnull Collection<E> values) {
        for (E value : values) {
            this.add(key, value);
        }
    }

    @Override
    public void add(@Nonnull Collection<E> keys, @Nonnull Collection<E> values) {
        if (!values.isEmpty()) {
            for (E key : keys) {
                this.add(key, values);
            }
        }
    }

    @Override
    public boolean containsKey(@Nonnull E key) {
        return Optional
                .ofNullable(cache.get(key))
                .map(SoftReference::get)
                .orElse(null) != null;
    }

    @Override
    public void remove(@Nonnull E key) {
        cache.remove(key);
        mapWithQueueValues.remove(key);
        queueKey.remove(key);
    }

    @Override
    public Set<E> get(@Nonnull E key) {
        return Optional
                .ofNullable(cache.get(key))
                .map(SoftReference::get)
                .orElse(null); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * The input setA isn't modified. Return a modified copy.
     *
     * @param setA Set to analyze
     * @param key Key to check in cache
     * @return tmp Result of the operation
     */
    @Override
    public Set<E> removeCacheValuesFromSet(@Nonnull Set<E> setA, @Nonnull E key) {
        Set<E> tmp = new HashSet<>(setA);
        if (containsKey(key)) {
            Set<E> values = get(key);
            if (values != null) {
                tmp.removeAll(values);
            }
        }
        return tmp;
    }

    @Override
    public boolean isEmptyIntersection(@Nonnull Set<E> setA, @Nonnull E key) {
        if (containsKey(key)) {
            Set<E> values = get(key);
            if (values != null) {
                for (E x : setA) {
                    if (values.contains(x)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void clear() {
        cache.clear();
        queueKey.clear();
        mapWithQueueValues.clear();
    }

    @Override
    public long size() {
        return cache.size();
    }

}
