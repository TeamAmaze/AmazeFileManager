package com.amaze.filemanager.utils.cloud;

import com.amaze.filemanager.utils.SmbStreamer.StreamSource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Vishal on 30-04-2017.
 */

public class CloudStreamSource extends StreamSource {
    protected String mime;
    protected long fp;
    protected long len;
    protected String name;
    protected int bufferSize;
    private InputStream inputStream;

    public CloudStreamSource(String fileName, long length, InputStream inputStream) {

        fp = 0;
        len = length;
        this.name = fileName;
        this.inputStream = inputStream;
        bufferSize = 1024*60;
    }

    /**
     * You may notice a strange name for the smb input stream.
     * I made some modifications to the original one in the jcifs library for my needs,
     * but streaming required returning to the original one so I renamed it to "old".
     * However, I needed to specify a buffer size in the constructor. It looks now like this:
     *
     *
     public SmbFileInputStreamOld( SmbFile file, int readBuffer, int openFlags) throws SmbException, MalformedURLException, UnknownHostException {
     this.file = file;
     this.openFlags = SmbFile.O_RDONLY & 0xFFFF;
     this.access = (openFlags >>> 16) & 0xFFFF;
     if (file.type != SmbFile.TYPE_NAMED_PIPE) {
     file.open( openFlags, access, SmbFile.ATTR_NORMAL, 0 );
     this.openFlags &= ~(SmbFile.O_CREAT | SmbFile.O_TRUNC);
     } else {
     file.connect0();
     }
     readSize = readBuffer;
     fs = file.length();
     }
     *
     * Setting buffer size by properties didn't work for me so I created this constructor.
     * In the libs folder there is a library modified by me. If you want to use a stock one, you
     * have to set somehow the buffer size to be equal with http server's buffer size which is 8192.
     *
     * @throws IOException
     */
    public void open() throws IOException {
        try {
            if(fp>0)
                inputStream.skip(fp);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    public int read(byte[] buff) throws IOException{
        return read(buff, 0, buff.length);
    }
    public int read(byte[] bytes, int start, int offs) throws IOException {
        int read =  inputStream.read(bytes, start, offs);
        fp += read;
        return read;
    }
    public long moveTo(long position) throws IOException {
        fp = position;
        return fp;
    }

    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String getMimeType(){
        return mime;
    }
    public long length(){
        return len;
    }
    public String getName(){
        return name;
    }
    public long available(){
        return len - fp;
    }

    public void reset(){
        fp = 0;
    }

    public int getBufferSize(){
        return bufferSize;
    }
}
