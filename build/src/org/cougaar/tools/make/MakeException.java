package org.cougaar.tools.make;

public class MakeException extends Exception {
    public MakeException(String msg) {
	super(msg);
        System.err.println(msg);
    }
}
