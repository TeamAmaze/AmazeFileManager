package com.amaze.filemanager.utils.cloud;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OpenMode;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vishal on 19/4/17.
 *
 * Class provides helper methods for cloud utilities
 */

public class CloudUtil {

    /**
     * @deprecated use getCloudFiles()
     */
    public static ArrayList<HybridFileParcelable> listFiles(String path, CloudStorage cloudStorage,
                                                            OpenMode openMode) throws CloudPluginException {
        final ArrayList<HybridFileParcelable> baseFiles = new ArrayList<>();
        getCloudFiles(path, cloudStorage, openMode, new OnFileFound() {
            @Override
            public void onFileFound(HybridFileParcelable file) {
                baseFiles.add(file);
            }
        });
        return baseFiles;
    }

    public static void getCloudFiles(String path, CloudStorage cloudStorage, OpenMode openMode,
                                     OnFileFound fileFoundCallback) throws CloudPluginException {
        String strippedPath = stripPath(openMode, path);
        try {
            for (CloudMetaData cloudMetaData : cloudStorage.getChildren(strippedPath)) {
                HybridFileParcelable baseFile = new HybridFileParcelable(
                        path + "/" + cloudMetaData.getName(),
                        "",
                        (cloudMetaData.getModifiedAt() == null) ? 0l : cloudMetaData.getModifiedAt(),
                        cloudMetaData.getSize(),
                        cloudMetaData.getFolder());
                baseFile.setName(cloudMetaData.getName());
                baseFile.setMode(openMode);
                fileFoundCallback.onFileFound(baseFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CloudPluginException();
        }
    }

    /**
     * Strips down the cloud path to remove any prefix
     * @param openMode
     * @return
     */
    public static String stripPath(OpenMode openMode, String path) {
        String strippedPath = path;
        switch (openMode) {
            case DROPBOX:
                if (path.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_DROPBOX, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_DROPBOX + "/", "");
                }
                break;
            case BOX:
                if (path.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_BOX, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_BOX + "/", "");
                }
                break;
            case ONEDRIVE:
                if (path.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_ONE_DRIVE, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/", "");
                }
                break;
            case GDRIVE:
                if (path.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")) {
                    // we're at root, just replace the prefix
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE, "");
                } else {
                    // we're not at root, replace prefix + /
                    strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/", "");
                }
                break;
            default:
                break;
        }
        return strippedPath;
    }

    public static void launchCloud(final HybridFileParcelable baseFile, final OpenMode serviceType, final Activity activity) {
        final CloudStreamer streamer = CloudStreamer.getInstance();

        new Thread(() -> {
            try {
                streamer.setStreamSrc(baseFile.getInputStream(activity), baseFile.getName(), baseFile.length(activity));
                activity.runOnUiThread(() -> {
                    try {
                        File file = new File(Uri.parse(CloudUtil.stripPath(serviceType, baseFile.getPath())).getPath());
                        Uri uri = Uri.parse(CloudStreamer.URL + Uri.fromFile(file).getEncodedPath());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(uri, MimeTypes.getMimeType(file));
                        PackageManager packageManager = activity.getPackageManager();
                        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                        if (resInfos != null && resInfos.size() > 0)
                            activity.startActivity(i);
                        else
                            Toast.makeText(activity,
                                    activity.getResources().getString(R.string.smb_launch_error),
                                    Toast.LENGTH_SHORT).show();
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {

                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Asynctask checks if the item pressed on is a cloud account, and if the token that
     * is saved for it is invalid or not, in which case, we'll clear off the
     * saved token and authenticate the user again
     * @param path the path of item in drawer
     * @param mainActivity reference to main activity to fire callbacks to delete/add connection
     */
    public static void checkToken(String path, final MainActivity mainActivity) {

        new AsyncTask<String, Void, Boolean>() {

            OpenMode serviceType;
            private DataUtils dataUtils = DataUtils.getInstance();

            @Override
            protected Boolean doInBackground(String... params) {
                boolean isTokenValid = true;
                String path = params[0];

                if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {
                    // dropbox account
                    serviceType = OpenMode.DROPBOX;
                    CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);

                    try {
                        cloudStorageDropbox.getUserLogin();
                    } catch (Exception e) {
                        e.printStackTrace();

                        isTokenValid = false;
                    }
                } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {

                    serviceType = OpenMode.ONEDRIVE;
                    CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

                    try {
                        cloudStorageOneDrive.getUserLogin();
                    } catch (Exception e) {
                        e.printStackTrace();

                        isTokenValid = false;
                    }
                } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {

                    serviceType = OpenMode.BOX;
                    CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);

                    try {
                        cloudStorageBox.getUserLogin();
                    } catch (Exception e) {
                        e.printStackTrace();

                        isTokenValid = false;
                    }
                } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {

                    serviceType = OpenMode.GDRIVE;
                    CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);

                    try {
                        cloudStorageGDrive.getUserLogin();
                    } catch (Exception e) {
                        e.printStackTrace();

                        isTokenValid = false;
                    }
                }
                return isTokenValid;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);

                if (!aBoolean) {
                    // delete account and create a new one
                    Toast.makeText(mainActivity, mainActivity.getResources()
                            .getString(R.string.cloud_token_lost), Toast.LENGTH_LONG).show();
                    mainActivity.deleteConnection(serviceType);
                    mainActivity.addConnection(serviceType);
                }
            }
        }.execute(path);
    }
}
