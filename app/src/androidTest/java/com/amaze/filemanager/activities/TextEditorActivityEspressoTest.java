package com.amaze.filemanager.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextEditorActivityEspressoTest {

    @Rule
    public ActivityTestRule<TextEditorActivity> activityRule = new ActivityTestRule<>(TextEditorActivity.class, true, false);

    private Context context;

    private Uri uri;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getTargetContext();

        File file = new File("/default.prop");
        uri = Uri.fromFile(file);
    }

    @Test
    public void testOpenFile() throws Exception {
        Intent intent = new Intent(context, TextEditorActivity.class)
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setType("text/plain")
            .setData(uri);
        activityRule.launchActivity(intent);
        CountDownLatch waiter = new CountDownLatch(1);
        while("".equals(activityRule.getActivity().mInput.getText().toString())){
            waiter.await();
        }
        waiter.countDown();
        assertNotEquals("", activityRule.getActivity().mInput.getText());
        assertNotEquals("foobar", activityRule.getActivity().mInput.getText());
        //Add extra time for you to see the Activity did load, and text is actually there
        //Thread.sleep(1000);
    }
}
