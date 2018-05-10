package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.content.Context;
import android.os.Environment;

import com.amaze.filemanager.filesystem.compressed.AbstractArchiveTestSupport;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractExtractorTest extends AbstractArchiveTestSupport {

    protected abstract Class<? extends Extractor> extractorClass();

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
}
