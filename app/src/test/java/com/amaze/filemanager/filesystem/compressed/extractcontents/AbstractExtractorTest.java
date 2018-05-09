package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.os.Build;
import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.compressed.TestArchives;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class}, minSdk = Build.VERSION_CODES.KITKAT)
public abstract class AbstractExtractorTest {

    protected abstract Extractor createExtractor();

    protected abstract String getArchiveType();

    @Before
    public void setUp() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        TestArchives.init(RuntimeEnvironment.application);
    }

    @Test
    public void testExtractFiles(){
        Extractor extractor = createExtractor();
    }

    private void copyArchiveToStorage() throws IOException{
        IOUtils.copy(new ByteArrayInputStream(TestArchives.readArchive(getArchiveType())), new FileOutputStream(getArchiveFile()));
    }

    private File getArchiveFile() {
        return new File(Environment.getExternalStorageDirectory(), "test-archive." + getArchiveType());
    }
}
