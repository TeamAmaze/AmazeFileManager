package com.amaze.filemanager.asynchronous.services.ftp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.amaze.filemanager.BuildConfig;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowNetwork;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import static com.amaze.filemanager.asynchronous.services.ftp.FTPService.KEY_PREFERENCE_PATH;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class FTPServiceTest {

    private static final Network localNetwork = ShadowNetwork.newInstance(1);
    private static final NetworkInfo localNetworkInfo = ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.BLOCKED,
            ConnectivityManager.TYPE_DUMMY, Integer.MAX_VALUE, false, false);

    private ConnectivityManager cm;
    private ShadowConnectivityManager scm;

    private FTPClient ftpClient;

    @BeforeClass
    public static void bootstrap() throws Exception {

    }

    @Before
    public void setUp() throws Exception {
        cm = RuntimeEnvironment.systemContext.getSystemService(ConnectivityManager.class);
        scm = Shadows.shadowOf(cm);

        scm.addNetwork(localNetwork, localNetworkInfo);
        scm.setActiveNetworkInfo(localNetworkInfo);

        FTPService service = Robolectric.setupService(FTPService.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        prefs.edit().putString(KEY_PREFERENCE_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
        service.onStartCommand(new Intent(FTPService.ACTION_START_FTPSERVER).putExtra(FTPService.TAG_STARTED_BY_TILE, false), 0, 0);

        assertTrue(FTPService.isRunning());
        waitForServer();
    }

    @After
    public void tearDown() throws Exception {
        scm.clearAllNetworks();

        if (ftpClient != null && ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
            ftpClient = null;
        }
    }

    @Test
    public void testPwdRoot() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
    }

    @Test
    public void testMkdir() throws Exception {
        login();
        assertEquals("/", ftpClient.printWorkingDirectory());
        assertTrue(ftpClient.makeDirectory("/Documents"));
        assertTrue(ftpClient.changeWorkingDirectory("/Documents"));
        assertEquals("/Documents", ftpClient.printWorkingDirectory());
    }

    private void login() throws Exception {
        this.ftpClient = new FTPClient();
        ftpClient.connect("localhost", FTPService.DEFAULT_PORT);
        ftpClient.login("anonymous", "test@example.com");
    }

    //Required since FTPService is running on separate thread...
    private void waitForServer() throws Exception {
        boolean available = false;
        while(!available) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), FTPService.DEFAULT_PORT));
                socket.close();
                available = true;
            } catch(SocketException e) {
                available = false;
            }
        }
        return;
    }
}
