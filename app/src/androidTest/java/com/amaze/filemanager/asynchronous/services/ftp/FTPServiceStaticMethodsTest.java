package com.amaze.filemanager.asynchronous.services.ftp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This test is separated from FTPServiceEspressoTest since it does not actually requires the FTP
 * service itself.
 *
 * It is expected that you are not running all the cases in one go. <b>You have been warned</b>.
 */

@SmallTest
@RunWith(AndroidJUnit4.class)
public class FTPServiceStaticMethodsTest {

    /**
     * To test {@link FTPService#getLocalInetAddress(Context)} must not return an empty string.
     */
    @Test
    public void testGetLocalInetAddressMustNotBeEmpty(){
        if(!FTPService.isConnectedToLocalNetwork(InstrumentationRegistry.getTargetContext()))
            fail("Please connect your device to network to run this test!");
        assertNotNull(FTPService.getLocalInetAddress(InstrumentationRegistry.getTargetContext()));
        assertNotNull(FTPService.getLocalInetAddress(InstrumentationRegistry.getTargetContext()).getHostAddress());
    }

    /**
     * To test IP address returned by {@link FTPService#getLocalInetAddress(Context)} must be 192.168.43.1.
     *
     * <b>Remember to turn on wifi AP when running this test on <u>real</u> devices.</b>
     */
    @Test
    public void testGetLocalInetAddressMustBeAPAddress(){
        if(!FTPService.isEnabledWifiHotspot(InstrumentationRegistry.getTargetContext()))
            fail("Please enable wifi hotspot on your device to run this test!");

        assertEquals("192.168.43.1", FTPService.getLocalInetAddress(InstrumentationRegistry.getTargetContext()).getHostAddress());
    }
}
