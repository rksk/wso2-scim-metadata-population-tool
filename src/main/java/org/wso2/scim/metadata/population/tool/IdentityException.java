package org.wso2.scim.metadata.population.tool;

public class IdentityException extends Exception {
    public IdentityException(String msg) {
        super(msg);
    }
    public IdentityException(String msg, Throwable e) {
        super(msg, e);
    }
}
