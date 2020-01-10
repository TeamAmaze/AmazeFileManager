package com.amaze.filemanager.filesystem.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public final class Operator {
	private boolean started;
	private boolean failed;
	private final List<AbstractOperation> requiredOperations = new ArrayList<>();

	public Operator(AbstractOperation operation) {
		operation.setOperator(this);
		requiredOperations.add(operation);
	}

	@WorkerThread
	public void start() {
		if (started) {
			throw new IllegalStateException("Operations cannot be run twice!");
		}

		for (int i = 0; i < requiredOperations.size(); i++) {// DO NOT USE FOREACH, see https://stackoverflow.com/a/11177393/3124150
			AbstractOperation operation = requiredOperations.get(i);

			if(!operation.check()) {
				revert(new IOException("Check for " + operation + "did not succeed!"));
				return;
			}

			started = true;

			try {
				operation.operate();
			} catch (IOException e) {
				failed = true;
				revert(e);
				return;
			}
		}
	}

	@WorkerThread
	private void revert(@Nullable IOException e) {
		for (int i = requiredOperations.size()-1; i >= 0; i--) {
			AbstractOperation operation = requiredOperations.get(i);

			if (!started) {
				continue;
			}

			if (e == null) {
				operation.undo();
			} else {
				operation.errorUndo(e);
			}
		}
	}

	public void addRequiredOperation(AbstractOperation operation) {
		requiredOperations.add(operation);
	}

	public boolean hasFailed() {
		return failed;
	}

}
