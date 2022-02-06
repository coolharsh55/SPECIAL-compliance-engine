/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.util.Collection;
import java.util.function.Function;
import special.model.ANDNODE;
import special.model.ORNODE;

/**
 *
 * @author Luca Ioffredo
 * @param <T> Key's type
 */
public interface SingleKeyCache<T> {

    public boolean put(T key, ANDNODE value);

    public boolean put(T key, Collection<? extends ANDNODE> trees);

    public boolean put(T key, ORNODE disjuncts);

    public ORNODE computeIfAbsent(T key, Function<T, ORNODE> mappingFunction);

    public ORNODE remove(T key);

    public ORNODE get(T key);

    public boolean containsKey(T key);

    public boolean isFull();

    public void close();

    public int size();

    public void clear();

    public void destroy();
}
