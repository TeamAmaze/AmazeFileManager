package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.content.Context;
import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.compressed.TestArchives;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public abstract class AbstractExtractorTest {

    protected abstract Class<? extends Extractor> extractorClass();

    protected abstract String getArchiveType();

    @Before
    public void setUp() throws Exception {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        TestArchives.init(RuntimeEnvironment.application);
        copyArchiveToStorage();
    }

    @After
    public void tearDown() throws Exception {
        File extractedArchiveRoot = new File(Environment.getExternalStorageDirectory(), "test-archive");
        Files.walk(Paths.get(extractedArchiveRoot.getAbsolutePath()))
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void testExtractFiles() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Extractor extractor = extractorClass().getConstructor(Context.class, String.class, String.class, Extractor.OnUpdate.class)
                .newInstance(RuntimeEnvironment.application,
                        getArchiveFile().getAbsolutePath(),
                        Environment.getExternalStorageDirectory().getAbsolutePath(), new Extractor.OnUpdate() {

                            @Override
                            public void onStart(long totalBytes, String firstEntryName) {

                            }

                            @Override
                            public void onUpdate(String entryPath) {

                            }

                            @Override
                            public void onFinish() {
                                latch.countDown();
                                try {
                                    verifyExtractedArchiveContents();
                                } catch(IOException e) {
                                    e.printStackTrace();
                                    fail("Error verifying extracted archive contents");
                                }
                            }

                            @Override
                            public boolean isCancelled() {
                                return false;
                            }
                        });
        extractor.extractEverything();
        latch.await();
    }

    private void verifyExtractedArchiveContents() throws IOException {
        File extractedArchiveRoot = new File(Environment.getExternalStorageDirectory(), "test-archive");
        assertTrue(extractedArchiveRoot.exists());
        assertTrue(new File(extractedArchiveRoot, "1").exists());
        assertTrue(new File(extractedArchiveRoot, "2").exists());
        assertTrue(new File(extractedArchiveRoot, "3").exists());
        assertTrue(new File(extractedArchiveRoot, "4").exists());
        assertTrue(new File(extractedArchiveRoot, "a").exists());

        assertTrue(new File(new File(extractedArchiveRoot, "1"), "8").exists());
        assertTrue(new File(new File(extractedArchiveRoot, "2"), "7").exists());
        assertTrue(new File(new File(extractedArchiveRoot, "3"), "6").exists());
        assertTrue(new File(new File(extractedArchiveRoot, "4"), "5").exists());
        assertTrue(new File(new File(extractedArchiveRoot, "a/b/c/d"), "lipsum.bin").exists());

        assertTrue(IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "1"), "8"))).length == 2);
        assertTrue(IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "2"), "7"))).length == 3);
        assertTrue(IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "3"), "6"))).length == 4);
        assertTrue(IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "4"), "5"))).length == 5);
        assertTrue(IOUtils.toByteArray(new FileInputStream(new File(new File(extractedArchiveRoot, "a/b/c/d"), "lipsum.bin"))).length == 512);
    }

    private void copyArchiveToStorage() throws IOException{
        IOUtils.copy(new ByteArrayInputStream(TestArchives.readArchive(getArchiveType())), new FileOutputStream(getArchiveFile()));
    }

    private File getArchiveFile() {
        return new File(Environment.getExternalStorageDirectory(), "test-archive." + getArchiveType());
    }
}
