/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.os.Environment;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class OperationsTest {

  private File storageRoot = Environment.getExternalStorageDirectory();

  @Test
  public void testIsFileNameValid() {
    assertTrue(Operations.isFileNameValid("file.txt"));
    assertTrue(Operations.isFileNameValid("/storage/emulated/0/Documents/file.txt"));
    assertTrue(Operations.isFileNameValid("/system/etc/file.txt"));
    assertTrue(Operations.isFileNameValid("smb://127.0.0.1/trancelove/file.txt"));
    assertTrue(Operations.isFileNameValid("ssh://127.0.0.1:54225/home/trancelove/file.txt"));
    assertTrue(Operations.isFileNameValid("ftp://127.0.0.1:3721/pub/Incoming/file.txt"));
    assertTrue(
        Operations.isFileNameValid(
            "content://com.amaze.filemanager/storage_root/storage/emulated/0/Documents/file.txt"));
  }

  @Test
  public void testMkdir() throws InterruptedException {
    File newFolder = new File(storageRoot, "test");
    HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

    CountDownLatch waiter = new CountDownLatch(1);
    Operations.mkdir(
        newFolderHF,
        newFolderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
          @Override
          public void done(HybridFile hFile, boolean b) {
            waiter.countDown();
          }
        });
    waiter.await();
    assertTrue(newFolder.exists());
  }

  @Test
  public void testMkdirDuplicate() throws InterruptedException {
    File newFolder = new File(storageRoot, "test");
    HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

    CountDownLatch waiter1 = new CountDownLatch(1);
    Operations.mkdir(
        newFolderHF,
        newFolderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
          @Override
          public void done(HybridFile hFile, boolean b) {
            waiter1.countDown();
          }
        });
    waiter1.await();
    assertTrue(newFolder.exists());

    CountDownLatch waiter2 = new CountDownLatch(1);
    AtomicBoolean assertFlag = new AtomicBoolean(false);
    Operations.mkdir(
        newFolderHF,
        newFolderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
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
  public void testMkdirNewFolderSameNameAsCurrentFolder() throws InterruptedException {
    File newFolder = new File(storageRoot, "test");
    HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

    CountDownLatch waiter1 = new CountDownLatch(1);
    Operations.mkdir(
        newFolderHF,
        newFolderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
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
    Operations.mkdir(
        newFolder2HF,
        newFolder2HF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
          @Override
          public void done(HybridFile hFile, boolean b) {
            waiter2.countDown();
          }
        });
    waiter2.await();
    assertTrue(newFolder2.exists());

    CountDownLatch waiter3 = new CountDownLatch(1);
    AtomicBoolean assertFlag = new AtomicBoolean(false);
    Operations.mkdir(
        newFolder2HF,
        newFolder2HF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
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
  public void testRename() throws InterruptedException {
    File oldFolder = new File(storageRoot, "test1");
    HybridFile oldFolderHF = new HybridFile(OpenMode.FILE, oldFolder.getAbsolutePath());
    File newFolder = new File(storageRoot, "test2");
    HybridFile newFolderHF = new HybridFile(OpenMode.FILE, newFolder.getAbsolutePath());

    CountDownLatch waiter1 = new CountDownLatch(1);
    Operations.mkdir(
        newFolderHF,
        oldFolderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
          @Override
          public void done(HybridFile hFile, boolean b) {
            waiter1.countDown();
          }
        });
    waiter1.await();
    assertTrue(oldFolder.exists());

    CountDownLatch waiter2 = new CountDownLatch(1);
    Operations.rename(
        oldFolderHF,
        newFolderHF,
        false,
        ApplicationProvider.getApplicationContext(),
        new AbstractErrorCallback() {
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
  public void testRenameSameName() throws InterruptedException {
    File folder = new File(storageRoot, "test");
    HybridFile folderHF = new HybridFile(OpenMode.FILE, folder.getAbsolutePath());

    CountDownLatch waiter1 = new CountDownLatch(1);
    Operations.mkdir(
        folderHF,
        folderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
          @Override
          public void done(HybridFile hFile, boolean b) {
            waiter1.countDown();
          }
        });
    waiter1.await();
    assertTrue(folder.exists());

    CountDownLatch waiter2 = new CountDownLatch(1);
    AtomicBoolean assertFlag = new AtomicBoolean(false);
    Operations.rename(
        folderHF,
        folderHF,
        false,
        ApplicationProvider.getApplicationContext(),
        new AbstractErrorCallback() {
          @Override
          public void exists(HybridFile file) {
            assertFlag.set(true);
            waiter2.countDown();
          }
        });
    waiter2.await();
    assertTrue(folder.exists());
    assertTrue(assertFlag.get());
  }

  @Test
  public void testRenameSameName2() throws InterruptedException {
    File folder = new File(storageRoot, "test");
    HybridFile folderHF = new HybridFile(OpenMode.FILE, folder.getAbsolutePath());

    CountDownLatch waiter1 = new CountDownLatch(1);
    Operations.mkdir(
        folderHF,
        folderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
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
    Operations.mkdir(
        folder2HF,
        folder2HF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
          @Override
          public void done(HybridFile hFile, boolean b) {
            waiter2.countDown();
          }
        });
    waiter2.await();
    assertTrue(folder2.exists());

    CountDownLatch waiter3 = new CountDownLatch(1);
    AtomicBoolean assertFlag = new AtomicBoolean(false);
    Operations.rename(
        folderHF,
        folder2HF,
        false,
        ApplicationProvider.getApplicationContext(),
        new AbstractErrorCallback() {
          @Override
          public void exists(HybridFile file) {
            assertFlag.set(true);
            waiter3.countDown();
          }
        });
    waiter3.await();
    assertTrue(folder.exists());
    assertTrue(assertFlag.get());
  }

  @Test
  public void testRenameSameName3() throws InterruptedException {
    File folder = new File(storageRoot, "test");
    HybridFile folderHF = new HybridFile(OpenMode.FILE, folder.getAbsolutePath());

    CountDownLatch waiter1 = new CountDownLatch(1);
    Operations.mkdir(
        folderHF,
        folderHF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
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
    Operations.mkdir(
        folder2HF,
        folder2HF,
        ApplicationProvider.getApplicationContext(),
        false,
        new AbstractErrorCallback() {
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
    Operations.rename(
        folder2HF,
        folder3HF,
        false,
        ApplicationProvider.getApplicationContext(),
        new AbstractErrorCallback() {
          @Override
          public void done(HybridFile file, boolean b) {
            assertFlag.set(true);
            waiter3.countDown();
          }
        });
    waiter3.await();
    assertTrue(folder3.exists());
    assertTrue(assertFlag.get());
  }

  private abstract class AbstractErrorCallback implements Operations.ErrorCallBack {
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
