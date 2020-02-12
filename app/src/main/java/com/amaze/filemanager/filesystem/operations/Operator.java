package com.amaze.filemanager.filesystem.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

/**
 * Handles and runs {@link AbstractOperation}s
 */
public final class Operator {
    private boolean started;
    private boolean failed;
    private boolean doNothingOnRevert;
    private final List<AbstractOperation> requiredOperations = new ArrayList<>();

    /**
     * Creates and operator with an {@link AbstractOperation} as its first required operation
     */
    public Operator(AbstractOperation operation) {
        operation.setOperator(this);
        requiredOperations.add(operation);
    }

    /**
     * Starts the {@link AbstractOperation} added as required for this Operator
     * Should not be run on main thread
     */
    @WorkerThread
    public void start() {
        if (started) {
            throw new IllegalStateException("Operations cannot be run twice!");
        }

        for (int i = 0; i < requiredOperations.size(); i++) {// DO NOT USE FOREACH, see https://stackoverflow.com/a/11177393/3124150
            AbstractOperation operation = requiredOperations.get(i);

            if (!operation.check()) {
                revert(new IOException("Check for " + operation + "did not succeed!"));
                return;
            }

            started = true;

            try {
                operation.execute();
            } catch (IOException e) {
                failed = true;
                revert(e);
                return;
            }
        }
    }

    /**
     * Starts reversal of every {@link AbstractOperation}, in reverse.
     *
     * If {@link Operator#doNothingOnRevert} this function does nothing
     */
    @WorkerThread
    private void revert(@Nullable IOException e) {
        if (doNothingOnRevert) {
            return;
        }

        for (int i = requiredOperations.size() - 1; i >= 0; i--) {
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

    /**
     * Adds a required {@link AbstractOperation} to this Operator,
     * it is guaranteed to be run, in the future.
     */
    public void addRequiredOperation(AbstractOperation operation) {
        requiredOperations.add(operation);
        operation.setOperator(this);
    }

    public boolean hasFailed() {
        return failed;
    }

    /**
     * {@link Operator#revert(IOException)} does nothing
     */
    public void setDoNothingOnRevert(boolean doNothingOnRevert) {
        this.doNothingOnRevert = doNothingOnRevert;
    }

}
