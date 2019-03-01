package com.amaze.filemanager.utils;

/**
 * Created by arpitkh996 on 25-01-2016.
 */

import android.util.Log;

import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.RootHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RootUtils {
    public static final int CHMOD_READ = 4, CHMOD_WRITE = 2, CHMOD_EXECUTE = 1;
    public static final String DATA_APP_DIR = "/data/app";
    /**
     * This is the chmod command, it should be used with String.format().
     * String.format(CHMOD_COMMAND, options, permsOctalInt, path);
     */
    public static final String CHMOD_COMMAND = "chmod %s %o \"%s\"";
    private static final String LS = "ls -lAnH \"%\" --color=never";
    private static final String LSDIR = "ls -land \"%\" --color=never";
    public static final String SYSTEM_APP_DIR = "/system/app";
    private static final Pattern mLsPattern;

    static {
        mLsPattern = Pattern.compile(".[rwxsStT-]{9}\\s+.*");
    }

    /**
     * Mount filesystem associated with path for writable access (rw)
     * Since we don't have the root of filesystem to remount, we need to parse output of
     * # mount command.
     *
     * @param path the path on which action to perform
     * @return String the root of mount point that was ro, and mounted to rw; null otherwise
     */
    private static String mountFileSystemRW(String path) throws ShellNotRunningException {
        String command = "mount";
        ArrayList<String> output = RootHelper.runShellCommandToList(command);
        String mountPoint = "", types = null;
        for (String line : output) {
            String[] words = line.split(" ");

            if (path.contains(words[2])) {
                // current found point is bigger than last one, hence not a conflicting one
                // we're finding the best match, this omits for eg. / and /sys when we're actually
                // looking for /system
                if (words[2].length() > mountPoint.length()) {
                    mountPoint = words[2];
                    types = words[5];
                }
            }
        }

        if (!mountPoint.equals("") && types != null) {
            // we have the mountpoint, check for mount options if already rw
            if (types.contains("rw")) {
                // already a rw filesystem return
                return null;
            } else if (types.contains("ro")) {
                // read-only file system, remount as rw
                String mountCommand = "mount -o rw,remount " + mountPoint;
                ArrayList<String> mountOutput = RootHelper.runShellCommandToList(mountCommand);

                if (mountOutput.size() != 0) {
                    // command failed, and we got a reason echo'ed
                    return null;
                } else return mountPoint;
            }
        }
        return null;
    }

    /**
     * Mount path for read-only access (ro)
     *
     * @param path the root of device/filesystem to be mounted as ro
     */
    private static void mountFileSystemRO(String path) throws ShellNotRunningException {
        String command = "umount -r \"" + path + "\"";
        RootHelper.runShellCommand(command);
    }

    /**
     * Copies file using root
     */
    public static void copy(String source, String destination) throws ShellNotRunningException {
        // remounting destination as rw
        String mountPoint = mountFileSystemRW(destination);

        RootHelper.runShellCommand("cp -r \"" + source + "\" \"" + destination + "\"");

        if (mountPoint != null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * Creates an empty directory using root
     *
     * @param path path to new directory
     * @param name name of directory
     */
    public static void mkDir(String path, String name) throws ShellNotRunningException {

        String mountPoint = mountFileSystemRW(path);

        RootHelper.runShellCommand("mkdir \"" + path + "/" + name + "\"");
        if (mountPoint != null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * Creates an empty file using root
     *
     * @param path path to new file
     */
    public static void mkFile(String path) throws ShellNotRunningException {
        String mountPoint = mountFileSystemRW(path);

        RootHelper.runShellCommand("touch \"" + path + "\"");
        if (mountPoint != null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * Returns file permissions in octal notation
     * Method requires busybox
     */
    private static int getFilePermissions(String path) throws ShellNotRunningException {
        String line = RootHelper.runShellCommandToList("stat -c  %a \"" + path + "\"").get(0);

        return Integer.valueOf(line);
    }

    /**
     * Recursively removes a path with it's contents (if any)
     *
     * @return boolean whether file was deleted or not
     */
    public static boolean delete(String path) throws ShellNotRunningException {
        String mountPoint = mountFileSystemRW(path);
        ArrayList<String> result = RootHelper.runShellCommandToList("rm -rf \"" + path + "\"");

        if (mountPoint != null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }

        return result.size() != 0;
    }

    /**
     * Moves file using root
     */
    public static void move(String path, String destination) throws ShellNotRunningException {
        // remounting destination as rw
        String mountPoint = mountFileSystemRW(destination);

        //mountOwnerRW(mountPath);
        RootHelper.runShellCommand("mv \"" + path + "\" \"" + destination + "\"");

        if (mountPoint != null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * Renames file using root
     *
     * @param oldPath path to file before rename
     * @param newPath path to file after rename
     * @return if rename was successful or not
     */
    public static boolean rename(String oldPath, String newPath) throws ShellNotRunningException {
        String mountPoint = mountFileSystemRW(oldPath);
        ArrayList<String> output = RootHelper.runShellCommandToList("mv \"" + oldPath + "\" \"" + newPath + "\"");

        if (mountPoint != null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }

        return output.size() == 0;
    }

    public static void cat(String sourcePath, String destinationPath)
            throws ShellNotRunningException {

        String mountPoint = mountFileSystemRW(destinationPath);

        RootHelper.runShellCommand("cat \"" + sourcePath + "\" > \"" + destinationPath + "\"");
        if (mountPoint != null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * This converts from a set of booleans to OCTAL permissions notations.
     * For use with {@link RootUtils#CHMOD_COMMAND}
     * (true, false, false,  true, true, false,  false, false, true) => 0461
     */
    public static int permissionsToOctalString(boolean ur, boolean uw, boolean ux,
                                                  boolean gr, boolean gw, boolean gx,
                                                  boolean or, boolean ow, boolean ox) {
        int u = ((ur?CHMOD_READ:0) | (uw?CHMOD_WRITE:0) | (ux?CHMOD_EXECUTE:0)) << 6;
        int g = ((gr?CHMOD_READ:0) | (gw?CHMOD_WRITE:0) | (gx?CHMOD_EXECUTE:0)) << 3;
        int o = (or?CHMOD_READ:0) | (ow?CHMOD_WRITE:0) | (ox?CHMOD_EXECUTE:0);
        return u | g | o;
    }

}

