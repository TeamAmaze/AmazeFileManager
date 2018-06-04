package com.amaze.filemanager.ui.icons;

import android.webkit.MimeTypeMap;

import com.amaze.filemanager.BuildConfig;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMimeTypeMap;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class IconsTest {

    @Before
    public void setUp(){
        //By default Robolectric's MimeTypeMap is empty, we need to populate them
        ShadowMimeTypeMap mimeTypeMap = Shadows.shadowOf(MimeTypeMap.getSingleton());
        mimeTypeMap.addExtensionMimeTypMapping("zip", "application/zip");
        mimeTypeMap.addExtensionMimeTypMapping("rar", "application/x-rar-compressed");
        mimeTypeMap.addExtensionMimeTypMapping("tar", "application/x-tar");
    }

    @Test
    public void testReturnArchiveTypes(){
        assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.zip", false));
        assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.rar", false));
        assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar", false));
        assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.gz", false));
        assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.lzma", false));
        assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.xz", false));
        assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.bz2", false));

    }
}
