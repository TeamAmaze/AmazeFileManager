package com.amaze.filemanager.filesystem.ftp;

import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.ftpserver.AndroidFtpFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

@Config(minSdk = 14, constants = BuildConfig.class, shadows = {
    ShadowMultiDex.class
})
@RunWith(RobolectricTestRunner.class)
public class AndroidFtpFileTest
{
    @Test
    public void testCreate()
    {
        DocumentFile documentFile = DocumentFile.fromSingleUri(RuntimeEnvironment.application, Uri.parse("file:///mnt/sdcard/1.txt"));
        AndroidFtpFile file = new AndroidFtpFile(RuntimeEnvironment.application, documentFile);
        System.err.println(file.getAbsolutePath());
    }
}
