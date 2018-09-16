package com.amaze.filemanager.asynchronous.services.ftp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.test.ShadowContentResolverWithLocalOutputStreamSupport;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowWifiManager;
import org.robolectric.shadows.multidex.ShadowMultiDex;
import org.robolectric.util.ReflectionHelpers;

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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Locale;

import static com.amaze.filemanager.asynchronous.services.ftp.FtpService.KEY_PREFERENCE_PATH;
import static com.amaze.filemanager.asynchronous.services.ftp.FtpService.KEY_PREFERENCE_PORT;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class, ShadowContentResolverWithLocalOutputStreamSupport.class})
public class FtpServiceTest {

    private static final int FTP_PORT = 62222;

    private FTPClient ftpClient;

    private FtpService service;

    @Before
    public void setUp() throws Exception {

        ShadowConnectivityManager cm = Shadows.shadowOf(RuntimeEnvironment.application.getSystemService(ConnectivityManager.class));
        ShadowWifiManager wifiManager = Shadows.shadowOf(RuntimeEnvironment.application.getSystemService(WifiManager.class));
        cm.setActiveNetworkInfo(ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, -1, true, true));
        wifiManager.setWifiEnabled(true);
        ReflectionHelpers.callInstanceMethod(wifiManager.getConnectionInfo(), "setInetAddress", ReflectionHelpers.ClassParameter.from(InetAddress.class, InetAddress.getLoopbackAddress()));

        service = Robolectric.setupService(FtpService.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        prefs.edit()
            .putString(KEY_PREFERENCE_PATH, Environment.getExternalStorageDirectory().getAbsolutePath())
            .putInt(KEY_PREFERENCE_PORT, FTP_PORT).commit();
        service.onStartCommand(new Intent(FtpService.ACTION_START_FTPSERVER).putExtra(FtpService.TAG_STARTED_BY_TILE, false), 0, 0);

        assertTrue(FtpService.isRunning());
        waitForServer();

        //ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() throws Exception {

        if (ftpClient != null && ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
            ftpClient = null;
        }
        service.onDestroy();
    }

    @Test
    public void testPwdRoot() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
    }

    @Test
    public void testCdPastRoot() throws Exception {
        login();
        ftpClient.changeWorkingDirectory("/");
        assertEquals("/", ftpClient.printWorkingDirectory());
        ftpClient.changeWorkingDirectory("../");
        assertEquals("/", ftpClient.printWorkingDirectory());
        ftpClient.changeWorkingDirectory("../../../../../../../");
        assertEquals("/", ftpClient.printWorkingDirectory());
    }

    @Test
    public void testMkdir() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.makeDirectory("/Documents"));
        assertTrue(ftpClient.changeWorkingDirectory("/Documents"));
        assertEquals("/Documents", ftpClient.printWorkingDirectory());
        assertTrue(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
    }

    @Test
    public void testMkdirWithoutSlashPrefix() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.makeDirectory("Documents"));
        assertTrue(ftpClient.changeWorkingDirectory("Documents"));
        assertEquals("/Documents", ftpClient.printWorkingDirectory());
        assertTrue(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
    }

    @Test
    public void testMkdirPastRoot() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertFalse(ftpClient.makeDirectory("../../../../../../../../../../Documents"));
        assertFalse(ftpClient.changeWorkingDirectory("../../../../../../../../../../Documents"));
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertFalse(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
    }

    @Test
    public void testRepeatMkdir() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.makeDirectory("/Documents"));        assertTrue(ftpClient.changeWorkingDirectory("/Documents"));
        assertEquals("/Documents", ftpClient.printWorkingDirectory());
        assertTrue(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
        assertFalse(ftpClient.makeDirectory("/Documents"));
        assertFalse(ftpClient.makeDirectory("/Documents"));
    }

    @Test
    public void testRmdir() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.makeDirectory("/Documents"));
        assertTrue(ftpClient.changeWorkingDirectory("/Documents"));
        assertEquals("/Documents", ftpClient.printWorkingDirectory());
        assertTrue(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
        assertTrue(ftpClient.changeToParentDirectory());
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.removeDirectory("/Documents"));
        assertFalse(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
    }

    @Test
    public void testRmdirWithoutSlashPrefix() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.makeDirectory("Documents"));
        assertTrue(ftpClient.changeWorkingDirectory("Documents"));
        assertEquals("/Documents", ftpClient.printWorkingDirectory());
        assertTrue(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
        assertTrue(ftpClient.changeToParentDirectory());
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.removeDirectory("Documents"));
        assertFalse(new File(Environment.getExternalStorageDirectory(), "Documents").exists());
    }

    @Test
    public void testRmdirPastRoot() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertFalse(ftpClient.removeDirectory("../../../../../../../../../../Documents"));
        assertFalse(ftpClient.removeDirectory("../../../../../../../../../../../../bin"));
        assertTrue(ftpClient.changeWorkingDirectory("../../../../../../../../../.."));
        assertEquals("/", ftpClient.printWorkingDirectory());
    }

    @Test
    public void testUploadFile() throws IOException {
        byte[] bytes1 = new byte[32], bytes2 = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.setSeed(System.currentTimeMillis());
        sr.nextBytes(bytes1);
        sr.nextBytes(bytes2);

        String randomString = Base64.encodeToString(bytes1, Base64.DEFAULT);

        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
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

    @Test
    public void testDownloadFile() throws IOException {
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

        ShadowContentResolverWithLocalOutputStreamSupport contentResolver = Shadow.extract(RuntimeEnvironment.application.getContentResolver());
        contentResolver.registerInputStream(Uri.fromFile(testFile1), new ByteArrayInputStream(randomString.getBytes(Charset.forName("UTF-8"))));
        contentResolver.registerInputStream(Uri.fromFile(testFile2), new ByteArrayInputStream(bytes2));

        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        ftpClient.changeWorkingDirectory("/");

        FTPFile[] files = ftpClient.listFiles();
        assertEquals(2, files.length);
        for(FTPFile file:files){
            System.err.println(String.format(Locale.US, "%s %d", file.getName(), file.getSize()));
        }

        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
        ByteArrayOutputStream verify = new ByteArrayOutputStream();
        ftpClient.retrieveFile("test.txt", verify);
        verify.close();
        assertTrue(verify.size() > 0);
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

    private void login() throws IOException {
        this.ftpClient = new FTPClient();
        ftpClient.connect("localhost", FTP_PORT);
        ftpClient.login("anonymous", "test@example.com");
    }

    //Required since FTPService is running on separate thread...
    private void waitForServer() throws IOException {
        boolean available = false;
        while(!available) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), FTP_PORT));
                socket.close();
                available = true;
            } catch(SocketException e) {
                available = false;
            }
        }
        return;
    }
}
