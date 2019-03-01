package com.amaze.filemanager.filesystem;

import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.utils.OpenMode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(maxSdk = 27, constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class OperationsTest {

    private File storageRoot = Environment.getExternalStorageDirectory();

    @Test
    public void testMkdir() throws InterruptedException
    {
        File newFolder = new File(storageRoot, "test");
        HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

        CountDownLatch waiter = new CountDownLatch(1);
        Operations.mkdir(newFolderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter.countDown();
            }
        });
        waiter.await();
        assertTrue(newFolder.exists());
    }

    @Test
    public void testMkdirDuplicate() throws InterruptedException
    {
        File newFolder = new File(storageRoot, "test");
        HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

        CountDownLatch waiter1 = new CountDownLatch(1);
        Operations.mkdir(newFolderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter1.countDown();
            }
        });
        waiter1.await();
        assertTrue(newFolder.exists());

        CountDownLatch waiter2 = new CountDownLatch(1);
        AtomicBoolean assertFlag = new AtomicBoolean(false);
        Operations.mkdir(newFolderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void exists(HybridFile file) {
                assertFlag.set(true);
                waiter2.countDown();
            }
        });
        waiter2.await();
        assertTrue(assertFlag.get());
    }

    @Test
    public void testMkdirNewFolderSameNameAsCurrentFolder() throws InterruptedException
    {
        File newFolder = new File(storageRoot, "test");
        HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

        CountDownLatch waiter1 = new CountDownLatch(1);
        Operations.mkdir(newFolderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter1.countDown();
            }
        });
        waiter1.await();
        assertTrue(newFolder.exists());

        File newFolder2 = new File(newFolder, "test");
        HybridFile newFolder2HF = new HybridFile(OpenMode.FILE, newFolder2.getAbsolutePath());
        CountDownLatch waiter2 = new CountDownLatch(1);
        Operations.mkdir(newFolder2HF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter2.countDown();
            }
        });
        waiter2.await();
        assertTrue(newFolder2.exists());

        CountDownLatch waiter3 = new CountDownLatch(1);
        AtomicBoolean assertFlag = new AtomicBoolean(false);
        Operations.mkdir(newFolder2HF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void exists(HybridFile file) {
                assertFlag.set(true);
                waiter3.countDown();
            }
        });
        waiter3.await();
        assertTrue(assertFlag.get());
    }

    @Test
    public void testRename() throws InterruptedException
    {
        File oldFolder = new File(storageRoot, "test1");
        HybridFile oldFolderHF = new HybridFile(OpenMode.FILE, oldFolder.getAbsolutePath());
        File newFolder = new File(storageRoot, "test2");
        HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

        CountDownLatch waiter1 = new CountDownLatch(1);
        Operations.mkdir(oldFolderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter1.countDown();
            }
        });
        waiter1.await();
        assertTrue(oldFolder.exists());

        CountDownLatch waiter2 = new CountDownLatch(1);
        Operations.rename(oldFolderHF, newFolderHF, false, RuntimeEnvironment.application, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter2.countDown();
            }
        });
        waiter2.await();
        assertFalse(oldFolder.exists());
        assertTrue(newFolder.exists());
    }

    @Test
    public void testRenameSameName() throws InterruptedException
    {
        File folder = new File(storageRoot, "test");
        HybridFile folderHF = new HybridFile(OpenMode.FILE, folder.getAbsolutePath());

        CountDownLatch waiter1 = new CountDownLatch(1);
        Operations.mkdir(folderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter1.countDown();
            }
        });
        waiter1.await();
        assertTrue(folder.exists());

        CountDownLatch waiter2 = new CountDownLatch(1);
        AtomicBoolean assertFlag = new AtomicBoolean(false);
        Operations.rename(folderHF, folderHF, false, RuntimeEnvironment.application, new AbstractErrorCallback() {
            @Override
            public void exists(HybridFile file){
                assertFlag.set(true);
                waiter2.countDown();
            }
        });
        waiter2.await();
        assertTrue(folder.exists());
        assertTrue(assertFlag.get());
    }

    @Test
    public void testRenameSameName2() throws InterruptedException
    {
        File folder = new File(storageRoot, "test");
        HybridFile folderHF = new HybridFile(OpenMode.FILE, folder.getAbsolutePath());

        CountDownLatch waiter1 = new CountDownLatch(1);
        Operations.mkdir(folderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter1.countDown();
            }
        });
        waiter1.await();
        assertTrue(folder.exists());

        File folder2 = new File(storageRoot, "test2");
        HybridFile folder2HF = new HybridFile(OpenMode.FILE, folder2.getAbsolutePath());

        CountDownLatch waiter2 = new CountDownLatch(1);
        Operations.mkdir(folder2HF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter2.countDown();
            }
        });
        waiter2.await();
        assertTrue(folder2.exists());

        CountDownLatch waiter3 = new CountDownLatch(1);
        AtomicBoolean assertFlag = new AtomicBoolean(false);
        Operations.rename(folderHF, folder2HF, false, RuntimeEnvironment.application, new AbstractErrorCallback() {
            @Override
            public void exists(HybridFile file){
                assertFlag.set(true);
                waiter3.countDown();
            }
        });
        waiter3.await();
        assertTrue(folder.exists());
        assertTrue(assertFlag.get());
    }

    @Test
    public void testRenameSameName3() throws InterruptedException
    {
        File folder = new File(storageRoot, "test");
        HybridFile folderHF = new HybridFile(OpenMode.FILE, folder.getAbsolutePath());

        CountDownLatch waiter1 = new CountDownLatch(1);
        Operations.mkdir(folderHF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter1.countDown();
            }
        });
        waiter1.await();
        assertTrue(folder.exists());

        File folder2 = new File(folder, "test2");
        HybridFile folder2HF = new HybridFile(OpenMode.FILE, folder2.getAbsolutePath());

        CountDownLatch waiter2 = new CountDownLatch(1);
        Operations.mkdir(folder2HF, RuntimeEnvironment.application, false, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile hFile, boolean b) {
                waiter2.countDown();
            }
        });
        waiter2.await();
        assertTrue(folder2.exists());

        File folder3 = new File(folder, "test");
        HybridFile folder3HF = new HybridFile(OpenMode.FILE, folder3.getAbsolutePath());

        CountDownLatch waiter3 = new CountDownLatch(1);
        AtomicBoolean assertFlag = new AtomicBoolean(false);
        Operations.rename(folder2HF, folder3HF, false, RuntimeEnvironment.application, new AbstractErrorCallback() {
            @Override
            public void done(HybridFile file, boolean b){
                assertFlag.set(true);
                waiter3.countDown();
            }
        });
        waiter3.await();
        assertTrue(folder3.exists());
        assertTrue(assertFlag.get());
    }

    private abstract class AbstractErrorCallback implements Operations.ErrorCallBack
    {
        @Override
        public void exists(HybridFile file) {}

        @Override
        public void launchSAF(HybridFile file) {}

        @Override
        public void launchSAF(HybridFile file, HybridFile file1) {}

        @Override
        public void done(HybridFile hFile, boolean b) {}

        @Override
        public void invalidName(HybridFile file) {}
    }
}
