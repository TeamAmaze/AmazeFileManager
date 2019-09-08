package com.amaze.filemanager.asynchronous.services;

import android.content.Intent;
import android.os.Environment;
import androidx.annotation.NonNull;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class ExtractServiceTest {

    private File zipfile1 = new File(Environment.getExternalStorageDirectory(), "zip-slip.zip");
    private File zipfile2 = new File(Environment.getExternalStorageDirectory(), "zip-slip-win.zip");
    private File zipfile3 = new File(Environment.getExternalStorageDirectory(), "test-archive.zip");
    private File rarfile = new File(Environment.getExternalStorageDirectory(), "test-archive.rar");
    private File tarfile = new File(Environment.getExternalStorageDirectory(), "test-archive.tar");
    private File tarballfile = new File(Environment.getExternalStorageDirectory(), "test-archive.tar.gz");
    private File tarLzmafile = new File(Environment.getExternalStorageDirectory(), "test-archive.tar.lzma");
    private File tarXzfile = new File(Environment.getExternalStorageDirectory(), "test-archive.tar.xz");
    private File tarBz2file = new File(Environment.getExternalStorageDirectory(), "test-archive.tar.bz2");
    private File sevenZipfile = new File(Environment.getExternalStorageDirectory(), "test-archive.7z");
    private File passwordProtectedZipfile = new File(Environment.getExternalStorageDirectory(), "test-archive-encrypted.zip");
    private File passwordProtected7Zipfile = new File(Environment.getExternalStorageDirectory(), "test-archive-encrypted.7z");
    private File listPasswordProtected7Zipfile = new File(Environment.getExternalStorageDirectory(), "test-archive-encrypted-list.7z");

    private ExtractService service;

    @Before
    public void setUp() throws Exception {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("zip-slip.zip"), new FileOutputStream(zipfile1));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("zip-slip-win.zip"), new FileOutputStream(zipfile2));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.zip"), new FileOutputStream(zipfile3));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.rar"), new FileOutputStream(rarfile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.tar"), new FileOutputStream(tarfile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.tar.gz"), new FileOutputStream(tarballfile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.tar.lzma"), new FileOutputStream(tarLzmafile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.tar.xz"), new FileOutputStream(tarXzfile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.tar.bz2"), new FileOutputStream(tarBz2file));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive.7z"), new FileOutputStream(sevenZipfile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive-encrypted.zip"), new FileOutputStream(passwordProtectedZipfile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive-encrypted.7z"), new FileOutputStream(passwordProtected7Zipfile));
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test-archive-encrypted-list.7z"), new FileOutputStream(listPasswordProtected7Zipfile));
        service = Robolectric.setupService(ExtractService.class);
    }

    @After
    public void tearDown() throws Exception {
        File extractedArchiveRoot = new File(Environment.getExternalStorageDirectory(), "test-archive");
        Files.walk(Paths.get(extractedArchiveRoot.getAbsolutePath()))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testExtractZipSlip(){
        performTest(zipfile1);
        assertEquals(RuntimeEnvironment.application.getString(R.string.multiple_invalid_archive_entries), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractZipSlipWin(){
        performTest(zipfile2);
        assertEquals(RuntimeEnvironment.application.getString(R.string.multiple_invalid_archive_entries), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractZipNormal(){
        performTest(zipfile3);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractRar(){
        performTest(rarfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractTar(){
        performTest(tarfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractTarGz(){
        performTest(tarballfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractTarLzma(){
        performTest(tarLzmafile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractTarXz(){
        performTest(tarXzfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtractTarBz2(){
        performTest(tarBz2file);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testExtract7z(){
        performTest(sevenZipfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test @Ignore("Work isn't finished yet, skipping test")
    public void testExtractPasswordProtectedZip(){
        performTest(passwordProtectedZipfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test @Ignore("Work isn't finished yet, skipping test")
    public void testExtractPasswordProtected7Zip(){
        performTest(passwordProtected7Zipfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    @Test @Ignore("Work isn't finished yet, skipping test")
    public void testExtractListPasswordProtected7Zip(){
        performTest(listPasswordProtected7Zipfile);
        assertNull(ShadowToast.getLatestToast());
        assertNull(ShadowToast.getTextOfLatestToast());
    }

    private void performTest(@NonNull File archiveFile){
        Intent intent = new Intent(RuntimeEnvironment.application, ExtractService.class)
                .putExtra(ExtractService.KEY_PATH_ZIP, archiveFile.getAbsolutePath())
                .putExtra(ExtractService.KEY_ENTRIES_ZIP, new String[0])
                .putExtra(ExtractService.KEY_PATH_EXTRACT, new File(Environment.getExternalStorageDirectory(), "test-archive").getAbsolutePath());
        service.onStartCommand(intent,0, 0);
    }
}
