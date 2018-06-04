package com.amaze.filemanager.filesystem.compressed;

import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.Bzip2Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.GzipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.LzmaExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.SevenZipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.XzExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.Bzip2Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.GzipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.LzmaDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.SevenZipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.XzDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.ZipDecompressor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.File;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class CompressedHelperTest {

    private static final Extractor.OnUpdate doNothingOnUpdateListener = new Extractor.OnUpdate() {
        @Override public void onStart(long totalBytes, String firstEntryName) {}
        @Override public void onUpdate(String entryPath) {}
        @Override public void onFinish() {}
        @Override public boolean isCancelled() {return false;}
    };

    @Test
    public void testGetExtractorInstance(){
        performTestGetExtractorInstance(ZipExtractor.class, "zip");
        performTestGetExtractorInstance(RarExtractor.class, "rar");
        performTestGetExtractorInstance(TarExtractor.class, "tar");
        performTestGetExtractorInstance(GzipExtractor.class, "tar.gz");
        performTestGetExtractorInstance(LzmaExtractor.class, "tar.lzma");
        performTestGetExtractorInstance(XzExtractor.class, "tar.xz");
        performTestGetExtractorInstance(Bzip2Extractor.class, "tar.bz2");
        performTestGetExtractorInstance(SevenZipExtractor.class, "7z");
        performTestGetExtractorInstance(SevenZipExtractor.class, "tar.7z");
        performTestGetExtractorInstance(null, "cpio");
    }

    @Test
    public void testGetDecompressorInstance(){
        performTestGetDecompressorInstance(ZipDecompressor.class, "zip");
        performTestGetDecompressorInstance(RarDecompressor.class, "rar");
        performTestGetDecompressorInstance(TarDecompressor.class, "tar");
        performTestGetDecompressorInstance(GzipDecompressor.class, "tar.gz");
        performTestGetDecompressorInstance(LzmaDecompressor.class, "tar.lzma");
        performTestGetDecompressorInstance(XzDecompressor.class, "tar.xz");
        performTestGetDecompressorInstance(Bzip2Decompressor.class, "tar.bz2");
        performTestGetDecompressorInstance(SevenZipDecompressor.class, "7z");
        performTestGetDecompressorInstance(SevenZipDecompressor.class, "tar.7z");
        performTestGetDecompressorInstance(null, "cpio");
    }

    @Test
    public void testIsFileExtractable(){
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("zip").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("rar").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("tar").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("tar.gz").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("tar.bz2").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("tar.xz").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("tar.lzma").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("7z").getAbsolutePath()));
        assertTrue(CompressedHelper.isFileExtractable(createFileObj("tar.7z").getAbsolutePath()));
        assertFalse(CompressedHelper.isFileExtractable(createFileObj("cpio").getAbsolutePath()));
    }

    @Test
    public void testGetFileName(){
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("zip").getName()));
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("rar").getName()));
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("tar").getName()));
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("tar.gz").getName()));
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("tar.bz2").getName()));
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("tar.lzma").getName()));
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("tar.xz").getName()));
        assertEquals("test-archive", CompressedHelper.getFileName(createFileObj("7z").getName()));
        assertEquals("test-archive.tar", CompressedHelper.getFileName(createFileObj("tar.7z").getName()));
    }

    private void performTestGetExtractorInstance(Class<? extends Extractor> assertedExtractorClass, String type) {
        File archive = createFileObj(type);
        Extractor result = CompressedHelper.getExtractorInstance(RuntimeEnvironment.application, archive,
                Environment.getExternalStorageDirectory().getAbsolutePath(), doNothingOnUpdateListener);

        if(assertedExtractorClass != null)
            assertEquals(assertedExtractorClass, result.getClass());
        else
            assertNull(result);
    }

    private void performTestGetDecompressorInstance(Class<? extends Decompressor> assertedDecompressorClass, String type){
        File archive = createFileObj(type);
        Decompressor result = CompressedHelper.getCompressorInstance(RuntimeEnvironment.application, archive);

        if(assertedDecompressorClass != null)
            assertEquals(assertedDecompressorClass, result.getClass());
        else
            assertNull(result);
    }

    private File createFileObj(String type){
        return new File(Environment.getExternalStorageDirectory(), "test-archive."+type);
    }
}
