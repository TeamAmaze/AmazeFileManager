package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
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
     */
    public static ArrayList<BaseFile> getDocumentFilesList(String path, Context context) {
        SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
        String rootUriString = manager.getString(MainActivity.KEY_PREF_OTG, null);
        DocumentFile rootUri = DocumentFile.fromTreeUri(context, Uri.parse(rootUriString));
        ArrayList<BaseFile> files = new ArrayList<>();

        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {

            // first omit 'otg:/' before iterating through DocumentFile
            if (path.equals(OTGUtil.PREFIX_OTG + "/")) break;
            if (parts[i].equals("otg:") || parts[i].equals("")) continue;
            Log.d(context.getClass().getSimpleName(), "Currently at: " + parts[i]);
            // iterating through the required path to find the end point
            rootUri = rootUri.findFile(parts[i]);
        }

        Log.d(context.getClass().getSimpleName(), "Found URI for: " + rootUri.getName());
        // we have the end point DocumentFile, list the files inside it and return
        for (DocumentFile file : rootUri.listFiles()) {
            try {
                if (file.exists()) {
                    long size = 0;
                    if (!file.isDirectory()) size = file.length();
                    Log.d(context.getClass().getSimpleName(), "Found file: " + file.getName());
                    BaseFile baseFile = new BaseFile(path + "/" + file.getName(),
                            RootHelper.parseDocumentFilePermission(file), file.lastModified(), size, file.isDirectory());
                    baseFile.setName(file.getName());
                    baseFile.setMode(OpenMode.OTG);
                    files.add(baseFile);
                }
            } catch (Exception e) {
            }
        }

        return files;
    }
}
