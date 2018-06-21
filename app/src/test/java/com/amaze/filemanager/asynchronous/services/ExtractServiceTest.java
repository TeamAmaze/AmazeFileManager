package com.amaze.filemanager.asynchronous.services;

import android.content.Intent;
import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
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

    private void performTest(File archiveFile){
        Intent intent = new Intent(RuntimeEnvironment.application, ExtractService.class)
                .putExtra(ExtractService.KEY_PATH_ZIP, archiveFile.getAbsolutePath())
                .putExtra(ExtractService.KEY_ENTRIES_ZIP, new String[0])
                .putExtra(ExtractService.KEY_PATH_EXTRACT, new File(Environment.getExternalStorageDirectory(), "test-archive").getAbsolutePath());
        service.onStartCommand(intent,0, 0);
    }
}
