/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DoubleKey class used to map two values of T in a Map. x and y are the key.
 *
 * @param <T>
 * @author Luca Ioffredo
 */
public class DoubleKey<T,E> implements Serializable {

    private final T x;
    private final E y;

    public DoubleKey(T x, E y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoubleKey)) {
            return false;
        }
        DoubleKey otherKey = (DoubleKey) o;
        return x.equals(otherKey.x) && y.equals(otherKey.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
