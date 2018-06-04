package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import android.content.Context;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.AbstractArchiveTestSupport;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.junit.Assert.*;

public abstract class AbstractDecompressorTest extends AbstractArchiveTestSupport {

    @Test
    public void performTest() throws Exception{
        Decompressor decompressor = decompressorClass().getConstructor(Context.class).newInstance(RuntimeEnvironment.application);
        decompressor.setFilePath(getArchiveFile().getAbsolutePath());
        List<CompressedObjectParcelable> result = decompressor.changePath("test-archive", false, (data) -> {}).execute().get();
        assertNotNull(result);
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/1/", -1, 0, true)));
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/2/", -1, 0, true)));
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/3/", -1, 0, true)));
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/4/", -1, 0, true)));
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/a/", -1, 0, true)));
        result = decompressor.changePath("test-archive/1", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/1/8", -1, 2, false)));
        result = decompressor.changePath("test-archive/2", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/2/7", -1, 3, false)));
        result = decompressor.changePath("test-archive/3", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/3/6", -1, 4, false)));
        result = decompressor.changePath("test-archive/4", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/4/5", -1, 5, false)));
        result = decompressor.changePath("test-archive/a", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/a/b/", -1, 0, true)));
        result = decompressor.changePath("test-archive/a/b", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/a/b/c/", -1, 0, true)));
        result = decompressor.changePath("test-archive/a/b/c", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/a/b/c/d/", -1, 0, true)));
        result = decompressor.changePath("test-archive/a/b/c/d", false, (data) -> {}).execute().get();
        assertTrue(result.contains(new CompressedObjectParcelable("test-archive/a/b/c/d/lipsum.bin", -1, 512, false)));
    }

    protected abstract Class<? extends Decompressor> decompressorClass();
}
