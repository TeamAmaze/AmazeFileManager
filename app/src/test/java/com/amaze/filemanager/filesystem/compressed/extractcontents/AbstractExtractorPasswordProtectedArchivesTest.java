package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.os.Environment;

import com.amaze.filemanager.filesystem.compressed.ArchivePasswordCache;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;

public abstract class AbstractExtractorPasswordProtectedArchivesTest extends AbstractExtractorTest {

    @Test(expected = IOException.class)
    public void testExtractFilesWithoutPassword() throws Exception {
        ArchivePasswordCache.getInstance().clear();
        try {
            doTestExtractFiles();
        } catch(IOException e) {
            assertExceptionIsExpected(e);
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void testExtractFilesWithWrongPassword() throws Exception {
        ArchivePasswordCache.getInstance().clear();
        ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "abcdef");
        try {
            doTestExtractFiles();
        } catch(IOException e) {
            assertExceptionIsExpected(e);
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void testExtractFilesWithRepeatedWrongPassword() throws Exception {
        ArchivePasswordCache.getInstance().clear();
        ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "abcdef");
        try {
            doTestExtractFiles();
        } catch(IOException e) {
            assertExceptionIsExpected(e);
            throw e;
        }
        ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "pqrstuv");
        try {
            doTestExtractFiles();
        } catch(IOException e) {
            assertExceptionIsExpected(e);
            throw e;
        }
    }

    @Test
    @Override
    public void testExtractFiles() throws Exception {
        ArchivePasswordCache.getInstance().put(getArchiveFile().getAbsolutePath(), "123456");
        doTestExtractFiles();
    }

    @Override
    protected File getArchiveFile() {
        return new File(Environment.getExternalStorageDirectory(), "test-archive-encrypted." + getArchiveType());
    }

    protected abstract Class[] expectedRootExceptionClass();

    protected void assertExceptionIsExpected(IOException e) throws IOException{
        for(Class<? extends Throwable> c : expectedRootExceptionClass()){
            if(e.getCause() != null ? (c.isAssignableFrom(e.getCause().getClass())) :
                    c.isAssignableFrom(e.getClass()))
                return;
        }
        fail("Exception verification failed.");
        throw e;
    }
}
