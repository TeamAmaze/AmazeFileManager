/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>
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

package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.OpenMode;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;

import java.io.File;
import java.util.ArrayList;

public class RootHelper {
    public static String runAndWait(String cmd, boolean root) {

        Command c = new Command(0, cmd) {
            @Override
            public void commandOutput(int i, String s) {

            }

            @Override
            public void commandTerminated(int i, String s) {

            }

            @Override
            public void commandCompleted(int i, int i2) {

            }
        };
        try {
            RootTools.getShell(root).add(c);
        } catch (Exception e) {
            return null;
        }

        if (!waitForCommand(c, -1)) {
            return null;
        }

        return c.toString();
    }

    public static ArrayList<String> runAndWait1(String cmd, final boolean root, final long time) {
        final ArrayList<String> output = new ArrayList<String>();
        Command cc = new Command(1, cmd) {
            @Override
            public void commandOutput(int i, String s) {
                output.add(s);
            }

            @Override
            public void commandTerminated(int i, String s) {

                System.out.println("error" + root + s+time);

            }

            @Override
            public void commandCompleted(int i, int i2) {

            }
        };
        try {
            RootTools.getShell(root).add(cc);
        } catch (Exception e) {
            //       Logger.errorST("Exception when trying to run shell command", e);
            e.printStackTrace();
            return null;
        }

        if (!waitForCommand(cc, time)) {
            return null;
        }

        return output;
    }

    public static ArrayList<String> runAndWait1(String cmd, final boolean root) {
        final ArrayList<String> output = new ArrayList<String>();
        Command cc = new Command(1, cmd) {
            @Override
            public void commandOutput(int i, String s) {
                output.add(s);
            }

            @Override
            public void commandTerminated(int i, String s) {

                System.out.println("error" + root + s);

            }

            @Override
            public void commandCompleted(int i, int i2) {

            }
        };
        try {
            RootTools.getShell(root).add(cc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (!waitForCommand(cc, -1)) {
            return null;
        }

        return output;
    }

    private static boolean waitForCommand(Command cmd, long time) {
        long t = 0;
        while (!cmd.isFinished()) {
            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                        t += 2000;
                        if (t != -1 && t >= time)
                            return true;

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                return false;
            }
        }

        //Logger.debug("Command Finished!");
        return true;
    }


    public static String getCommandLineString(String input) {
        return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
    }

    private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";
    public static ArrayList<BaseFile> getFilesList(String path, boolean showHidden) {
        File f = new File(path);
        ArrayList<BaseFile> files = new ArrayList<>();
        try {
            if (f.exists() && f.isDirectory()) {
                for (File x : f.listFiles()) {
                    long size = 0;
                    if (!x.isDirectory()) size = x.length();
                    BaseFile baseFile=new BaseFile(x.getPath(), parseFilePermission(x), x.lastModified() , size, x.isDirectory());
                    baseFile.setName(x.getName());
                    baseFile.setMode(OpenMode.FILE);
                    if (showHidden) {
                        files.add(baseFile);
                    } else {
                        if (!x.isHidden()) {
                            files.add(baseFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }


        return files;
    }

    /**
     * Returns an array of list of files at a specific path in OTG
     * @param path the path to the directory tree, starts with prefix 'otg:/'
     *             Independent of URI (or mount point) for the OTG
     * @param context context for loading
     * @return an array of list of files at the path
     */
    public static ArrayList<BaseFile> getDocumentFilesList(String path, Context context) {

        SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(context);
        String rootUriString = manager.getString(MainActivity.KEY_PREF_OTG, null);
        DocumentFile rootUri = DocumentFile.fromTreeUri(context, Uri.parse(rootUriString));
        ArrayList<BaseFile> files = new ArrayList<>();

        String[] parts = path.split("/");
        for (int i=0; i<parts.length; i++) {

            // first omit 'otg:/' before iterating through DocumentFile
            if (path.equals("otg:/")) break;
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
                    BaseFile baseFile=new BaseFile(path + "/" + file.getName(),
                            parseDocumentFilePermission(file), file.lastModified() , size, file.isDirectory());
                    baseFile.setName(file.getName());
                    baseFile.setMode(OpenMode.OTG);
                    files.add(baseFile);
                }
            } catch (Exception e) {
            }
        }

        return files;
    }

    public static BaseFile generateBaseFile(File x, boolean showHidden) {
        long size = 0;
        if (!x.isDirectory())
            size = x.length();
        BaseFile baseFile=new BaseFile(x.getPath(), parseFilePermission(x), x.lastModified() , size, x.isDirectory());
        baseFile.setName(x.getName());
        baseFile.setMode(OpenMode.FILE);
        if (showHidden) {
            return (baseFile);
        } else if (!x.isHidden()) {
            return (baseFile);
        }
        return null;
    }

    public static BaseFile generateBaseFile(DocumentFile file, boolean showHidden) {
        long size = 0;
        if (!file.isDirectory())
            size = file.length();
        BaseFile baseFile=new BaseFile(file.getName(), parseDocumentFilePermission(file),
                file.lastModified() , size, file.isDirectory());
        baseFile.setName(file.getName());
        baseFile.setMode(OpenMode.OTG);

        return baseFile;
    }

    public static String parseFilePermission(File f) {
        String per = "";
        if (f.canRead()) {
            per = per + "r";
        }
        if (f.canWrite()) {
            per = per + "w";
        }
        if (f.canExecute()) {
            per = per + "x";
        }
        return per;
    }

    public static String parseDocumentFilePermission(DocumentFile file) {
        String per = "";
        if (file.canRead()) {
            per = per + "r";
        }
        if (file.canWrite()) {
            per = per + "w";
        }
        if (file.canWrite()) {
            per = per + "x";
        }
        return per;
    }

    public static boolean fileExists(String path){
        File f=new File(path);
        String p=f.getParent();
        if (p != null && p.length() >0) {
            ArrayList<BaseFile> ls = getFilesList(p,true,true,null);
            for(BaseFile strings:ls){
                if(strings.getPath()!=null && strings.getPath().equals(path)){
                    return true;
                }

            }
        }
    return false;
    }
    static boolean contains(String[] a,String name){
        for(String s:a){
            //Log.e("checking",s);
            if(s.equals(name))return true;
        }
        return false;
    }
    public static boolean isDirectory(String a, boolean root,int count) {
        File f = new File(a);
        String name = f.getName();
        String p = f.getParent();
        if (p != null && p.length() > 1) {
            ArrayList<String> ls = runAndWait1("ls -la " + p, root, 2000);
            for (String s : ls) {
                if (contains(s.split(" "),name)) {
                    try {
                        BaseFile path = Futils.parseName(s);
                        if (path.getPermisson().trim().startsWith("d")) return true;
                        else if (path.getPermisson().trim().startsWith("l")) {
                            if(count>5)
                                return f.isDirectory();
                            else
                            return isDirectory(path.getLink().trim(), root, ++count);
                        }
                        else return f.isDirectory();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

        }
        return f.isDirectory();
    }

    static boolean isDirectory(BaseFile path) {
        if (path.getPermisson().startsWith("d")) return true;
        else return new File(path.getPath()).isDirectory();
    }
    public interface GetModeCallBack{
        void getMode(OpenMode mode);
    }
    public static ArrayList<BaseFile> getFilesList(String path, boolean root, boolean showHidden,GetModeCallBack getModeCallBack) {
        String p = " ";
        OpenMode mode=OpenMode.FILE;
        if (showHidden) p = "a ";
        ArrayList<BaseFile> a = new ArrayList<>();
        ArrayList<String> ls = new ArrayList<>();
        if (root) {
            if (!path.startsWith("/storage") && !path.startsWith("/sdcard")) {
                String cpath = getCommandLineString(path);
                ls = runAndWait1("ls -l" + p + cpath, root);
                if (ls != null) {
                    for (int i=0;i<ls.size();i++) {
                        String file=ls.get(i);
                        if (!file.contains("Permission denied"))
                            try {
                                BaseFile array = Futils.parseName(file);
                                array.setMode(OpenMode.ROOT);
                                if (array != null) {
                                    array.setName(array.getPath());
                                    array.setPath( path + "/" + array.getPath());
                                    if (array.getLink().trim().length() > 0) {
                                        boolean isdirectory = isDirectory(array.getLink(), root,0);
                                        array.setDirectory(isdirectory);
                                    } else array.setDirectory(isDirectory(array));
                                    a.add(array);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                    }
                    mode=OpenMode.ROOT;
                }
            } else if (Futils.canListFiles(new File(path))) {
                a = getFilesList(path, showHidden);
                mode = OpenMode.FILE;
            } else {
                mode = OpenMode.FILE;
                a = new ArrayList<>();
            }
        } else if (Futils.canListFiles(new File(path))) {
            a = getFilesList( path, showHidden);
            mode=OpenMode.FILE;
        } else {
            mode=OpenMode.FILE;
            a = new ArrayList<>();
        }
        if (a.size() == 0 && Futils.canListFiles(new File(path))) {
            a = getFilesList( path, showHidden);
            mode=OpenMode.FILE;
        }
        if(getModeCallBack!=null)getModeCallBack.getMode(mode);
        return a;

    }

}