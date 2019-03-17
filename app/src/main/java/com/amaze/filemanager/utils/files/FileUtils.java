/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Marcin Zasuwa <marcinadd@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.utils.files;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.DatabaseViewerActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.superclasses.PermissionsActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.fragments.preference_fragments.PrefFrag;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OnProgressUpdate;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.share.ShareTask;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;

import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

import jcifs.smb.SmbFile;

/**
 * Functions that deal with files
 */
public class FileUtils {

    public static long folderSize(File directory, OnProgressUpdate<Long> updateState) {
        long length = 0;
        try {
            for (File file:directory.listFiles()) {
                if (file.isFile())
                    length += file.length();
                else
                    length += folderSize(file, null); //null because updateState would be called for children dirs

                if(updateState != null)
                    updateState.onUpdate(length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return length;
    }

    public static long folderSize(HybridFile directory, OnProgressUpdate<Long> updateState) {
        if(directory.isSimpleFile())
            return folderSize(new File(directory.getPath()), updateState);
        else
            return directory.folderSize(AppConfig.getInstance());
    }

    public static long folderSize(SmbFile directory) {
        long length = 0;
        try {
            for (SmbFile file:directory.listFiles()) {

                if (file.isFile())
                    length += file.length();
                else
                    length += folderSize(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return length;
    }

    /**
     * Use recursive <code>ls</code> to get folder size.
     *
     * It is slow, it is stupid, and may be inaccurate (because of permission problems).
     * Only for fallback use when <code>du</code> is not available.
     *
     * @see HybridFile#folderSize(Context)
     * @return Folder size in bytes
     */
    public static Long folderSizeSftp(SFTPClient client, String remotePath) {
        Long retval = 0L;
        try {
            for (RemoteResourceInfo info : client.ls(remotePath)) {
                if (info.isDirectory())
                    retval += folderSizeSftp(client, info.getPath());
                else
                    retval += info.getAttributes().getSize();
            }
        } catch (SFTPException e) {
            //Usually happens when permission denied listing files in directory
            Log.e("folderSizeSftp", "Problem accessing " + remotePath, e);
        } finally {
            return retval;
        }
    }


    public static long folderSizeCloud(OpenMode openMode, CloudMetaData sourceFileMeta) {

        DataUtils dataUtils = DataUtils.getInstance();
        long length = 0;
        CloudStorage cloudStorage = dataUtils.getAccount(openMode);
        for (CloudMetaData metaData : cloudStorage.getChildren(CloudUtil.stripPath(openMode, sourceFileMeta.getPath()))) {

            if (metaData.getFolder()) {
                length += folderSizeCloud(openMode, metaData);
            } else {
                length += metaData.getSize();
            }
        }

        return length;
    }

    /**
     * Helper method to get size of an otg folder
     */
    public static long otgFolderSize(String path, final Context context) {
        final AtomicLong totalBytes = new AtomicLong(0);
        OTGUtil.getDocumentFiles(path, context, file -> totalBytes.addAndGet(getBaseFileSize(file, context)));
        return totalBytes.longValue();
    }

    /**
     * Helper method to calculate source files size
     */
    public static long getTotalBytes(ArrayList<HybridFileParcelable> files, Context context) {
        long totalBytes = 0L;
        for (HybridFileParcelable file : files) {
            totalBytes += getBaseFileSize(file, context);
        }
        return totalBytes;
    }

    private static long getBaseFileSize(HybridFileParcelable baseFile, Context context) {
        if (baseFile.isDirectory(context)) {
            return baseFile.folderSize(context);
        } else {
            return baseFile.length(context);
        }
    }

    /**
     * Calls {@link #scanFile(Uri, Context)} using {@link Uri}.
     *
     * @see {@link #scanFile(Uri, Context)}
     * @param file File to scan
     * @param c {@link Context}
     */
    public static void scanFile(@NonNull File file, @NonNull Context c) {
        scanFile(Uri.fromFile(file), c);
    }

    /**
     * Triggers {@link Intent#ACTION_MEDIA_SCANNER_SCAN_FILE} intent to refresh the media store.
     *
     * @param uri File's {@link Uri}
     * @param c {@link Context}
     */
    private static void scanFile(@NonNull Uri uri, @NonNull Context c) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        c.sendBroadcast(mediaScanIntent);
    }

    /**
     * Triggers media scanner for multiple paths
     * Don't use filesystem API directly as files might not be present anymore (eg. move/rename)
     * which may lead to {@link java.io.FileNotFoundException}
     * @param hybridFiles
     * @param context
     */
    public static void scanFile(@NonNull Context context, @NonNull HybridFile[] hybridFiles) {
        AsyncTask.execute(() -> {
            if (hybridFiles[0].exists(context) && hybridFiles[0].isLocal()) {
                String[] paths = new String[hybridFiles.length];
                for (int i = 0; i<hybridFiles.length; i++) {
                    HybridFile hybridFile = hybridFiles[i];
                    paths[i] = hybridFile.getPath();
                }
                MediaScannerConnection.scanFile(context, paths, null, null);
            } else {
                for (HybridFile hybridFile : hybridFiles) {
                    scanFile(hybridFile, context);
                }
            }
        });
    }

    /**
     * Triggers media store for the file path
     * @param hybridFile the file which was changed (directory not supported)
     * @param context
     */
    public static void scanFile(@NonNull HybridFile hybridFile, Context context) {

        if((hybridFile.isLocal() || hybridFile.isOtgFile()) && hybridFile.exists(context)) {

            DocumentFile documentFile = FileUtil.getDocumentFile(hybridFile.getFile(), false, context);
            //If FileUtil.getDocumentFile() returns null, fall back to DocumentFile.fromFile()
            if(documentFile == null)
                documentFile = DocumentFile.fromFile(hybridFile.getFile());

            FileUtils.scanFile(documentFile.getUri(), context);
        }
    }


    public static void crossfade(View buttons,final View pathbar) {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        buttons.setAlpha(0f);
        buttons.setVisibility(View.VISIBLE);


        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        buttons.animate()
                .alpha(1f)
                .setDuration(100)
                .setListener(null);
        pathbar.animate()
                .alpha(0f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        pathbar.setVisibility(View.GONE);
                    }
                });
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
    }

    public static void revealShow(final View view, boolean reveal) {
        if (reveal) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
            animator.setDuration(300); //ms
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    view.setVisibility(View.VISIBLE);
                }
            });
            animator.start();
        } else {

            ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f);
            animator.setDuration(300); //ms
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                }
            });
            animator.start();
        }
    }

    public static void crossfadeInverse(final View buttons,final View pathbar) {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.

        pathbar.setAlpha(0f);
        pathbar.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        pathbar.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);
        buttons.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        buttons.setVisibility(View.GONE);
                    }
                });
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
    }

    public static void shareCloudFile(String path, final OpenMode openMode, final Context context) {
        new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... params) {
                String shareFilePath = params[0];
                CloudStorage cloudStorage = DataUtils.getInstance().getAccount(openMode);
                return cloudStorage.createShareLink(CloudUtil.stripPath(openMode, shareFilePath));
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                FileUtils.copyToClipboard(context, s);
                Toast.makeText(context,
                        context.getString(R.string.cloud_share_copied), Toast.LENGTH_LONG).show();
            }
        }.execute(path);
    }

    public static void shareFiles(ArrayList<File> a, Activity c,AppTheme appTheme,int fab_skin) {

        ArrayList<Uri> uris = new ArrayList<>();
        boolean b = true;
        for (File f : a) {
            uris.add(Uri.fromFile(f));
        }

        String mime = MimeTypes.getMimeType(a.get(0).getPath(), a.get(0).isDirectory());
        if (a.size() > 1)
            for (File f : a) {
                if (!mime.equals(MimeTypes.getMimeType(f.getPath(), f.isDirectory()))) {
                    b = false;
                }
            }

        if (!b || mime==(null))
            mime = "*/*";
        try {

            new ShareTask(c,uris,appTheme,fab_skin).execute(mime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static float readableFileSizeFloat(long size) {
        if (size <= 0) return 0;
        return (float) (size / (1024*1024));
    }

    /**
     * Install .apk file.
     * @param permissionsActivity needed to ask for {@link Manifest.permission#REQUEST_INSTALL_PACKAGES} permission
     */
    public static void installApk(final @NonNull File f, final @NonNull PermissionsActivity permissionsActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !permissionsActivity.getPackageManager().canRequestPackageInstalls()) {
            permissionsActivity.requestInstallApkPermission(() -> installApk(f, permissionsActivity));
        }

        Intent chooserIntent = new Intent();
        chooserIntent.setAction(Intent.ACTION_INSTALL_PACKAGE);
        chooserIntent.setData(Uri.fromFile(f));

        try {
            permissionsActivity.startActivity(chooserIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(permissionsActivity, R.string.error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open a file not supported by Amaze
     *
     * @param f the file
     * @param forcechooser force the chooser to show up even when set default by user
     */
    public static void openunknown(File f, Context c, boolean forcechooser, boolean useNewStack) {
        Intent chooserIntent = new Intent();
        chooserIntent.setAction(Intent.ACTION_VIEW);

        String type = MimeTypes.getMimeType(f.getPath(), f.isDirectory());
        if (type != null && type.trim().length() != 0 && !type.equals("*/*")) {
            Uri uri = fileToContentUri(c, f);
            if (uri == null) uri = Uri.fromFile(f);
            chooserIntent.setDataAndType(uri, type);

            Intent activityIntent;
            if (forcechooser) {
                if(useNewStack) applyNewDocFlag(chooserIntent);
                activityIntent = Intent.createChooser(chooserIntent, c.getString(R.string.openwith));
            } else {
                activityIntent = chooserIntent;
                if(useNewStack) applyNewDocFlag(activityIntent);
            }

            try {
                c.startActivity(activityIntent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(c, R.string.noappfound, Toast.LENGTH_SHORT).show();
                openWith(f, c, useNewStack);
            }
        } else {
            // failed to load mime type
            openWith(f, c, useNewStack);
        }
    }

    /**
     * Open file from OTG
     */
    public static void openunknown(DocumentFile f, Context c, boolean forcechooser, boolean useNewStack) {
        Intent chooserIntent = new Intent();
        chooserIntent.setAction(Intent.ACTION_VIEW);
        chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String type = f.getType();
        if (type != null && type.trim().length() != 0 && !type.equals("*/*")) {
            chooserIntent.setDataAndType(f.getUri(), type);
            Intent activityIntent;
            if (forcechooser) {
                if(useNewStack) applyNewDocFlag(chooserIntent);
                activityIntent = Intent.createChooser(chooserIntent, c.getString(R.string.openwith));
            } else {
                activityIntent = chooserIntent;
                if(useNewStack) applyNewDocFlag(chooserIntent);
            }

            try {
                c.startActivity(activityIntent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(c, R.string.noappfound, Toast.LENGTH_SHORT).show();
                openWith(f, c, useNewStack);
            }
        } else {
            openWith(f, c, useNewStack);
        }
    }

    private static void applyNewDocFlag(Intent i) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {

            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME
                | Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS);
        }
    }

    private static final String INTERNAL_VOLUME = "internal";
    public static final String EXTERNAL_VOLUME = "external";

    private static final String EMULATED_STORAGE_SOURCE = System.getenv("EMULATED_STORAGE_SOURCE");
    private static final String EMULATED_STORAGE_TARGET = System.getenv("EMULATED_STORAGE_TARGET");
    private static final String EXTERNAL_STORAGE = System.getenv("EXTERNAL_STORAGE");
    public static String normalizeMediaPath(String path) {
        // Retrieve all the paths and check that we have this environment vars
        if (TextUtils.isEmpty(EMULATED_STORAGE_SOURCE) ||
                TextUtils.isEmpty(EMULATED_STORAGE_TARGET) ||
                TextUtils.isEmpty(EXTERNAL_STORAGE)) {
            return path;
        }

        // We need to convert EMULATED_STORAGE_SOURCE -> EMULATED_STORAGE_TARGET
        if (path.startsWith(EMULATED_STORAGE_SOURCE)) {
            path = path.replace(EMULATED_STORAGE_SOURCE, EMULATED_STORAGE_TARGET);
        }
        return path;
    }
    public static Uri fileToContentUri(Context context, File file) {
        // Normalize the path to ensure media search
        final String normalizedPath = normalizeMediaPath(file.getAbsolutePath());

        // Check in external and internal storages
        Uri uri = fileToContentUri(context, normalizedPath, file.isDirectory(), EXTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        uri = fileToContentUri(context, normalizedPath, file.isDirectory(), INTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        return null;
    }

    private static Uri fileToContentUri(Context context, String path, boolean isDirectory, String volume) {
        final String where = MediaStore.MediaColumns.DATA + " = ?";
        Uri baseUri;
        String[] projection;
        int mimeType = Icons.getTypeOfFile(path, isDirectory);

        switch (mimeType) {
            case Icons.IMAGE:
                baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{BaseColumns._ID};
                break;
            case Icons.VIDEO:
                baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{BaseColumns._ID};
                break;
            case Icons.AUDIO:
                baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{BaseColumns._ID};
                break;
            default:
                baseUri = MediaStore.Files.getContentUri(volume);
                projection = new String[]{BaseColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};
        }

        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(baseUri, projection, where, new String[]{path}, null);
        try {
            if (c != null && c.moveToNext()) {
                boolean isValid = false;
                if (mimeType == Icons.IMAGE || mimeType == Icons.VIDEO || mimeType == Icons.AUDIO) {
                    isValid = true;
                } else {
                    int type = c.getInt(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE));
                    isValid = type != 0;
                }

                if (isValid) {
                    // Do not force to use content uri for no media files
                    long id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
                    return Uri.withAppendedPath(baseUri, String.valueOf(id));
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    /**
     * Method supports showing a UI to ask user to open a file without any extension/mime
     */
    public static void openWith(final File f, final Context c, final boolean useNewStack) {
        MaterialDialog.Builder a=new MaterialDialog.Builder(c);
        a.title(c.getString(R.string.openas));
        String[] items=new String[]{c.getString(R.string.text),c.getString(R.string.image),c.getString(R.string.video),c.getString(R.string.audio),c.getString(R.string.database),c.getString(R.string.other)};

        a.items(items).itemsCallback((materialDialog, view, i, charSequence) -> {
            Uri uri = fileToContentUri(c, f);
            if (uri == null) uri = Uri.fromFile(f);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            switch (i) {
                case 0:
                    if(useNewStack) applyNewDocFlag(intent);
                    intent.setDataAndType(uri, "text/*");
                    break;
                case 1:
                    intent.setDataAndType(uri, "image/*");
                    break;
                case 2:
                    intent.setDataAndType(uri, "video/*");
                    break;
                case 3:
                    intent.setDataAndType(uri, "audio/*");
                    break;
                case 4:
                    intent = new Intent(c, DatabaseViewerActivity.class);
                    intent.putExtra("path", f.getPath());
                    break;
                case 5:
                    intent.setDataAndType(uri, "*/*");
                    break;
            }
            try {
                c.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(c, R.string.noappfound, Toast.LENGTH_SHORT).show();
                openWith(f, c, useNewStack);
            }
        });
        try {
            a.build().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openWith(final DocumentFile f, final Context c, final boolean useNewStack) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(c);
        a.title(c.getString(R.string.openas));
        String[] items = new String[]{c.getString(R.string.text), c.getString(R.string.image), c.getString(R.string.video), c.getString(R.string.audio), c.getString(R.string.database), c.getString(R.string.other)};

        a.items(items).itemsCallback((materialDialog, view, i, charSequence) -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            switch (i) {
                case 0:
                    if(useNewStack) applyNewDocFlag(intent);
                    intent.setDataAndType(f.getUri(), "text/*");
                    break;
                case 1:
                    intent.setDataAndType(f.getUri(), "image/*");
                    break;
                case 2:
                    intent.setDataAndType(f.getUri(), "video/*");
                    break;
                case 3:
                    intent.setDataAndType(f.getUri(), "audio/*");
                    break;
                case 4:
                    intent = new Intent(c, DatabaseViewerActivity.class);
                    intent.putExtra("path", f.getUri());
                    break;
                case 5:
                    intent.setDataAndType(f.getUri(), "*/*");
                    break;
            }
            try {
                c.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(c, R.string.noappfound, Toast.LENGTH_SHORT).show();
                openWith(f, c, useNewStack);
            }
        });

        a.build().show();
    }

    /**
     * Method determines if there is something to go back to
     */
    public static boolean canGoBack(Context context, HybridFile currentFile) {
        switch (currentFile.getMode()) {

            // we're on main thread and can't list the cloud files
            case DROPBOX:
            case BOX:
            case GDRIVE:
            case ONEDRIVE:
            case OTG:
            case SFTP:
                return true;
            default:
                return true;// TODO: 29/9/2017 there might be nothing to go back to (check parent)
        }
    }

    public static long[] getSpaces(HybridFile hFile, Context context, final OnProgressUpdate<Long[]> updateState) {
        long totalSpace = hFile.getTotal(context);
        long freeSpace = hFile.getUsableSpace();
        long fileSize = 0l;

        if (hFile.isDirectory(context)) {
            fileSize = hFile.folderSize(context);
        } else {
            fileSize = hFile.length(context);
        }
        return new long[] {totalSpace, freeSpace, fileSize};
    }

    public static boolean copyToClipboard(Context context, String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(context.getString(R.string.clipboard_path_copy), text);
            clipboard.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String[] getFolderNamesInPath(String path) {
        if(!path.endsWith("/")) path += "/";
        return ("root" + path).split("/");
    }

    public static String[] getPathsInPath(String path) {
        if(path.endsWith("/")) path = path.substring(0, path.length()-1);

        ArrayList<String> paths = new ArrayList<>();

        while (path.length() > 0) {
            paths.add(path);
            path = path.substring(0, path.lastIndexOf("/"));
        }

        paths.add("/");
        Collections.reverse(paths);

        return paths.toArray(new String[paths.size()]);
    }

    public static boolean canListFiles(File f) {
        return f.canRead() && f.isDirectory();
    }

    public static void openFile(final File f, final MainActivity m, SharedPreferences sharedPrefs) {
        boolean useNewStack = sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK, false);
        boolean defaultHandler = isSelfDefault(f, m);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(m);
        final Toast[] studioCount = {null};

        if(f.getName().toLowerCase().endsWith(".apk")) {
            GeneralDialogCreation.showPackageDialog(f, m);
        } else if (defaultHandler && CompressedHelper.isFileExtractable(f.getPath())) {
            GeneralDialogCreation.showArchiveDialog(f, m);
        } else if (defaultHandler && f.getName().toLowerCase().endsWith(".db")) {
            Intent intent = new Intent(m, DatabaseViewerActivity.class);
            intent.putExtra("path", f.getPath());
            m.startActivity(intent);
        }  else if (Icons.getTypeOfFile(f.getPath(), f.isDirectory()) == Icons.AUDIO) {
            final int studio_count = sharedPreferences.getInt("studio", 0);
            Uri uri = Uri.fromFile(f);
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "audio/*");

            // Behold! It's the  legendary easter egg!
            if (studio_count!=0) {
                new CountDownTimer(studio_count, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int sec = (int)millisUntilFinished/1000;
                        if (studioCount[0] !=null)
                            studioCount[0].cancel();
                        studioCount[0] = Toast.makeText(m, sec + "", Toast.LENGTH_LONG);
                        studioCount[0].show();
                    }

                    @Override
                    public void onFinish() {
                        if (studioCount[0] !=null)
                            studioCount[0].cancel();
                        studioCount[0] = Toast.makeText(m, m.getString(R.string.opening),
                                Toast.LENGTH_LONG);
                        studioCount[0].show();
                        m.startActivity(intent);
                    }
                }.start();
            } else
                m.startActivity(intent);
        } else {
            try {
                openunknown(f, m, false, useNewStack);
            } catch (Exception e) {
                Toast.makeText(m, m.getString(R.string.noappfound),Toast.LENGTH_LONG).show();
                openWith(f, m, useNewStack);
            }
        }
    }

    private static boolean isSelfDefault(File f, Context c){
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), MimeTypes.getMimeType(f.getPath(), f.isDirectory()));
        String s="";
        ResolveInfo rii = c.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (rii !=  null && rii.activityInfo != null) s = rii.activityInfo.packageName;

        return s.equals("com.amaze.filemanager") || rii == null;
    }

    /**
     * Support file opening for {@link DocumentFile} (eg. OTG)
     */
    public static void openFile(final DocumentFile f, final MainActivity m, SharedPreferences sharedPrefs) {
        boolean useNewStack = sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK, false);
        try {
            openunknown(f, m, false, useNewStack);
        } catch (Exception e) {
            Toast.makeText(m, m.getString(R.string.noappfound),Toast.LENGTH_LONG).show();
            openWith(f, m, useNewStack);
        }

        // not supporting inbuilt activities for now
        /*if (f.getName().toLowerCase().endsWith(".zip") ||
                f.getName().toLowerCase().endsWith(".jar") ||
                f.getName().toLowerCase().endsWith(".rar")||
                f.getName().toLowerCase().endsWith(".tar") ||
                f.getName().toLowerCase().endsWith(".tar.gz")) {
            //showArchiveDialog(f, m);
        } else if(f.getName().toLowerCase().endsWith(".apk")) {
            //showPackageDialog(f, m);
        } else if (f.getName().toLowerCase().endsWith(".db")) {
            Intent intent = new Intent(m, DatabaseViewerActivity.class);
            intent.putExtra("path", f.getUri());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            m.startActivity(intent);
        }  else if (Icons.isAudio(f.getName())) {
            final int studio_count = sharedPref.getInt("studio", 0);
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(f.getUri(), "audio*//*");

            // Behold! It's the  legendary easter egg!
            if (studio_count!=0) {
                new CountDownTimer(studio_count, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int sec = (int)millisUntilFinished/1000;
                        if (studioCount!=null)
                            studioCount.cancel();
                        studioCount = Toast.makeText(m, sec + "", Toast.LENGTH_LONG);
                        studioCount.show();
                    }

                    @Override
                    public void onFinish() {
                        if (studioCount!=null)
                            studioCount.cancel();
                        studioCount = Toast.makeText(m, m.getString(R.string.opening), Toast.LENGTH_LONG);
                        studioCount.show();
                        m.startActivity(intent);
                    }
                }.start();
            } else
                m.startActivity(intent);
        } else {
            try {
                openunknown(f, m, false);
            } catch (Exception e) {
                Toast.makeText(m, m.getString(R.string.noappfound),Toast.LENGTH_LONG).show();
                openWith(f, m);
            }
        }*/
    }

    public static ArrayList<HybridFile> toHybridFileConcurrentRadixTree(ConcurrentRadixTree<VoidValue> a) {
        ArrayList<HybridFile> b = new ArrayList<>();
        for (CharSequence o : a.getKeysStartingWith("")) {
            HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, o.toString());
            hFile.generateMode(null);
            b.add(hFile);
        }
        return b;
    }

    public static ArrayList<HybridFile> toHybridFileArrayList(LinkedList<String> a) {
        ArrayList<HybridFile> b = new ArrayList<>();
        for (String s : a) {
            HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, s);
            hFile.generateMode(null);
            b.add(hFile);
        }
        return b;
    }

    /**
     * We're parsing a line returned from a stdout of shell.
     * @param line must be the line returned from a 'ls' command
     */
    public static HybridFileParcelable parseName(String line) {
        boolean linked = false;
        StringBuilder name = new StringBuilder();
        StringBuilder link = new StringBuilder();
        String size = "-1";
        String date = "";
        String[] array = line.split(" ");
        if(array.length<6)return null;
        for (String anArray : array) {
            if (anArray.contains("->") && array[0].startsWith("l")) {
                linked = true;
            }
        }
        int p = getColonPosition(array);
        if(p!=-1){
            date = array[p - 1] + " | " + array[p];
            size = array[p - 2];}
        if (!linked) {
            for (int i = p + 1; i < array.length; i++) {
                name.append(" ").append(array[i]);
            }
            name = new StringBuilder(name.toString().trim());
        } else {
            int q = getLinkPosition(array);
            for (int i = p + 1; i < q; i++) {
                name.append(" ").append(array[i]);
            }
            name = new StringBuilder(name.toString().trim());
            for (int i = q + 1; i < array.length; i++) {
                link.append(" ").append(array[i]);
            }
        }
        long Size = (size==null || size.trim().length()==0)?-1:Long.parseLong(size);
        if(date.trim().length()>0) {
            ParsePosition pos = new ParsePosition(0);
            SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd | HH:mm");
            Date stringDate = simpledateformat.parse(date, pos);
            HybridFileParcelable baseFile=new HybridFileParcelable(name.toString(),array[0],stringDate.getTime(),Size,true);
            baseFile.setLink(link.toString());
            return baseFile;
        }else {
            HybridFileParcelable baseFile= new HybridFileParcelable(name.toString(),array[0],new File("/").lastModified(),Size,true);
            baseFile.setLink(link.toString());
            return baseFile;
        }
    }

    private static int getLinkPosition(String[] array){
        for(int i=0;i<array.length;i++){
            if(array[i].contains("->"))return i;
        }
        return  0;
    }

    private static int getColonPosition(String[] array){
        for(int i=0;i<array.length;i++){
            if(array[i].contains(":"))return i;
        }
        return  -1;
    }

    public static ArrayList<Boolean[]> parse(String permLine) {
        ArrayList<Boolean[]> arrayList= new ArrayList<>(3);
        Boolean[] read =new Boolean[]{permLine.charAt(1) == 'r',
                permLine.charAt(4) == 'r',
                permLine.charAt(7) == 'r'};

        Boolean[] write=new Boolean[]{permLine.charAt(2) == 'w',
                permLine.charAt(5) == 'w',
                permLine.charAt(8) == 'w'};

        Boolean[] execute=new Boolean[]{permLine.charAt(3) == 'x',
                permLine.charAt(6) == 'x',
                permLine.charAt(9) == 'x'};

        arrayList.add(read);
        arrayList.add(write);
        arrayList.add(execute);
        return arrayList;
    }

    public static boolean isStorage(String path) {
        for (String s : DataUtils.getInstance().getStorages())
            if (s.equals(path)) return true;
        return false;
    }

    public static boolean isPathAccessible(String dir, SharedPreferences pref) {
        File f = new File(dir);
        boolean showIfHidden = pref.getBoolean(PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES, false),
                isDirSelfOrParent = dir.endsWith("/.") || dir.endsWith("/.."),
                showIfRoot = pref.getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);

        return f.exists() && f.isDirectory()
                && (!f.isHidden() || (showIfHidden && !isDirSelfOrParent))
                && (!isRoot(dir) || showIfRoot);

        // TODO: 2/5/2017 use another system that doesn't create new object
    }

    public static boolean isRoot(String dir) {// TODO: 5/5/2017 hardcoding root might lead to problems down the line
        return !dir.contains(OTGUtil.PREFIX_OTG) && !dir.startsWith("/storage");
    }

    /**
     * Converts ArrayList of HybridFileParcelable to ArrayList of File
     */
    public static ArrayList<File> hybridListToFileArrayList(ArrayList<HybridFileParcelable> a) {
        ArrayList<File> b = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            b.add(new File(a.get(i).getPath()));
        }
        return b;
    }

    /**
     * Checks whether path for bookmark exists
     * If path is not found, empty directory is created
     */
    public static void checkForPath(Context context, String path, boolean isRootExplorer) {
        // TODO: Add support for SMB and OTG in this function
        if (!new File(path).exists()) {
            Toast.makeText(context, context.getString(R.string.bookmark_lost), Toast.LENGTH_SHORT).show();
            Operations.mkdir(RootHelper.generateBaseFile(new File(path), true), context,
                    isRootExplorer, new Operations.ErrorCallBack() {
                        //TODO empty
                        @Override
                        public void exists(HybridFile file) {

                        }

                        @Override
                        public void launchSAF(HybridFile file) {

                        }

                        @Override
                        public void launchSAF(HybridFile file, HybridFile file1) {

                        }

                        @Override
                        public void done(HybridFile hFile, boolean b) {

                        }

                        @Override
                        public void invalidName(HybridFile file) {

                        }
                    });
        }
    }

}
