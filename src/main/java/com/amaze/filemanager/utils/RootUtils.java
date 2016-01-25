package com.amaze.filemanager.utils;

/**
 * Created by arpitkh996 on 25-01-2016.
 */

import com.amaze.filemanager.filesystem.RootHelper;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RootUtils {
    public static final String DATA_APP_DIR = "/data/app";
    private static final String LS = "ls -lAnH \"%\" --color=never";
    private static final String LSDIR = "ls -land \"%\" --color=never";
    public static final String SYSTEM_APP_DIR = "/system/app";
    private static final Pattern mLsPattern;

    static {
        mLsPattern = Pattern.compile(".[rwxsStT-]{9}\\s+.*");
    }

    public static boolean isValid(String str) {
        return mLsPattern.matcher(str).matches();
    }

    public static boolean isUnixVirtualDirectory(String str) {
        return str.startsWith("/proc") || str.startsWith("/sys");
    }

    public static ArrayList<String> getDirListingSu(String str) {
            ArrayList<String> arrayLis=RootHelper.runAndWait1(LS.replace("%", str),true);
            return arrayLis;
    }

    public static ArrayList<String> getDirListing(String str)  {
        ArrayList<String> arrayLis=RootHelper.runAndWait1(LS.replace("%", str),false);
        return arrayLis;
    }

    public static void chmod(File sEFile, String str, boolean z) throws Exception {
        chmod(Arrays.asList(new File[]{sEFile}), str, z);
    }
    public static boolean isWritable(String m){
        try {
            if(m.toLowerCase().equals("rw"))
                return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static void chmod(List<File> list, String str, boolean z) throws Exception {
        String path =  list.get(0).getPath();
            String m=  RootTools.getMountedAs(path);
            boolean b=isWritable(m);
            if(!b)
                RootTools.remount(path,"rw");
            for (File path2 : list) {
                path = path2.getPath();
                String str2 = "chmod %s %s \"%s\"";
                Object[] objArr = new Object[3];
                objArr[0] = z ? "-R" : "";
                objArr[1] = str;
                objArr[2] = path;
                RootHelper.runAndWait(String.format(str2, objArr),true);
            }

            if(!b && m!=null && m.length()>0)
                RootTools.remount(path,m);
    }
    public static String parsePermission(String permLine) {
        int owner = 0;
        int READ = 4;
        int WRITE = 2;
        int EXECUTE = 1;
        if (permLine.charAt(1) == 'r') {
            owner += READ;
        }
        if (permLine.charAt(2) == 'w') {
            owner += WRITE;
        }
        if (permLine.charAt(3) == 'x') {
            owner += EXECUTE;
        }
        int group = 0;
        if (permLine.charAt(4) == 'r') {
            group += READ;
        }
        if (permLine.charAt(5) == 'w') {
            group += WRITE;
        }
        if (permLine.charAt(6) == 'x') {
            group += EXECUTE;
        }
        int world = 0;
        if (permLine.charAt(7) == 'r') {
            world += READ;
        }
        if (permLine.charAt(8) == 'w') {
            world += WRITE;
        }
        if (permLine.charAt(9) == 'x') {
            world += EXECUTE;
        }
        String finalValue = owner + "" + group + "" + world;
        return finalValue;
    }
    /*
    public static void createSymLink(String str, String str2) throws SEException {
        StorageManager instance = StorageManager.getInstance();
        try {
            Console instance2 = Console.getInstance();
            instance2.su();
            String str3 = "ln -s \"" + str + "\" \"" + str2 + "\"";
            instance.remount(str2, true);
            instance2.execute(str3);
            instance.remount(str2, false);
        } catch (Throwable e) {
            throw Exceptions.operationFailed(e);
        } catch (Throwable e2) {
            throw Exceptions.operationFailed(e2);
        } catch (Throwable th) {
            instance.remount(str2, false);
        }
    }
*/
    /*public static void copy(String str, String str2, boolean z) throws SEException {
        StorageManager instance = StorageManager.getInstance();
        try {
            Console instance2 = Console.getInstance();
            instance2.su();
            String str3 = "cp -%s \"%s\" \"%s\"";
            Object[] objArr = new Object[3];
            objArr[0] = z ? "fp" : "f";
            objArr[1] = str;
            objArr[2] = str2;
            String format = String.format(str3, objArr);
            instance.remount(str2, true);
            instance2.execute(format);
            instance.remount(str2, false);
        } catch (Throwable e) {
            throw Exceptions.copyError(str, str2, e);
        } catch (Throwable e2) {
            throw Exceptions.copyError(str, str2, e2);
        } catch (Throwable th) {
            instance.remount(str2, false);
        }
    }
*/
  /*  public static void copy(SEFile sEFile, SEFile sEFile2, boolean z) throws SEException {
        copy(sEFile.getPath(), sEFile2.getPath(), z);
    }
*/
    public static void dd(String str, String str2) throws Exception {
            String m=  RootTools.getMountedAs(str2);
            boolean b=isWritable(m);
            if(!b)RootTools.remount(str2,"rw");
            String str3 = "dd if=\"" + str + "\" of=\"" + str2 + "\"";
            RootHelper.runAndWait(str3,true);
            if(!b && m!=null && m.length()>0)
                RootTools.remount(str2,m);

        }
/*
    public static void move(String str, String str2) throws SEException {
        StorageManager instance = StorageManager.getInstance();
        try {
            Console instance2 = Console.getInstance();
            instance2.su();
            instance.remount(str2, true);
            instance2.execute("mv -f \"" + str + "\" \"" + str2 + "\"");
            instance.remount(str2, false);
        } catch (Throwable e) {
            throw Exceptions.copyError(str, str2, e);
        } catch (Throwable e2) {
            throw Exceptions.copyError(str, str2, e2);
        } catch (Throwable th) {
            instance.remount(str2, false);
        }
    }

    public static void copyAndDelete(String str, String str2) {
        copy(str, str2, true);
        deleteFile(str);
    }

    public static void rename(String str, String str2) throws SEException {
        StorageManager instance = StorageManager.getInstance();
        try {
            Console instance2 = Console.getInstance();
            instance2.su();
            instance.remount(str2, true);
            instance2.execute("mv \"" + str + "\" \"" + str2 + "\"");
            instance.remount(str2, false);
        } catch (Throwable e) {
            throw Exceptions.renameError(str2, e);
        } catch (Throwable e2) {
            throw Exceptions.renameError(str2, e2);
        } catch (Throwable th) {
            instance.remount(str2, false);
        }
    }

    public static void mkdir(String str) throws SEException {
        StorageManager instance = StorageManager.getInstance();
        try {
            Console instance2 = Console.getInstance();
            instance2.su();
            instance.remount(str, true);
            instance2.execute("mkdir -p -m 755 \"" + str + "\"");
            instance.remount(str, false);
        } catch (Throwable e) {
            throw Exceptions.mkdirError(str, e);
        } catch (Throwable e2) {
            throw Exceptions.mkdirError(str, e2);
        } catch (Throwable th) {
            instance.remount(str, false);
        }
    }

    public static void mkfile(String str) throws SEException {
        StorageManager instance = StorageManager.getInstance();
        try {
            Console instance2 = Console.getInstance();
            instance2.su();
            instance.remount(str, true);
            instance2.execute("touch \"" + str + "\"");
            instance.remount(str, false);
        } catch (Throwable e) {
            throw Exceptions.mkfileError(str, e);
        } catch (Throwable e2) {
            throw Exceptions.mkfileError(str, e2);
        } catch (Throwable th) {
            instance.remount(str, false);
        }
    }

    public static boolean deleteFile(String str) throws SEException {
        StorageManager instance = StorageManager.getInstance();
        try {
            Console instance2 = Console.getInstance();
            instance2.su();
            instance.remount(str, true);
            instance2.execute("rm -r \"" + str + "\"");
            instance.remount(str, false);
            return true;
        } catch (Throwable e) {
            throw Exceptions.deleteError(str, e);
        } catch (Throwable e2) {
            throw Exceptions.deleteError(str, e2);
        } catch (Throwable th) {
            instance.remount(str, false);
        }
    }
*/}

