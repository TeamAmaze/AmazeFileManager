package com.amaze.filemanager.exceptions;

/**
 * Created by vishal on 24/12/16.
 * Exception thrown when root is
 */

public class ShellNotRunningException extends Exception {
    public ShellNotRunningException() {
        super("Shell stopped running!");
    }
}
