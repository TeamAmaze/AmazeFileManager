package com.amaze.filemanager.asynchronous.services.ftp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.test.ShadowContentResolverWithLocalOutputStreamSupport;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowWifiManager;
import org.robolectric.shadows.multidex.ShadowMultiDex;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

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

    private void login() throws Exception {
        this.ftpClient = new FTPClient();
        ftpClient.connect("localhost", FTP_PORT);
        ftpClient.login("anonymous", "test@example.com");
    }

    //Required since FTPService is running on separate thread...
    private void waitForServer() throws Exception {
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
