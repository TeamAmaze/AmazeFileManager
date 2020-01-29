package com.amaze.filemanager.filesystem.operations.exceptions;

import com.amaze.filemanager.exceptions.ShellNotRunningException;

import java.io.IOException;

public class ShellNotRunningIOException extends IOException {

	public ShellNotRunningIOException(ShellNotRunningException e) {
		super(e);
	}
}
