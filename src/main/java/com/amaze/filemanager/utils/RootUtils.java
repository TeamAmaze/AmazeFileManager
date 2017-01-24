package com.amaze.filemanager.utils;

/**
 * Created by arpitkh996 on 25-01-2016.
 */

import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.RootHelper;

import java.util.ArrayList;
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

    /**
     * Get a shell based listing
     * Context is superuser level shell
     * @param str
     * @return
     */
    public static ArrayList<String> getDirListingSu(String str) throws RootNotPermittedException {
        ArrayList<String> arrayLis=RootHelper.runShellCommand(LS.replace("%", str));
        return arrayLis;
    }

    /**
     * Get a shell based listing
     * Context is an third-party context level shell
     * @param str
     * @return
     */
    public static List<String> getDirListing(String str) throws RootNotPermittedException {
        return RootHelper.runNonRootShellCommand(LS.replace("%", str));
    }

    /**
     * Change permissions (owner/group/others) of a specified path
     * @param path
     * @param octalNotation octal notation of permission
     * @throws RootNotPermittedException
     */
    public static void chmod(String path, int octalNotation) throws RootNotPermittedException {
        String command = "chmod %d \"%s\"";

        RootHelper.runShellCommand(String.format(command, octalNotation, path));
    }


    /**
     * Mount path for writable access (rw)
     * @param path
     * @throws RootNotPermittedException
     */
    private static void mountOwnerRW(String path) throws RootNotPermittedException{
        chmod(path, 644);
    }

    /**
     * Mount path for readable access (ro)
     * @param path
     * @throws RootNotPermittedException
     */
    private static void mountOwnerRO(String path) throws RootNotPermittedException{
        chmod(path, 444);
    }

    /**
     * Copies file using root
     * @param source
     * @param destination
     * @param mountPath
     * @throws RootNotPermittedException
     */
    public static void copy(String source, String destination, String mountPath) throws RootNotPermittedException {

        int mountPathPermissionsOctal = 644;
        if (mountPath != null) {

            // target is inside root director, mount the parent first, before writing
            mountPathPermissionsOctal = getFilePermissions(mountPath);
            mountOwnerRW(mountPath);
        }
        RootHelper.runShellCommand("cp \"" + source + "\" \"" + destination + "\"");

        if (mountPath != null) {
            chmod(mountPath, mountPathPermissionsOctal);
        }
    }

    /**
     * Creates an empty directory using root
     * @param path path to new directory
     * @param name name of directory
     * @param mountPath path to mount
     * @throws RootNotPermittedException
     */
    public static void mkDir(String path, String name, String mountPath) throws RootNotPermittedException {

        int mountPathPermissionsOctal = getFilePermissions(mountPath);
        mountOwnerRW(mountPath);
        RootHelper.runShellCommand("mkdir \"" + path + "/" + name + "\"");
        chmod(mountPath, mountPathPermissionsOctal);
    }

    /**
     * Creates an empty file using root
     * @param path path to new file
     * @param mountPath path to mount
     * @throws RootNotPermittedException
     */
    public static void mkFile(String path, String mountPath) throws RootNotPermittedException {

        int mountPathPermissionsOctal = getFilePermissions(mountPath);
        mountOwnerRW(mountPath);
        RootHelper.runShellCommand("touch \"" + path +"\"");
        chmod(mountPath, mountPathPermissionsOctal);
    }


    /**
     * Returns file permissions in octal notation
     * Method requires busybox
     * @param path
     * @return
     */
    public static int getFilePermissions(String path) throws RootNotPermittedException {
        String line = RootHelper.runShellCommand("stat -c  %a \"" + path + "\"").get(0);

        return Integer.valueOf(line.toString());
    }

    /**
     * Recursively removes a path with it's contents (if any)
     * @param path
     * @param mountPath path to mount before performing operation
     * @throws RootNotPermittedException
     */
    public static void delete(String path, String mountPath) throws RootNotPermittedException {

        mountOwnerRW(mountPath);
        RootHelper.runShellCommand("rm -r \"" + path + "\"");
    }

    public static boolean isBusyboxAvailable() throws RootNotPermittedException {
        ArrayList<String> output = RootHelper.runShellCommand("busybox");
        return output.size()!=0;
    }

    /**
     * Moves file using root
     * @param path
     * @param destination
     * @param mountPath path to mount before performing operation
     * @throws RootNotPermittedException
     */
    public static void move(String path, String destination, String mountPath)
            throws RootNotPermittedException {

        int mountPathPermissionsOctal = getFilePermissions(mountPath);
        mountOwnerRW(mountPath);
        RootHelper.runShellCommand("mv \"" + path + " \" \"" + destination + "\"");
        chmod(mountPath, mountPathPermissionsOctal);
    }

    /**
     * Renames file using root
     * @param oldPath path to file before rename
     * @param newPath path to file after rename
     * @param mountPath path to mount before performing operation
     * @throws RootNotPermittedException
     */
    public static void rename(String oldPath, String newPath, String mountPath)
            throws RootNotPermittedException {

        int mountPathPermissionsOctal = getFilePermissions(mountPath);
        mountOwnerRW(mountPath);
        RootHelper.runShellCommand("mv \"" + oldPath + "\" \"" + newPath + "\"");
        chmod(mountPath, mountPathPermissionsOctal);
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
}

