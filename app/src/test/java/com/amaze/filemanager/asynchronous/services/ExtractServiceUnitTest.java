package com.amaze.filemanager.asynchronous.services;

import android.content.Intent;
import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.activities.MainActivity;

import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

import static android.app.Service.START_STICKY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class ExtractServiceUnitTest {

    //Protocol Buffers 3.0.0 release tarball
    private static final String TARGZ_OUTPUT = "protobuf-cpp-3.0.0.tar.gz";
    private static final String TAR_OUTPUT = "protobuf-cpp-3.0.0.tar";
    private static final String TAR_XZ_OUTPUT = "protobuf-cpp-3.0.0.tar.xz";
    private static final String TAR_LZMA_OUTPUT = "protobuf-cpp-3.0.0.tar.lzma";

    private static final File targz = new File(Environment.getExternalStorageDirectory(), TARGZ_OUTPUT);
    private static final File tar = new File(Environment.getExternalStorageDirectory(), TAR_OUTPUT);
    private static final File tarxz = new File(Environment.getExternalStorageDirectory(), TAR_XZ_OUTPUT);
    private static final File tarlzma = new File(Environment.getExternalStorageDirectory(), TAR_LZMA_OUTPUT);

    private ExtractService service;

    @Before
    public void setUp() throws Exception {
        initArchives();
        service = Robolectric.setupService(ExtractService.class);
    }

    private static final void initArchives() throws Exception {
        if(!targz.exists()) IOUtils.copy(ExtractService.class.getClassLoader().getResourceAsStream(TARGZ_OUTPUT), new FileOutputStream(targz));
        if(!tar.exists()) IOUtils.copy(new GZIPInputStream(new FileInputStream(targz)), new FileOutputStream(tar));
        if(!tarxz.exists()) IOUtils.copy(new FileInputStream(tar), new XZCompressorOutputStream(new FileOutputStream(tarxz)));
        if(!tarlzma.exists()) IOUtils.copy(new FileInputStream(tar), new LZMAOutputStream(new FileOutputStream(tarlzma), new LZMA2Options(), -1));
    }

    @Test
    public void testTarGz() {
        extractWithExtractService(targz);
    }

    @Test
    public void testTarXz() {
        extractWithExtractService(tarxz);
    }

    @Test
    public void testTarLzma() {
        extractWithExtractService(tarlzma);
    }

    private void extractWithExtractService(File file) {
        Intent intent = new Intent(RuntimeEnvironment.application, ExtractService.class);
        intent.putExtra(ExtractService.KEY_PATH_ZIP, file.getAbsolutePath());
        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, new String[0]);
        intent.putExtra(ExtractService.KEY_PATH_EXTRACT, Environment.getExternalStorageDirectory().getAbsolutePath());

        assertEquals(START_STICKY, service.onStartCommand(intent, 0, 0));
        assertNotNull(Shadows.shadowOf(RuntimeEnvironment.application).getBroadcastIntents());
        assertTrue(Shadows.shadowOf(RuntimeEnvironment.application).getBroadcastIntents().size()>0);
        assertTrue(Shadows.shadowOf(RuntimeEnvironment.application).getBroadcastIntents().get(0).hasExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE));
    }
}
