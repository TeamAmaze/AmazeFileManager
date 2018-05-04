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
    private static final String SHARED_PATH_ROOT = "/storage/sdcard0";

    private FileSystemView fileSystemView;

    @Before
    public void setUp() {
        fileSystemView = new AndroidFileSystemView(RuntimeEnvironment.application, SHARED_PATH_ROOT);
    }

    @After
    public void tearDown() {
        fileSystemView.dispose();
    }

    @Test
    public void testGetFtpFileSimple() throws FtpException {
        FtpFile result = fileSystemView.getFile("/1.txt");
        assertNotNull(result);
        assertEquals(SHARED_PATH_ROOT + "/1.txt", result.getAbsolutePath());
        assertNotNull(result.getPhysicalFile());
        assertTrue(result.isWritable());
    }

    //@Test
    public void testMultipleGetFtpFile() throws FtpException {
        assertNotNull(fileSystemView.getHomeDirectory());
        assertEquals(SHARED_PATH_ROOT, fileSystemView.getHomeDirectory().getAbsolutePath());
        assertNotNull(fileSystemView.getWorkingDirectory());
        assertEquals(SHARED_PATH_ROOT, fileSystemView.getWorkingDirectory().getAbsolutePath());

        assertTrue(fileSystemView.changeWorkingDirectory("/DCIM/Android"));
        assertNotNull(fileSystemView.getWorkingDirectory());
        assertEquals(SHARED_PATH_ROOT + "/DCIM/Android", fileSystemView.getWorkingDirectory().getAbsolutePath());

        assertNotNull(fileSystemView.getFile("/DCIM/Android/DSC10001.jpg"));
        assertEquals(SHARED_PATH_ROOT + "/DCIM/Android/DSC10001.jpg", fileSystemView.getFile("/DCIM/Android/DSC10001.jpg").getAbsolutePath());

        assertTrue(fileSystemView.changeWorkingDirectory("/"));
        assertNotNull(fileSystemView.getWorkingDirectory());
        assertEquals(SHARED_PATH_ROOT, fileSystemView.getWorkingDirectory().getAbsolutePath());
    }
}
