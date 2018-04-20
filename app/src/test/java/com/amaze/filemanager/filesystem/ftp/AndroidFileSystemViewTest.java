package com.amaze.filemanager.filesystem.ftp;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.ftpserver.AndroidFileSystemView;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import static org.junit.Assert.*;

@Config(minSdk = 14, constants = BuildConfig.class, shadows = {
    ShadowMultiDex.class
})
@RunWith(RobolectricTestRunner.class)
public class AndroidFileSystemViewTest
{
    private FileSystemView fileSystemView;

    @Before
    public void setUp() {
        fileSystemView = new AndroidFileSystemView(RuntimeEnvironment.application, "/mnt/sdcard");
    }

    @After
    public void tearDown() {
        fileSystemView.dispose();
    }

    @Test
    public void testGetFtpFile() throws FtpException {
        FtpFile result = fileSystemView.getFile("/mnt/sdcard/1.txt");
        assertNotNull(result);
        assertNotNull(result.getPhysicalFile());
    }
}
