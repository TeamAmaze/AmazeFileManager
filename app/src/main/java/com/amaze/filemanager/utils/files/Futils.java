/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.DbViewer;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.ui.LayoutElement;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnProgressUpdate;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.share.ShareTask;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jcifs.smb.SmbFile;

/**
 * Functions that deal with files
 */
public class Futils {

    public static final int READ = 4;
    public static final int WRITE = 2;
    public static final int EXECUTE = 1;
    private Toast studioCount;
    private DataUtils dataUtils = DataUtils.getInstance();

    public Futils() {
    }

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

    public static long folderSize(HFile directory, OnProgressUpdate<Long> updateState) {
        return folderSize(new File(directory.getPath()), updateState);
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
    public static long folderSize(String path, Context context) {
        return getTotalBytes(OTGUtil.getDocumentFilesList(path, context), context);
    }

    /**
     * Helper method to calculate source files size
     */
    public static long getTotalBytes(ArrayList<BaseFile> files, Context context) {
        long totalBytes = 0L;
        for (BaseFile file : files) {
            totalBytes += getBaseFileSize(file, context);
        }
        return totalBytes;
    }

    private static long getBaseFileSize(BaseFile baseFile, Context context) {
        if (baseFile.isDirectory(context)) {
            return baseFile.folderSize(context);
        } else {
            return baseFile.length(context);
        }
    }

    public static void scanFile(String path, Context c) {
        System.out.println(path + " " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 19) {
            MediaScannerConnection.scanFile(c, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {

                @Override
                public void onScanCompleted(String path, Uri uri) {

                }
            });
        } else {
            Uri contentUri = Uri.fromFile(new File(path));
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
            c.sendBroadcast(mediaScanIntent);
        }
    }

    public void crossfade(View buttons,final View pathbar) {
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

    public void revealShow(final View view, boolean reveal) {
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

    public void crossfadeInverse(final View buttons,final View pathbar) {
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

    public void shareFiles(ArrayList<File> a, Activity c,int theme,int fab_skin) {
        shareFiles(a,c, AppTheme.fromIndex(theme), fab_skin);
    }

    public void shareCloudFile(String path, final OpenMode openMode, final Context context) {
        new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... params) {
                String shareFilePath = params[0];
                CloudStorage cloudStorage = dataUtils.getAccount(openMode);
                return cloudStorage.createShareLink(CloudUtil.stripPath(openMode, shareFilePath));
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                Futils.copyToClipboard(context, s);
                Toast.makeText(context,
                        context.getResources().getString(R.string.cloud_share_copied), Toast.LENGTH_LONG).show();
            }
        }.execute(path);
    }

    public void shareFiles(ArrayList<File> a, Activity c,AppTheme appTheme,int fab_skin) {
        ArrayList<Uri> uris = new ArrayList<>();
        boolean b = true;
        for (File f : a) {
            uris.add(Uri.fromFile(f));
        }
        System.out.println("uri done");
        String mime = MimeTypes.getMimeType(a.get(0));
        if (a.size() > 1)
            for (File f : a) {
                if (!mime.equals(MimeTypes.getMimeType(f))) {
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
        if (size <= 0)
            return 0;
        float digitGroups = (float) (size / (1024*1024));
        return digitGroups;
    }

    public static void openunknown(File f, Context c, boolean forcechooser) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);

        String type = MimeTypes.getMimeType(f);
        if(type!=null && type.trim().length()!=0 && !type.equals("*/*"))
        {
            Uri uri=fileToContentUri(c, f);
            if(uri==null)uri=Uri.fromFile(f);
            intent.setDataAndType(uri, type);
        Intent startintent;
        if (forcechooser) startintent=Intent.createChooser(intent, c.getResources().getString(R.string.openwith));
        else startintent=intent;
        try {
            c.startActivity(startintent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        Toast.makeText(c,R.string.noappfound,Toast.LENGTH_SHORT).show();
        openWith(f,c);
        }}else{openWith(f, c);}

    }

    public void openunknown(DocumentFile f, Context c, boolean forcechooser) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String type = f.getType();
        if(type!=null && type.trim().length()!=0 && !type.equals("*/*")) {
            intent.setDataAndType(f.getUri(), type);
            Intent startintent;
            if (forcechooser) startintent=Intent.createChooser(intent, c.getResources().getString(R.string.openwith));
            else startintent=intent;
            try {
                c.startActivity(startintent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(c,R.string.noappfound,Toast.LENGTH_SHORT).show();
                openWith(f,c);
            }
        } else {
            openWith(f, c);
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
        Uri uri = fileToContentUri(context, normalizedPath, EXTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        uri = fileToContentUri(context, normalizedPath, INTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        return null;
    }

    private static Uri fileToContentUri(Context context, String path, String volume) {
        String[] projection = null;
        final String where = MediaStore.MediaColumns.DATA + " = ?";
        Uri baseUri = MediaStore.Files.getContentUri(volume);
        boolean isMimeTypeImage = false, isMimeTypeVideo = false, isMimeTypeAudio = false;
        isMimeTypeImage = Icons.isPicture( path);
        if (!isMimeTypeImage) {
            isMimeTypeVideo = Icons.isVideo(path);
            if (!isMimeTypeVideo) {
                isMimeTypeAudio = Icons.isVideo(path);
            }
        }
        if (isMimeTypeImage || isMimeTypeVideo || isMimeTypeAudio) {
            projection = new String[]{BaseColumns._ID};
            if (isMimeTypeImage) {
                baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if (isMimeTypeVideo) {
                baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if (isMimeTypeAudio) {
                baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
        } else {
            projection = new String[]{BaseColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};
        }
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(baseUri, projection, where, new String[]{path}, null);
        try {
            if (c != null && c.moveToNext()) {
                boolean isValid = false;
                if (isMimeTypeImage || isMimeTypeVideo || isMimeTypeAudio) {
                    isValid = true;
                } else {
                    int type = c.getInt(c.getColumnIndexOrThrow(
                            MediaStore.Files.FileColumns.MEDIA_TYPE));
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

    public static void openWith(final File f,final Context c) {
        MaterialDialog.Builder a=new MaterialDialog.Builder(c);
        a.title(c.getResources().getString(R.string.openas));
        String[] items=new String[]{c.getResources().getString(R.string.text),c.getResources().getString(R.string.image),c.getResources().getString(R.string.video),c.getResources().getString(R.string.audio),c.getResources().getString(R.string.database),c.getResources().getString(R.string.other)};

        a.items(items).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                Uri uri = fileToContentUri(c, f);
                if (uri == null) uri = Uri.fromFile(f);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                switch (i) {
                    case 0:
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
                        intent = new Intent(c, DbViewer.class);
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
                    openWith(f, c);
                }
            }
        });
        try {
            a.build().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openWith(final DocumentFile f,final Context c) {
        MaterialDialog.Builder a=new MaterialDialog.Builder(c);
        a.title(c.getResources().getString(R.string.openas));
        String[] items=new String[]{c.getResources().getString(R.string.text),c.getResources().getString(R.string.image),c.getResources().getString(R.string.video),c.getResources().getString(R.string.audio),c.getResources().getString(R.string.database),c.getResources().getString(R.string.other)};

        a.items(items).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {

                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                switch (i) {
                    case 0:
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
                        intent = new Intent(c, DbViewer.class);
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
                    openWith(f, c);
                }
            }
        });
        try {
            a.build().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method determines if there is something to go back to
     * @param currentFile
     * @param context
     * @return
     */
    public boolean canGoBack(Context context, HFile currentFile) {
        switch (currentFile.getMode()) {

            // we're on main thread and can't list the cloud files
            case DROPBOX:
            case BOX:
            case GDRIVE:
            case ONEDRIVE:
            case OTG:
                return true;
            default:
                HFile parentFile = new HFile(currentFile.getMode(), currentFile.getParent(context));
                ArrayList<BaseFile> parentFiles = parentFile.listFiles(context, currentFile.isRoot());
                if (parentFiles == null) return false;
                else return true;
        }
    }

    public static long[] getSpaces(HFile hFile, Context context, final OnProgressUpdate<Long[]> updateState) {
        /*if(hFile.isSmb()) {
            if (hFile.isDirectory(context)) {

            }
            return new long[]{-1, -1, -1};
        } else if (hFile.isDropBoxFile()) {
            CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
            CloudMetaData fileMetaDataDropbox = cloudStorageDropbox.getMetadata(CloudUtil.stripPath(OpenMode.DROPBOX,
                    hFile.getPath()));

            return new long[]{cloudStorageDropbox.getAllocation().getTotal(),
                    (cloudStorageDropbox.getAllocation().getTotal() - cloudStorageDropbox.getAllocation().getUsed()),
                    folderSizeCloud(OpenMode.DROPBOX, fileMetaDataDropbox)
            };
        } else if (hFile.isBoxFile()) {
            CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
            CloudMetaData fileMetaDataBox = cloudStorageBox.getMetadata(CloudUtil.stripPath(OpenMode.BOX,
                    hFile.getPath()));

            return new long[]{cloudStorageBox.getAllocation().getTotal(),
                    (cloudStorageBox.getAllocation().getTotal() - cloudStorageBox.getAllocation().getUsed()),
                    folderSizeCloud(OpenMode.BOX, fileMetaDataBox)
            };
        } else if (hFile.isGoogleDriveFile()) {
            CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);

            CloudMetaData fileMetaDataGDrive = cloudStorageGDrive.getMetadata(CloudUtil.stripPath(OpenMode.GDRIVE,
                    hFile.getPath()));

            return new long[]{cloudStorageGDrive.getAllocation().getTotal(),
                    (cloudStorageGDrive.getAllocation().getTotal() - cloudStorageGDrive.getAllocation().getUsed()),
                    folderSizeCloud(OpenMode.GDRIVE, fileMetaDataGDrive)
            };
        } else if (hFile.isOneDriveFile()) {
            CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

            CloudMetaData fileMetaDataOneDrive = cloudStorageOneDrive.getMetadata(CloudUtil.stripPath(OpenMode.ONEDRIVE,
                    hFile.getPath()));
            return new long[]{cloudStorageOneDrive.getAllocation().getTotal(),
                    (cloudStorageOneDrive.getAllocation().getTotal() - cloudStorageOneDrive.getAllocation().getUsed()),
                    folderSizeCloud(OpenMode.ONEDRIVE, fileMetaDataOneDrive)
            };
        } else if (!hFile.isOtgFile() && !hFile.isCustomPath()
                && !android.util.Patterns.EMAIL_ADDRESS.matcher(hFile.getPath()).matches()) {
            try {
                File file = new File(hFile.getPath());
                final long totalSpace = file.getTotalSpace(),
                        freeSpace = file.getFreeSpace(),
                        folderSize = folderSize(hFile,
                                new OnProgressUpdate<Long>() {
                                    @Override
                                    public void onUpdate(Long data) {
                                        if(updateState != null)
                                            updateState.onUpdate(new Long[] {totalSpace, freeSpace, data});
                                    }
                                });

                final long totalSpace = hFile.length(context);
                final long freeSpace = hFile.getUsableSpace();
                long folderSize = 0l;

                if (hFile.isDirectory(context)) {
                    folderSize = folderSize(new File(hFile.getPath()),
                            new OnProgressUpdate<Long>() {
                                @Override
                                public void onUpdate(Long data) {
                                    if(updateState != null)
                                        updateState.onUpdate(new Long[] {totalSpace, freeSpace, data});
                                }
                    });
                }
                return new long[] {totalSpace, freeSpace, folderSize};
            } catch (Exception e) {
                return new long[]{-1, -1, -1};
            }
        } else {
            return new long[]{-1, -1, -1};
        }*/

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
                    .getSystemService(context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(context.getString(R.string.clipboard_path_copy), text);
            clipboard.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Bundle getPaths(String path, Context c) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> paths = new ArrayList<>();
        Bundle b = new Bundle();
        while (path.contains("/")) {

            paths.add(path);
            names.add(path.substring(1 + path.lastIndexOf("/"), path.length()));
            path = path.substring(0, path.lastIndexOf("/"));
        }
        names.remove("");
        paths.remove("/");
        names.add("root");
        paths.add("/");
        // Toast.makeText(c,paths.get(0)+"\n"+paths.get(1)+"\n"+paths.get(2),Toast.LENGTH_LONG).show();
        b.putStringArrayList("names", names);
        b.putStringArrayList("paths", paths);
        return b;
    }

    public boolean deletedirectory(File f){
        boolean b=true;
        for(File file:f.listFiles()){
            boolean c;
            if(file.isDirectory()){c=deletedirectory(file);}
            else {c=file.delete();}
            if(!c)b=false;

        }if(b)b=f.delete();
        return b;
    }

    public static boolean canListFiles(File f) {
        try {
            return f.canRead() && f.isDirectory();
        } catch (Exception e) {
            return false;
        }
    }

    public void openFile(final File f, final MainActivity m) {
        boolean defaultHandler = isSelfDefault(f, m);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(m);
        if (defaultHandler && f.getName().toLowerCase().endsWith(".zip") ||
                f.getName().toLowerCase().endsWith(".jar") ||
                f.getName().toLowerCase().endsWith(".rar")||
                f.getName().toLowerCase().endsWith(".tar") ||
                f.getName().toLowerCase().endsWith(".tar.gz")) {
            GeneralDialogCreation.showArchiveDialog(f, m);
        } else if(f.getName().toLowerCase().endsWith(".apk")) {
            GeneralDialogCreation.showPackageDialog(f, m);
        } else if (defaultHandler && f.getName().toLowerCase().endsWith(".db")) {
            Intent intent = new Intent(m, DbViewer.class);
            intent.putExtra("path", f.getPath());
            m.startActivity(intent);
        }  else if (Icons.isAudio(f.getPath())) {
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
                        if (studioCount!=null)
                            studioCount.cancel();
                        studioCount = Toast.makeText(m, sec + "", Toast.LENGTH_LONG);
                        studioCount.show();
                    }

                    @Override
                    public void onFinish() {
                        if (studioCount!=null)
                            studioCount.cancel();
                        studioCount = Toast.makeText(m, m.getString(R.string.opening),
                                Toast.LENGTH_LONG);
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
                Toast.makeText(m, m.getResources().getString(R.string.noappfound),Toast.LENGTH_LONG).show();
                openWith(f, m);
            }
        }
    }

    private boolean isSelfDefault(File f, Context c){
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), MimeTypes.getMimeType(f));
        String s="";
        ResolveInfo rii = c.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (rii !=  null && rii.activityInfo != null) s = rii.activityInfo.packageName;

        return s.equals("com.amaze.filemanager") || rii == null;
    }

    public void openFile(final DocumentFile f, final MainActivity m) {
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(m);
        try {
            openunknown(f, m, false);
        } catch (Exception e) {
            Toast.makeText(m, m.getResources().getString(R.string.noappfound),Toast.LENGTH_LONG).show();
            openWith(f, m);
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
            Intent intent = new Intent(m, DbViewer.class);
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
                Toast.makeText(m, m.getResources().getString(R.string.noappfound),Toast.LENGTH_LONG).show();
                openWith(f, m);
            }
        }*/
    }

    /**
     *
     * @deprecated use new LayoutElement()
     */
    public static LayoutElement newElement(BitmapDrawable i, String d, String permissions, String symlink,
                                    String size, long longSize, boolean directorybool, boolean b,
                                    String date) {
        return new LayoutElement(i, new File(d).getName(), d, permissions, symlink,
                size, longSize, b, date, directorybool);
    }

    public static ArrayList<HFile> toHFileArray(ArrayList<String> a) {
        ArrayList<HFile> b = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            HFile hFile=new HFile(OpenMode.UNKNOWN,a.get(i));
            hFile.generateMode(null);
            b.add(hFile);
        }
        return b;
    }

    /**
     * We're parsing a line returned from a stdout of shell.
     * @param line must be the line returned from a 'ls' command
     * @return
     */
    public static BaseFile parseName(String line) {
        boolean linked = false;
        String name = "", link = "", size = "-1", date = "";
        String[] array = line.split(" ");
        if(array.length<6)return null;
        for (int i = 0; i < array.length; i++) {
            if (array[i].contains("->") && array[0].startsWith("l")) {
                linked = true;
            }
        }
        int p = getColonPosition(array);
        if(p!=-1){
            date = array[p - 1] + " | " + array[p];
            size = array[p - 2];}
        if (!linked) {
            for (int i = p + 1; i < array.length; i++) {
                name = name + " " + array[i];
            }
            name = name.trim();
        } else {
            int q = getLinkPosition(array);
            for (int i = p + 1; i < q; i++) {
                name = name + " " + array[i];
            }
            name = name.trim();
            for (int i = q + 1; i < array.length; i++) {
                link = link + " " + array[i];
            }
        }
        long Size = (size==null || size.trim().length()==0)?-1:Long.parseLong(size);
        if(date.trim().length()>0) {
            ParsePosition pos = new ParsePosition(0);
            SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd | HH:mm");
            Date stringDate = simpledateformat.parse(date, pos);
            BaseFile baseFile=new BaseFile(name,array[0],stringDate.getTime(),Size,true);
            baseFile.setLink(link);
            return baseFile;
        }else {
            BaseFile baseFile= new BaseFile(name,array[0],new File("/").lastModified(),Size,true);
            baseFile.setLink(link);
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
        ArrayList<Boolean[]> arrayList= new ArrayList<>();
        Boolean[] read=new Boolean[]{false,false,false};
        Boolean[] write=new Boolean[]{false,false,false};
        Boolean[] execute=new Boolean[]{false,false,false};
        int owner = 0;// TODO: 17/5/2017 many variables are unused
        if (permLine.charAt(1) == 'r') {
            owner += READ;
            read[0]=true;
        }
        if (permLine.charAt(2) == 'w') {
            owner += WRITE;
            write[0]=true;
        }
        if (permLine.charAt(3) == 'x') {
            owner += EXECUTE;
            execute[0]=true;
        }
        int group = 0;
        if (permLine.charAt(4) == 'r') {
            group += READ;
            read[1]=true;
        }
        if (permLine.charAt(5) == 'w') {
            group += WRITE;
            write[1]=true;
        }
        if (permLine.charAt(6) == 'x') {
            group += EXECUTE;
            execute[1]=true;
        }
        int world = 0;
        if (permLine.charAt(7) == 'r') {
            world += READ;
            read[2]=true;
        }
        if (permLine.charAt(8) == 'w') {
            world += WRITE;
            write[2]=true;
        }
        if (permLine.charAt(9) == 'x') {
            world += EXECUTE;
            execute[2]=true;
        }
        arrayList.add(read);
        arrayList.add(write);
        arrayList.add(execute);
        return arrayList;
    }

}
