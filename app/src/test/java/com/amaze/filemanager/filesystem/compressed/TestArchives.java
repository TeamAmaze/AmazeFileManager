package com.amaze.filemanager.filesystem.compressed;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.apache.commons.compress.utils.IOUtils;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public abstract class TestArchives {

    private static final String[] ARCHIVE_TYPES = {"tar.gz", "zip", "tar", "rar"};

    private static final ClassLoader classLoader = TestArchives.class.getClassLoader();

    public static void init(Context context) {
        for (String type: ARCHIVE_TYPES) {
            readArchive(context, type);
        }
    }

    public static byte[] readArchive(String type) throws IOException {
        return IOUtils.toByteArray(classLoader.getResourceAsStream("test-archive." + type));
    }

    private static void readArchive(Context context, String type) {
        try {
            Uri uri = Uri.parse("content://foo.bar.test.streamprovider/temp/test-archive." + type);

            ContentResolver contentResolver = context.getContentResolver();
            ShadowContentResolver shadowContentResolver = Shadows.shadowOf(contentResolver);
            shadowContentResolver.registerInputStream(uri, new ByteArrayInputStream(readArchive(type)));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}