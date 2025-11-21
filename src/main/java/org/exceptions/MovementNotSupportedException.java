package org.exceptions;

public class MovementNotSupportedException extends RuntimeException {
    public MovementNotSupportedException(String message) { super(message); }
    public MovementNotSupportedException(String message, Throwable cause) { super(message, cause); }
}

