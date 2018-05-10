package com.amaze.filemanager.filesystem.compressed;

import android.os.Environment;

import com.amaze.filemanager.BuildConfig;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
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
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public abstract class AbstractArchiveTestSupport {
    protected abstract String getArchiveType();

    @Before
    public void setUp() throws Exception {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        TestArchives.init(RuntimeEnvironment.application);
        copyArchiveToStorage();
    }

    private final void copyArchiveToStorage() throws IOException {
        IOUtils.copy(new ByteArrayInputStream(TestArchives.readArchive(getArchiveType())), new FileOutputStream(getArchiveFile()));
    }

    protected final File getArchiveFile() {
        return new File(Environment.getExternalStorageDirectory(), "test-archive." + getArchiveType());
    }
}
