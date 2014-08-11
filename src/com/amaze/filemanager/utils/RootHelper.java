package com.amaze.filemanager.utils;

import java.io.File;
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

    public static String getCommandLineString(String input) {
        return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
    }	private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";


    public static ArrayList<String[]> getFilesList(String path,boolean root,boolean showHidden)
    {
String cpath=getCommandLineString(path);
        Futils futils=new Futils();
        ArrayList<String[]> a=new ArrayList<String[]>();
        String ls="";
        if(futils.canListFiles(new File(path))){
        ls = runAndWait("ls -l " + cpath,false);}
        else if(root){ls = runAndWait("ls -l " + cpath,true);}
        else{return new ArrayList<String[]>();}
        if (ls == null)
        {
      //      Logger.errorST("Error: Could not get list of files in directory: " + path);
            return new ArrayList<String[]>();
        }

        if (ls.equals("\n") || ls.equals(""))
        {
        //    Logger.debug("No files in directory");
            return new ArrayList<String[]>();
        }
        else
        {
            List<String> files = Arrays.asList(ls.split("\n"));
            for (String file : files)
            {String[] array=futils.parseName(file);
                array[0]=path+"/"+array[0];
                if(new File(array[0]).isHidden()){if(showHidden){a.add(array);}}
                else{
                a.add(array);}

            }
return a;

        }
    }}
