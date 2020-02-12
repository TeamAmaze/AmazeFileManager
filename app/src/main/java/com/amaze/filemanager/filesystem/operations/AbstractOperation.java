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
 * Any task that needs an undo can be encapsulated with an {@link AbstractOperation},
 * this, with the {@link Operator}, allows for easy sequencing of recursive operations,
 * by just calling {@link AbstractOperation#requires}
 */
public abstract class AbstractOperation {
    private Operator operator;

    /**
     * To be used only by the {@link Operator} for dependency injection
     */
    /* package-protected */ void setOperator(@NonNull Operator operator) {
        this.operator = operator;
    }

    private Operator getOperator() {
        return operator;
    }

    /**
     * If this check passes the {@link AbstractOperation} will be executed,
     * nothing here is reversable and this function can have no side effects!
     */
    protected abstract boolean check();

    /**
     * The proper operation,
     * if another operation is required to be started call {@link AbstractOperation#requires}
     * and it will eventually start (in the future)
     */
    protected abstract void execute() throws IOException;

    /**
     * This must return the file system to the state before the operation was started.
     * It must not call any "{@link AbstractOperation#requires}" operations.
     * It is assured that every required {@link AbstractOperation} by this {@link AbstractOperation}
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

    /**
     * Adds a required {@link AbstractOperation} to the {@link Operator}
     * running this {@link AbstractOperation}, it is guaranteed to be run, in the future.
     */
    protected void requires(AbstractOperation operation) {
        getOperator().addRequiredOperation(operation);
    }
}
