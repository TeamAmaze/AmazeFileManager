package com.amaze.filemanager.utils.files;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.amaze.filemanager.utils.test.DummyFileGenerator;
import com.amaze.filemanager.utils.ProgressHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class GenericCopyUtilEspressoTest {

    private GenericCopyUtil copyUtil;

    private File file1, file2;

    @Before
    public void setUp() throws IOException {
        copyUtil = new GenericCopyUtil(InstrumentationRegistry.getTargetContext(), new ProgressHandler());
        file1 = File.createTempFile("test", "bin");
        file2 = File.createTempFile("test", "bin");
        file1.deleteOnExit();
        file2.deleteOnExit();
    }

    @Test
    public void testCopyFile1() throws IOException, NoSuchAlgorithmException
    {
        doTestCopyFile1(512);
        doTestCopyFile1(187139366);
    }

    @Test
    public void testCopyFile2() throws IOException, NoSuchAlgorithmException
    {
        doTestCopyFile2(512);
        doTestCopyFile2(187139366);
    }

    @Test
    public void testCopyFile3() throws IOException, NoSuchAlgorithmException
    {
        doTestCopyFile3(512);
        doTestCopyFile3(187139366);
    }

    //doCopy(ReadableByteChannel in, WritableByteChannel out)
    private void doTestCopyFile1(int size) throws IOException, NoSuchAlgorithmException{
        byte[] checksum = DummyFileGenerator.createFile(file1, size);
        copyUtil.doCopy(new FileInputStream(file1).getChannel(), Channels.newChannel(new FileOutputStream(file2)));
        assertEquals(file1.length(), file2.length());
        assertSha1Equals(checksum, file2);
    }

    //copy(FileChannel in, FileChannel out)
    private void doTestCopyFile2(int size) throws IOException, NoSuchAlgorithmException{
        byte[] checksum = DummyFileGenerator.createFile(file1, size);
        copyUtil.copyFile(new FileInputStream(file1).getChannel(), new FileOutputStream(file2).getChannel());
        assertEquals(file1.length(), file2.length());
        assertSha1Equals(checksum, file2);
    }

    //copy(BufferedInputStream in, BufferedOutputStream out)
    private void doTestCopyFile3(int size) throws IOException, NoSuchAlgorithmException{
        byte[] checksum = DummyFileGenerator.createFile(file1, size);
        copyUtil.copyFile(new BufferedInputStream(new FileInputStream(file1)), new BufferedOutputStream(new FileOutputStream(file2)));
        assertEquals(file1.length(), file2.length());
        assertSha1Equals(checksum, file2);
    }

    private void assertSha1Equals(byte[] expected, File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        DigestInputStream in = new DigestInputStream(new FileInputStream(file), md);
        byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
        while (in.read(buffer) > -1) {}
        in.close();
        assertArrayEquals(expected, md.digest());
    }
}
