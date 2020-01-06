package com.amaze.filemanager.filesystem.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public abstract class AbstractOperation {
	private Operator operator;

	/* package-protected */ void setOperator(@NonNull Operator operator) {
		this.operator = operator;
	}

	protected final Operator getOperator() {
		return operator;
	}

	/**
	 * If this check passes the operation will be started,
	 * nothing here is reversable and this operation can have no side effects!
	 */
	protected abstract boolean check();

	/**
	 * The proper operation,
	 * if another operation is required to be started call requires() and it will eventually start
	 */
	protected abstract void operate() throws IOException;

	/**
	 * This must return the file system to the state before the operation was started.
	 * It must not call any "reverse()" operations, every required operation by this operation
	 * has already been reversed.
	 */
	protected abstract void undo();

	/**
	 * To do something only when an error is detected, override this function.
	 */
	@CallSuper
	protected void errorUndo(@Nullable IOException e) {
		undo();
	}
}
