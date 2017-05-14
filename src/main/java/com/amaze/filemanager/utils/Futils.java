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

package com.amaze.filemanager.utils;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.BasicActivity;
import com.amaze.filemanager.activities.DbViewer;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.HiddenAdapter;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.services.asynctasks.CountFolderItems;
import com.amaze.filemanager.services.asynctasks.GenerateHashes;
import com.amaze.filemanager.services.asynctasks.LoadFolderSpaceData;
import com.amaze.filemanager.ui.LayoutElement;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.share.ShareTask;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import eu.chainfire.libsuperuser.Shell;
import jcifs.smb.SmbFile;

import static com.amaze.filemanager.R.string.loading;
import static com.amaze.filemanager.activities.MainActivity.dataUtils;

public class Futils {

    private static final SimpleDateFormat sSDF = new SimpleDateFormat("MMM dd, yyyy");
    public static final int READ = 4;
    public static final int WRITE = 2;
    public static final int EXECUTE = 1;
    private Toast studioCount;

    public Futils() {
    }
    //methods for fastscroller
    public static float getViewRawY(View view) {
        int[] location = new int[2];
        location[0] = 0;
        location[1] = (int) view.getY();
        ((View)view.getParent()).getLocationInWindow(location);
        return location[1];
    }

    public static float getValueInRange(float min, float max, float value) {
        float minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

    public static MaterialDialog showBasicDialog(Activity m, String fabskin, AppTheme appTheme, String[] texts) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m)
                .content(texts[0])
                .widgetColor(Color.parseColor(fabskin))
                .theme(appTheme.getMaterialDialogTheme())
                .title(texts[1])
                .positiveText(texts[2])
                .positiveColor(Color.parseColor(fabskin))
                .negativeText(texts[3])
                .negativeColor(Color.parseColor(fabskin));
        if (texts[4] != (null)) {
            a.neutralText(texts[4])
             .neutralColor(Color.parseColor(fabskin));
        }
        return a.build();
    }

    public MaterialDialog showNameDialog(final MainActivity m, String[] texts) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(texts[0], texts[1], false, new
                MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                    }
                });
        a.widgetColor(Color.parseColor(BaseActivity.accentSkin));

        a.theme(m.getAppTheme().getMaterialDialogTheme());
        a.title(texts[2]);
        a.positiveText(texts[3]);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.neutralText(texts[4]);
        if (texts[5] != (null)) {
            a.negativeText(texts[5]);
            a.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        }
        MaterialDialog dialog = a.build();
        return dialog;
    }

    public static long folderSize(File directory, OnProgressUpdate<Long> updateState) {
        long length = 0;
        try {
            for (File file:directory.listFiles()) {
                if (file.isFile())
                    length += file.length();
                else
                    length += folderSize(file, updateState);

                if(updateState != null)
                    updateState.onUpdate(length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return length;
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
     * @param path
     * @param context
     * @return
     */
    public static long folderSize(String path, Context context) {
        long length = 0L;
        for (BaseFile baseFile : OTGUtil.getDocumentFilesList(path, context)) {
            if (baseFile.isDirectory()) length += folderSize(baseFile.getPath(), context);
            else length += baseFile.length();

        }
        return length;
    }

    public static void setTint(CheckBox box, int color) {
        if(Build.VERSION.SDK_INT>=21)return;
        ColorStateList sl = new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked}
        }, new int[]{
                Color.parseColor("#666666"),
                color
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            box.setButtonTintList(sl);
        } else {
            Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(box.getContext(), R.drawable.abc_btn_check_material));
            DrawableCompat.setTintList(drawable, sl);
            box.setButtonDrawable(drawable);
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

    private boolean isSelfDefault(File f, Context c){
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), MimeTypes.getMimeType(f));
        String s="";
        ResolveInfo rii = c.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (rii !=  null && rii.activityInfo != null) s = rii.activityInfo.packageName;

        return s.equals("com.amaze.filemanager") || rii == null;
    }

    public void openunknown(File f, Context c, boolean forcechooser) {
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

    public void openWith(final File f,final Context c) {
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

    public void deleteFiles(ArrayList<LayoutElement> a, final MainFragment b, List<Integer> pos, AppTheme appTheme) {
        final MaterialDialog.Builder c = new MaterialDialog.Builder(b.getActivity());
        c.title(b.getResources().getString(R.string.confirm));

        int fileCounter = 0, dirCounter = 0;
        long longSizeTotal = 0;
        final ArrayList<BaseFile> todelete = new ArrayList<>();
        StringBuilder dirNames = new StringBuilder();
        StringBuilder fileNames = new StringBuilder();
        for (int i = 0; i < pos.size(); i++) {
            final LayoutElement elem = a.get(pos.get(i));
            todelete.add(elem.generateBaseFile());
            if (elem.isDirectory()) {
                dirNames.append("\n")
                        .append(++dirCounter)
                        .append(". ")
                        .append(elem.getTitle());
                // TODO: Get folder size ?
            } else {
                fileNames.append("\n")
                        .append(++fileCounter)
                        .append(". ")
                        .append(elem.getTitle())
                        .append(" (")
                        .append(elem.getSize())
                        .append(")");
                longSizeTotal += elem.getlongSize();
            }
        }

        String titleFiles = b.getResources().getString(R.string.title_files).toUpperCase();
        String titleDirs = b.getResources().getString(R.string.title_dirs).toUpperCase();
      
        StringBuilder message = new StringBuilder();
        message.append(b.getResources().getString(R.string.questiondelete))
                .append("\n\n");
        if (dirCounter == 0 && fileCounter == 1) {
            final LayoutElement elem = a.get(pos.get(0));
            message.append(elem.getTitle())
                    .append(" (")
                    .append(elem.getSize())
                    .append(")");
        } else if (fileCounter == 0) {
            message.append(titleDirs)
                    .append(":")
                    .append(dirNames);
        } else if(dirCounter == 0) {
            message.append(titleFiles)
                    .append(":")
                    .append(fileNames);
        } else {
            message.append(titleDirs)
                    .append(":")
                    .append(dirNames)
                    .append("\n\n")
                    .append(titleFiles)
                    .append(":")
                    .append(fileNames);
        }

        if (fileCounter + dirCounter > 1 && longSizeTotal > 0) {
            message.append("\n\n")
                    .append(b.getResources().getString(R.string.total))
                    .append(" ")
                    .append(Formatter.formatFileSize(b.getContext(), longSizeTotal));
        }

        c.content(message.toString());
        c.theme(appTheme.getMaterialDialogTheme());
        c.negativeText(b.getResources().getString(R.string.cancel).toUpperCase());
        c.positiveText(b.getResources().getString(R.string.delete).toUpperCase());
        c.positiveColor(Color.parseColor(b.fabSkin));
        c.negativeColor(Color.parseColor(b.fabSkin));
        c.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                Toast.makeText(b.getActivity(), b.getResources().getString(R.string.deleting), Toast.LENGTH_SHORT).show();
                b.MAIN_ACTIVITY.mainActivityHelper.deleteFiles(todelete);
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

                //materialDialog.cancel();
            }
        });
        c.build().show();
    }

    /**
     * Method determines if there is something to go back to
     * @param currentFile
     * @param context
     * @return
     */
    public boolean canGoBack(Context context, HFile currentFile) {

        // we're on main thread and can't list the cloud files
        switch (currentFile.getMode()) {
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

    public String getDate(File f) {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy | KK:mm a");
        return sdf.format(f.lastModified());
    }

    public static String getDate(long f) {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy | KK:mm a");
        return sdf.format(f);
    }

    public static String getDate(long f, String year) {
        String date = sSDF.format(f);
        if(date.substring(date.length()-4,date.length()).equals(year))
            date=date.substring(0,date.length()-6);
        return date;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void showPropertiesDialogWithPermissions(BaseFile baseFile, final String permissions,
                                                    BasicActivity basic, boolean isRoot, AppTheme appTheme) {
        showPropertiesDialog(baseFile, permissions, basic, isRoot, appTheme, true, false);
    }

    public void showPropertiesDialogWithoutPermissions(final BaseFile f, BasicActivity activity, AppTheme appTheme) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, false);
    }
    public void showPropertiesDialogForStorage(final BaseFile f, BasicActivity activity, AppTheme appTheme) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, true);
    }

    private void showPropertiesDialog(final BaseFile baseFile, final String permissions,
                                                    BasicActivity basic, boolean isRoot, AppTheme appTheme,
                                                    boolean showPermissions, boolean forStorage) {
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final Context c = basic.getApplicationContext();
        int accentColor = basic.getColorPreference().getColor(ColorUsage.ACCENT);
        long last = baseFile.getDate();
        final String date = getDate(last),
                items = basic.getResources().getString(R.string.calculating),
                name  = baseFile.getName(),
                parent = baseFile.getReadablePath(baseFile.getParent(c));

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        String fabskin = PreferenceUtils.getAccentString(sharedPrefs);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(basic);
        builder.title(basic.getResources().getString(R.string.properties));
        builder.theme(appTheme.getMaterialDialogTheme());

        View v = basic.getLayoutInflater().inflate(R.layout.properties_dialog, null);
        TextView itemsText = (TextView) v.findViewById(R.id.t7);

        /*View setup*/ {
            TextView mNameTitle = (TextView) v.findViewById(R.id.title_name);
            mNameTitle.setTextColor(accentColor);

            TextView mDateTitle = (TextView) v.findViewById(R.id.title_date);
            mDateTitle.setTextColor(accentColor);

            TextView mSizeTitle = (TextView) v.findViewById(R.id.title_size);
            mSizeTitle.setTextColor(accentColor);

            TextView mLocationTitle = (TextView) v.findViewById(R.id.title_location);
            mLocationTitle.setTextColor(accentColor);

            TextView md5Title = (TextView) v.findViewById(R.id.title_md5);
            md5Title.setTextColor(accentColor);

            TextView sha256Title = (TextView) v.findViewById(R.id.title_sha256);
            sha256Title.setTextColor(accentColor);

            ((TextView) v.findViewById(R.id.t5)).setText(name);
            ((TextView) v.findViewById(R.id.t6)).setText(parent);
            itemsText.setText(items);
            ((TextView) v.findViewById(R.id.t8)).setText(date);

            LinearLayout mNameLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_name);
            LinearLayout mLocationLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_location);
            LinearLayout mSizeLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_size);
            LinearLayout mDateLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_date);

            // setting click listeners for long press
            mNameLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, name);
                    Toast.makeText(c, c.getResources().getString(R.string.name) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            mLocationLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, parent);
                    Toast.makeText(c, c.getResources().getString(R.string.location) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            mSizeLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, items);
                    Toast.makeText(c, c.getResources().getString(R.string.size) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            mDateLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Futils.copyToClipboard(c, date);
                    Toast.makeText(c, c.getResources().getString(R.string.date) + " " +
                            c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }

        CountFolderItems countFolderItems = new CountFolderItems(c, itemsText, baseFile);
        countFolderItems.executeOnExecutor(executor);

        GenerateHashes hashGen = new GenerateHashes(baseFile, c, v);
        hashGen.executeOnExecutor(executor);

        /*Chart creation and data loading*/ {
            boolean isRightToLeft = c.getResources().getBoolean(R.bool.is_right_to_left);
            boolean isDarkTheme = appTheme.getMaterialDialogTheme() == Theme.DARK;
            PieChart chart = (PieChart) v.findViewById(R.id.chart);

            chart.setTouchEnabled(false);
            chart.setDrawEntryLabels(false);
            chart.setDescription(null);
            chart.setNoDataText(c.getString(loading));
            chart.setRotationAngle(!isRightToLeft? 0f:180f);
            chart.setHoleColor(Color.TRANSPARENT);
            chart.setCenterTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

            chart.getLegend().setEnabled(true);
            chart.getLegend().setForm(Legend.LegendForm.CIRCLE);
            chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            chart.getLegend().setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

            chart.animateY(1000);

            if(forStorage) {
                final String[] LEGENDS = new String[]{c.getString(R.string.used), c.getString(R.string.free)};
                final int[] COLORS = {Utils.getColor(c, R.color.piechart_red), Utils.getColor(c, R.color.piechart_green)};

                long totalSpace = getTotalSpace(baseFile),
                        freeSpace = getFreeSpace(baseFile),
                        usedSpace = totalSpace - freeSpace;

                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(usedSpace, LEGENDS[0]));
                entries.add(new PieEntry(freeSpace, LEGENDS[1]));

                PieDataSet set = new PieDataSet(entries, null);
                set.setColors(COLORS);
                set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                set.setSliceSpace(5f);
                set.setAutomaticallyDisableSliceSpacing(true);
                set.setValueLinePart2Length(1.05f);
                set.setSelectionShift(0f);

                PieData pieData = new PieData(set);
                pieData.setValueFormatter(new SizeFormatter(c));
                pieData.setValueTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

                String totalSpaceFormatted = Formatter.formatFileSize(c, totalSpace);

                chart.setCenterText(new SpannableString(c.getString(R.string.total) + "\n" + totalSpaceFormatted));
                chart.setData(pieData);
            } else {
                LoadFolderSpaceData loadFolderSpaceData = new LoadFolderSpaceData(c, appTheme, chart, baseFile);
                loadFolderSpaceData.executeOnExecutor(executor);
            }

            chart.invalidate();
        }

        if(!forStorage && showPermissions) {
            final MainFragment main = ((MainActivity) basic).mainFragment;
            AppCompatButton appCompatButton = (AppCompatButton) v.findViewById(R.id.permissionsButton);
            appCompatButton.setAllCaps(true);

            final View permissionsTable = v.findViewById(R.id.permtable);
            final View button = v.findViewById(R.id.set);
            if (isRoot && permissions.length() > 6) {
                appCompatButton.setVisibility(View.VISIBLE);
                appCompatButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (permissionsTable.getVisibility() == View.GONE) {
                            permissionsTable.setVisibility(View.VISIBLE);
                            button.setVisibility(View.VISIBLE);
                            setPermissionsDialog(permissionsTable, button, baseFile, permissions, c,
                                    main);
                        } else {
                            button.setVisibility(View.GONE);
                            permissionsTable.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }

        builder.customView(v, true);
        builder.positiveText(basic.getResources().getString(R.string.ok));
        builder.positiveColor(Color.parseColor(fabskin));
        builder.dismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                executor.shutdown();
            }
        });

        MaterialDialog materialDialog = builder.build();
        materialDialog.show();
        materialDialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);

        /*
        View bottomSheet = c.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(BottomSheetBehavior.STATE_DRAGGING);
        */
    }

    public void showCloudDialog(final MainActivity mainActivity, AppTheme appTheme, final OpenMode openMode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String fabskin = PreferenceUtils.getAccentString(sp);
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivity);

        switch (openMode) {
            case DROPBOX:
                builder.title(mainActivity.getResources().getString(R.string.cloud_dropbox));
                break;
            case BOX:
                builder.title(mainActivity.getResources().getString(R.string.cloud_box));
                break;
            case GDRIVE:
                builder.title(mainActivity.getResources().getString(R.string.cloud_drive));
                break;
            case ONEDRIVE:
                builder.title(mainActivity.getResources().getString(R.string.cloud_onedrive));
                break;
        }

        builder.theme(appTheme.getMaterialDialogTheme());
        builder.content(mainActivity.getResources().getString(R.string.cloud_remove));

        builder.positiveText(mainActivity.getResources().getString(R.string.yes));
        builder.positiveColor(Color.parseColor(fabskin));
        builder.negativeText(mainActivity.getResources().getString(R.string.no));
        builder.negativeColor(Color.parseColor(fabskin));

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                mainActivity.deleteConnection(openMode);
            }
        });

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void showEncryptWarningDialog(final Intent intent, final MainFragment main, AppTheme appTheme,
                                         final RecyclerAdapter.EncryptButtonCallbackInterface
                                                 encryptButtonCallbackInterface) {

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(main.getContext());

        final MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.warning));
        builder.content(main.getResources().getString(R.string.crypt_warning_key));
        builder.theme(appTheme.getMaterialDialogTheme());
        builder.negativeText(main.getResources().getString(R.string.warning_never_show));
        builder.positiveText(main.getResources().getString(R.string.warning_confirm));
        builder.positiveColor(Color.parseColor(main.fabSkin));

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                try {
                    encryptButtonCallbackInterface.onButtonPressed(intent);
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast.makeText(main.getActivity(),
                            main.getResources().getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                preferences.edit().putBoolean(Preffrag.PREFERENCE_CRYPT_WARNING_REMEMBER, true).apply();
                try {
                    encryptButtonCallbackInterface.onButtonPressed(intent);
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast.makeText(main.getActivity(),
                            main.getResources().getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.show();
    }

    public void showEncryptAuthenticateDialog(final Intent intent, final MainFragment main, AppTheme appTheme,
                                              final RecyclerAdapter.EncryptButtonCallbackInterface
                                                      encryptButtonCallbackInterface) {

        MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.crypt_encrypt));

        View rootView = View.inflate(main.getActivity(), R.layout.dialog_encrypt_authenticate, null);

        final AppCompatEditText passwordEditText = (AppCompatEditText)
                rootView.findViewById(R.id.edit_text_dialog_encrypt_password);
        final AppCompatEditText passwordConfirmEditText = (AppCompatEditText)
                rootView.findViewById(R.id.edit_text_dialog_encrypt_password_confirm);

        builder.customView(rootView, true);

        builder.positiveText(main.getResources().getString(R.string.ok));
        builder.negativeText(main.getResources().getString(R.string.cancel));
        builder.theme(appTheme.getMaterialDialogTheme());
        builder.positiveColor(Color.parseColor(main.fabSkin));
        builder.negativeColor(Color.parseColor(main.fabSkin));

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.cancel();
            }
        });

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {

            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                if (TextUtils.isEmpty(passwordEditText.getText()) ||
                        TextUtils.isEmpty(passwordConfirmEditText.getText())) {
                    dialog.cancel();
                    return;
                }

                try {
                    encryptButtonCallbackInterface.onButtonPressed(intent,
                            passwordEditText.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(main.getActivity(),
                            main.getResources().getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.show();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void showDecryptFingerprintDialog(final Intent intent, final MainFragment main, AppTheme appTheme,
                                             final RecyclerAdapter.DecryptButtonCallbackInterface
                                                     decryptButtonCallbackInterface) throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, UnrecoverableEntryException, InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchProviderException, BadPaddingException, KeyStoreException, IllegalBlockSizeException {

        MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.crypt_decrypt));

        View rootView = View.inflate(main.getActivity(),
                R.layout.dialog_decrypt_fingerprint_authentication, null);

        Button cancelButton = (Button) rootView.findViewById(R.id.button_decrypt_fingerprint_cancel);
        cancelButton.setTextColor(Color.parseColor(main.fabSkin));
        builder.customView(rootView, true);
        builder.canceledOnTouchOutside(false);

        builder.theme(appTheme.getMaterialDialogTheme());

        final MaterialDialog dialog = builder.show();
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        FingerprintManager manager = (FingerprintManager) main.getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        FingerprintManager.CryptoObject object = new
                FingerprintManager.CryptoObject(CryptUtil.initCipher(main.getContext()));

        FingerprintHandler handler = new FingerprintHandler(main.getActivity(), intent, dialog,
                decryptButtonCallbackInterface);
        handler.authenticate(manager, object);
    }

    public void showDecryptDialog(final Intent intent, final MainFragment main, AppTheme appTheme,
                                  final String password,
                                  final RecyclerAdapter.DecryptButtonCallbackInterface
                                          decryptButtonCallbackInterface) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.crypt_decrypt));

        builder.input(main.getResources().getString(R.string.authenticate_password), "", false,
                new MaterialDialog.InputCallback() {
            @Override
            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
            }
        });

        builder.theme(appTheme.getMaterialDialogTheme());
        builder.positiveText(main.getResources().getString(R.string.ok));
        builder.negativeText(main.getResources().getString(R.string.cancel));
        builder.positiveColor(Color.parseColor(main.fabSkin));
        builder.negativeColor(Color.parseColor(main.fabSkin));
        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                EditText editText = dialog.getInputEditText();

                if (editText.getText().toString().equals(password))
                    decryptButtonCallbackInterface.confirm(intent);
                else decryptButtonCallbackInterface.failed();
            }
        });
        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public static long[] getSpaces(HFile hFile, final OnProgressUpdate<Long[]> updateState) {
        if(hFile.isSmb()) {
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
                        folderSize = folderSize(new File(hFile.getPath()),
                                new OnProgressUpdate<Long>() {
                                    @Override
                                    public void onUpdate(Long data) {
                                        if(updateState != null)
                                            updateState.onUpdate(new Long[] {totalSpace, freeSpace, data});
                                    }
                                });

                return new long[] {totalSpace, freeSpace, folderSize};
            } catch (Exception e) {
                return new long[]{-1, -1, -1};
            }
        } else {
            return new long[]{-1, -1, -1};
        }
    }

    public static long getFreeSpace(HFile hFile) {
        if (hFile.isSmb()) {
            return -1;
        } else if (hFile.isDropBoxFile()) {
            CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
            CloudMetaData fileMetaDataDropbox = cloudStorageDropbox.getMetadata(CloudUtil.stripPath(OpenMode.DROPBOX,
                    hFile.getPath()));

            return (cloudStorageDropbox.getAllocation().getTotal() - cloudStorageDropbox.getAllocation().getUsed());
        } else if (hFile.isBoxFile()) {
            CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
            CloudMetaData fileMetaDataBox = cloudStorageBox.getMetadata(CloudUtil.stripPath(OpenMode.BOX,
                    hFile.getPath()));

            return (cloudStorageBox.getAllocation().getTotal() - cloudStorageBox.getAllocation().getUsed());
        } else if (hFile.isGoogleDriveFile()) {
            CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);

            CloudMetaData fileMetaDataGDrive = cloudStorageGDrive.getMetadata(CloudUtil.stripPath(OpenMode.GDRIVE,
                    hFile.getPath()));

            return (cloudStorageGDrive.getAllocation().getTotal() - cloudStorageGDrive.getAllocation().getUsed());
        } else if (hFile.isOneDriveFile()) {
            CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

            CloudMetaData fileMetaDataOneDrive = cloudStorageOneDrive.getMetadata(CloudUtil.stripPath(OpenMode.ONEDRIVE,
                    hFile.getPath()));
            return (cloudStorageOneDrive.getAllocation().getTotal() - cloudStorageOneDrive.getAllocation().getUsed());
        } else if (!hFile.isOtgFile() && !hFile.isCustomPath()
                && !android.util.Patterns.EMAIL_ADDRESS.matcher(hFile.getPath()).matches()) {
            try {
                return new File(hFile.getPath()).getFreeSpace();
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static long getTotalSpace(HFile hFile) {
        if(hFile.isSmb()) {
            return -1;
        } else if (hFile.isDropBoxFile()) {
            CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);

            return cloudStorageDropbox.getAllocation().getTotal();
        } else if (hFile.isBoxFile()) {
            CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);

            return cloudStorageBox.getAllocation().getTotal();
        } else if (hFile.isGoogleDriveFile()) {
            CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);

            return cloudStorageGDrive.getAllocation().getTotal();
        } else if (hFile.isOneDriveFile()) {
            CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

            return cloudStorageOneDrive.getAllocation().getTotal();
        } else if (!hFile.isOtgFile() && !hFile.isCustomPath()
                && !android.util.Patterns.EMAIL_ADDRESS.matcher(hFile.getPath()).matches()) {
            try {
                return new File(hFile.getPath()).getTotalSpace();
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
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
            showArchiveDialog(f, m);
        } else if(f.getName().toLowerCase().endsWith(".apk")) {
            showPackageDialog(f, m);
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

    public static void showSMBHelpDialog(Context m,String acc){
        MaterialDialog.Builder b=new MaterialDialog.Builder(m);
        b.content(m.getText(R.string.smb_instructions));
        b.positiveText(R.string.doit);
        b.positiveColor(Color.parseColor(acc));
        b.build().show();
    }

    public void showPackageDialog(final File f, final MainActivity m) {
        MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
        mat.title(R.string.packageinstaller).content(R.string.pitext)
           .positiveText(R.string.install)
           .negativeText(R.string.view)
           .neutralText(R.string.cancel)
           .positiveColor(Color.parseColor(BaseActivity.accentSkin))
           .negativeColor(Color.parseColor(BaseActivity.accentSkin))
           .neutralColor(Color.parseColor(BaseActivity.accentSkin))
           .callback(new MaterialDialog.ButtonCallback() {
               @Override
               public void onPositive(MaterialDialog materialDialog) {
                   openunknown(f, m, false);
               }

               @Override
               public void onNegative(MaterialDialog materialDialog) {
                   m.openZip(f.getPath());
               }
           })
           .theme(m.getAppTheme().getMaterialDialogTheme())
           .build()
           .show();
    }

    public void showArchiveDialog(final File f, final MainActivity m) {
        MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
        mat.title(R.string.archive)
                .content(R.string.archtext)
                .positiveText(R.string.extract)
                .negativeText(R.string.view)
                .neutralText(R.string.cancel)
                .positiveColor(Color.parseColor(BaseActivity.accentSkin))
                .negativeColor(Color.parseColor(BaseActivity.accentSkin))
                .neutralColor(Color.parseColor(BaseActivity.accentSkin))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        m. mainActivityHelper.extractFile(f);
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        //m.addZipViewTab(f.getPath());
                        if (f.getName().toLowerCase().endsWith(".rar"))
                            m.openRar(Uri.fromFile(f).toString());
                        else
                            m.openZip(Uri.fromFile(f).toString());
                    }
                });
        if (m.getAppTheme().equals(AppTheme.DARK)) mat.theme(Theme.DARK);
        MaterialDialog b = mat.build();

        if (!f.getName().toLowerCase().endsWith(".rar") && !f.getName().toLowerCase().endsWith(".jar") && !f.getName().toLowerCase().endsWith(".apk") && !f.getName().toLowerCase().endsWith(".zip"))
            b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
        b.show();

    }

    public LayoutElement newElement(BitmapDrawable i, String d, String permissions, String symlink, String size, long longSize, boolean directorybool, boolean b, String date) {
        LayoutElement item = new LayoutElement(i, new File(d).getName(), d,permissions,symlink,size,longSize,b,date,directorybool);
        return item;
    }

    public ArrayList<HFile> toHFileArray(ArrayList<String> a) {
        ArrayList<HFile> b = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            HFile hFile=new HFile(OpenMode.UNKNOWN,a.get(i));
            hFile.generateMode(null);
            b.add(hFile);
        }
        return b;
    }

    public void showCompressDialog(final MainActivity m, final ArrayList<BaseFile> b, final String current) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(m.getResources().getString(R.string.enterzipname), ".zip", false, new
                MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                    }
                });
        a.widgetColor(Color.parseColor(BaseActivity.accentSkin));
        a.theme(m.getAppTheme().getMaterialDialogTheme());
        a.title(m.getResources().getString(R.string.enterzipname));
        a.positiveText(R.string.create);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                if (materialDialog.getInputEditText().getText().toString().equals(".zip"))
                    Toast.makeText(m, "File should have a name", Toast.LENGTH_SHORT).show();
                else {
                    String name = current + "/" + materialDialog.getInputEditText().getText().toString();
                    m.mainActivityHelper.compressFiles(new File(name), b);
                }
            }
        });
        a.negativeText(m.getResources().getString(R.string.cancel));
        a.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.build().show();
    }

    public void showSortDialog(final MainFragment m, AppTheme appTheme) {
        String[] sort = m.getResources().getStringArray(R.array.sortby);
        int current = Integer.parseInt(m.sharedPref.getString("sortby", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.theme(appTheme.getMaterialDialogTheme());
        a.items(sort).itemsCallbackSingleChoice(current > 3 ? current - 4 : current, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                return true;
            }
        });

        a.negativeText(R.string.ascending).positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.positiveText(R.string.descending).negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.sharedPref.edit().putString("sortby", "" + dialog.getSelectedIndex()).commit();
                m.getSortModes();
                m.updateList();
                dialog.dismiss();
            }
        });

        a.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.sharedPref.edit().putString("sortby", "" + (dialog.getSelectedIndex() + 4)).commit();
                m.getSortModes();
                m.updateList();
                dialog.dismiss();
            }
        });
        a.title(R.string.sortby);
        a.build().show();
    }

    public void showSortDialog(final AppsList m, AppTheme appTheme) {
        String[] sort = m.getResources().getStringArray(R.array.sortbyApps);
        int current = Integer.parseInt(m.Sp.getString("sortbyApps", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.theme(appTheme.getMaterialDialogTheme());
        a.items(sort).itemsCallbackSingleChoice(current > 2 ? current - 3 : current, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                return true;
            }
        });
        a.negativeText(R.string.ascending).positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.positiveText(R.string.descending).negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.Sp.edit().putString("sortbyApps", "" + dialog.getSelectedIndex()).commit();
                m.getSortModes();
                m.getLoaderManager().restartLoader(AppsList.ID_LOADER_APP_LIST, null, m);
                dialog.dismiss();
            }
        });

        a.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                m.Sp.edit().putString("sortbyApps", "" + (dialog.getSelectedIndex() + 3)).commit();
                m.getSortModes();
                m.getLoaderManager().restartLoader(AppsList.ID_LOADER_APP_LIST, null, m);
                dialog.dismiss();
            }
        });

        a.title(R.string.sortby);
        a.build().show();
    }

    public void showHistoryDialog(final DataUtils dataUtils, final MainFragment m, AppTheme appTheme) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.negativeText(R.string.clear);
        a.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.title(R.string.history);
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dataUtils.clearHistory();
            }
        });
        a.theme(appTheme.getMaterialDialogTheme());

        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(), m, this, R.layout.bookmarkrow,
                toHFileArray(dataUtils.getHistory()), null, true);
        a.adapter(adapter, null);

        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }

    public void showHiddenDialog(DataUtils dataUtils, final MainFragment m, AppTheme appTheme) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.title(R.string.hiddenfiles);
        a.theme(appTheme.getMaterialDialogTheme());
        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(), m, this, R.layout.bookmarkrow,
                toHFileArray(dataUtils.getHiddenfiles()), null, false);
        a.adapter(adapter, null);
        a.dividerColor(Color.GRAY);
        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }

    public boolean isAtleastKitkat(){
        return Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT;
    }

    public void setPermissionsDialog(final View v, View but, final HFile file,
                                     final String f, final Context context, final MainFragment mainFrag) {
        final CheckBox readown = (CheckBox) v.findViewById(R.id.creadown);
        final CheckBox readgroup = (CheckBox) v.findViewById(R.id.creadgroup);
        final CheckBox readother = (CheckBox) v.findViewById(R.id.creadother);
        final CheckBox writeown = (CheckBox) v.findViewById(R.id.cwriteown);
        final CheckBox writegroup = (CheckBox) v.findViewById(R.id.cwritegroup);
        final CheckBox writeother = (CheckBox) v.findViewById(R.id.cwriteother);
        final CheckBox exeown = (CheckBox) v.findViewById(R.id.cexeown);
        final CheckBox exegroup = (CheckBox) v.findViewById(R.id.cexegroup);
        final CheckBox exeother = (CheckBox) v.findViewById(R.id.cexeother);
        String perm = f;
        if (perm.length() < 6) {
            v.setVisibility(View.GONE);
            but.setVisibility(View.GONE);
            Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Boolean[]> arrayList = parse(perm);
        Boolean[] read = arrayList.get(0);
        Boolean[] write = arrayList.get(1);
        final Boolean[] exe = arrayList.get(2);
        readown.setChecked(read[0]);
        readgroup.setChecked(read[1]);
        readother.setChecked(read[2]);
        writeown.setChecked(write[0]);
        writegroup.setChecked(write[1]);
        writeother.setChecked(write[2]);
        exeown.setChecked(exe[0]);
        exegroup.setChecked(exe[1]);
        exeother.setChecked(exe[2]);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int a = 0, b = 0, c = 0;
                if (readown.isChecked()) a = 4;
                if (writeown.isChecked()) b = 2;
                if (exeown.isChecked()) c = 1;
                int owner = a + b + c;
                int d = 0;
                int e = 0;
                int f = 0;
                if (readgroup.isChecked()) d = 4;
                if (writegroup.isChecked()) e = 2;
                if (exegroup.isChecked()) f = 1;
                int group = d + e + f;
                int g = 0, h = 0, i = 0;
                if (readother.isChecked()) g = 4;
                if (writeother.isChecked()) h = 2;
                if (exeother.isChecked()) i = 1;
                int other = g + h + i;
                String finalValue = owner + "" + group + "" + other;

                String command = "chmod " + finalValue + " " + file.getPath();
                if (file.isDirectory())
                    command = "chmod -R " + finalValue + " \"" + file.getPath() + "\"";

                try {
                    RootHelper.runShellCommand(command, new Shell.OnCommandResultListener() {
                        @Override
                        public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                            if (exitCode < 0) {
                                Toast.makeText(context, mainFrag.getString(R.string.operationunsuccesful),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(context,
                                        mainFrag.getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    mainFrag.updateList();
                } catch (RootNotPermittedException e1) {
                    Toast.makeText(context, mainFrag.getResources().getString(R.string.rootfailure),
                            Toast.LENGTH_LONG).show();
                    e1.printStackTrace();
                }

            }
        });
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

    public ArrayList<Boolean[]> parse(String permLine) {
        ArrayList<Boolean[]> arrayList= new ArrayList<>();
        Boolean[] read=new Boolean[]{false,false,false};
        Boolean[] write=new Boolean[]{false,false,false};
        Boolean[] execute=new Boolean[]{false,false,false};
        int owner = 0;
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

    public static class SizeFormatter implements IValueFormatter {

        private Context context;

        public SizeFormatter(Context c) {
            context = c;
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                                        ViewPortHandler viewPortHandler) {
            String prefix = entry.getData() != null && entry.getData() instanceof String?
                    (String) entry.getData():"";

            return prefix + Formatter.formatFileSize(context, (long) value);
        }
    }

}
