package com.amaze.filemanager.filesystem.compressed;

import android.content.Context;

import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.GzipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.GzipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.ZipDecompressor;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by JeongHyeon on 2018-04-01.
 */
public class CompressedHelperTest {

    /* Extractor check
        This program use 6 extension and  4 extractor.
        Check if each extensions matched correct extractor
     */
    @Test
    public void getExtractorInstance() throws Exception {
        Context context = null;//for extension check, don't need
        Extractor.OnUpdate listener = null;//for extension check, don't need
        File file = new File("/test/test.zip");//.zip used by ZipExtractor
        Extractor result = CompressedHelper.getExtractorInstance(context, file,"/test2",listener);
        assertEquals(result.getClass(),ZipExtractor.class);
        file = new File("/test/test.jar");//.jar used by ZipExtractor
        result = CompressedHelper.getExtractorInstance(context, file,"/test2",listener);
        assertEquals(result.getClass(),ZipExtractor.class);
        file = new File("/test/test.apk");//.apk used by ZipExtractor
        result = CompressedHelper.getExtractorInstance(context, file,"/test2",listener);
        assertEquals(result.getClass(),ZipExtractor.class);
        file = new File("/test/test.tar");//.tar used by TarExtractor
        result = CompressedHelper.getExtractorInstance(context, file,"/test2",listener);
        assertEquals(result.getClass(),TarExtractor.class);
        file = new File("/test/test.tar.gz");//.tar.gz used by GzipExtractor
        result = CompressedHelper.getExtractorInstance(context, file,"/test2",listener);
        assertEquals(result.getClass(), GzipExtractor.class);
        file = new File("/test/test.rar");//.rar used by RarExtractor
        result = CompressedHelper.getExtractorInstance(context, file,"/test2",listener);
        assertEquals(result.getClass(),RarExtractor.class);

        //null test
        file = new File("/test/test.7z");//Can't use 7zip
        result = CompressedHelper.getExtractorInstance(context, file,"/test2",listener);
        assertNull(result);
    }

    /*  Decompressor check
        This program use 6 extension and  4 decompressor.
        Check if each extensions matched correct decompressor
     */
    @Test
    public void getCompressorInstance() throws Exception {
        Context context = null;//for extension check, don't need
        File file = new File("/test/test.zip");//.zip used by ZipDecompressor
        Decompressor result = CompressedHelper.getCompressorInstance(context,file);
        assertEquals(result.getClass(),ZipDecompressor.class);
        file = new File("/test/test.jar");//.jar used by ZipDecompressor
        result = CompressedHelper.getCompressorInstance(context,file);
        assertEquals(result.getClass(),ZipDecompressor.class);
        file = new File("/test/test.apk");//.apk used by ZipDecompressor
        result = CompressedHelper.getCompressorInstance(context,file);
        assertEquals(result.getClass(),ZipDecompressor.class);
        file = new File("/test/test.tar");//.tar used by TarDecompressor
        result = CompressedHelper.getCompressorInstance(context,file);
        assertEquals(result.getClass(),TarDecompressor.class);
        file = new File("/test/test.tar.gz");//.tar.gz used by GzipDecompressor
        result = CompressedHelper.getCompressorInstance(context,file);
        assertEquals(result.getClass(),GzipDecompressor.class);
        file = new File("/test/test.rar");//.rar used by RarDecompressor
        result = CompressedHelper.getCompressorInstance(context,file);
        assertEquals(result.getClass(),RarDecompressor.class);

        //null test
        file = new File("/test/test.7z");//Can't use 7zip
        result = CompressedHelper.getCompressorInstance(context,file);
        assertNull(result);
    }

    /* isFileExtractable() fuction test
        extension check
     */
    @Test
    public void isFileExtractableTest() throws Exception {
        //extension in code. So, it return true
        assertTrue(CompressedHelper.isFileExtractable("/test/test.zip"));
        assertTrue(CompressedHelper.isFileExtractable("/test/test.rar"));
        assertTrue(CompressedHelper.isFileExtractable("/test/test.tar"));
        assertTrue(CompressedHelper.isFileExtractable("/test/test.tar.gz"));
        assertTrue(CompressedHelper.isFileExtractable("/test/test.jar"));
        assertTrue(CompressedHelper.isFileExtractable("/test/test.apk"));

        //extension not in code. So, it return false
        assertFalse(CompressedHelper.isFileExtractable("/test/test.7z"));
        assertFalse(CompressedHelper.isFileExtractable("/test/test.z"));

    }

    /* I think this make fault.
        GzipDecompressor exists so .gz file should be valid.
        But .gz makes fault.
     */
    @Test
    public void isFileExtractableFaultTest1() throws Exception{
        assertTrue(CompressedHelper.isFileExtractable("/test/test.gz"));
    }

    /* I think this make fault.
        If some user or programmer change file file extension and user downloads that file,
        or some programmer make some unpopular extension such as .~~zip, .~~rar
        it will be openable invalid file.
     */
    @Test
    public void isFileExtractableFaultTest2() throws Exception{
        assertFalse(CompressedHelper.isFileExtractable("/test/test.7zip"));
    }

    /* getFileName() function test
        it return file name. But, if it is invalid compressed file, return file name with extension
     */
    @Test
    public void getFileNameTest() throws Exception {
        assertEquals("test",CompressedHelper.getFileName("test.zip"));
        assertEquals("test",CompressedHelper.getFileName("test.rar"));
        assertEquals("test",CompressedHelper.getFileName("test.tar"));
        assertEquals("test",CompressedHelper.getFileName("test.tar.gz"));
        assertEquals("test",CompressedHelper.getFileName("test.jar"));
        assertEquals("test",CompressedHelper.getFileName("test.apk"));

        //no extension(directory)
        assertEquals("test",CompressedHelper.getFileName("test"));

        //invalid extension
        assertEquals("test.7z",CompressedHelper.getFileName("test.7z"));
        assertEquals("test.z",CompressedHelper.getFileName("test.z"));

        //no path
        assertEquals("",CompressedHelper.getFileName(""));
    }


}