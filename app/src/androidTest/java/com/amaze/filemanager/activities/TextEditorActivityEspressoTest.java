package com.amaze.filemanager.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextEditorActivityEspressoTest {

    private static final String FILE_CONTENT = "This is a test";

    @Rule
    public ActivityTestRule<TextEditorActivity> activityRule = new ActivityTestRule<>(TextEditorActivity.class, true, false);

    private Context context;

    private Uri uri;

    private File file;

    @Before
    public void setUp() throws IOException {
        context = InstrumentationRegistry.getTargetContext();

        file = File.createTempFile("TEST_AMAZE_", ".txt");
        android.util.Log.d("TEST", "File created at " + file.getAbsolutePath());
        Writer writer = new FileWriter(file);
        writer.write(FILE_CONTENT);
        writer.close();
        uri = Uri.fromFile(file);
    }

    @After
    public void tearDown()
    {
        if(file.exists())
            file.delete();
        assertFalse(file.exists());
    }

    @Test
    public void testOpenFile() throws Exception {
        Intent intent = new Intent(context, TextEditorActivity.class)
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setType("text/plain")
            .setData(uri);
        activityRule.launchActivity(intent);
        assertNotEquals("", activityRule.getActivity().mInput.getText().toString());
        assertNotEquals("foobar", activityRule.getActivity().mInput.getText().toString());
        assertEquals(FILE_CONTENT, activityRule.getActivity().mInput.getText().toString().trim());
        //Add extra time for you to see the Activity did load, and text is actually there
        //Thread.sleep(1000);
    }
}
