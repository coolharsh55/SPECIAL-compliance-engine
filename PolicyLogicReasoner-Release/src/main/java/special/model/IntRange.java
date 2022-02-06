/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model;

/**
 *
 * @author Luca Ioffredo
 */
public class IntRange {

    private final int min;
    private final int max;

    public IntRange(final int min, final int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IntRange)) {
            return false;
        }
        IntRange cc = (IntRange) o;
        return cc.max == this.max && cc.min == this.min;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.min;
        hash = 97 * hash + this.max;
        return hash;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public boolean isIncluseIn(final IntRange i) {
        if (i != null) {
            return i.min <= this.min && this.max <= i.max;
        }
        return false;
    }
    
    public boolean include(final IntRange i) {
        if(i != null) {
            return this.min <= i.min && i.max <= this.max;
        }
        return false;
    }
}
