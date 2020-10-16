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

package com.amaze.filemanager.asynchronous.services.ftp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(AndroidJUnit4.class)
public class FtpServiceEspressoTest {

  private FtpService service;

  @Before
  public void setUp() throws ReflectiveOperationException {
    service = create();
  }

  @After
  public void shutDown() {
    service.onDestroy();
  }

  @Test
  public void testFtpService() throws IOException {
    PreferenceManager.getDefaultSharedPreferences(service)
        .edit()
        .putBoolean(FtpService.KEY_PREFERENCE_SECURE, false)
        .commit();
    service.onStartCommand(
        new Intent(FtpService.ACTION_START_FTPSERVER)
            .putExtra(FtpService.TAG_STARTED_BY_TILE, false),
        0,
        0);
    assertTrue(FtpService.isRunning());
    waitForServer();

    FTPClient ftpClient = new FTPClient();
    loginAndVerifyWith(ftpClient);
    testUploadWith(ftpClient);
    testDownloadWith(ftpClient);
  }

  @Test
  public void testSecureFtpService() throws IOException {
    PreferenceManager.getDefaultSharedPreferences(service)
        .edit()
        .putBoolean(FtpService.KEY_PREFERENCE_SECURE, true)
        .commit();
    service.onStartCommand(
        new Intent(FtpService.ACTION_START_FTPSERVER)
            .putExtra(FtpService.TAG_STARTED_BY_TILE, false),
        0,
        0);
    assertTrue(FtpService.isRunning());
    waitForServer();

    FTPSClient ftpClient = new FTPSClient(true);
    loginAndVerifyWith(ftpClient);
    testUploadWith(ftpClient);
    testDownloadWith(ftpClient);
  }

  private void loginAndVerifyWith(FTPClient ftpClient) throws IOException {
    ftpClient.connect("localhost", FtpService.DEFAULT_PORT);
    ftpClient.login("anonymous", "test@example.com");
    ftpClient.changeWorkingDirectory("/");
    FTPFile[] files = ftpClient.listFiles();
    assertNotNull(files);
    assertTrue(
        "No files found on device? It is also possible that app doesn't have permission to access storage, which may occur on broken Android emulators",
        files.length > 0);
    boolean downloadFolderExists = false;
    for (FTPFile f : files) {
      if (f.getName().equalsIgnoreCase("download")) downloadFolderExists = true;
    }
    ftpClient.logout();
    ftpClient.disconnect();

    assertTrue(
        "Download folder not found on device. Either storage is not available, or something is really wrong with FtpService. Check logcat.",
        downloadFolderExists);
  }

  private void testUploadWith(FTPClient ftpClient) throws IOException {
    byte[] bytes1 = new byte[32], bytes2 = new byte[32];
    SecureRandom sr = new SecureRandom();
    sr.setSeed(System.currentTimeMillis());
    sr.nextBytes(bytes1);
    sr.nextBytes(bytes2);

    String randomString = Base64.encodeToString(bytes1, Base64.DEFAULT);

    ftpClient.connect("localhost", FtpService.DEFAULT_PORT);
    ftpClient.login("anonymous", "test@example.com");
    ftpClient.changeWorkingDirectory("/");
    ftpClient.enterLocalPassiveMode();
    ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
    InputStream in = new ByteArrayInputStream(randomString.getBytes("utf-8"));
    ftpClient.storeFile("test.txt", in);
    in.close();
    in = new ByteArrayInputStream(bytes2);
    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    ftpClient.storeFile("test.bin", in);
    in.close();
    ftpClient.logout();
    ftpClient.disconnect();

    File verify = new File(Environment.getExternalStorageDirectory(), "test.txt");
    assertTrue(verify.exists());
    ByteArrayOutputStream verifyContent = new ByteArrayOutputStream();
    IOUtils.copy(new FileInputStream(verify), verifyContent);
    assertEquals(randomString, verifyContent.toString("utf-8"));
    verify.delete();
    verify = new File(Environment.getExternalStorageDirectory(), "test.bin");
    assertTrue(verify.exists());
    verifyContent = new ByteArrayOutputStream();
    IOUtils.copy(new FileInputStream(verify), verifyContent);
    assertArrayEquals(bytes2, verifyContent.toByteArray());
    verify.delete();
  }

  private void testDownloadWith(FTPClient ftpClient) throws IOException {
    File testFile1 = new File(Environment.getExternalStorageDirectory(), "test.txt");
    File testFile2 = new File(Environment.getExternalStorageDirectory(), "test.bin");

    byte[] bytes1 = new byte[32], bytes2 = new byte[32];
    SecureRandom sr = new SecureRandom();
    sr.setSeed(System.currentTimeMillis());
    sr.nextBytes(bytes1);
    sr.nextBytes(bytes2);

    String randomString = Base64.encodeToString(bytes1, Base64.DEFAULT);

    Writer writer = new FileWriter(testFile1);
    writer.write(randomString);
    writer.close();

    OutputStream out = new FileOutputStream(testFile2);
    out.write(bytes2, 0, bytes2.length);
    out.close();

    ftpClient.connect("localhost", FtpService.DEFAULT_PORT);
    ftpClient.login("anonymous", "test@example.com");
    ftpClient.changeWorkingDirectory("/");
    ftpClient.enterLocalPassiveMode();
    ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
    ByteArrayOutputStream verify = new ByteArrayOutputStream();
    ftpClient.retrieveFile("test.txt", verify);
    verify.close();
    assertEquals(randomString, verify.toString("utf-8"));

    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    verify = new ByteArrayOutputStream();
    ftpClient.retrieveFile("test.bin", verify);
    verify.close();
    assertArrayEquals(bytes2, verify.toByteArray());

    ftpClient.logout();
    ftpClient.disconnect();

    testFile1.delete();
    testFile2.delete();
  }

  private FtpService create() throws ReflectiveOperationException {
    FtpService service = new FtpService();
    // Trick borrowed from org.robolectric.android.controller.ServiceController
    Class activityThreadClazz = Class.forName("android.app.ActivityThread");
    Method attach =
        Service.class.getDeclaredMethod(
            "attach",
            Context.class,
            activityThreadClazz,
            String.class,
            IBinder.class,
            Application.class,
            Object.class);
    attach.invoke(
        service,
        InstrumentationRegistry.getInstrumentation().getTargetContext(),
        null,
        service.getClass().getSimpleName(),
        null,
        null,
        null);
    return service;
  }

  private void waitForServer() throws IOException {
    boolean available = false;
    while (!available) {
      Socket socket = new Socket();
      try {
        socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), FtpService.DEFAULT_PORT));
        socket.close();
        available = true;
      } catch (SocketException e) {
        available = false;
      }
    }
  }
}
