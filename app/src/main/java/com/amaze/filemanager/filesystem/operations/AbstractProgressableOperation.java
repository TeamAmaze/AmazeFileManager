package com.amaze.filemanager.filesystem.operations;

import com.amaze.filemanager.utils.OnProgressUpdate;

public abstract class AbstractProgressableOperation<T> extends AbstractOperation {

	protected final OnProgressUpdate<T> onProgressUpdate;

	public AbstractProgressableOperation(OnProgressUpdate<T> onProgressUpdate) {
		super();
		this.onProgressUpdate = onProgressUpdate;
	}

	protected void updateProgress(T newProgress) {
		onProgressUpdate.onUpdate(newProgress);
	}
}
