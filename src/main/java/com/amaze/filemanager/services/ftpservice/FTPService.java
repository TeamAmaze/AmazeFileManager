package com.amaze.filemanager.services.ftpservice;

/**
 * Created by yashwanthreddyg on 09-06-2016.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amaze.filemanager.R;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class FTPService extends Service implements Runnable {

    public static final int DEFAULT_PORT = 2211;
    public static final String DEFAULT_USERNAME = "";
    public static final int DEFAULT_TIMEOUT = 600;   // default timeout, in sec
    public static final boolean DEFAULT_SECURE = false;
    public static final String PORT_PREFERENCE_KEY = "ftpPort";
    public static final String KEY_PREFERENCE_PATH = "ftp_path";
    public static final String KEY_PREFERENCE_USERNAME = "ftp_username";
    public static final String KEY_PREFERENCE_PASSWORD = "ftp_password";
    public static final String KEY_PREFERENCE_TIMEOUT = "ftp_timeout";
    public static final String KEY_PREFERENCE_SECURE = "ftp_secure";
    public static final String DEFAULT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    private static final String TAG = FTPService.class.getSimpleName();

    /**
     * TODO: 25/10/16 This is ugly
     */
    private static int port = 2211;

    // Service will (global) broadcast when server start/stop
    static public final String ACTION_STARTED = "com.amaze.filemanager.services.ftpservice.FTPReceiver.FTPSERVER_STARTED";
    static public final String ACTION_STOPPED = "com.amaze.filemanager.services.ftpservice.FTPReceiver.FTPSERVER_STOPPED";
    static public final String ACTION_FAILEDTOSTART = "com.amaze.filemanager.services.ftpservice.FTPReceiver.FTPSERVER_FAILEDTOSTART";

    // RequestStartStopReceiver listens for these actions to start/stop this server
    static public final String ACTION_START_FTPSERVER = "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_START_FTPSERVER";
    static public final String ACTION_STOP_FTPSERVER = "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_STOP_FTPSERVER";

    private String username, password;
    private boolean isPasswordProtected = false;

    private FtpServer server;
    protected static Thread serverThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int attempts = 10;
        while (serverThread != null) {
            if (attempts > 0) {
                attempts--;
                FTPService.sleepIgnoreInterupt(1000);
            } else {
                return START_STICKY;
            }
        }

        serverThread = new Thread(this);
        serverThread.start();

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void run() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        FtpServerFactory serverFactory = new FtpServerFactory();
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);

        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

        String usernamePreference = preferences.getString(KEY_PREFERENCE_USERNAME, DEFAULT_USERNAME);
        if (!usernamePreference.equals(DEFAULT_USERNAME)) {
            username = usernamePreference;
            password = preferences.getString(KEY_PREFERENCE_PASSWORD, "");
            isPasswordProtected = true;
        }

        BaseUser user = new BaseUser();
        if (!isPasswordProtected) {
            user.setName("anonymous");
        } else {
            user.setName(username);
            user.setPassword(password);
        }

        user.setHomeDirectory(preferences.getString(KEY_PREFERENCE_PATH, DEFAULT_PATH));
        List<Authority> list = new ArrayList<>();
        list.add(new WritePermission());
        user.setAuthorities(list);
        try {
            serverFactory.getUserManager().save(user);
        } catch (FtpException e) {
            e.printStackTrace();
        }
        ListenerFactory fac = new ListenerFactory();

        port = preferences.getInt(PORT_PREFERENCE_KEY, DEFAULT_PORT);

        if (preferences.getBoolean(KEY_PREFERENCE_SECURE, DEFAULT_SECURE)) {
            SslConfigurationFactory sslConfigurationFactory = new SslConfigurationFactory();

            File file;
            try {

                InputStream stream = getResources().openRawResource(R.raw.key);
                file = File.createTempFile("keystore", "bks");
                FileOutputStream outputStream = new FileOutputStream(file);
                IOUtils.copy(stream, outputStream);
            } catch (Exception e) {
                e.printStackTrace();
                file = null;
            }

            if (file != null) {
                sslConfigurationFactory.setKeystoreFile(file);
                sslConfigurationFactory.setKeystorePassword("vishal007");
                fac.setSslConfiguration(sslConfigurationFactory.createSslConfiguration());
                fac.setImplicitSsl(true);
            } else {
                // no keystore found
                preferences.edit().putBoolean(KEY_PREFERENCE_SECURE, false).apply();
            }
        }

        fac.setPort(port);
        fac.setIdleTimeout(preferences.getInt(KEY_PREFERENCE_TIMEOUT, DEFAULT_TIMEOUT));

        serverFactory.addListener("default", fac.createListener());
        try {
            server = serverFactory.createServer();
            server.start();
            sendBroadcast(new Intent(FTPService.ACTION_STARTED));
        } catch (Exception e) {
            sendBroadcast(new Intent(FTPService.ACTION_FAILEDTOSTART));
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() Stopping server");
        if (serverThread == null) {
            Log.w(TAG, "Stopping with null serverThread");
            return;
        }
        serverThread.interrupt();
        try {
            serverThread.join(10000); // wait 10 sec for server thread to finish
        } catch (InterruptedException e) {
        }
        if (serverThread.isAlive()) {
            Log.w(TAG, "Server thread failed to exit");
        } else {
            Log.d(TAG, "serverThread join()ed ok");
            serverThread = null;
        }
        if (server != null) {
            server.stop();
            sendBroadcast(new Intent(FTPService.ACTION_STOPPED));
        }
        Log.d(TAG, "FTPServerService.onDestroy() finished");
    }

    //Restart the service if the app is closed from the recent list
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 2000, restartServicePI);
    }

    public static boolean isRunning() {
        // return true if and only if a server Thread is running
        if (serverThread == null) {
            Log.d(TAG, "Server is not running (null serverThread)");
            return false;
        }
        if (!serverThread.isAlive()) {
            Log.d(TAG, "serverThread non-null but !isAlive()");
        } else {
            Log.d(TAG, "Server is alive");
        }
        return true;
    }

    public static void sleepIgnoreInterupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public static boolean isConnectedToLocalNetwork(Context context) {
        boolean connected = false;
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        connected = ni != null
                && ni.isConnected()
                && (ni.getType() & (ConnectivityManager.TYPE_WIFI | ConnectivityManager.TYPE_ETHERNET)) != 0;
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an WIFI AP");
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            try {
                Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
                connected = (Boolean) method.invoke(wm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an USB AP");
            try {
                for (NetworkInterface netInterface : Collections.list(NetworkInterface
                        .getNetworkInterfaces())) {
                    if (netInterface.getDisplayName().startsWith("rndis")) {
                        connected = true;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return connected;
    }

    public static boolean isConnectedToWifi(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected()
                && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static InetAddress getLocalInetAddress(Context context) {
        if (!isConnectedToLocalNetwork(context)) {
            Log.e(TAG, "getLocalInetAddress called and no connection");
            return null;
        }

        if (isConnectedToWifi(context)) {

            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int ipAddress = wm.getConnectionInfo().getIpAddress();
            if (ipAddress == 0)
                return null;
            return intToInet(ipAddress);
        }

        try {
            Enumeration<NetworkInterface> netinterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (netinterfaces.hasMoreElements()) {
                NetworkInterface netinterface = netinterfaces.nextElement();
                Enumeration<InetAddress> adresses = netinterface.getInetAddresses();
                while (adresses.hasMoreElements()) {
                    InetAddress address = adresses.nextElement();
                    // this is the condition that sometimes gives problems
                    if (!address.isLoopbackAddress()
                            && !address.isLinkLocalAddress())
                        return address;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InetAddress intToInet(int value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = byteOfInt(value, i);
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            // This only happens if the byte array has a bad length
            return null;
        }
    }

    public static byte byteOfInt(int value, int which) {
        int shift = which * 8;
        return (byte) (value >> shift);
    }

    public static int getPort() {
        return port;
    }

    public static boolean isPortAvailable(int port) {

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }

        return false;
    }
}
