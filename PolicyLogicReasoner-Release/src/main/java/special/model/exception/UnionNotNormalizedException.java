/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model.exception;

import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;

/**
 *
 * @author Luca Ioffredo
 */
public class UnionNotNormalizedException extends OWLReasonerRuntimeException {

    public UnionNotNormalizedException() {
        // TODO Auto-generated constructor stub
    }

    public UnionNotNormalizedException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public UnionNotNormalizedException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public UnionNotNormalizedException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }
}
