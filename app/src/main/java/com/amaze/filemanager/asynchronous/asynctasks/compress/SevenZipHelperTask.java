package com.amaze.filemanager.asynchronous.asynctasks.compress;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

public class SevenZipHelperTask extends CompressedHelperTask {

    private String filePath, relativePath;

    public SevenZipHelperTask(String filePath, String relativePath, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        SevenZFile sevenzFile = null;
        try {
            sevenzFile = new SevenZFile(new File(filePath));

            for (SevenZArchiveEntry entry : sevenzFile.getEntries()) {
                String name = entry.getName();
                if (name.endsWith(SEPARATOR)) name = name.substring(0, name.length() - 1);

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
