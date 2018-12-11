package com.amaze.filemanager.filesystem.usb;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.activities.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import static android.os.Build.VERSION_CODES.KITKAT;
import static com.amaze.filemanager.filesystem.usb.ReflectionHelpers.addUsbOtgDevice;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class SingletonUsbOtgTest {
    @Test
    @Config(minSdk = KITKAT)
    public void usbConnectionTest() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create();

        addUsbOtgDevice(controller.get());

        controller.resume().get();

        Uri rootBefore = Uri.parse("ssh://testuser:testpassword@127.0.0.1:22222");

        SingletonUsbOtg.getInstance().setUsbOtgRoot(rootBefore);

        controller.pause().resume().get();

        Uri rootAfter = SingletonUsbOtg.getInstance().getUsbOtgRoot();

        assertEquals("Uris are different: (before:) " + rootBefore + " (after:) " + rootAfter,
                rootBefore, rootAfter);
    }
}
