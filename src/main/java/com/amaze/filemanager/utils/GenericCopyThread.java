package com.amaze.filemanager.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by vishal on 26/10/16.
 *
 * Base class to handle file copy.
 */

public class GenericCopyThread implements Runnable {

    private BaseFile mSourceFile;
    private HFile mTargetFile;
    private Context mContext;

    public Thread thread;

    public static final int DEFAULT_BUFFER_SIZE =  8192;

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

            // initializing the input channels based on file types
            if (mSourceFile.isOtgFile()) {
                // source is in otg
                ContentResolver contentResolver = mContext.getContentResolver();
                DocumentFile documentSourceFile = RootHelper.getDocumentFile(mSourceFile.getPath(), mContext);

                inChannel = ((FileInputStream) contentResolver.openInputStream(documentSourceFile.getUri())).getChannel();
            } else if (mSourceFile.isSmb()) {

                // source is in smb
                bufferedInputStream = new BufferedInputStream(mSourceFile.getInputStream(), DEFAULT_BUFFER_SIZE);
            } else {

                // source file is neither smb nor otg; getting a channel from direct file instead of stream
                inChannel = new RandomAccessFile(new File(mSourceFile.getPath()), "r").getChannel();
            }

            // initializing the output channels based on file types
            if (mTargetFile.isOtgFile()) {

                // target in OTG, obtain streams from DocumentFile Uri's

                ContentResolver contentResolver = mContext.getContentResolver();
                DocumentFile documentTargetFile = RootHelper.getDocumentFile(mTargetFile.getPath(), mContext);
                outChannel = ((FileOutputStream) contentResolver.openOutputStream(documentTargetFile.getUri())).getChannel();
            } else if (mTargetFile.isSmb()) {

                bufferedOutputStream = new BufferedOutputStream(mTargetFile.getOutputStream(mContext), DEFAULT_BUFFER_SIZE);
            } else {
                // copying normal file, target not in OTG
                outChannel = new RandomAccessFile(new File(mTargetFile.getPath()), "rw").getChannel();
            }

            if (bufferedInputStream!=null) {
                if (bufferedOutputStream!=null) copyFile(bufferedInputStream, bufferedOutputStream);
                else if (outChannel!=null) {
                    copyFile(bufferedInputStream, outChannel);
                }
            } else if (inChannel!=null) {
                if (bufferedOutputStream!=null) copyFile(inChannel, bufferedOutputStream);
                else if (outChannel!=null)  copyFile(inChannel, outChannel);
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
     */
    public void startThread(BaseFile sourceFile, HFile targetFile) {

        this.mSourceFile = sourceFile;
        this.mTargetFile = targetFile;
        thread = new Thread(this);
        thread.start();
    }

    private void copyFile(BufferedInputStream bufferedInputStream, FileChannel outChannel)
            throws IOException {

        MappedByteBuffer byteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0,
                mSourceFile.getSize());
        int count;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        do {
            count = bufferedInputStream.read(buffer);
            if (count!=-1) {

                byteBuffer.put(buffer, 0, count);
                ServiceWatcherUtil.POSITION+=count;
            }
        } while (count!=-1);
    }

    private void copyFile(FileChannel inChannel, FileChannel outChannel) throws IOException {

        MappedByteBuffer inByteBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
        MappedByteBuffer outByteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, inChannel.size());

        while (inByteBuffer.hasRemaining()) {

            outByteBuffer.put(inByteBuffer.get());
            ServiceWatcherUtil.POSITION++;
        }
        int count;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (inByteBuffer.hasRemaining()) {
            ByteBuffer byteBuffer = inByteBuffer.get(buffer);
            count = byteBuffer.capacity();
            outByteBuffer.put(byteBuffer.array(), 0, count);
        }
    }

    private void copyFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream)
            throws IOException{
        int count;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        do {
            count = bufferedInputStream.read(buffer);
            if (count!=-1) {

                bufferedOutputStream.write(buffer, 0 , count);
                ServiceWatcherUtil.POSITION+=count;
            }
        } while (count!=-1);
        bufferedOutputStream.flush();
    }

    private void copyFile(FileChannel inChannel, BufferedOutputStream bufferedOutputStream)
            throws IOException {
        MappedByteBuffer inBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, mSourceFile.getSize());
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int length, oldPosition, newPosition;
        do {

            oldPosition = inBuffer.position();
            inBuffer.get(buffer);
            newPosition = inBuffer.position();
            length = newPosition-oldPosition;
            if (length!=0) {

                bufferedOutputStream.write(buffer, 0, length);
                ServiceWatcherUtil.POSITION+=length;
            }
        } while (length==0);
        bufferedOutputStream.flush();
    }
}
