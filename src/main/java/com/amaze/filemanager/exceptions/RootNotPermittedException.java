package com.amaze.filemanager.exceptions;

/**
 * Created by vishal on 24/12/16.
 * Exception thrown when root is
 */

public class RootNotPermittedException extends Exception {

    private static final String MESSAGE = "Exception thrown when root is";

    public RootNotPermittedException() { super(MESSAGE); }
    public RootNotPermittedException(String message) { super(message); }
    public RootNotPermittedException(String message, Throwable cause) { super(message, cause); }
    public RootNotPermittedException(Throwable cause) { super(MESSAGE, cause); }

}
