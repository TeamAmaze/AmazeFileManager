package com.amaze.filemanager.activities;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class MainActivityTest {

    @Test
    public void testMainActivity() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().pause().destroy();
    }

}
