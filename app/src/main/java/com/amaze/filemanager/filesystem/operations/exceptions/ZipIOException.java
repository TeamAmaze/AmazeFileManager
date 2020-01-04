package com.amaze.filemanager.filesystem.operations.exceptions;

import net.lingala.zip4j.exception.ZipException;

import java.io.IOException;

public class ZipIOException extends IOException {

	public ZipIOException(ZipException e) {
		super(e);
	}
}
