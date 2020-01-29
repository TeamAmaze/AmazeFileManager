package com.amaze.filemanager.activities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {

    @Test
    public void testMainActivity() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().pause().destroy();
    }

}
