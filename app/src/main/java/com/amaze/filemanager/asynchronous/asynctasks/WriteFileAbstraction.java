package com.amaze.filemanager.asynchronous.asynctasks;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;

import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.exceptions.StreamNotFoundException;
import com.amaze.filemanager.filesystem.EditableFileAbstraction;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.RootUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 16/1/2018, at 18:36.
 */

public class WriteFileAbstraction extends AsyncTask<Void, String, Integer> {

    public static final int NORMAL = 0;
    public static final int EXCEPTION_STREAM_NOT_FOUND = -1;
    public static final int EXCEPTION_IO = -2;
    public static final int EXCEPTION_SHELL_NOT_RUNNING = -3;

    private WeakReference<Context> context;
    private ContentResolver contentResolver;
    private EditableFileAbstraction fileAbstraction;
    private File cachedFile;
    private boolean isRootExplorer;
    private OnAsyncTaskFinished<Integer> onAsyncTaskFinished;

    private String dataToSave;

    public WriteFileAbstraction(Context context, ContentResolver contentResolver,
                                EditableFileAbstraction file, String dataToSave, File cachedFile,
                                boolean isRootExplorer,
                                OnAsyncTaskFinished<Integer> onAsyncTaskFinished) {
        this.context = new WeakReference<>(context);
        this.contentResolver = contentResolver;
        this.fileAbstraction = file;
        this.cachedFile = cachedFile;
        this.dataToSave = dataToSave;
        this.isRootExplorer = isRootExplorer;
        this.onAsyncTaskFinished = onAsyncTaskFinished;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            OutputStream outputStream;

            switch (fileAbstraction.scheme) {
                case EditableFileAbstraction.SCHEME_CONTENT:
                    if(fileAbstraction.uri == null) throw new NullPointerException("Something went really wrong!");

                    try {
                        outputStream = contentResolver.openOutputStream(fileAbstraction.uri);
                    } catch (RuntimeException e) {
                        throw new StreamNotFoundException(e);
                    }

                    break;
                case EditableFileAbstraction.SCHEME_FILE:
                    final HybridFileParcelable hybridFileParcelable = fileAbstraction.hybridFileParcelable;
                    if(hybridFileParcelable == null) throw new NullPointerException("Something went really wrong!");

                    Context context = this.context.get();
                    if(context == null) { cancel(true); return null; }
                    outputStream = FileUtil.getOutputStream(hybridFileParcelable.getFile(), context);

                    if (isRootExplorer && outputStream == null) {
                        // try loading stream associated using root
                        try {
                            if (cachedFile != null && cachedFile.exists()){
                                outputStream = new FileOutputStream(cachedFile);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            outputStream = null;
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("The scheme for '" + fileAbstraction.scheme + "' cannot be processed!");
            }

            if(outputStream == null) throw new StreamNotFoundException();

            outputStream.write(dataToSave.getBytes());
            outputStream.close();

            if (cachedFile != null && cachedFile.exists()) {
                // cat cache content to original file and delete cache file
                RootUtils.cat(cachedFile.getPath(), fileAbstraction.hybridFileParcelable.getPath());

                cachedFile.delete();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return EXCEPTION_IO;
        } catch (StreamNotFoundException e) {
            e.printStackTrace();
            return EXCEPTION_STREAM_NOT_FOUND;
        } catch (ShellNotRunningException e) {
            e.printStackTrace();
            return EXCEPTION_SHELL_NOT_RUNNING;
        }

        return NORMAL;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);

        onAsyncTaskFinished.onAsyncTaskFinished(integer);
    }
}