package com.amaze.filemanager.filesystem.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public abstract class AbstractOperation {
	private boolean started;
	private boolean failed;
	private final List<AbstractOperation> requiredOperations = new ArrayList<>();

	public AbstractOperation() {
		requiredOperations.add(this);
	}

	@WorkerThread
	public boolean start() {
		if (started) {
			throw new IllegalStateException("Operations cannot be run twice!");
		}

		for (AbstractOperation operation : requiredOperations) {
			if(!operation.check()) {
				revert(new IOException("Check for " + operation + "did not succeed!"));
			}

			operation.started = true;

			try {
				operation.operate();
			} catch (IOException e) {
				operation.failed = true;
				revert(e);
			}

		}

		return true;
	}

	@WorkerThread
	private void revert(@Nullable IOException e) {
		for (int i = requiredOperations.size()-1; i >= 0; i--) {
			AbstractOperation operation = requiredOperations.get(i);

			if (!operation.started) {
				continue;
			}

			if (e == null) {
				operation.undo();
			} else {
				operation.errorUndo(e);
			}
		}
	}

	public boolean hasFailed() {
		return failed;
	}

	protected final void requires(AbstractOperation operation) {
		requiredOperations.add(operation);
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
