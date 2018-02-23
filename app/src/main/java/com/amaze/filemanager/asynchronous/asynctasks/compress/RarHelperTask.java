package com.amaze.filemanager.asynchronous.asynctasks.compress;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

/**
 * Created by Arpit on 25-01-2015 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class RarHelperTask extends CompressedHelperTask {

    private static final String ZIP_INTERNAL_SEPARATOR = "\\";

    private String fileLocation;

    /**
     * AsyncTask to load RAR file items.
     * @param realFileDirectory the location of the zip file
     * @param dir relativeDirectory to access inside the zip file
     */
    public RarHelperTask(String realFileDirectory, String dir, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(dir.replace(SEPARATOR, ZIP_INTERNAL_SEPARATOR), goBack, l);
        fileLocation = realFileDirectory;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        try {
            Archive zipfile = new Archive(new File(fileLocation));

            for (FileHeader header : zipfile.getFileHeaders()) {
                String name = header.getFileNameString();//This uses \ as separator, not /
                name = name.replace(ZIP_INTERNAL_SEPARATOR, SEPARATOR);

                boolean isInBaseDir = relativePath.equals("") && !name.contains(SEPARATOR);
                boolean isInRelativeDir = name.contains(SEPARATOR)
                        && name.substring(0, name.lastIndexOf(SEPARATOR)+1).equals(relativePath);

                if (isInBaseDir || isInRelativeDir) {
                    elements.add(new CompressedObjectParcelable(getName(name, "/"),
                            name, 0, header.getDataSize(), header.isDirectory()));
                }
            }
        } catch (RarException | IOException e) {
            e.printStackTrace();
        }
    }

}

