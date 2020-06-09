package com.amaze.filemanager.test;

import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.OnFileFound;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Shadow of {@link Shell.Interactive}, for {@link com.amaze.filemanager.filesystem.RootHelperTest}.
 *
 * Only tested for {@link com.amaze.filemanager.filesystem.RootHelper#getFiles(String, boolean, boolean, RootHelper.GetModeCallBack, OnFileFound)},
 * so only guarantees work for that.
 *
 * <strong>DO NOT RUN THIS ON NON-UNIX OS. YOU SHOULD KNOW THIS ALREADY.</strong>
 */
@Implements(Shell.Interactive.class)
public class ShadowShellInteractive {

    @Implementation
    public boolean isRunning(){
        return true;
    }

    @Implementation
    public boolean waitForIdle(){
        return true;
    }

    @Implementation
    public void addCommand(String command, int code, Shell.OnCommandResultListener onCommandResultListener) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        pb.environment().put("LC_ALL", "en_US.utf8");
        pb.environment().put("LANG", "en_US.utf8");
        pb.environment().put("TIME_STYLE", "long-iso");
        Process process = pb.start();
        int exitValue = process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
        List<String> result = new ArrayList<>();
        String line;
        while((line = reader.readLine()) != null){
            result.add(line);
        }
        onCommandResultListener.onCommandResult(exitValue, code, result);
    }
}
