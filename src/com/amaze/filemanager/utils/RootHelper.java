package com.amaze.filemanager.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class RootHelper
{
    public static String runAndWait(String cmd,boolean root)
    {

        CommandCapture cc = new CommandCapture(0, false, cmd);

        try
        {if(root)
            Shell.runRootCommand(cc);
            else
            Shell.runCommand(cc);
        }
        catch (Exception e)
        {
     //       Logger.errorST("Exception when trying to run shell command", e);

            return null;
        }

        if (!waitForCommand(cc))
        {
            return null;
        }

        return cc.toString();
    }

    private static boolean waitForCommand(Command cmd)
    {
        while (!cmd.isFinished())
        {
            synchronized (cmd)
            {
                try
                {
                    if (!cmd.isFinished())
                    {
                        cmd.wait(2000);
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished())
            {
       //         Logger.errorST("Error: Command is not executing and is not finished!");
                return false;
            }
        }

        //Logger.debug("Command Finished!");
        return true;
    }

    public static boolean fileOrFolderExists(String path)
    {
        //Logger.debug("File or folder exists: " + path);

        // Remove trailing slash if it exists (for directories)
        if (path.charAt(path.length() - 1) == '/')
        {
            path = path.substring(0, path.length() - 1);
        }

        int i = path.lastIndexOf('/');
        if (i == -1)
        {
//            Logger.debug("Could not find path folder (invalid filename?)");
            return false;
        }

        String parentDir = path.substring(0, i);

        List<String> fileList = getFilesList(parentDir,false);

        boolean exists = fileList.contains(path.substring(i + 1));
  //      Logger.debug("Exists: " + (exists ? "true" : "false"));

        return exists;
    }

    public static List<String> getFilesList(String path,boolean root)
    {
    //    Logger.debug("Getting file list: " + path);

        String ls = runAndWait("ls " + path,root);
        if (ls == null)
        {
      //      Logger.errorST("Error: Could not get list of files in directory: " + path);
            return new ArrayList<String>();
        }

        if (ls.equals("\n") || ls.equals(""))
        {
        //    Logger.debug("No files in directory");
            return new ArrayList<String>();
        }
        else
        {
            List<String> files = Arrays.asList(ls.split("\n"));
            for (String file : files)
            {
          //      Logger.debug("Directory List: " + file);
            }

            return files;
        }
    }}
