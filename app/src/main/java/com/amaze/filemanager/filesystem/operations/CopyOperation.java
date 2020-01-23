package com.amaze.filemanager.filesystem.operations;

import android.content.Context;
import android.util.Log;

import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.operations.exceptions.CancellationIOException;
import com.amaze.filemanager.filesystem.operations.exceptions.ShellNotRunningIOException;
import com.amaze.filemanager.filesystem.operations.singlefile.DeleteOperation;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;

public class CopyOperation extends AbstractOperation {
    private final CopyService copyService;
    private final Context context;
    private final ServiceWatcherUtil watcherUtil;
    private final ProgressHandler progressHandler;
    private final List<HybridFileParcelable> sourceFiles;
    private final String targetPath;
    private final boolean move;
    private final OpenMode mode;
    private final boolean isRootExplorer;

    private int sourceProgress = 0;
    private ArrayList<HybridFile> failedFOps;

    /**
     * @param mode target file open mode (current path's open mode)
     */
    public CopyOperation(CopyService copyService, ServiceWatcherUtil watcherUtil,
                         ProgressHandler progressHandler,
                         List<HybridFileParcelable> sourceFiles, String targetPath,
                         boolean move, OpenMode mode, boolean isRootExplorer) {
        this.copyService = copyService;
        this.context = copyService;
        this.watcherUtil = watcherUtil;
        this.progressHandler = progressHandler;
        this.sourceFiles = sourceFiles;
        this.targetPath = targetPath;
        this.move = move;
        this.mode = mode;
        this.isRootExplorer = isRootExplorer;

        failedFOps = new ArrayList<>();
    }


    @Override
    protected boolean check() {
        return true;
    }

    @Override
    protected void operate() throws IOException {
        // initial start of copy, initiate the watcher
        watcherUtil.watch(copyService);

        if (FileUtil.checkFolder((targetPath), context) == 1) {
            for (int i = 0; i < sourceFiles.size(); i++) {
                sourceProgress = i;
                HybridFileParcelable f1 = (sourceFiles.get(i));

                try {
                    HybridFile hFile;
                    if (targetPath.contains(context.getExternalCacheDir().getPath())) {
                        // the target open mode is not the one we're currently in!
                        // we're processing the file for cache
                        hFile = new HybridFile(OpenMode.FILE, targetPath, sourceFiles.get(i).getName(),
                                f1.isDirectory());
                    } else {

                        // the target open mode is where we're currently at
                        hFile = new HybridFile(mode, targetPath, sourceFiles.get(i).getName(),
                                f1.isDirectory());
                    }

                    if (progressHandler.getCancelled()) {
                        throw new CancellationIOException();
                    }

                    if ((f1.getMode() == OpenMode.ROOT || mode == OpenMode.ROOT)
                            && isRootExplorer) {
                        // either source or target are in root
                        Log.d(getClass().getSimpleName(), "either source or target are in root");
                        progressHandler.setSourceFilesProcessed(++sourceProgress);
                        copyRoot(f1, hFile, move);
                        continue;
                    }
                    progressHandler.setSourceFilesProcessed(++sourceProgress);
                    copyFiles((f1), hFile, progressHandler);
                } catch (IllegalStateException | IOException e) {
                    for (int j = i; j < sourceFiles.size(); j++)
                        failedFOps.add(sourceFiles.get(j));

                    throw e;
                }
            }

        } else if (isRootExplorer) {
            for (int i = 0; i < sourceFiles.size(); i++) {
                if (!progressHandler.getCancelled()) {
                    HybridFile hFile = new HybridFile(mode, targetPath, sourceFiles.get(i).getName(),
                            sourceFiles.get(i).isDirectory());
                    progressHandler.setSourceFilesProcessed(++sourceProgress);
                    progressHandler.setFileName(sourceFiles.get(i).getName());
                    copyRoot(sourceFiles.get(i), hFile, move);

                    /*
                    if(checkFiles(new HybridFile(sourceFiles.get(i).getMode(), path),
                            new HybridFile(OpenMode.ROOT,targetPath+"/"+name))) {
                        failedFOps.add(sourceFiles.get(i));
                    }
                    */
                }
            }
        } else {
            failedFOps.addAll(sourceFiles);
            throw new IOException("Copy is impossible!");
        }

        // making sure to delete files after copy operation is done
        // and not if the copy was cancelled
        if (move && !progressHandler.getCancelled()) {
            for (HybridFileParcelable a : sourceFiles) {
                if (!failedFOps.contains(a)) {
                    DeleteOperation delete = new DeleteOperation(context, isRootExplorer, a);
                    requires(delete);
                }
            }
        }
    }

    void copyRoot(HybridFileParcelable sourceFile, HybridFile targetFile, boolean move) throws ShellNotRunningIOException {
        try {
            if (!move) {
                RootUtils.copy(sourceFile.getPath(), targetFile.getPath());
            } else {
                RootUtils.move(sourceFile.getPath(), targetFile.getPath());
            }

            ServiceWatcherUtil.position += sourceFile.getSize();
        } catch (ShellNotRunningException e) {
            e.printStackTrace();
            failedFOps.add(sourceFile);
            throw new ShellNotRunningIOException(e);
        }

        FileUtils.scanFile(targetFile.getFile(), context);
    }

    private void copyFiles(final HybridFileParcelable sourceFile, final HybridFile targetFile,
                           final ProgressHandler progressHandler) throws IllegalStateException, IOException {

        if (progressHandler.getCancelled()) return;
        if (sourceFile.isDirectory()) {
            if (!targetFile.exists()) targetFile.mkdir(context);

            // various checks
            // 1. source file and target file doesn't end up in loop
            // 2. source file has a valid name or not
            if (!Operations.isFileNameValid(sourceFile.getName())
                    || Operations.isCopyLoopPossible(sourceFile, targetFile)) {
                throw new IOException("File name invalid: " + sourceFile.getName() + "!");
            }
            targetFile.setLastModified(sourceFile.lastModified());

            if (progressHandler.getCancelled()) throw new CancellationIOException();

            sourceFile.forEachChildrenFile(context, false, file -> {
                CopyOperation childOperation = new CopyOperation(copyService, watcherUtil,
                        progressHandler, Collections.singletonList(file), targetFile.getPath(), move,
                        targetFile.getMode(), isRootExplorer);
                requires(childOperation);
            });
        } else {
            if (!Operations.isFileNameValid(sourceFile.getName())) {
                throw new IOException("File name invalid: " + sourceFile.getName() + "!");
            }

            GenericCopyUtil copyUtil = new GenericCopyUtil(context, progressHandler);

            progressHandler.setFileName(sourceFile.getName());
            copyUtil.copy(sourceFile, targetFile);
        }
    }

    @Override
    protected void undo() {
        Operator operator = new Operator(new DeleteOperation(context, isRootExplorer, new HybridFileParcelable(targetPath)));
        operator.setDoNothingOnRevert(true);
        operator.start();
    }

    public ArrayList<HybridFile> getFailedFiles() {
        return failedFOps;
    }

    //check if copy is successful
    // avoid using the method as there is no way to know when we would be returning from command callbacks
    // rather confirm from the command result itself, inside it's callback
    private boolean checkFiles(HybridFile hFile1, HybridFile hFile2) throws ShellNotRunningException {
        if (RootHelper.isDirectory(hFile1.getPath(), isRootExplorer, 5)) {
            if (RootHelper.fileExists(hFile2.getPath())) return false;
            ArrayList<HybridFileParcelable> baseFiles = RootHelper.getFilesList(hFile1.getPath(), true, true, null);
            if (baseFiles.size() > 0) {
                boolean b = true;
                for (HybridFileParcelable baseFile : baseFiles) {
                    if (!checkFiles(new HybridFile(baseFile.getMode(), baseFile.getPath()),
                            new HybridFile(hFile2.getMode(), hFile2.getPath() + "/" + (baseFile.getName()))))
                        b = false;
                }
                return b;
            }
            return RootHelper.fileExists(hFile2.getPath());
        } else {
            ArrayList<HybridFileParcelable> baseFiles = RootHelper.getFilesList(hFile1.getParent(), true, true, null);
            int i = -1;
            int index = -1;
            for (HybridFileParcelable b : baseFiles) {
                i++;
                if (b.getPath().equals(hFile1.getPath())) {
                    index = i;
                    break;
                }
            }
            ArrayList<HybridFileParcelable> baseFiles1 = RootHelper.getFilesList(hFile1.getParent(), true, true, null);
            int i1 = -1;
            int index1 = -1;
            for (HybridFileParcelable b : baseFiles1) {
                i1++;
                if (b.getPath().equals(hFile1.getPath())) {
                    index1 = i1;
                    break;
                }
            }

            return baseFiles.get(index).getSize() == baseFiles1.get(index1).getSize();
        }
    }

    @Override
    protected void errorUndo(@Nullable IOException e) {
        if (e != null) {
            e.printStackTrace();
        }
        super.errorUndo(e);
    }
}
