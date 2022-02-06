/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

/**
 *
 * @author Luca Ioffredo
 */
public class WrapperBoolean {

    private boolean value;

    public WrapperBoolean(boolean v) {
        this.value = v;
    }

    public boolean getValue() {
        return this.value;
    }

    public void setValue(boolean v) {
        this.value = v;
    }
}
