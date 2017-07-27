package com.amaze.filemanager.utils;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by arpitkh996 on 13-01-2016.
 */
public class Logger {

    public static void log(final Exception s, final String s1, Context context) {
        if (context == null) return;
        final File f = new File(context.getExternalFilesDir("internal"), "log.txt");
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileWriter output = null;
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    if (s != null)
                        s.printStackTrace(pw);
                    output = new FileWriter(f.getPath());
                    BufferedWriter writer = new BufferedWriter(output);
                    writer.write(s1 + "\n");
                    writer.write(sw.toString());
                    writer.close();
                    output.close();
                } catch (IOException e) {
                }
            }
        }).start();
    }
}
