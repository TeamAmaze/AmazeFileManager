package com.amaze.filemanager.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.TextView;

import com.amaze.filemanager.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class TextEditorActivityTest {

    private final String fileContents = "fsdfsdfs";
    private TextView text;

    @Test
    public void testOpenFileUri() {
        File file = simulateFile();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(file));
        generateActivity(intent);

        assertThat(text.getText().toString(), is(fileContents + "\n"));
    }

    @Test
    public void testOpenContentUri() {
        File file = simulateFile();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(getFileContentUri(RuntimeEnvironment.application, file));
        generateActivity(intent);

        assertThat(text.getText().toString(), is(fileContents + "\n"));
    }

    private void generateActivity(Intent intent) {
        ActivityController<TextEditorActivity> controller = Robolectric.buildActivity(TextEditorActivity.class, intent)
                .create().start().visible();

        TextEditorActivity activity = controller.get();
        text = activity.findViewById(R.id.fname);
    }

    private File simulateFile() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        File file = new File("text.txt");

        try {
            file.createNewFile();

            if(!file.canWrite()) file.setWritable(true);
            assertThat(file.canWrite(), is(true));

            PrintWriter out = new PrintWriter(file);
            out.write(fileContents);
            out.flush();
            out.close();
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return file;
    }

    private Uri getFileContentUri(Context context, File file) {
        fail("Cannot create content URI");
        return null;
    }

}