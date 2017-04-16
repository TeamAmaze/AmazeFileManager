package com.amaze.filemanager.utils;

/**
 * Created by arpitkh996 on 25-01-2016.
 */

import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.RootHelper;

import java.util.ArrayList;
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
     * Change permissions (owner/group/others) of a specified path
     * @param path
     * @param octalNotation octal notation of permission
     * @throws RootNotPermittedException
     */
    public static void chmod(String path, int octalNotation) throws RootNotPermittedException {

        String mountPoint = mountFileSystemRW(path);

        String command = "chmod %d \"%s\"";

        RootHelper.runShellCommand(String.format(command, octalNotation, path));

        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }


    /**
     * Mount filesystem associated with path for writable access (rw)
     * Since we don't have the root of filesystem to remount, we need to parse output of
     * # mount command.
     * @param path the path on which action to perform
     * @return String the root of mount point that was ro, and mounted to rw; null otherwise
     * @throws RootNotPermittedException
     */
    private static String mountFileSystemRW(String path) throws RootNotPermittedException {
        String command = "mount";
        ArrayList<String> output = RootHelper.runShellCommand(command);
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

        if (!mountPoint.equals("") && types!=null) {

            // we have the mountpoint, check for mount options if already rw
            if (types.contains("rw")) {
                // already a rw filesystem return
                return null;
            } else if (types.contains("ro")) {
                // read-only file system, remount as rw
                String mountCommand = "mount -o rw,remount " + mountPoint;
                ArrayList<String> mountOutput = RootHelper.runShellCommand(mountCommand);

                if (mountOutput.size()!=0) {
                    // command failed, and we got a reason echo'ed
                    return null;
                } else return mountPoint;
            }
        }
        return null;
    }

    /**
     * Mount path for read-only access (ro)
     * @param path the root of device/filesystem to be mounted as ro
     * @throws RootNotPermittedException
     */
    private static void mountFileSystemRO(String path) throws RootNotPermittedException {
        String command = "umount -r \"" + path + "\"";
        RootHelper.runShellCommand(command);
    }

    /**
     * Copies file using root
     * @param source
     * @param destination
     * @throws RootNotPermittedException
     */
    public static void copy(String source, String destination) throws RootNotPermittedException {

        // remounting destination as rw
        String mountPoint = mountFileSystemRW(destination);

        RootHelper.runShellCommand("cp \"" + source + "\" \"" + destination + "\"");

        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * Creates an empty directory using root
     * @param path path to new directory
     * @param name name of directory
     * @throws RootNotPermittedException
     */
    public static void mkDir(String path, String name) throws RootNotPermittedException {

        String mountPoint = mountFileSystemRW(path);

        RootHelper.runShellCommand("mkdir \"" + path + "/" + name + "\"");
        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * Creates an empty file using root
     * @param path path to new file
     * @throws RootNotPermittedException
     */
    public static void mkFile(String path) throws RootNotPermittedException {

        String mountPoint = mountFileSystemRW(path);

        RootHelper.runShellCommand("touch \"" + path +"\"");
        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }


    /**
     * Returns file permissions in octal notation
     * Method requires busybox
     * @param path
     * @return
     */
    private static int getFilePermissions(String path) throws RootNotPermittedException {
        String line = RootHelper.runShellCommand("stat -c  %a \"" + path + "\"").get(0);

        return Integer.valueOf(line);
    }

    /**
     * Recursively removes a path with it's contents (if any)
     * @param path
     * @return boolean whether file was deleted or not
     * @throws RootNotPermittedException
     */
    public static boolean delete(String path) throws RootNotPermittedException {

        String mountPoint = mountFileSystemRW(path);

        ArrayList<String> result = RootHelper.runShellCommand("rm -rf \"" + path + "\"");

        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }

        return result.size()!=0;
    }

    /*public static boolean isBusyboxAvailable() throws RootNotPermittedException {
        ArrayList<String> output = RootHelper.runShellCommand("busybox");
        return output.size()!=0;
    }*/

    /**
     * Moves file using root
     * @param path
     * @param destination
     * @throws RootNotPermittedException
     */
    public static void move(String path, String destination)
            throws RootNotPermittedException {

        // remounting destination as rw
        String mountPoint = mountFileSystemRW(destination);

        //mountOwnerRW(mountPath);
        RootHelper.runShellCommand("mv \"" + path + "\" \"" + destination + "\"");

        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
    }

    /**
     * Renames file using root
     * @param oldPath path to file before rename
     * @param newPath path to file after rename
     * @throws RootNotPermittedException
     * @return if rename was successful or not
     */
    public static boolean rename(String oldPath, String newPath)
            throws RootNotPermittedException {

        String mountPoint = mountFileSystemRW(oldPath);

        ArrayList<String> output = RootHelper.runShellCommand("mv \"" + oldPath + "\" \"" + newPath + "\"");

        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }

        return output.size()==0;
    }

    public static void cat(String sourcePath, String destinationPath)
            throws RootNotPermittedException {

        String mountPoint = mountFileSystemRW(destinationPath);

        RootHelper.runShellCommand("cat \"" + sourcePath + "\" > \"" + destinationPath + "\"");
        if (mountPoint!=null) {
            // we mounted the filesystem as rw, let's mount it back to ro
            mountFileSystemRO(mountPoint);
        }
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

