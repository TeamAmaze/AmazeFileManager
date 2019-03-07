package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.support.annotation.Nullable;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZArchiveEntry;
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

public class SevenZipHelperTask extends CompressedHelperTask {

    private String filePath, relativePath;

    private String password;

    public SevenZipHelperTask(String filePath, String relativePath, boolean goBack,
                              OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        this(filePath, relativePath, goBack, l, null);
    }

    public SevenZipHelperTask(String filePath, String relativePath, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l, @Nullable String password) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
        this.password = password;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
