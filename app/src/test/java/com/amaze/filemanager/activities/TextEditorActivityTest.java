package com.amaze.filemanager.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.TextView;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.application.AppConfig;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class TextEditorActivityTest {

    private final String fileContents = "fsdfsdfs";
    private TextView text;

    @After
    public void tearDown(){
        AppConfig.getInstance().onTerminate();
    }

    @Test
    public void testOpenFileUri() throws IOException {
        File file = simulateFile();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(file));
        generateActivity(intent);

        assertThat(text.getText().toString(), is(fileContents + "\n"));
    }

    @Test
    public void testOpenContentUri() throws Exception {
        Uri uri = Uri.parse("content://foo.bar.test.streamprovider/temp/thisisatest.txt");

        ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();
        ShadowContentResolver shadowContentResolver = Shadows.shadowOf(contentResolver);
        shadowContentResolver.registerInputStream(uri, new ByteArrayInputStream(fileContents.getBytes("UTF-8")));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("text/plain");
        intent.setData(uri);

        generateActivity(intent);
        assertEquals(fileContents, text.getText().toString().trim());
    }

    private void generateActivity(Intent intent) {
        ActivityController<TextEditorActivity> controller = Robolectric.buildActivity(TextEditorActivity.class, intent)
                .create().start().visible();

        TextEditorActivity activity = controller.get();
        text = activity.findViewById(R.id.fname);
        activity.onDestroy();
    }

    private File simulateFile() throws IOException {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        File file = new File(Environment.getExternalStorageDirectory(), "text.txt");

        file.createNewFile();

        if(!file.canWrite()) file.setWritable(true);
        assertThat(file.canWrite(), is(true));

        PrintWriter out = new PrintWriter(file);
        out.write(fileContents);
        out.flush();
        out.close();

        return file;
    }

    private Uri getFileContentUri(Context context, File file) {
        fail("Cannot create content URI");
        return null;
    }

}