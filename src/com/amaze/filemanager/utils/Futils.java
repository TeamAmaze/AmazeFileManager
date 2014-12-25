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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.TextReader;
import com.amaze.filemanager.adapters.HiddenAdapter;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.ZipTask;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Futils {

    public Futils() {
    }

    ArrayList<File> lis = new ArrayList<File>();
    ArrayList<File> images = new ArrayList<File>();
    AlertDialog.Builder b = null;

    public void scanFile(String path, Context c) {
        System.out.println(path+" "+Build.VERSION.SDK_INT);
        if(Build.VERSION.SDK_INT>=19){
        MediaScannerConnection.scanFile(c, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {

            @Override
            public void onScanCompleted(String path, Uri uri) {
                System.out.println("SCAN COMPLETED: " + path);

            }
        });}else{
        Uri contentUri = Uri.fromFile(new File(path));
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
        c.sendBroadcast(mediaScanIntent);
    }}

    public String getString(Context c, int a) {
        return c.getResources().getString(a);
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

    public String getFileExtension(String url) {
        try {
            int a = url.lastIndexOf(".");
            int b = url.length();
            return url.substring(a, b);
        } catch (Exception e) {
            return "";
        }

    }


    public void openunknown(File f, Context c) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);

        String type = MimeTypes.getMimeType(f);
        if(type!=null && type.trim().length()!=0 && !type.equals("*/*"))
        {intent.setDataAndType(Uri.fromFile(f), type);
        try {
            c.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        Toast.makeText(c,R.string.noappfound,Toast.LENGTH_SHORT).show();
        openWith(f,c);
        }}else{openWith(f, c);}

    }

    public void openWith(final File f,final Context c) {
        MaterialDialog.Builder a=new MaterialDialog.Builder(c);
        a.title("Open As");
        String[] items=new String[]{"Text","Image","Video","Audio","Other"};
        a.items(items).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                switch (i) {
                    case 0:
                        intent.setDataAndType(Uri.fromFile(f), "text/*");
                        break;
                    case 1:
                        intent.setDataAndType(Uri.fromFile(f), "image/*");
                        break;
                    case 2:
                        intent.setDataAndType(Uri.fromFile(f), "video/*");
                        break;
                    case 3:
                        intent.setDataAndType(Uri.fromFile(f), "audio/*");
                        break;
                    case 4:
                        intent.setDataAndType(Uri.fromFile(f), "*/*");
                        break;
                }
                try {
                    c.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(c,R.string.noappfound,Toast.LENGTH_SHORT).show();
                    openWith(f,c);
                }
            }
        });
        a.build().show();
    }public String getSize(File f) {
        long i =f.length();

        return readableFileSize(i);

    }

    public String getSize(String[] array,boolean showsize) {
        if(showsize) {
            String sym = array[1];
            long i;
            if (sym != null && sym.length() != 0) {
                i = new File(array[1]).length();
            } else
                i = new File(array[0]).length();

            return readableFileSize(i);
        }else return "";
    }

    public void deleteFiles(ArrayList<Layoutelements> a, final Main b, List<Integer> pos) {
        final MaterialDialog.Builder c = new MaterialDialog.Builder(b.getActivity());
        c.title(getString(b.getActivity(), R.string.confirm));
        final ContentResolver contentResolver=b.getActivity().getContentResolver();
        String names = "";
        final ArrayList<File> todelete = new ArrayList<File>();
        for (int i = 0; i < pos.size(); i++) {
            String path = a.get(pos.get(i)).getDesc();
            todelete.add(new File(path));
            names = names + "\n" + (i + 1) + ". " + new File(path).getName();
        }
        c.content(getString(b.getActivity(), R.string.questiondelete) + names);

        c.positiveColor(Color.parseColor(b.skin));
        c.negativeColor(Color.parseColor(b.skin));
        if(b.theme1==1)
            c.theme(Theme.DARK);
        c.negativeText(getString(b.getActivity(), R.string.no));
        c.positiveText(getString(b.getActivity(), R.string.yes));

        c.callback(new MaterialDialog.Callback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                Toast.makeText(b.getActivity(), getString(b.getActivity(), R.string.deleting), Toast.LENGTH_LONG).show();
                new DeleteTask(b.getActivity().getContentResolver(), b.getActivity()).execute(todelete);
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

                //materialDialog.cancel();
            }
        });
        c.build().show();
    }

    public String count(File f,Resources root,boolean showSize) {
        if(showSize)try {
            Integer i=RootHelper.getCount(f);
            if(i!=null){return i+" "+root.getString(R.string.items);}
            else{return "";}

        } catch (Exception e) {
            return "";
        }else{return "";}
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
    public String getdate(long f,String form,String year) {

        SimpleDateFormat sdf = new SimpleDateFormat(form);
        String date=(sdf.format(f)).toString();
        if(date.substring(date.length()-2,date.length()).equals(year))
            date=date.substring(0,date.length()-6);
        return date;
    }
    public ArrayList<File> getImages(File f) {

        getImage(f);

        return images;
    }

    public void getImage(File file) {

        if (file.isDirectory()) {
            if (!file.getName().equals(".thumbnails")) {
                // do you have permission to read this directory?
                if (file.canRead()) {
                    for (File temp : file.listFiles()) {
                        if (temp.isDirectory()) {

                            getImage(temp);

                        } else {
                            if (getFileExtension(temp.getName()).equals(".jpg")
                                    || getFileExtension(temp.getName()).equals(
                                    ".jpeg")
                                    || getFileExtension(temp.getName()).equals(
                                    (".png"))) {
                                images.add(temp);
                            }
                        }
                    }
                } else {
                    System.out
                            .println(file.getAbsoluteFile() + "Permission Denied");
                }
            }
        }
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
    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis =  new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
    public void showProps(final File f, final Main c,boolean root) {
        String date = getString(c.getActivity(), R.string.date) + getdate(f);
        String items = "", size = "", name, parent;
        name = getString(c.getActivity(), R.string.name) + f.getName();
        parent = getString(c.getActivity(), R.string.location) + f.getParent();
        if (f.isDirectory()) {
            size = getString(c.getActivity(), R.string.size) + readableFileSize(folderSize(f));
            items = getString(c.getActivity(), R.string.totalitems) + count(f,c.getResources(),true);
        } else if (f.isFile()) {
            items = "";
            size = getString(c.getActivity(), R.string.size) + getSize(f);
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c.getActivity());
        String skin = sp.getString("skin_color", "#5677fc");
        MaterialDialog.Builder a = new MaterialDialog.Builder(c.getActivity());
        a.title(getString(c.getActivity(), R.string.properties));
        String md5="";
        try {
            md5="md5:"+getMD5Checksum(f.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(c.theme1==1)
            a.theme(Theme.DARK);
        a.content(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                + date+"\n"+md5);
        a.positiveText(R.string.copy_path);
        a.negativeText(getString(c.getActivity(), R.string.copy) + " md5");
        a.neutralText(R.string.cancel);
        a.positiveColor(Color.parseColor(skin)).negativeColor(Color.parseColor(skin)).neutralColor(Color.parseColor(skin));
        a.callback(new MaterialDialog.Callback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {

                c.copyToClipboard(c.getActivity(), f.getPath());
                Toast.makeText(c.getActivity(), c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {
                try {
                    c.copyToClipboard(c.getActivity(), getMD5Checksum(f.getPath()));
                    Toast.makeText(c.getActivity(), c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        MaterialDialog b= a.build();
        if(f.isDirectory())b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
        b.show();
    }

    public void showProps(final File f, final Context c,int theme1) {
        String date = getString(c, R.string.date) + getdate(f);
        String items = "", size = "", name, parent;
        name = getString(c, R.string.name) + f.getName();
        parent = getString(c, R.string.location) + f.getParent();
        if (f.isDirectory()) {
            size = getString(c, R.string.size) + readableFileSize(folderSize(f));
            items = getString(c, R.string.totalitems) + count(f,c.getResources(),true);
        } else if (f.isFile()) {
            items = "";
            size = getString(c, R.string.size) + getSize(f);
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String skin = sp.getString("skin_color", "#5677fc");
        MaterialDialog.Builder a = new MaterialDialog.Builder(c);
        a.title(getString(c, R.string.properties));
        String md5="";
        try {
            md5="md5:"+getMD5Checksum(f.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(theme1==1)
            a.theme(Theme.DARK);
        a.content(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                + date+"\n"+md5);
        a.positiveText(R.string.copy_path);
        a.negativeText(getString(c, R.string.copy) + " md5");
        a.neutralText(R.string.cancel);
        a.positiveColor(Color.parseColor(skin)).negativeColor(Color.parseColor(skin)).neutralColor(Color.parseColor(skin));
        a.callback(new MaterialDialog.Callback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {

                copyToClipboard(c, f.getPath());
                Toast.makeText(c, c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {
                try {
                    copyToClipboard(c, getMD5Checksum(f.getPath()));
                    Toast.makeText(c, c.getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
       MaterialDialog b= a.build();
        if(f.isDirectory())b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
               b.show();
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
    public boolean deletefiles(File f) {

        // make sure directory exists
        if (!f.exists()) {

            System.out.println("Directory does not exist.");
            return false;

        } else {

            try {
                if(f.isDirectory())
                    return deletedirectory(f);
                    else
                return f.delete();

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

    public boolean rename(File f, String name) {
        String newname = f.getParent() + "/" + name;
        if(f.getParentFile().canWrite()){
            boolean b = f.renameTo(new File(newname));}
        else{try{RootTools.getShell(true).add(new Command(0,"mv "+f.getPath()+" "+newname) {
            @Override
            public void commandOutput(int i, String s) {
                System.out.println(s);
            }

            @Override
            public void commandTerminated(int i, String s) {

            }

            @Override
            public void commandCompleted(int i, int i2) {

            }
        });}catch (Exception e){return false;}}
        return true;
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
        if (Icons.isText(f.getPath())) {
            Intent i = new Intent(m, TextReader.class);
            i.putExtra("path", f.getPath());
            m.startActivity(i);
        } else if (Icons.isCode(f.getName())) {
            Intent i = new Intent(m, TextReader.class);
            i.putExtra("path", f.getPath());
            m.startActivity(i);
        } else if (f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".jar")) {
            showArchiveDialog(f, m);

        }else if(f.getName().toLowerCase().endsWith(".apk")){showPackageDialog(f,m);}else {
            try {
                openunknown(f, m);
            } catch (Exception e) {
                Toast.makeText(m, getString(m, R.string.noappfound),Toast.LENGTH_LONG).show();
                openWith(f, m);
            }
        }
    }
public void showPackageDialog(final File f,final MainActivity m){
    MaterialDialog.Builder mat=new MaterialDialog.Builder(m);
    mat.title(R.string.packageinstaller).content(R.string.pitext).positiveText(R.string.install).negativeText(R.string.view).neutralText(R.string.cancel).callback(new MaterialDialog.Callback() {
        @Override
        public void onPositive(MaterialDialog materialDialog) {
            openunknown(f,m);
        }

        @Override
        public void onNegative(MaterialDialog materialDialog) {
            m.addZipViewTab(f.getPath());
        }
    });
    mat.positiveColor(Color.parseColor(m.skin));
    mat.neutralColor(Color.parseColor(m.skin));
    mat.negativeColor(Color.parseColor(m.skin));
    if(m.theme1==1)mat.theme(Theme.DARK);
    mat.build().show();

}
    public void showArchiveDialog(final File f,final MainActivity m){
        MaterialDialog.Builder mat=new MaterialDialog.Builder(m);
        mat.title(R.string.archive).content(R.string.archtext).positiveText(R.string.extract).negativeText(R.string.view).neutralText(R.string.cancel).callback(new MaterialDialog.Callback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                Intent intent = new Intent(m, ExtractService.class);
                intent.putExtra("zip",f.getPath());
                m.startService(intent);


            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {
                m.addZipViewTab(f.getPath());
            }
        });
        mat.positiveColor(Color.parseColor(m.skin));
        mat.neutralColor(Color.parseColor(m.skin));
        mat.negativeColor(Color.parseColor(m.skin));
        if(m.theme1==1)mat.theme(Theme.DARK);
        mat.build().show();

    }
    public Layoutelements newElement(Drawable i, String d,String permissions,String symlink,String size,String directorybool,boolean b) {
        Layoutelements item = new Layoutelements(i, new File(d).getName(), d,permissions,symlink,size,directorybool,b);
        return item;
    }

    public ArrayList<File> toFileArray(ArrayList<String> a) {
        ArrayList<File> b = new ArrayList<File>();
        for (int i = 0; i < a.size(); i++) {
            b.add(new File(a.get(i)));
        }
        return b;
    }

    public ArrayList<String> toStringArray(ArrayList<File> a) {
        ArrayList<String> b = new ArrayList<String>();
        for (int i = 0; i < a.size(); i++) {
            b.add(a.get(i).getPath());
        }
        return b;
    }

    public void showNameDialog(final MainActivity m, final ArrayList<String> b, final String current) {
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        View v = m.getLayoutInflater().inflate(R.layout.dialog, null);
        final EditText e = (EditText) v.findViewById(R.id.newname);
        e.setText("Newzip.zip");
        a.customView(v);
        if(m.theme1==1)
            a.theme(Theme.DARK);
        a.title(getString(m, R.string.enterzipname));
        e.setHint(getString(m, R.string.enterzipname));
        a.positiveText(R.string.create);
        a.callback(new MaterialDialog.Callback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                Intent intent2 = new Intent(m, ZipTask.class);
                String name = current + "/" + e.getText().toString();
                intent2.putExtra("name", name);
                intent2.putExtra("files", b);
                m.startService(intent2);
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

            }
        });
        a.negativeText(getString(m, R.string.cancel));
        a.positiveColor(Color.parseColor(m.skin));
        a.negativeColor(Color.parseColor(m.skin));
        a.build().show();
    }

    public void showSortDialog(final Main m) {
        String[] sort = m.getResources().getStringArray(R.array.sortby);
        int current = Integer.parseInt(m.Sp.getString("sortby", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        if(m.theme1==1)a.theme(Theme.DARK);
        a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                m.Sp.edit().putString("sortby", "" + which).commit();
                m.getSortModes();
                m.loadlist(new File(m.current), false);
                dialog.dismiss();
            }
        });
        a.title(R.string.sortby);
        a.build().show();
    }

    public void showHistoryDialog(final Main m) {
        final ArrayList<String> paths = m.history.readTable();
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(m.skin));
        a.title(R.string.history);
        if(m.theme1==1)
            a.theme(Theme.DARK);
        LayoutInflater layoutInflater = (LayoutInflater) m.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.list_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setDivider(null);
        a.customView(view);
        a.autoDismiss(true);
        MaterialDialog x=a.build();
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(),m, R.layout.bookmarkrow, toFileArray(paths),m.hidden,x,true);
        listView.setAdapter(adapter);
        x.show();

    }

    public void showHiddenDialog(final Main m) {
          final ArrayList<String> paths = m.hidden.readTable();
            final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(m.skin));
        a.title(R.string.hiddenfiles);
        if(m.theme1==1)
            a.theme(Theme.DARK);
        LayoutInflater layoutInflater = (LayoutInflater) m.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.list_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        a.customView(view);
        a.autoDismiss(true);
        listView.setDivider(null);
        MaterialDialog x=a.build();
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(),m, R.layout.bookmarkrow, toFileArray(paths),m.hidden,x,false);
        listView.setAdapter(adapter);
        x.show();

    }
    public void setPermissionsDialog(final Layoutelements f, final Main main){
        if(main.rootMode){
            final File file=new File(f.getDesc());
            final MaterialDialog.Builder a=new MaterialDialog.Builder(main.getActivity());
            View v=main.getActivity().getLayoutInflater().inflate(R.layout.permissiontable,null);
            final CheckBox readown=(CheckBox) v.findViewById(R.id.creadown);
            final CheckBox readgroup=(CheckBox) v.findViewById(R.id.creadgroup);
            final CheckBox readother=(CheckBox) v.findViewById(R.id.creadother);
            final CheckBox writeown=(CheckBox) v.findViewById(R.id.cwriteown);
            final CheckBox writegroup=(CheckBox) v.findViewById(R.id.cwritegroup);
            final CheckBox writeother=(CheckBox) v.findViewById(R.id.cwriteother);
            final CheckBox exeown=(CheckBox) v.findViewById(R.id.cexeown);
            final CheckBox exegroup=(CheckBox) v.findViewById(R.id.cexegroup);
            final CheckBox exeother=(CheckBox) v.findViewById(R.id.cexeother);
            String perm=f.getPermissions();
            StringBuilder finalPermissions = new StringBuilder();
            finalPermissions.append(parseSpecialPermissions(perm));
            finalPermissions.append(parsePermissions(perm.substring(1, 4)));
            finalPermissions.append(parsePermissions(perm.substring(4, 7)));
            finalPermissions.append(parsePermissions(perm.substring(7, 10)));
            Boolean[] read=unparsePermissions(Integer.parseInt(""+(finalPermissions.charAt(1))));
            Boolean[] write=unparsePermissions(Integer.parseInt(""+(finalPermissions.charAt(2))));
            Boolean[] exe=unparsePermissions(Integer.parseInt(""+(finalPermissions.charAt(3))));
            readown.setChecked(read[0]);
            readgroup.setChecked(read[1]);
            readother.setChecked(read[2]);
            writeown.setChecked(write[0]);
            writegroup.setChecked(write[1]);
            writeother.setChecked(write[2]);
            exeown.setChecked(exe[0]);
            exegroup.setChecked(exe[1]);
            exeother.setChecked(exe[2]);
            a.positiveText(R.string.set);
            a.positiveColor(Color.parseColor(main.skin));
            a.callback(new MaterialDialog.Callback() {
                @Override
                public void onPositive(MaterialDialog materialDialog) {
                    int a=0,b=0,c=0;
                    if(readown.isChecked())a=4;
                    if(writeown.isChecked())b=2;
                    if(exeown.isChecked())c=1;
                    int owner=a+b+c;
                    int d=0,e=0,f=0;
                    if(readgroup.isChecked())d=4;
                    if(writegroup.isChecked())e=2;
                    if(exegroup.isChecked())f=1;
                    int group=d+e+f;
                    int g=0,h=0,i=0;
                    if(readother.isChecked())g=4;
                    if(writeother.isChecked())h=2;
                    if(exeother.isChecked())i=1;
                    int other=g+h+i;
                    String  finalValue="0"+owner+group+other;
                    String recursive="";

                    String command="chmod "+finalValue+" "+file.getPath();
                    if(file.isDirectory())command="chmod -R "+finalValue+" "+file.getPath();
                    Command com=new Command(1,command) {
                        @Override
                        public void commandOutput(int i, String s) {
                            Toast.makeText(main.getActivity(),s,Toast.LENGTH_LONG);
                        }

                        @Override
                        public void commandTerminated(int i, String s) {
                            Toast.makeText(main.getActivity(),s,Toast.LENGTH_LONG);
                        }

                        @Override
                        public void commandCompleted(int i, int i2) {
                            Toast.makeText(main.getActivity(),main.getResources().getString(R.string.done),Toast.LENGTH_LONG);
                        }
                    };
                    try {//
                        RootTools.remount(file.getPath(),"RW");
                        RootTools.getShell(true).add(com);
                        main.updateList();
                    } catch (Exception e1) {
                        Toast.makeText(main.getActivity(),main.getResources().getString(R.string.error),Toast.LENGTH_LONG).show();
                        e1.printStackTrace();
                    }

                }

                @Override
                public void onNegative(MaterialDialog materialDialog) {

                }
            });
            a.title(file.getName());
            a.customView(v);
            if(main.theme1==1)a.theme(Theme.DARK);
            a.build().show();}else{Toast.makeText(main.getActivity(),main.getResources().getString(R.string.enablerootmde),Toast.LENGTH_LONG).show();}
    }String per=null;
    public String getFilePermissionsSymlinks(String file,final Context c,boolean root)
    {per=null;
        File f=new File(file);
        if(f.isDirectory()){
            String ls = RootHelper.runAndWait("ls -l " + f.getParent(),root);
            String[] array=ls.split("\n");
            for(String x:array){String[] a=x.split(" ");
                if(a[a.length-1].equals(f.getName())){

                    return  getPermissions(x);}
            }
            return  null;}else{

            String ls = RootHelper.runAndWait("ls -l " + file,root);
            if(ls!=null){
                per=getPermissions(ls);}
            return per;}
    }
    public String[] parseName(String line){
        boolean linked=false;String name="",link="",size="-1";
        String[] array=line.split(" ");
        for(int i=0;i<array.length;i++){
            if(array[i].contains("->")){linked=true;}
        }
        if(!linked){int p=getColonPosition(array);
            size=array[p-2];
            for(int i=p+1;i<array.length;i++){name=name+" "+array[i];}
            name=name.trim();
            if(size.equals(""))size="-1";
            return new String[]{name,"",array[0],size};
        }
        else if(linked){int p=getColonPosition(array);
            size=array[p-2];
            int q=getLinkPosition(array);
            for(int i=p+1;i<q;i++){name=name+" "+array[i];}
            name=name.trim();
            for(int i=q+1;i<array.length;i++){link=link+" "+array[i];}
            if(size.equals(""))size="-1";
            return  new String[]{name,link,array[0],size};
        }
        if(size.equals(""))size="-1";
        return new String[]{name,"",array[0],size};
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
        return  0;
    }
    public String getPermissions(String line) {
        try {if(line.length()>=40) {
            String[] lineArray = line.split(" ");
            String rawPermissions = lineArray[0];


            StringBuilder finalPermissions = new StringBuilder();
            finalPermissions.append(parseSpecialPermissions(rawPermissions));
            finalPermissions.append(parsePermissions(rawPermissions.substring(1, 4)));
            finalPermissions.append(parsePermissions(rawPermissions.substring(4, 7)));
            finalPermissions.append(parsePermissions(rawPermissions.substring(7, 10)));
            return (finalPermissions.toString());
        }}catch (Exception e)
        {e.printStackTrace();
            return null;}
        return null;
    }
    public Boolean[] unparsePermissions(int i){

        switch (i){
            case 0:return new Boolean[]{false,false,false};
            case 1:return new Boolean[]{false,false,true};
            case 2:return new Boolean[]{false,true,false};
            case 3:return new Boolean[]{false,true,true};
            case 4:return new Boolean[]{true,false,false};
            case 5:return new Boolean[]{true,false,true};
            case 6:return new Boolean[]{true,true,false};
            case 7:return new Boolean[]{true,true,true};
            default:return null;
        }
    }
    public int parsePermissions(String permission) {
        int tmp;
        if (permission.charAt(0) == 'r')
            tmp = 4;
        else
            tmp = 0;

        RootTools.log("permission " + tmp);
        RootTools.log("character " + permission.charAt(0));

        if (permission.charAt(1) == 'w')
            tmp += 2;
        else
            tmp += 0;

        RootTools.log("permission " + tmp);
        RootTools.log("character " + permission.charAt(1));

        if (permission.charAt(2) == 'x')
            tmp += 1;
        else
            tmp += 0;

        RootTools.log("permission " + tmp);
        RootTools.log("character " + permission.charAt(2));

        return tmp;
    }

    public int parseSpecialPermissions(String permission) {
        int tmp = 0;
        if (permission.charAt(2) == 's')
            tmp += 4;

        if (permission.charAt(5) == 's')
            tmp += 2;

        if (permission.charAt(8) == 't')
            tmp += 1;

        RootTools.log("special permissions " + tmp);

        return tmp;
    }
}