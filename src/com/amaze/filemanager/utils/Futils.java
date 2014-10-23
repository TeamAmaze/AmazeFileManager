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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.TextReader;
import com.amaze.filemanager.adapters.DialogAdapter;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.ZipTask;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class Futils {
    public Futils() {
    }

    ArrayList<File> lis = new ArrayList<File>();
    ArrayList<File> images = new ArrayList<File>();
    AlertDialog.Builder b = null;

    public void scanFile(String path, Context c) {

        Uri contentUri = Uri.fromFile(new File(path));
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
        c.sendBroadcast(mediaScanIntent);
    }

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
        intent.setDataAndType(Uri.fromFile(f), type);
        c.startActivity(intent);

    }

    public void openWith(File f, Context c) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), "*/*");
        c.startActivity(intent);

    }public String getSize(File f) {
        long i =f.length();

        return readableFileSize(i);

    }

    public String getSize(String[] array) {
        String sym=array[1];
        long i;
        if(sym!=null && sym.length()!=0){i=new File(array[1]).length();}
        else
            i = new File(array[0]).length();

        return readableFileSize(i);

    }

    public void deleteFiles(ArrayList<Layoutelements> a, final Main b, List<Integer> pos) {
        AlertDialog.Builder c = new AlertDialog.Builder(b.getActivity());
        View v = b.getActivity().getLayoutInflater().inflate(R.layout.dialoginfo, null);
        TextView tb = (TextView) v.findViewById(R.id.info);
        c.setTitle(getString(b.getActivity(), R.string.confirm));
        String names = "";
        final ArrayList<File> todelete = new ArrayList<File>();
        for (int i = 0; i < pos.size(); i++) {
            String path = a.get(pos.get(i)).getDesc();
            todelete.add(new File(path));
            names = names + "\n" + "(" + (i + 1) + ".)" + new File(path).getName();
        }
        tb.setText(getString(b.getActivity(), R.string.questiondelete) + names);
        c.setView(v);
        c.setNegativeButton(getString(b.getActivity(), R.string.no), null);
        c.setPositiveButton(getString(b.getActivity(), R.string.yes), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                Toast.makeText(b.getActivity(), getString(b.getActivity(), R.string.deleting), Toast.LENGTH_LONG).show();
                if(todelete.get(0).getParentFile().canWrite()){
                    Intent i = new Intent(b.getActivity(), DeleteTask.class);
                    i.putStringArrayListExtra("files", toStringArray(todelete));
                    b.getActivity().startService(i);}
                else if(b.rootMode){for(File f:todelete){
                    RootTools.deleteFileOrDirectory(f.getPath(),true);}
                    b.updateList();
                }
            }
        });
        c.show();
    }

    public String count(File f,boolean root) {
        try {
            Integer i=RootHelper.getCount(f);
            if(i!=null){return i+" items";}
            else{return "";}

        } catch (Exception e) {
            return "";
        }
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
    public String getdate(long f,String form) {

        SimpleDateFormat sdf = new SimpleDateFormat(form);
        return (sdf.format(f)).toString();
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

    public void showProps(File f, Activity c,boolean root) {
        String date = getString(c, R.string.date) + getdate(f);
        String items = "", size = "", name, parent;
        name = getString(c, R.string.name) + f.getName();
        parent = getString(c, R.string.location) + f.getParent();
        if (f.isDirectory()) {
            size = getString(c, R.string.size) + readableFileSize(folderSize(f,root));
            items = getString(c, R.string.totalitems) + count(f,root);
        } else if (f.isFile()) {
            items = "";
            size = getString(c, R.string.size) + getSize(f);
        }
        AlertDialog.Builder a = new AlertDialog.Builder(c);
        View v = c.getLayoutInflater().inflate(R.layout.dialoginfo, null);
        TextView tb = (TextView) v.findViewById(R.id.info);
        a.setTitle(getString(c, R.string.properties));
        tb.setText(name + "\n" + parent + "\n" + size + "\n" + items + "\n"
                + date);
        a.setView(v);
        a.show();
    }

    public static long folderSize(File directory,boolean rootMode) {
        long length = 0;
        for (File file:directory.listFiles()) {

            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file,rootMode);
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

    public static void delete(File file) throws IOException {

        if (file.isDirectory()) {

            // directory is empty, then delete it
            if (file.list().length == 0) {

                file.delete();
                System.out.println("Directory is deleted : "
                        + file.getAbsolutePath());

            } else {

                // list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    // construct the file structure
                    File fileDelete = new File(file, temp);

                    // recursive delete
                    delete(fileDelete);
                }

                // check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                    System.out.println("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        } else {
            // if file, then delete it
            file.delete();
            System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }

    public boolean deletefiles(File f) {

        // make sure directory exists
        if (!f.exists()) {

            System.out.println("Directory does not exist.");
            return false;

        } else {

            try {

                delete(f);
                return true;
            } catch (IOException e) {
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
        } else {
            try {
                openunknown(f, m);
            } catch (Exception e) {
                Toast.makeText(m, getString(m, R.string.noappfound),Toast.LENGTH_LONG).show();
                openWith(f, m);
            }
        }
    }

    public Layoutelements newElement(Drawable i, String d,String permissions,String symlink,String size) {
        Layoutelements item = new Layoutelements(i, new File(d).getName(), d,permissions,symlink,size);
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
        AlertDialog.Builder a = new AlertDialog.Builder(m);
        View v = m.getLayoutInflater().inflate(R.layout.dialog, null);
        final EditText e = (EditText) v.findViewById(R.id.newname);
        e.setText("Newzip.zip");
        a.setView(v);
        a.setTitle(getString(m, R.string.enterzipname));
        e.setHint(getString(m, R.string.enterzipname));
        a.setPositiveButton(getString(m, R.string.create), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                Intent intent2 = new Intent(m, ZipTask.class);
                String name = current + "/" + e.getText().toString();
                intent2.putExtra("name", name);
                intent2.putExtra("files", b);
                m.startService(intent2);
                // TODO: Implement this method
            }
        });
        a.setNegativeButton(getString(m, R.string.cancel), null);
        a.show();
    }

    public void longClickSearchItem(final Main main, String files) {
        final File f = new File(files);
        AlertDialog.Builder ba = new AlertDialog.Builder(main.getActivity());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                main.getActivity(), android.R.layout.select_dialog_item);
        Toast.makeText(main.getActivity(), files, Toast.LENGTH_SHORT).show();
        ba.setTitle(f.getName());
        adapter.add(getString(main.getActivity(), R.string.openparent));
        adapter.add(getString(main.getActivity(), R.string.openwith));
        adapter.add(getString(main.getActivity(), R.string.about));
        adapter.add(getString(main.getActivity(), R.string.share));
        adapter.add(getString(main.getActivity(), R.string.compress));
        if (!f.isDirectory() && f.getName().endsWith(".zip"))
            adapter.add(getString(main.getActivity(), R.string.extract));
        ba.setAdapter(adapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                switch (p2) {
                    case 0:
                        main.loadlist(new File(main.slist.get(p2).getDesc()).getParentFile(), true);
                        break;
                    case 1:
                        openunknown(f, main.getActivity());
                        break;
                    case 2:
                        showProps(f, main.getActivity(),main.rootMode);
                        break;
                    case 3:
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_SEND);
                        i.setType("*/*");
                        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                        main.startActivity(i);
                        break;
                    case 4:
                        ArrayList<String> copies1 = new ArrayList<String>();
                        copies1.add(f.getPath());
                        showNameDialog((MainActivity) main.getActivity(), copies1, main.current);

                        break;
                    case 5:
                        Intent intent = new Intent(main.getActivity(), ExtractService.class);
                        intent.putExtra("zip", f.getPath());
                        main.getActivity().startService(intent);
                        break;
                }
            }
        });
        ba.show();
    }

    public void showSortDialog(final Main m) {
        String[] sort = m.getResources().getStringArray(R.array.sortby);
        int current = Integer.parseInt(m.Sp.getString("sortby", "0"));
        AlertDialog.Builder a = new AlertDialog.Builder(m.getActivity());

        a.setSingleChoiceItems(new ArrayAdapter<String>(m.getActivity(), android.R.layout.select_dialog_singlechoice, sort), current, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {

                m.Sp.edit().putString("sortby", "" + i).commit();
                m.getSortModes();
                m.loadlist(new File(m.current), false);
                dialog.cancel();
            }
        });
        a.setTitle(getString(m.getActivity(), R.string.sortby));
        a.setNegativeButton(getString(m.getActivity(), R.string.cancel), null);
        a.show();
    }

    public void showDirectorySortDialog(final Main m) {
        String[] sort = m.getResources().getStringArray(R.array.directorysortmode);
        AlertDialog.Builder a = new AlertDialog.Builder(m.getActivity());
        int current = Integer.parseInt(m.Sp.getString("dirontop", "0"));

        a.setSingleChoiceItems(new ArrayAdapter<String>(m.getActivity(), android.R.layout.select_dialog_singlechoice, sort), current, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                m.Sp.edit().putString("dirontop", "" + i).commit();
                m.getSortModes();
                m.loadlist(new File(m.current), false);
            }
        });

        a.setTitle(getString(m.getActivity(), R.string.directorysort));
        a.setNegativeButton(getString(m.getActivity(), R.string.cancel), null);
        a.show();
    }

    public void showHistoryDialog(final Main m) {
        final ArrayList<String> paths = m.history.readTable();

        AlertDialog.Builder a = new AlertDialog.Builder(m.getActivity());
        a.setTitle(getString(m.getActivity(), R.string.history));
        DialogAdapter adapter = new DialogAdapter(m.getActivity(), R.layout.bookmarkrow, toFileArray(paths));
        a.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                m.loadlist(new File(paths.get(i)), true);
            }
        });
        a.setNegativeButton(getString(m.getActivity(), R.string.cancel), null);
        a.show();

    }

    public void showBookmarkDialog(final Main m, Shortcuts sh) {
        try {
            final ArrayList<File> fu = sh.readS();

            AlertDialog.Builder ba = new AlertDialog.Builder(m.getActivity());
            ba.setTitle(getString(m.getActivity(), R.string.books));

            DialogAdapter adapter = new DialogAdapter(
                    m.getActivity(), android.R.layout.select_dialog_item, fu);
            ba.setAdapter(adapter, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface p1, int p2) {
                    final File f = fu.get(p2);
                    if (f.isDirectory()) {

                        m.loadlist(f, false);
                    } else {
                        openFile(f, (MainActivity) m.getActivity());
                    }
                }
            });
            ba.setNegativeButton(getString(m.getActivity(), R.string.cancel), null);
            ba.show();
        } catch (IOException e) {
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        }
    }
    public void setPermissionsDialog(final Layoutelements f, final Main main){
        if(main.rootMode){
            final File file=new File(f.getDesc());
            AlertDialog.Builder a=new AlertDialog.Builder(main.getActivity());
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

            a.setPositiveButton("Set",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int j) {
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
                            Toast.makeText(main.getActivity(),"done",Toast.LENGTH_LONG);
                        }
                    };
                    try {//
                        RootTools.remount(file.getPath(),"RW");
                        RootTools.getShell(true).add(com);
                        main.updateList();
                    } catch (Exception e1) {
                        Toast.makeText(main.getActivity(),"Error",Toast.LENGTH_LONG).show();
                        e1.printStackTrace();
                    }
                }
            });
            a.setTitle(file.getName());
            a.setView(v);
            a.show();}else{Toast.makeText(main.getActivity(),"Enable Root Mode",Toast.LENGTH_LONG).show();}
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
        boolean linked=false;String name="",link="";
        String[] array=line.split(" ");
        for(int i=0;i<array.length;i++){
            if(array[i].contains("->")){linked=true;}
        }
        if(!linked){int p=getColonPosition(array);
            for(int i=p+1;i<array.length;i++){name=name+" "+array[i];}
            name=name.trim();
            return new String[]{name,"",array[0]};
        }
        else if(linked){int p=getColonPosition(array);
            int q=getLinkPosition(array);
            for(int i=p+1;i<q;i++){name=name+" "+array[i];}
            name=name.trim();
            for(int i=q+1;i<array.length;i++){link=link+" "+array[i];}
            return  new String[]{name,link,array[0]};
        }
        return new String[]{name,"",array[0]};
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