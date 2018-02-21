package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.content.Context;
import android.net.Uri;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

/**
 * Created by Vishal on 11/23/2014 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class ZipHelperTask extends CompressedHelperTask {

    private WeakReference<Context> context;
    private Uri fileLocation;
    private String relativePath;

    /**
     * AsyncTask to load ZIP file items.
     * @param realFileDirectory the location of the zip file
     * @param dir relativeDirectory to access inside the zip file
     */
    public ZipHelperTask(Context c, String realFileDirectory, String dir, boolean goback,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goback, l);
        context = new WeakReference<>(c);
        fileLocation = Uri.parse(realFileDirectory);
        relativePath = dir;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        try {
            if (new File(fileLocation.getPath()).canRead()) {
                ZipFile zipfile = new ZipFile(fileLocation.getPath());
                for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    checkAndAdd(elements, entry);
                }
            } else {
                ZipInputStream zipfile1 = new ZipInputStream(context.get().getContentResolver().openInputStream(fileLocation));
                for (ZipEntry entry = zipfile1.getNextEntry(); entry != null; entry = zipfile1.getNextEntry()) {
                    checkAndAdd(elements, entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAndAdd(ArrayList<CompressedObjectParcelable> elements, ZipEntry entry) {
        String name = entry.getName();
        if (name.endsWith(SEPARATOR)) name = name.substring(0, name.length() - 1);

        boolean isInBaseDir = relativePath.equals("") && !name.contains(SEPARATOR);
        boolean isInRelativeDir = name.contains(SEPARATOR)
                && name.substring(0, name.lastIndexOf(SEPARATOR)+1).equals(relativePath);

        if (isInBaseDir || isInRelativeDir) {
            elements.add(processEntry(entry));
        }
    }

    private CompressedObjectParcelable processEntry(ZipEntry entry) {
        return new CompressedObjectParcelable(getName(entry.getName(), SEPARATOR), entry.getName(),
                entry.getTime(), entry.getSize(), entry.isDirectory());
    }

}
