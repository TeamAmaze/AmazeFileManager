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
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.DbViewer;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.HiddenAdapter;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.asynctasks.GenerateMD5Task;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.share.ShareTask;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;

import java.io.File;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class Futils {

public  final int READ = 4;
    public  final int WRITE = 2;
    public  final int EXECUTE = 1;
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

    public MaterialDialog showBasicDialog(Context c,String fabskin,int theme1, String[] texts) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(c);
        a.content(texts[0]);
        a.widgetColor(Color.parseColor(fabskin));
        if(theme1==1)
            a.theme(Theme.DARK);
        a.title(texts[1]);
        a.positiveText(texts[2]);
        a.positiveColor(Color.parseColor(fabskin));
        a.negativeText(texts[3]);
        a.negativeColor(Color.parseColor(fabskin));
        if(texts[4]!=(null)){
            a.neutralText(texts[4]);
            a.neutralColor(Color.parseColor(fabskin));
        }
        MaterialDialog dialog=a.build();
        return dialog;
    }
    public MaterialDialog showBasicDialog(final Activity m,String fabskin,int theme1, String[] texts) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.content(texts[0]);
        a.widgetColor(Color.parseColor(fabskin));
        if(theme1==1)
            a.theme(Theme.DARK);
        a.title(texts[1]);
        a.positiveText(texts[2]);
        a.positiveColor(Color.parseColor(fabskin));
        a.negativeText(texts[3]);
        a.negativeColor(Color.parseColor(fabskin));
        if(texts[4]!=(null)){
            a.neutralText(texts[4]);
            a.neutralColor(Color.parseColor(fabskin));
        }
        MaterialDialog dialog=a.build();
        return dialog;
    }
    public MaterialDialog showNameDialog(final MainActivity m, String[] texts) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(texts[0], texts[1], false, new
                MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                    }
                });
            a.widgetColor(Color.parseColor(m.fabskin));
            if(m.theme1==1)
                a.theme(Theme.DARK);
            a.title(texts[2]);
            a.positiveText(texts[3]);
            a.positiveColor(Color.parseColor(m.fabskin));
            a.neutralText(texts[4]);
            if(texts[5]!=(null)){
                a.negativeText(texts[5]);
                a.negativeColor(Color.parseColor(m.fabskin));
            }
            MaterialDialog dialog=a.build();
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
    private  ColorStateList createEditTextColorStateList(int color) {
        int[][] states = new int[3][];
        int[] colors = new int[3];
        int i = 0;
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = Color.parseColor("#f6f6f6");
        i++;
        states[i] = new int[]{-android.R.attr.state_pressed, -android.R.attr.state_focused};
        colors[i] = Color.parseColor("#666666");
        i++;
        states[i] = new int[]{};
        colors[i] =color ;
        return new ColorStateList(states, colors);
    }
    public  void setTint(CheckBox box, int color) {
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
    public void setTint(EditText editText, int color) {
        if(Build.VERSION.SDK_INT>=21)return;
        ColorStateList editTextColorStateList = createEditTextColorStateList(color);
        if (editText instanceof AppCompatEditText) {
            ((AppCompatEditText) editText).setSupportBackgroundTintList(editTextColorStateList);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editText.setBackgroundTintList(editTextColorStateList);
        }
    }
    public int checkFolder(final String f,Context context) {
        if(f==null)return 0;
        if(f.startsWith("smb://"))return 1;
        File folder=new File(f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(folder, context)) {
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }

            // On Android 5, trigger storage access framework.
            if (FileUtil.isWritableNormalOrSaf(folder, context)) {
                return 1;

            }
        } else if (Build.VERSION.SDK_INT == 19 && FileUtil.isOnExtSdCard(folder, context)) {
            // Assume that Kitkat workaround works
            return 1;
        } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
            return 1;
        } else {
            return 0;
        }
        return 0;
    }

    public void scanFile(String path, Context c) {
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

    public String getString(Context c, int a) {
        return c.getResources().getString(a);
    }


    public void shareFiles(ArrayList<File> a, Activity c,int theme,int fab_skin) {
        ArrayList<Uri> uris = new ArrayList<Uri>();
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

            new ShareTask(c,uris,theme,fab_skin).execute(mime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String readableFileSize(long size) {
        if (size <= 0)
            return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size
                / Math.pow(1024, digitGroups))
                + "" + units[digitGroups];
    }
    private boolean isSelfDefault(File f, Context c){
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), MimeTypes.getMimeType(f));
        String s="";
        ResolveInfo rii = c.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (rii !=  null && rii.activityInfo != null) s = rii.activityInfo.packageName;
        if (s.equals("com.amaze.filemanager") || rii==null) return true;
        else return false;
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
        a.title(getString(c, R.string.openas));
        String[] items=new String[]{getString(c,R.string.text),getString(c,R.string.image),getString(c,R.string.video),getString(c,R.string.audio),getString(c,R.string.database),getString(c,R.string.other)};

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

    public void deleteFiles(ArrayList<Layoutelements> a, final Main b, List<Integer> pos) {
        final MaterialDialog.Builder c = new MaterialDialog.Builder(b.getActivity());
        c.title(getString(b.getActivity(), R.string.confirm));
        String names = "";
        final ArrayList<BaseFile> todelete = new ArrayList<>();
        for (int i = 0; i < pos.size(); i++) {
            todelete.add(a.get(pos.get(i)).generateBaseFile());
            names = names + "\n" + (i + 1) + ". " + a.get(pos.get(i)).getTitle();
        }
        c.content(getString(b.getActivity(), R.string.questiondelete) + names);

        if(b.theme1==1)
            c.theme(Theme.DARK);
        c.negativeText(getString(b.getActivity(), R.string.no));
        c.positiveText(getString(b.getActivity(), R.string.yes));
        c.positiveColor(Color.parseColor(b.fabSkin));
        c.negativeColor(Color.parseColor(b.fabSkin));
        c.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                Toast.makeText(b.getActivity(), getString(b.getActivity(), R.string.deleting), Toast.LENGTH_SHORT).show();
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
        return (sdf.format(f.lastModified())).toString();
    }
    public String getdate(long f) {

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy | KK:mm a");
        return (sdf.format(f)).toString();
    }

    public String getdate(long f,String form,String year) {

        SimpleDateFormat sdf = new SimpleDateFormat(form);
        String date=(sdf.format(f)).toString();
        if(date.substring(date.length()-2,date.length()).equals(year))
            date=date.substring(0,date.length()-6);
        return date;
    }

    public int calculateInSampleSize(BitmapFactory.Options options,
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

    public void showProps(final BaseFile hFile, final String perm, final Main c,boolean root) {
        long last=hFile.getDate();
        String date = getdate(last);
        String items = getString(c.getActivity(),R.string.calculating), size = getString(c.getActivity(),R.string.calculating), name, parent;
        name = hFile.getName();
        parent = hFile.getReadablePath(hFile.getParent());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c.getActivity());
        String fabskin = PreferenceUtils.getAccentString(sp);
        MaterialDialog.Builder a = new MaterialDialog.Builder(c.getActivity());
        a.title(getString(c.getActivity(), R.string.properties));
        if(c.theme1==1)
            a.theme(Theme.DARK);
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
        a.positiveText(R.string.copy_path);
        a.negativeText(getString(c.getActivity(), R.string.md5_2));
        a.positiveColor(Color.parseColor(fabskin));
        a.negativeColor(Color.parseColor(fabskin));
        a.neutralText(R.string.cancel);
        a.neutralColor(Color.parseColor(fabskin));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                c.MAIN_ACTIVITY.copyToClipboard(c.getActivity(), hFile.getPath());
                Toast.makeText(c.getActivity(), c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {
            }
        });
        MaterialDialog materialDialog=a.build();
        materialDialog.show();
        new GenerateMD5Task(materialDialog, hFile, name, parent, size, items, date,c.getActivity
                (),v).execute(hFile.getPath());
    }
    public long[] getSpaces(HFile hFile){
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
    public void showProps(final HFile f, final Activity c,int theme1) {
        String date = null;
        try {
            date = getdate(f.lastModified());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
        String items =  getString(c,R.string.calculating), size = getString(c,R.string.calculating), name, parent;
        name =  f.getName();
        parent = f.getReadablePath(f.getParent());
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String fabskin = PreferenceUtils.getAccentString(sp);
        MaterialDialog.Builder a = new MaterialDialog.Builder(c);
        a.title(getString(c, R.string.properties));
        if(theme1==1)
            a.theme(Theme.DARK);

        View v=c.getLayoutInflater().inflate(R.layout.properties_dialog,null);
        v.findViewById(R.id.appX).setVisibility(View.GONE);
        a.customView(v, true);
        a.positiveText(R.string.copy_path);
        a.negativeText(getString(c, R.string.md5_2));
        a.positiveColor(Color.parseColor(fabskin));
        a.negativeColor(Color.parseColor(fabskin));
        a.neutralText(R.string.cancel);
        a.neutralColor(Color.parseColor(fabskin));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                copyToClipboard(c, f.getPath());
                Toast.makeText(c, c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {
            }
        });
        MaterialDialog materialDialog=a.build();
        materialDialog.show();
        new GenerateMD5Task(materialDialog, (f), name, parent, size, items, date,c,v).execute(f.getPath());
    }

    public boolean copyToClipboard(Context context, String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("Path copied to clipboard", text);
            clipboard.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Bundle getPaths(String path, Context c) {
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<String> paths = new ArrayList<String>();
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

    public boolean rename(File f, String name,boolean root) {
        String newname = f.getParent() + "/" + name;
        if(f.getParentFile().canWrite()){
            return f.renameTo(new File(newname));}
        else if(root) {
            RootTools.remount(f.getPath(),"rw");
            RootHelper.runAndWait("mv " + f.getPath() + " " + newname, true);
            RootTools.remount(f.getPath(),"ro");
            return true;
        }
        return false;
    }

    public boolean canListFiles(File f) {
        try {
            if (f.canRead() && f.isDirectory())
                return true;
            else
                return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void openFile(final File f, final MainActivity m) {
        boolean defaultHandler = isSelfDefault(f, m);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(m);
        if (defaultHandler && f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".jar") || f.getName().toLowerCase().endsWith(".rar")|| f.getName().toLowerCase().endsWith(".tar")|| f.getName().toLowerCase().endsWith(".tar.gz")) {
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
                        studioCount = Toast.makeText(m, "Opening..", Toast.LENGTH_LONG);
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
                Toast.makeText(m, getString(m, R.string.noappfound),Toast.LENGTH_LONG).show();
                openWith(f, m);
            }
        }
    }
public void showSMBHelpDialog(Context m,String acc){

    MaterialDialog.Builder b=new MaterialDialog.Builder(m);
    b.content(Html.fromHtml("<html>\n" +
            "<body>\n" +
            "<center>\n" +
            "<h1>How to access shared windows folder on android (smb)</h1>\n" +
            "</center>\n" +
            "<ol>\n" +
            "<li>\n" +
            "<b>Enable File Sharing</b>\n" +
            "<br>Open the Control Panel, click Choose homegroup and sharing options under Network and Internet, and click Change advanced sharing settings. Enable the file and printer sharing feature.\n" +
            "</li><br><li><b>Additional File Sharing settings</b><br>You may also want to configure the other advanced sharing settings here. \n" +
            "For example, you could enable access to your files without a password if you trust all the devices on your local network.Once file and printer sharing is enabled, you can open File Explorer or Windows Explorer, right-click a folder you want to share, and select Properties. \n" +
            "Click the Share button and make the folder available on the network.\n" +
            "</li><li><br><b>Make sure both devices are on same Wifi</b><br> \n" +
            "This feature makes files available on the local network, so your PC and mobile devices have to be on the same local network. You can’t access a shared Windows folder over the Internet or when your smartphone is connected to its mobile data — it has to be connected to Wi-Fi.</li><li>\n" +
            "<br><b>Find IP Address</b>\n" +
            "<br>Open Command Prompt. Type 'ipconfig' and press Enter. Look for Default Gateway under your network adapter for your router's IP address. Look for \\\"IPv4 Address\\\" under the same adapter section to find your computer's IP address.</li><li><br>\n" +
            "<b>Enter details in smb dialog box</b>\n" +
            "<br>\n" +
            "</ol>\n" +
            "</body>\n" +
            "</html>"));
    b.positiveText(R.string.doit);
    b.positiveColor(Color.parseColor(acc));
    b.build().show();
}
public void showPackageDialog(final File f,final MainActivity m){
    MaterialDialog.Builder mat=new MaterialDialog.Builder(m);
    mat.title(R.string.packageinstaller).content(R.string.pitext)
            .positiveText(R.string.install)
            .negativeText(R.string.view)
            .neutralText(R.string.cancel)
            .positiveColor(Color.parseColor(m.fabskin))
            .negativeColor(Color.parseColor(m.fabskin))
            .neutralColor(Color.parseColor(m.fabskin))
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog materialDialog) {
                    openunknown(f, m, false);
                }

                @Override
                public void onNegative(MaterialDialog materialDialog) {
                    m.openZip(f.getPath());
                }
            });
    if(m.theme1==1)mat.theme(Theme.DARK);
    mat.build().show();

}

    public void showArchiveDialog(final File f, final MainActivity m) {
        MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
        mat.title(R.string.archive)
                .content(R.string.archtext)
                .positiveText(R.string.extract)
                .negativeText(R.string.view)
                .neutralText(R.string.cancel)
                .positiveColor(Color.parseColor(m.fabskin))
                .negativeColor(Color.parseColor(m.fabskin))
                .neutralColor(Color.parseColor(m.fabskin))
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
        if (m.theme1 == 1) mat.theme(Theme.DARK);
        MaterialDialog b = mat.build();

        if (!f.getName().toLowerCase().endsWith(".rar") && !f.getName().toLowerCase().endsWith(".jar") && !f.getName().toLowerCase().endsWith(".apk") && !f.getName().toLowerCase().endsWith(".zip"))
            b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
        b.show();

    }

    public Layoutelements newElement(Drawable i, String d,String permissions,String symlink,String size,long longSize,boolean directorybool,boolean b,String date) {
        Layoutelements item = new Layoutelements(i, new File(d).getName(), d,permissions,symlink,size,longSize,b,date,directorybool);
        return item;
    }

    public ArrayList<File> toFileArray(ArrayList<String> a) {
        ArrayList<File> b = new ArrayList<File>();
        for (int i = 0; i < a.size(); i++) {
            b.add(new File(a.get(i)));
        }
        return b;
    }
    public ArrayList<HFile> toHFileArray(ArrayList<String> a) {
        ArrayList<HFile> b = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            HFile hFile=new HFile(HFile.UNKNOWN,a.get(i));
            hFile.generateMode(null);
            b.add(hFile);
        }
        return b;
    }

    public void showCompressDialog(final MainActivity m, final ArrayList<BaseFile> b, final String current) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(getString(m, R.string.enterzipname), ".zip", false, new
                MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                    }
                });
        a.widgetColor(Color.parseColor(m.fabskin));
        if(m.theme1==1)
            a.theme(Theme.DARK);
        a.title(getString(m, R.string.enterzipname));
        a.positiveText(R.string.create);
        a.positiveColor(Color.parseColor(m.fabskin));
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
        a.negativeText(getString(m, R.string.cancel));
        a.negativeColor(Color.parseColor(m.fabskin));
        a.build().show();
    }

    public void showSortDialog(final Main m) {
        String[] sort = m.getResources().getStringArray(R.array.sortby);
        int current = Integer.parseInt(m.Sp.getString("sortby", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        if(m.theme1==1)a.theme(Theme.DARK);
        a.items(sort).itemsCallbackSingleChoice(current > 3 ? current - 4 : current, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                return true;
            }
        });
        a.positiveText(R.string.ascending).positiveColor(Color.parseColor(m.fabSkin));
        a.negativeText(R.string.descending).negativeColor(Color.parseColor(m.fabSkin));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                int which = dialog.getSelectedIndex();
                m.Sp.edit().putString("sortby", "" + which).commit();
                m.getSortModes();
                m.updateList();
                dialog.dismiss();

            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                int which = 4 + dialog.getSelectedIndex();
                m.Sp.edit().putString("sortby", "" + which).commit();
                m.getSortModes();
                m.updateList();
                dialog.dismiss();
            }
        });
        a.title(R.string.sortby);
        a.build().show();
    }

    public void showSortDialog(final AppsList m) {
        String[] sort = m.getResources().getStringArray(R.array.sortbyApps);
        int current = Integer.parseInt(m.Sp.getString("sortbyApps", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        if(m.theme1==1)a.theme(Theme.DARK);
        a.items(sort).itemsCallbackSingleChoice(current > 2 ? current - 3 : current, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                return true;
            }
        });
        a.positiveText(R.string.ascending).positiveColor(Color.parseColor(m.fabSkin));
        a.negativeText(R.string.descending).negativeColor(Color.parseColor(m.fabSkin));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                int which = dialog.getSelectedIndex();
                m.Sp.edit().putString("sortbyApps", "" + which).commit();
                m.getSortModes();
                m.loadlist(false);
                dialog.dismiss();
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                int which = dialog.getSelectedIndex() + 3;
                m.Sp.edit().putString("sortbyApps", "" + which).commit();
                m.getSortModes();
                m.loadlist(false);
                dialog.dismiss();
            }
        });
        a.title(R.string.sortby);
        a.build().show();
    }

    public void showHistoryDialog(final Main m) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(m.fabSkin));
        a.negativeText(R.string.clear);
        a.negativeColor(Color.parseColor(m.fabSkin));
        a.title(R.string.history);
        a.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                 DataUtils.clearHistory();
            }
        });
        if(m.theme1==1)
            a.theme(Theme.DARK);

        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(),m, R.layout.bookmarkrow, toHFileArray(DataUtils.history),null,true);
        a.adapter(adapter, new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {

            }
        });

        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }

    public void showHiddenDialog(final Main m) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(m.fabSkin));
        a.title(R.string.hiddenfiles);
        if(m.theme1==1)
            a.theme(Theme.DARK);
        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(),m, R.layout.bookmarkrow, toHFileArray(DataUtils.getHiddenfiles()),null,false);
        a.adapter(adapter, new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {

            }
        });
        a.dividerColor(Color.GRAY);
        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }
    public boolean isAtleastKitkat(){
        return Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT;
    }
    public void setPermissionsDialog(final View v,View but,final HFile file, final String f, final Main main) {
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
                Toast.makeText(main.getActivity(),R.string.not_allowed,Toast.LENGTH_SHORT).show();
                return;
            }
            ArrayList<Boolean[]> arrayList=parse(perm);
            Boolean[] read=arrayList.get(0);
            Boolean[] write=arrayList.get(1);
            Boolean[] exe=arrayList.get(2);
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
                    int d = 0, e = 0, f = 0;
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
                    Command com = new Command(1, command) {
                        @Override
                        public void commandOutput(int i, String s) {
                            Toast.makeText(main.getActivity(), s, Toast.LENGTH_LONG);
                        }

                        @Override
                        public void commandTerminated(int i, String s) {
                            Toast.makeText(main.getActivity(), s, Toast.LENGTH_LONG);
                        }

                        @Override
                        public void commandCompleted(int i, int i2) {
                            Toast.makeText(main.getActivity(), main.getResources().getString(R.string.done), Toast.LENGTH_LONG);
                        }
                    };
                    try {//
                        RootTools.remount(file.getPath(), "RW");
                        RootTools.getShell(true).add(com);
                        main.updateList();
                    } catch (Exception e1) {
                        Toast.makeText(main.getActivity(), main.getResources().getString(R.string.error), Toast.LENGTH_LONG).show();
                        e1.printStackTrace();
                    }

                }
            });
    }

    public BaseFile parseName(String line) {
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

    public int getLinkPosition(String[] array){
        for(int i=0;i<array.length;i++){
            if(array[i].contains("->"))return i;
        }
        return  0;
    }

public int getColonPosition(String[] array){
        for(int i=0;i<array.length;i++){
            if(array[i].contains(":"))return i;
        }
        return  -1;
    }

    public ArrayList<Boolean[]> parse(String permLine) {
        ArrayList<Boolean[]> arrayList=new ArrayList<Boolean[]>();
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
