package com.amaze.filemanager.filesystem.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * Any task that needs an undo can be encapsulated with an AbstractOperation,
 *  this, with the Operator, allows for easy sequencing of recursive operations,
 *  by just calling requires()
 */
public abstract class AbstractOperation {
	private Operator operator;

	/**
	 * To be used only by the Operator for dependency injection
	 */
	/* package-protected */ void setOperator(@NonNull Operator operator) {
		this.operator = operator;
	}

	private Operator getOperator() {
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
	 * It must not call any "reverse()" operations.
	 * It is assured that every required AbstractOperation by this AbstractOperation
	 *  has already been reversed.
	 */
	protected abstract void undo();

	/**
	 * To do something only when an error is detected, override this function.
	 */
	@CallSuper
	protected void errorUndo(@Nullable IOException e) {
		undo();
	}

	/**
	 * Adds a required AbstractOperation to the Operator running this AbstractOperation,
	 *  it is guaranteed to be run, in the future.
	 */
	protected void requires(AbstractOperation operation) {
		getOperator().addRequiredOperation(operation);
	}
}
