package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;

import java.util.ArrayList;

/**
 * Created by Vishal on 27-04-2017.
 */

public class OTGUtil {

    public static final String PREFIX_OTG = "otg:/";

    /**
     * Returns an array of list of files at a specific path in OTG
     *
     * @param path    the path to the directory tree, starts with prefix 'otg:/'
     *                Independent of URI (or mount point) for the OTG
     * @param context context for loading
     * @return an array of list of files at the path
     * @deprecated use getDocumentFiles()
     */
    public static ArrayList<HybridFileParcelable> getDocumentFilesList(String path, Context context) {
        final ArrayList<HybridFileParcelable> files = new ArrayList<>();
        getDocumentFiles(path, context, new OnFileFound() {
            @Override
            public void onFileFound(HybridFileParcelable file) {
                files.add(file);
            }
        });
        return files;
    }

    /**
     * Get the files at a specific path in OTG
     *
     * @param path    the path to the directory tree, starts with prefix 'otg:/'
     *                Independent of URI (or mount point) for the OTG
     * @param context context for loading
     * @return an array of list of files at the path
     */
    public static void getDocumentFiles(String path, Context context, OnFileFound fileFound) {
        SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
        String rootUriString = manager.getString(MainActivity.KEY_PREF_OTG, null);
        DocumentFile rootUri = DocumentFile.fromTreeUri(context, Uri.parse(rootUriString));

        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            // first omit 'otg:/' before iterating through DocumentFile
            if (path.equals(OTGUtil.PREFIX_OTG + "/")) break;
            if (parts[i].equals("otg:") || parts[i].equals("")) continue;
            Log.d(context.getClass().getSimpleName(), "Currently at: " + parts[i]);
            // iterating through the required path to find the end point
            rootUri = rootUri.findFile(parts[i]);
        }

        // we have the end point DocumentFile, list the files inside it and return
        for (DocumentFile file : rootUri.listFiles()) {
            if (file.exists()) {
                long size = 0;
                if (!file.isDirectory()) size = file.length();
                Log.d(context.getClass().getSimpleName(), "Found file: " + file.getName());
                HybridFileParcelable baseFile = new HybridFileParcelable(path + "/" + file.getName(),
                        RootHelper.parseDocumentFilePermission(file), file.lastModified(), size, file.isDirectory());
                baseFile.setName(file.getName());
                baseFile.setMode(OpenMode.OTG);
                fileFound.onFileFound(baseFile);
            }
        }
    }

    /**
     * Traverse to a specified path in OTG
     *
     * @param path
     * @param context
     * @param createRecursive flag used to determine whether to create new file while traversing to path,
     *                        in case path is not present. Notably useful in opening an output stream.
     * @return
     */
    public static DocumentFile getDocumentFile(String path, Context context, boolean createRecursive) {
        SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
        String rootUriString = manager.getString(MainActivity.KEY_PREF_OTG, null);

        // start with root of SD card and then parse through document tree.
        DocumentFile rootUri = DocumentFile.fromTreeUri(context, Uri.parse(rootUriString));

        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {

            if (path.equals("otg:/")) break;
            if (parts[i].equals("otg:") || parts[i].equals("")) continue;
            Log.d(context.getClass().getSimpleName(), "Currently at: " + parts[i]);
            // iterating through the required path to find the end point

            DocumentFile nextDocument = rootUri.findFile(parts[i]);
            if (createRecursive) {
                if (nextDocument == null || !nextDocument.exists()) {
                    nextDocument = rootUri.createFile(parts[i].substring(parts[i].lastIndexOf(".")), parts[i]);
                    Log.d(context.getClass().getSimpleName(), "NOT FOUND! File created: " + parts[i]);
                }
            }
            rootUri = nextDocument;
        }
        return rootUri;
    }
}
