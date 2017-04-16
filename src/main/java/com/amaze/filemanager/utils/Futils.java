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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
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
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.DbViewer;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.HiddenAdapter;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.services.asynctasks.GenerateMD5Task;
import com.amaze.filemanager.ui.LayoutElements;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.share.ShareTask;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import eu.chainfire.libsuperuser.Shell;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

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


    public static long folderSize(File directory) {
        long length = 0;
        try {
            for (File file:directory.listFiles()) {

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
     * Helper method to get size of an otg folder
     * @param path
     * @param context
     * @return
     */
    public static long folderSize(String path, Context context) {
        long length = 0L;
        for (BaseFile baseFile : RootHelper.getDocumentFilesList(path, context)) {
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

    public void deleteFiles(ArrayList<LayoutElements> a, final MainFragment b, List<Integer> pos, AppTheme appTheme) {
        final MaterialDialog.Builder c = new MaterialDialog.Builder(b.getActivity());
        c.title(b.getResources().getString(R.string.confirm));
        int fileCounter = 0, dirCounter = 0;
        final ArrayList<BaseFile> todelete = new ArrayList<>();
        StringBuilder dirNames = new StringBuilder();
        StringBuilder fileNames = new StringBuilder();
        for (int i = 0; i < pos.size(); i++) {
            todelete.add(a.get(pos.get(i)).generateBaseFile());
            if(a.get(pos.get(i)).isDirectory())
                dirNames.append("\n")
                        .append(++dirCounter)
                        .append(". ")
                        .append(a.get(pos.get(i)).getTitle());
            else
                fileNames.append("\n")
                        .append(++fileCounter)
                        .append(". ")
                        .append(a.get(pos.get(i)).getTitle())
                        .append(" (")
                        .append(a.get(pos.get(i)).getSize())
                        .append(")");
        }

        String titleFiles = b.getResources().getString(R.string.title_files).toUpperCase();
        String titleDirs = b.getResources().getString(R.string.title_dirs).toUpperCase();

        if(fileNames.length() == 0)
            c.content(b.getResources().getString(R.string.questiondelete) + "\n\n" + "---" +
                    titleDirs + "---" + dirNames);
        else if(dirNames.length() == 0)
            c.content(b.getResources().getString(R.string.questiondelete) + "\n\n" + "---" +
                    titleFiles + "---" + fileNames);
        else
            c.content(b.getResources().getString(R.string.questiondelete) + "\n\n" + "---" +
                    titleDirs + "---" + dirNames + "\n\n" + "---" +
                    titleFiles + "---" + fileNames);
        c.theme(appTheme.getMaterialDialogTheme());
        c.negativeText(b.getResources().getString(R.string.no));
        c.positiveText(b.getResources().getString(R.string.yes));
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

    public boolean canGoBack(File f) {
        try {
            f.getParentFile().listFiles();
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public String getdate(File f) {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy | KK:mm a");
        return sdf.format(f.lastModified());
    }

    public static String getdate(long f) {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy | KK:mm a");
        return sdf.format(f);
    }

    public static String getdate(long f, String year) {
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

    public void showProps(final BaseFile hFile, final String perm, final MainFragment c, boolean root, AppTheme appTheme) {
        long last=hFile.getDate();
        String date = getdate(last);
        String items = c.getResources().getString(R.string.calculating), size = c.getResources().getString(R.string.calculating), name, parent;
        name = hFile.getName();
        parent = hFile.getReadablePath(hFile.getParent(c.getContext()));
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c.getActivity());
        String fabskin = PreferenceUtils.getAccentString(sp);
        MaterialDialog.Builder a = new MaterialDialog.Builder(c.getActivity());
        a.title(c.getResources().getString( R.string.properties));
        a.theme(appTheme.getMaterialDialogTheme());
        View v=c.getActivity().getLayoutInflater().inflate(R.layout.properties_dialog,null);
        AppCompatButton appCompatButton=(AppCompatButton)v.findViewById(R.id.appX);
        appCompatButton.setAllCaps(true);
        final View permtabl=v.findViewById(R.id.permtable);
        final View but=v.findViewById(R.id.set);
        if(root && perm.length()>6) {
            appCompatButton.setVisibility(View.VISIBLE);
            appCompatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (permtabl.getVisibility() == View.GONE) {
                        permtabl.setVisibility(View.VISIBLE);
                        but.setVisibility(View.VISIBLE);
                        setPermissionsDialog(permtabl, but, hFile, perm, c);
                    } else {
                        but.setVisibility(View.GONE);
                        permtabl.setVisibility(View.GONE);

                    }
                }
            });
        }
        a.customView(v, true);
        //a.neutralText(R.string.ok);
        a.positiveText(c.getResources().getString(R.string.ok));
        a.neutralColor(Color.parseColor(fabskin));
        MaterialDialog materialDialog=a.build();
        materialDialog.show();
        /*View bottomSheet = c.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(BottomSheetBehavior.STATE_DRAGGING);*/
        new GenerateMD5Task(materialDialog, hFile, name, parent, items, date,
                c.MAIN_ACTIVITY, v).execute(hFile.getPath());
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

    public static long[] getSpaces(HFile hFile){
        if(!hFile.isSmb() && hFile.isDirectory()){
            try {
                File file=new File(hFile.getPath());
                long[] ints=new long[]{file.getTotalSpace(), file.getFreeSpace(),folderSize
                        (new File(hFile.getPath()))};
                return ints;
            } catch (Exception e) {
                return new long[]{-1,-1,-1};
            }
        }
        return new long[]{-1,-1,-1};
    }

    public void showProps(final HFile f, final BaseActivity c, AppTheme appTheme) {
        String date = null;
        try {
            date = getdate(f.lastModified());
        } catch (MalformedURLException | SmbException e) {
            e.printStackTrace();
        }

        String items = c.getResources().getString(R.string.calculating), size = c.getResources().getString(R.string.calculating), name, parent;
        name = f.getName();
        parent = f.getReadablePath(f.getParent());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String fabskin = PreferenceUtils.getAccentString(sp);

        View v = c.getLayoutInflater().inflate(R.layout.properties_dialog, null);
        v.findViewById(R.id.appX).setVisibility(View.GONE);

        MaterialDialog materialDialog = new MaterialDialog.Builder(c)
                .title(c.getResources().getString(R.string.properties))
                .theme(appTheme.getMaterialDialogTheme())
                .customView(v, true)
                .neutralText(R.string.ok)
                .neutralColor(Color.parseColor(fabskin))
                .build();
        materialDialog.show();
        new GenerateMD5Task(materialDialog, (f), name, parent, items, date, c, v).execute(f.getPath());
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

    public LayoutElements newElement(BitmapDrawable i, String d, String permissions, String symlink, String size, long longSize, boolean directorybool, boolean b, String date) {
        LayoutElements item = new LayoutElements(i, new File(d).getName(), d,permissions,symlink,size,longSize,b,date,directorybool);
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
        a.positiveText(R.string.ascending).positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.negativeText(R.string.descending).negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                int which = dialog.getSelectedIndex();
                m.sharedPref.edit().putString("sortby", "" + which).commit();
                m.getSortModes();
                m.updateList();
                dialog.dismiss();

            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                int which = 4 + dialog.getSelectedIndex();
                m.sharedPref.edit().putString("sortby", "" + which).commit();
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
        a.positiveText(R.string.ascending).positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.negativeText(R.string.descending).negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                int which = dialog.getSelectedIndex();
                m.Sp.edit().putString("sortbyApps", "" + which).commit();
                m.getSortModes();
                m.getLoaderManager().restartLoader(AppsList.ID_LOADER_APP_LIST, null, m);
                dialog.dismiss();
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                int which = dialog.getSelectedIndex() + 3;
                m.Sp.edit().putString("sortbyApps", "" + which).commit();
                m.getSortModes();
                m.getLoaderManager().restartLoader(AppsList.ID_LOADER_APP_LIST, null, m);
                dialog.dismiss();
            }
        });
        a.title(R.string.sortby);
        a.build().show();
    }

    public void showHistoryDialog(final MainFragment m, AppTheme appTheme) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.negativeText(R.string.clear);
        a.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        a.title(R.string.history);
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                DataUtils.clearHistory();
            }
        });
        a.theme(appTheme.getMaterialDialogTheme());

        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(),m, this, R.layout.bookmarkrow, toHFileArray(DataUtils.history),null,true);
        a.adapter(adapter, null);

        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }

    public void showHiddenDialog(final MainFragment m, AppTheme appTheme) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        a.title(R.string.hiddenfiles);
        a.theme(appTheme.getMaterialDialogTheme());
        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(),m, this, R.layout.bookmarkrow, toHFileArray(DataUtils.getHiddenfiles()),null,false);
        a.adapter(adapter, null);
        a.dividerColor(Color.GRAY);
        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }
    public boolean isAtleastKitkat(){
        return Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT;
    }
    public void setPermissionsDialog(final View v,View but,final HFile file, final String f, final MainFragment mainFrag) {
        final CheckBox readown=(CheckBox) v.findViewById(R.id.creadown);
        final CheckBox readgroup=(CheckBox) v.findViewById(R.id.creadgroup);
        final CheckBox readother=(CheckBox) v.findViewById(R.id.creadother);
        final CheckBox writeown=(CheckBox) v.findViewById(R.id.cwriteown);
        final CheckBox writegroup=(CheckBox) v.findViewById(R.id.cwritegroup);
        final CheckBox writeother=(CheckBox) v.findViewById(R.id.cwriteother);
        final CheckBox exeown=(CheckBox) v.findViewById(R.id.cexeown);
        final CheckBox exegroup=(CheckBox) v.findViewById(R.id.cexegroup);
        final CheckBox exeother=(CheckBox) v.findViewById(R.id.cexeother);
        String perm=f;
        if(perm.length()<6){
            v.setVisibility(View.GONE);
            but.setVisibility(View.GONE);
            Toast.makeText(mainFrag.getActivity(),R.string.not_allowed,Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Boolean[]> arrayList=parse(perm);
        Boolean[] read=arrayList.get(0);
        Boolean[] write=arrayList.get(1);
        final Boolean[] exe=arrayList.get(2);
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
                    command = "chmod -R " + finalValue + " \"" + file.getPath()+"\"";

                try {
                    RootHelper.runShellCommand(command, new Shell.OnCommandResultListener() {
                        @Override
                        public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                            if (exitCode<0) {
                                Toast.makeText(mainFrag.getActivity(), mainFrag.getString(R.string.operationunsuccesful),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(mainFrag.getActivity(),
                                        mainFrag.getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    mainFrag.updateList();
                } catch (RootNotPermittedException e1) {
                    Toast.makeText(mainFrag.getActivity(), mainFrag.getResources().getString(R.string.rootfailure),
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
}
