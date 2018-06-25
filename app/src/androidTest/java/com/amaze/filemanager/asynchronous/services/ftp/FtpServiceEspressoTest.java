package com.amaze.filemanager.asynchronous.services.ftp;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class FtpServiceEspressoTest {

    private FtpService service;

    @Before
    public void setUp() throws Exception {
        service = create();
    }

    @After
    public void shutDown() throws Exception {
        service.onDestroy();
    }

    @Test
    public void testFTPService() throws Exception{
        PreferenceManager.getDefaultSharedPreferences(service).edit().putBoolean(FtpService.KEY_PREFERENCE_SECURE, false).commit();
        service.onStartCommand(new Intent(FtpService.ACTION_START_FTPSERVER).putExtra(FtpService.TAG_STARTED_BY_TILE, false), 0, 0);
        assertTrue(FtpService.isRunning());
        waitForServer();

        loginAndVerifyWith(new FTPClient());
    }

    @Test
    public void testSecureFTPService() throws Exception
    {
        PreferenceManager.getDefaultSharedPreferences(service).edit().putBoolean(FtpService.KEY_PREFERENCE_SECURE, true).commit();
        service.onStartCommand(new Intent(FtpService.ACTION_START_FTPSERVER).putExtra(FtpService.TAG_STARTED_BY_TILE, false), 0, 0);
        assertTrue(FtpService.isRunning());
        waitForServer();

        loginAndVerifyWith(new FTPSClient(true));
    }

    private void loginAndVerifyWith(FTPClient ftpClient) throws Exception
    {
        ftpClient.connect("localhost", FtpService.DEFAULT_PORT);
        ftpClient.login("anonymous", "test@example.com");
        ftpClient.changeWorkingDirectory("/");
        FTPFile[] files = ftpClient.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0);
        boolean downloadFolderExists = false;
        for(FTPFile f : files){
            if(f.getName().equalsIgnoreCase("download"))
                downloadFolderExists = true;
        }
        ftpClient.logout();
        ftpClient.disconnect();

        if(!downloadFolderExists)
            fail("Download folder not found on device. Either storage is not available, or something is really wrong with FtpService. Check logcat.");
    }

    private FtpService create() throws Exception
    {
        FtpService service = new FtpService();
        // Trick borrowed from org.robolectric.android.controller.ServiceController
        Class activityThreadClazz = Class.forName("android.app.ActivityThread");
        Method attach = Service.class.getDeclaredMethod("attach", Context.class, activityThreadClazz, String.class, IBinder.class, Application.class, Object.class);
        attach.invoke(service, InstrumentationRegistry.getTargetContext(),
                null,
                service.getClass().getSimpleName(),
                null,
                null,
                null);
        return service;
    }

    private void waitForServer() throws Exception
    {
        boolean available = false;
        while(!available) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), FtpService.DEFAULT_PORT));
                socket.close();
                available = true;
            } catch(SocketException e) {
                available = false;
            }
        }
    }
}
