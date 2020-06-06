package com.amaze.filemanager.asynchronous.services.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    @Test
    public void testGetLocalInetAddressMustNotBeEmpty(){
        if(!FtpService.isConnectedToLocalNetwork(InstrumentationRegistry.getInstrumentation().getTargetContext()))
            fail("Please connect your device to network to run this test!");
        assertNotNull(FtpService.getLocalInetAddress(InstrumentationRegistry.getInstrumentation().getTargetContext()));
        assertNotNull(FtpService.getLocalInetAddress(InstrumentationRegistry.getInstrumentation().getTargetContext()).getHostAddress());
    }

    /**
     * To test IP address returned by {@link FtpService#getLocalInetAddress(Context)} must be 192.168.43.1.
     *
     * <b>Remember to turn on Wi-Fi AP when running this test on <u>real</u> devices.</b>
     */
    @Test
    public void testGetLocalInetAddressMustBeAPAddress(){
        if(!FtpService.isEnabledWifiHotspot(InstrumentationRegistry.getInstrumentation().getTargetContext()))
            fail("Please enable Wi-Fi hotspot on your device to run this test!");

        assertEquals("192.168.43.1", FtpService.getLocalInetAddress(InstrumentationRegistry.getInstrumentation().getTargetContext()).getHostAddress());
    }
}
