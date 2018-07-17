package com.amaze.filemanager.activities;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.test.ShadowContentResolverWithLocalOutputStreamSupport;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jahirfiquitiva.libs.fabsmenu.FABsMenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class, ShadowContentResolverWithLocalOutputStreamSupport.class})
public class ShareContentIntentTest {

    private static final String content = "Test content";

    private static final Uri uri = Uri.parse("content://foo.bar.test.streamprovider/temp/thisisatest.txt");

    private MainActivity activity;

    @Before
    public void setUp() throws IOException {
        activity = Robolectric.setupActivity(MainActivity.class);
        activity.onResume();
        setupContentResolver();
    }

    @Test
    public void testNoContentIntent(){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setClipData(null);
        intent.setType("application/octet-stream");
        activity.onNewIntent(intent);

        assertNotNull(ShadowToast.getLatestToast());
        assertEquals(activity.getString(R.string.no_content_for_saving_intent), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testSaveUriInContent() throws IOException, InterruptedException {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("text/plain");

        performShareTest(intent);
        verifyFileAndContent("thisisatest.txt");
    }

    @Test
    public void testSaveUriInExtraText() throws IOException, InterruptedException {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, uri.toString());
        intent.setType("text/plain");

        performShareTest(intent);
        verifyFileAndContent("thisisatest.txt");
    }

    @Test
    public void testSaveContentInClipData() throws IOException, InterruptedException {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setClipData(new ClipData(new ClipDescription("Test ClipData description", new String[]{"text/plain"}), new ClipData.Item(content)));
        performShareTest(intent);
        performSaveAsContent();
    }

    @Test
    public void testSaveContentInExtraText() throws IOException, InterruptedException {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, content);
        performShareTest(intent);
        performSaveAsContent();
    }

    private void setupContentResolver() throws IOException {

        ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();
        ShadowContentResolverWithLocalOutputStreamSupport shadowContentResolver = Shadow.extract(contentResolver);
        shadowContentResolver.registerInputStream(uri, new ByteArrayInputStream(content.getBytes("UTF-8")));
    }

    private void performShareTest(Intent intent) {
        activity.onNewIntent(intent);

        //Expect no error Toast pops up
        assertNull(ShadowToast.getLatestToast());

        //Expect FAB button turns to save button
        FABsMenu fabButton = activity.findViewById(R.id.fabs_menu);
        assertEquals(View.VISIBLE, fabButton.getVisibility());
        assertEquals(R.drawable.ic_file_download_white_24dp, Shadows.shadowOf(fabButton.getMenuButtonIcon()).getCreatedFromResId());
        fabButton.getMenuButton().performClick();
    }

    private void performSaveAsContent() throws IOException, InterruptedException {
        //Save as dialog pops up, input test.txt
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(MaterialDialog.class.isAssignableFrom(dialog.getClass()));
        MaterialDialog createFileDialog = (MaterialDialog)dialog;
        assertEquals(activity.getString(R.string.newfile), createFileDialog.getTitleView().getText());
        assertNotNull(createFileDialog.getCustomView().findViewById(R.id.singleedittext_input));
        EditText editText = createFileDialog.getCustomView().findViewById(R.id.singleedittext_input);
        editText.setText("test.txt", TextView.BufferType.NORMAL);
        createFileDialog.getActionButton(DialogAction.POSITIVE).performClick();
        verifyFileAndContent("test.txt");
    }

    private void verifyFileAndContent(String filename) throws IOException, InterruptedException {
        //Expect content is saved to file
        File verify = new File(Environment.getExternalStorageDirectory(), filename);
        while(true){
            if(verify.exists()) {
                assertEquals(content, new String(IOUtils.toByteArray(new FileInputStream(verify)), "utf-8"));
                return;
            }
        }
    }
}
