package com.amaze.filemanager.filesystem.operations;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.operations.exceptions.CancellationIOException;
import com.amaze.filemanager.filesystem.operations.singlefile.DeleteOperation;
import com.amaze.filemanager.utils.OpenMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;
import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR_CHAR;

public abstract class AbstractExtractOperation extends AbstractOperation {

    protected Context context;
    protected String filePath, outputPath;
    protected AbstractExtractOperation.OnUpdate listener;
    private final Filter filter;
    protected List<String> invalidArchiveEntries;

    public AbstractExtractOperation(@NonNull Context context, @NonNull String filePath,
                                    @NonNull String outputPath,
                                    @NonNull OnUpdate listener,
                                    Filter filter) {
        this.context = context;
        this.filePath = filePath;
        this.outputPath = outputPath;
        this.listener = listener;
        this.filter = filter;
        this.invalidArchiveEntries = new ArrayList<>();
    }

    @Override
    protected boolean check() {
        return true;
    }

    /**
     * The proper operation,
     * if another operation is required to be started call requires() and it will eventually start
     */
    protected final void execute() throws IOException {
        extractWithFilter(filter);
    }

    protected void undo() {
        AbstractOperation operation = new DeleteOperation(context, false, new HybridFile(OpenMode.UNKNOWN, outputPath));
        Operator operator = new Operator(operation);
        operator.setDoNothingOnRevert(true);
        operator.start();
    }

    public List<String> getInvalidArchiveEntries() {
        return invalidArchiveEntries;
    }

    protected abstract void extractWithFilter(@NonNull AbstractExtractOperation.Filter filter) throws IOException;

    protected final void crashOnCancelled() throws CancellationIOException {
        if (listener.isCancelled()) {
            throw new CancellationIOException();
        }
    }

    public interface Filter {
        boolean shouldExtract(String relativePath, boolean isDirectory);
    }

    public interface OnUpdate {
        void onStart(long totalBytes, String firstEntryName);

        void onUpdate(String entryPath);

        void onFinish();

        boolean isCancelled();
    }

    public static String fixEntryName(String entryName) {
        if (entryName.indexOf('\\') >= 0) {
            return fixEntryName(entryName.replaceAll("\\\\", SEPARATOR));
        } else if (entryName.indexOf(SEPARATOR_CHAR) == 0) {
            //if entryName starts with "/" (e.g. "/test.txt"), strip the prefixing "/"s
            return entryName.replaceAll("^/+", "");
        } else {
            return entryName;
        }
    }
}
