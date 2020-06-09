package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class XzHelperTaskTest2 extends AbstractCompressedHelperTaskTest {

    @Test @Override
    public void testRoot(){
        CompressedHelperTask task = createTask("");
        AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
        assertEquals(result.result.size(), 1);
        assertEquals("compress", result.result.get(0).name);
    }

    @Test @Override
    public void testSublevels(){
        CompressedHelperTask task = createTask("compress");
        AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = task.doInBackground();
        assertEquals(result.result.size(), 3);
        assertEquals("a", result.result.get(0).name);
        assertEquals("bç", result.result.get(1).name);
        assertEquals("r.txt", result.result.get(2).name);
        assertEquals(4, result.result.get(2).size);

        task = createTask("compress/a");
        result = task.doInBackground();
        assertEquals(result.result.size(), 0);

        task = createTask("compress/bç");
        result = task.doInBackground();
        assertEquals(result.result.size(), 1);
        assertEquals("t.txt", result.result.get(0).name);
        assertEquals(6, result.result.get(0).size);
    }

    @Override
    protected CompressedHelperTask createTask(String relativePath) {
        return new XzHelperTask(new File(Environment.getExternalStorageDirectory(),
                "compress.tar.xz").getAbsolutePath(),
                relativePath, false, (data) -> {});
    }
}
