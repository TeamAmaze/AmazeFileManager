package com.amaze.filemanager.asynchronous.services.ftp;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This test is separated from FtpServiceEspressoTest since it does not actually requires the FTP
 * service itself.
 *
 * It is expected that you are not running all the cases in one go. <b>You have been warned</b>.
 */

@SmallTest
@RunWith(AndroidJUnit4.class)
public class FtpServiceStaticMethodsTest {

    /**
     * To test {@link FtpService#getLocalInetAddress(Context)} must not return an empty string.
     */
    @Test @Ignore("Test disabled due to [#1664](https://github.com/TeamAmaze/AmazeFileManager/issues/1664)")
    public void testGetLocalInetAddressMustNotBeEmpty(){
        if(!FtpService.isConnectedToLocalNetwork(InstrumentationRegistry.getTargetContext()))
            fail("Please connect your device to network to run this test!");
        assertNotNull(FtpService.getLocalInetAddress(InstrumentationRegistry.getTargetContext()));
        assertNotNull(FtpService.getLocalInetAddress(InstrumentationRegistry.getTargetContext()).getHostAddress());
    }

    /**
     * To test IP address returned by {@link FtpService#getLocalInetAddress(Context)} must be 192.168.43.1.
     *
     * <b>Remember to turn on wifi AP when running this test on <u>real</u> devices.</b>
     */
    @Test @Ignore("Test disabled due to [#1664](https://github.com/TeamAmaze/AmazeFileManager/issues/1664)")
    public void testGetLocalInetAddressMustBeAPAddress(){
        if(!FtpService.isEnabledWifiHotspot(InstrumentationRegistry.getTargetContext()))
            fail("Please enable wifi hotspot on your device to run this test!");

        assertEquals("192.168.43.1", FtpService.getLocalInetAddress(InstrumentationRegistry.getTargetContext()).getHostAddress());
    }
}
