package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.AsyncTask;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.helpers.RarDecompressor;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Arpit on 25-01-2015 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class RarHelperTask extends CompressedHelperTask {

    private String fileLocation;
    private String relativeDirectory;

    /**
     * AsyncTask to load RAR file items.
     * @param realFileDirectory the location of the zip file
     * @param dir relativeDirectory to access inside the zip file
     */
    public RarHelperTask(String realFileDirectory, String dir, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        fileLocation = realFileDirectory;
        relativeDirectory = dir;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        try {
            Archive zipfile = new Archive(new File(fileLocation));
            String relativeDirDiffSeparator = relativeDirectory.replace("/", "\\");

            for (FileHeader header : zipfile.getFileHeaders()) {
                String name = header.getFileNameString();//This uses \ as separator, not /
                boolean isInBaseDir = (relativeDirDiffSeparator == null || relativeDirDiffSeparator.equals("")) && !name.contains("\\");
                boolean isInRelativeDir = relativeDirDiffSeparator != null && name.contains("\\")
                        && name.substring(0, name.lastIndexOf("\\")).equals(relativeDirDiffSeparator);

                if (isInBaseDir || isInRelativeDir) {
                    elements.add(new CompressedObjectParcelable(RarDecompressor.convertName(header), 0, header.getDataSize(), header.isDirectory()));
                }
            }
        } catch (RarException | IOException e) {
            e.printStackTrace();
        }
    }

}

