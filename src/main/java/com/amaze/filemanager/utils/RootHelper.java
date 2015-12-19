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

package com.amaze.filemanager.utils;

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

    public static ArrayList<String[]> getFilesList(boolean showSize, String path, boolean showHidden) {
        File f = new File(path);
        ArrayList<String[]> files = new ArrayList<String[]>();
        try {
            if (f.exists() && f.isDirectory()) {
                for (File x : f.listFiles()) {
                    String k = "", size = "";
                    if (showSize && !x.isDirectory()) size = "" + x.length();
                    if (showHidden) {
                        files.add(new String[]{x.getPath(), "", parseFilePermission(x), k, x.lastModified() + "", size, x.isDirectory() + ""});
                    } else {
                        if (!x.isHidden()) {
                            files.add(new String[]{x.getPath(), "", parseFilePermission(x), k, x.lastModified() + "", size, x.isDirectory() + ""});
                        }
                    }
                }
            }
        } catch (Exception e) {
        }


        return files;
    }

    public static String[] addFile(File x, boolean showSize, boolean showHidden) {
        String k = "", size = "";
        if (showSize && !x.isDirectory())
            size = "" + x.length();
        if (showHidden) {
            return (new String[]{x.getPath(), "", parseFilePermission(x), k, x.lastModified() + "",
                    size, x.isDirectory() + ""});
        } else if (!x.isHidden()) {
            return (new String[]{x.getPath(), "", parseFilePermission(x), k, x.lastModified() + "", size, x.isDirectory() + ""});
        }
        return null;
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

    static boolean isDirectory(String a, boolean root,int count) {
        File f = new File(a);
        String name = f.getName();
        String p = f.getParent();
        if (p != null && p.length() > 1) {
            ArrayList<String> ls = runAndWait1("ls -la " + p, root, 2000);
            for (String s : ls) {
                if (s.contains(name)) {
                    try {
                        String[] path = new Futils().parseName(s);
                        if (path[2].trim().startsWith("d")) return true;
                        else if (path[2].trim().startsWith("l")) {
                            if(count>5)
                                return f.isDirectory();
                            else
                            return isDirectory(path[1].trim(), root, ++count);
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

    static boolean isDirectory(String[] path) {
        if (path[2].startsWith("d")) return true;
        else if (path[1].startsWith("l")) return new File(path[1]).isDirectory();
        else return new File(path[0]).isDirectory();
    }

    public static ArrayList<String[]> getFilesList(String path, boolean root, boolean showHidden, boolean showSize) {
        String p = " ";
        if (showHidden) p = "a ";
        Futils futils = new Futils();
        ArrayList<String[]> a = new ArrayList<>();
        ArrayList<String> ls = new ArrayList<>();
        if (root) {
            if (!path.startsWith("/storage") && !path.startsWith("/sdcard")) {
                String cpath = getCommandLineString(path);
                ls = runAndWait1("ls -l" + p + cpath, root);
                if (ls != null) {
                    for (String file : ls) {
                        if (!file.contains("Permission denied"))
                            try {
                                String[] array = futils.parseName(file);
                                if (array != null) {
                                    array[0] = path + "/" + array[0];
                                    if (array[1].trim().length() > 0) {
                                        boolean isdirectory = isDirectory(array[1], root,0);
                                        array[6] = isdirectory + "";

                                    } else array[6] = "" + isDirectory(array);
                                    a.add(array);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                    }
                }
            } else if (futils.canListFiles(new File(path))) {
                a = getFilesList(showSize, path, showHidden);
            } else {
                a = new ArrayList<String[]>();
            }
        } else if (futils.canListFiles(new File(path))) {
            a = getFilesList(showSize, path, showHidden);
        } else {
            a = new ArrayList<String[]>();
        }
        if (a.size() == 0 && futils.canListFiles(new File(path))) {
            a = getFilesList(showSize, path, showHidden);
        }
        return a;

    }

}