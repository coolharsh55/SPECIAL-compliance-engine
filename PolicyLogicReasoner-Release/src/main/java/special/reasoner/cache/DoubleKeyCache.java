/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.util.Collection;
import special.model.ANDNODE;
import special.model.ORNODE;

/**
 *
 * @author Luca Ioffredo
 * @param <T> Key of the cache
 * @param <E> Value's type of the Key
 */
public interface DoubleKeyCache<T1, T2, E> {

    public boolean put(T1 key1, T2 key2, E value);

    public boolean put(T1 key1, T2 key2, Collection<? extends E> trees);

    public boolean put(T1 key1, T2 key2, ORNODE disjuncts);

    public ORNODE remove(T1 key1, T2 key2);

    public ORNODE get(T1 key1, T2 key2);

    public boolean containsKey(T1 key1, T2 key2);

    public boolean isFull();

    public void close();

    public int size();

    public void clear();

    public void destroy();
}
