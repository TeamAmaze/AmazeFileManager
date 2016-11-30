package com.amaze.filemanager.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.services.ProgressHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by vishal on 26/10/16.
 */

public class GenericCopyThread implements Runnable {

    private BaseFile mSourceFile;
    private HFile mTargetFile;
    private Context mContext;
    private ProgressHandler progressHandler;

    public Thread thread;

    public GenericCopyThread(Context context) {
        this.mContext = context;
    }

    @Override
    public void run() {

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            if (!mSourceFile.isSmb() && !mSourceFile.isOtgFile()) {

                if (!mTargetFile.isOtgFile()) {

                    // copying normal file, target not in OTG
                    inputStream = (FileInputStream) mSourceFile.getInputStream();
                    outputStream = (FileOutputStream) mTargetFile.getOutputStream(mContext);
                    inChannel = inputStream.getChannel();
                    outChannel = outputStream.getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                } else {
                    // target in OTG, obtain streams from DocumentFile Uri's

                    bufferedInputStream = new BufferedInputStream(mSourceFile.getInputStream(), 1024);
                    ContentResolver contentResolver = mContext.getContentResolver();
                    DocumentFile documentTargetFile = RootHelper.getDocumentFile(mTargetFile.getPath(), mContext);

                    bufferedOutputStream = new BufferedOutputStream(contentResolver.openOutputStream(documentTargetFile.getUri()), 1024);
                    copyFile(bufferedInputStream, bufferedOutputStream);
                }
            } else if (mSourceFile.isOtgFile()) {

                // copying otg file
                ContentResolver contentResolver = mContext.getContentResolver();
                DocumentFile documentSourceFile = RootHelper.getDocumentFile(mSourceFile.getPath(), mContext);
                bufferedInputStream = new BufferedInputStream(contentResolver.openInputStream(documentSourceFile.getUri()), 1024);

                if (mTargetFile.getMode() == OpenMode.OTG) {

                    // whether the target is in OTG or not
                    DocumentFile documentTargetFile = RootHelper.getDocumentFile(mTargetFile.getPath(), mContext);

                    bufferedOutputStream = new BufferedOutputStream(contentResolver.openOutputStream(documentTargetFile.getUri()), 1024);
                } else {

                    // target is not in OTG, obtain the conventional output stream
                    bufferedOutputStream = new BufferedOutputStream(mTargetFile.getOutputStream(mContext), 1024);
                }
                copyFile(bufferedInputStream, bufferedOutputStream);
            } else {

                // copying smb file
                bufferedInputStream = new BufferedInputStream(mSourceFile.getInputStream(), 1024);
                bufferedOutputStream = new BufferedOutputStream(mTargetFile.getOutputStream(mContext), 1024);

                copyFile(bufferedInputStream, bufferedOutputStream);
            }

            // writing to file
            /*progressHandler.setFileName(mSourceFile.getName());
            progressHandler.setTotalSize(mSourceFile.getSize());
            progressHandler.addReadLength(Float.valueOf(inChannel.position()).intValue());
            progressHandler.addWrittenLength(Float.valueOf(outChannel.position()).intValue(), 0);*/
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(getClass().getSimpleName(), "I/O Error!");
        } finally {

            try {
                if (inChannel!=null) inChannel.close();
                if (outChannel!=null) outChannel.close();
                if (inputStream!=null) inputStream.close();
                if (outputStream!=null) outputStream.close();
                if (bufferedInputStream!=null) bufferedInputStream.close();
                if (bufferedOutputStream!=null) bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                // failure in closing stream
            }
        }
    }

    /**
     * Start a thread encapsulating this class's runnable interface, a call to {@link #run()} is made
     * @param sourceFile the source file, which is to be copied
     * @param targetFile the target file
     * @param progressHandler handles the progress of copy
     */
    public void startThread(BaseFile sourceFile, HFile targetFile, ProgressHandler progressHandler) {

        this.mSourceFile = sourceFile;
        this.mTargetFile = targetFile;
        this.progressHandler = progressHandler;
        thread = new Thread(this);
        thread.start();
    }

    private void copyFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream) {
        int count;
        byte[] buffer = new byte[1024];
        try {
            do {
                count = bufferedInputStream.read(buffer);
                if (count!=-1) {
                    for (int i=0; i<count; i++) {
                        bufferedOutputStream.write(buffer[i]);
                    }
                    bufferedOutputStream.flush();
                }
            } while (count!=-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
