/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.util.*;
import javax.annotation.Nonnull;

/**
 *
 * @author Luca Ioffredo
 * @param <T> T as Key of the cache
 * @param <E> E as Value of the T key
 */
public interface Cache<T, E> {

    void add(@Nonnull T key, @Nonnull E value);

    void add(@Nonnull T key, @Nonnull Collection<E> values);

    void add(@Nonnull Collection<T> keys, @Nonnull Collection<E> values);

    boolean containsKey(@Nonnull T key);

    void remove(@Nonnull T key);

    Set<E> get(@Nonnull T key);

    /**
     * Remove all values contained in the cache from the Set specified.
     *
     * @param setA Set where to remove the values contained in the cache.
     * @param key T that rapresents the key in the cache.
     * @return setA or difference between setA and the values from cache.
     */
    public Set<E> removeCacheValuesFromSet(Set<E> setA, T key);

    /**
     * False otherwise.
     *
     * @param setA Set to analyze
     * @param key T that rapresents the key in the cache.
     * @return true if the intersection between setA and the set values in the
     * cache is empty.
     */
    public boolean isEmptyIntersection(Set<E> setA, T key);

    void clear();

    long size();
}
