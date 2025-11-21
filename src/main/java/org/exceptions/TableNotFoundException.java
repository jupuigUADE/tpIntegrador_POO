package org.exceptions;

public class TableNotFoundException extends RuntimeException {
    public TableNotFoundException(String message) { super(message); }
    public TableNotFoundException(String message, Throwable cause) { super(message, cause); }
}

