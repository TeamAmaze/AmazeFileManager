package com.amaze.filemanager.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class RootCommands {


    public RootCommands(Context c){
        if(RootTools.isAccessGiven()){
        }
    }
	
	public  ArrayList<String> listFiles(Context c,final String path) {
        final ArrayList<String> mDirContent = new ArrayList<String>();
        Command command = new Command(0, "ls "+path)
        {

            @Override
            public void commandOutput(int i, String s) {
               System.out.println(s);
               mDirContent.add(path+"/"+s);
            }

            @Override
            public void commandTerminated(int i, String s) {

            }

            @Override
            public void commandCompleted(int i, int i2) {

            }
        };
        try {
            RootTools.getShell(true).add(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(c,mDirContent.size()+"",Toast.LENGTH_LONG).show();
        return mDirContent;
	}
}
