package com.amaze.filemanager.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
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
import java.nio.channels.ReadableByteChannel;

/**
 * Created by vishal on 26/10/16.
 *
 * Base class to handle file copy.
 */

public class GenericCopyUtil {

    private BaseFile mSourceFile;
    private HFile mTargetFile;
    private Context mContext;   // context needed to find the DocumentFile in otg/sd card
    public static final String PATH_FILE_DESCRIPTOR = "/proc/self/fd/";

    public static final int DEFAULT_BUFFER_SIZE =  8192;

    public GenericCopyUtil(Context context) {
        this.mContext = context;
    }

    /**
     * Starts copy of file
     * Supports : {@link File}, {@link jcifs.smb.SmbFile}, {@link DocumentFile}
     * @throws IOException
     */
    private void startCopy() throws IOException {

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
                DocumentFile documentSourceFile = RootHelper.getDocumentFile(mSourceFile.getPath(),
                        mContext, false);

                bufferedInputStream = new BufferedInputStream(contentResolver
                        .openInputStream(documentSourceFile.getUri()), DEFAULT_BUFFER_SIZE);
            } else if (mSourceFile.isSmb()) {

                // source is in smb
                bufferedInputStream = new BufferedInputStream(mSourceFile.getInputStream(), DEFAULT_BUFFER_SIZE);
            } else {

                // source file is neither smb nor otg; getting a channel from direct file instead of stream
                File file = new File(mSourceFile.getPath());
                if (FileUtil.isReadable(file)) {
                    inChannel = new RandomAccessFile(file, "r").getChannel();
                } else {
                    ContentResolver contentResolver = mContext.getContentResolver();
                    DocumentFile documentSourceFile = FileUtil.getDocumentFile(file,
                            mSourceFile.isDirectory(), mContext);

                    bufferedInputStream = new BufferedInputStream(contentResolver
                            .openInputStream(documentSourceFile.getUri()), DEFAULT_BUFFER_SIZE);
                }
            }

            // initializing the output channels based on file types
            if (mTargetFile.isOtgFile()) {

                // target in OTG, obtain streams from DocumentFile Uri's

                ContentResolver contentResolver = mContext.getContentResolver();
                DocumentFile documentTargetFile = RootHelper.getDocumentFile(mTargetFile.getPath(),
                        mContext, true);

                bufferedOutputStream = new BufferedOutputStream(contentResolver
                        .openOutputStream(documentTargetFile.getUri()), DEFAULT_BUFFER_SIZE);
            } else if (mTargetFile.isSmb()) {

                bufferedOutputStream = new BufferedOutputStream(mTargetFile.getOutputStream(mContext), DEFAULT_BUFFER_SIZE);
            } else {
                // copying normal file, target not in OTG
                File file = new File(mTargetFile.getPath());
                if (FileUtil.isWritable(file)) {
                    outChannel = new RandomAccessFile(file, "rw").getChannel();
                } else {
                    ContentResolver contentResolver = mContext.getContentResolver();
                    DocumentFile documentTargetFile = FileUtil.getDocumentFile(file,
                            mTargetFile.isDirectory(), mContext);

                    bufferedOutputStream = new BufferedOutputStream(contentResolver
                            .openOutputStream(documentTargetFile.getUri()), DEFAULT_BUFFER_SIZE);
                }
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

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(getClass().getSimpleName(), "I/O Error!");
            throw new IOException();
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
     * Method exposes this class to initiate copy
     * @param sourceFile the source file, which is to be copied
     * @param targetFile the target file
     */
    public void copy(BaseFile sourceFile, HFile targetFile) throws IOException {

        this.mSourceFile = sourceFile;
        this.mTargetFile = targetFile;

        startCopy();
    }

    private void copyFile(BufferedInputStream bufferedInputStream, FileChannel outChannel)
            throws IOException {

        MappedByteBuffer byteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0,
                mSourceFile.getSize());
        int count = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (count != -1) {

            count = bufferedInputStream.read(buffer);
            if (count!=-1) {

                byteBuffer.put(buffer, 0, count);
                ServiceWatcherUtil.POSITION+=count;
            }
        }
    }

    private void copyFile(FileChannel inChannel, FileChannel outChannel) throws IOException {

        //MappedByteBuffer inByteBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
        //MappedByteBuffer outByteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, inChannel.size());

        ReadableByteChannel inByteChannel = new CustomReadableByteChannel(inChannel);
        outChannel.transferFrom(inByteChannel, 0, mSourceFile.getSize());
    }

    private void copyFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream)
            throws IOException {
        int count = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        while (count != -1) {

            count = bufferedInputStream.read(buffer);
            if (count!=-1) {

                bufferedOutputStream.write(buffer, 0 , count);
                ServiceWatcherUtil.POSITION+=count;
            }
        }
        bufferedOutputStream.flush();
    }

    private void copyFile(FileChannel inChannel, BufferedOutputStream bufferedOutputStream)
            throws IOException {
        MappedByteBuffer inBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, mSourceFile.getSize());

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (inBuffer.hasRemaining()) {

            int count = 0;
            for (int i=0; i<buffer.length && inBuffer.hasRemaining(); i++) {
                buffer[i] = inBuffer.get();
                count++;
            }
            bufferedOutputStream.write(buffer, 0, count);
            ServiceWatcherUtil.POSITION = inBuffer.position();
        }
        bufferedOutputStream.flush();
    }

    /**
     * Inner class responsible for getting a {@link ReadableByteChannel} from the input channel
     * and to watch over the read progress
     */
    private class CustomReadableByteChannel implements ReadableByteChannel {

        ReadableByteChannel byteChannel;

        CustomReadableByteChannel(ReadableByteChannel byteChannel) {
            this.byteChannel = byteChannel;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int bytes;
            if (((bytes = byteChannel.read(dst))>0)) {

                ServiceWatcherUtil.POSITION += bytes;
                return bytes;

            }
            return 0;
        }

        @Override
        public boolean isOpen() {
            return byteChannel.isOpen();
        }

        @Override
        public void close() throws IOException {

            byteChannel.close();
        }
    }
}
