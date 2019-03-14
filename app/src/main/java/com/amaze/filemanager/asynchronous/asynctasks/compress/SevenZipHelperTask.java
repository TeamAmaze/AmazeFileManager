package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZArchiveEntry;
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZFile;

import org.apache.commons.compress.PasswordRequiredException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

public class SevenZipHelperTask extends CompressedHelperTask {

    private String filePath, relativePath;

    private String password;

    private boolean paused = false;

    public SevenZipHelperTask(String filePath, String relativePath, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    @Override
    void addElements(@NonNull ArrayList<CompressedObjectParcelable> elements) {
        while(true) {
            if (paused) continue;

            SevenZFile sevenzFile = null;
            try {
                sevenzFile = (password != null) ?
                        new SevenZFile(new File(filePath), password.toCharArray()) :
                        new SevenZFile(new File(filePath));

                for (SevenZArchiveEntry entry : sevenzFile.getEntries()) {
                    String name = entry.getName();
                    boolean isInBaseDir = relativePath.equals("") && !name.contains(SEPARATOR);
                    boolean isInRelativeDir = name.contains(SEPARATOR)
                            && name.substring(0, name.lastIndexOf(SEPARATOR)).equals(relativePath);

                    if (isInBaseDir || isInRelativeDir) {
                        elements.add(new CompressedObjectParcelable(entry.getName(),
                                entry.getLastModifiedDate().getTime(), entry.getSize(), entry.isDirectory()));
                    }
                }
                paused = false;
                break;
            } catch (PasswordRequiredException e) {
                paused = true;
                publishProgress(e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onProgressUpdate(IOException... values) {
        super.onProgressUpdate(values);
        if (values.length < 1) return;

        IOException result = values[0];
        //We only handle PasswordRequiredException here.
        if(result instanceof PasswordRequiredException)
        {
            System.err.println("Display dialog!");
            //Display dialog!
            password = "123456";
            paused = false;
        }
    }

}
