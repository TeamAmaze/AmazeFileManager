package com.amaze.filemanager.filesystem;

import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.test.ShadowShellInteractive;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import eu.chainfire.libsuperuser.Shell;

import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class, ShadowShellInteractive.class})
public class RootHelperTest {

    private static final File sysroot = new File(Environment.getExternalStorageDirectory(), "sysroot");

    private static final List<String> expected = Arrays.asList("srv", "var", "tmp", "bin", "lib", "usr", "1.txt", "2.txt", "3.txt", "4.txt", "symlink1.txt", "symlink2.txt", "symlink3.txt", "symlink4.txt");

    @Before
    public void setUp() throws IOException {
        sysroot.mkdir();
        for(String s: new String[]{"srv","var","tmp"}){
            File subdir = new File(sysroot, s);
            subdir.mkdir();
            Files.createSymbolicLink(Paths.get(new File(Environment.getExternalStorageDirectory(), s).getAbsolutePath()), Paths.get(subdir.getAbsolutePath()));
        }
        for(String s: new String[]{"bin","lib","usr"}){
            new File(Environment.getExternalStorageDirectory(), s).mkdir();
        }
        for(int i=1;i<=4;i++){
            File f = new File(Environment.getExternalStorageDirectory(), i+".txt");
            FileOutputStream out = new FileOutputStream(f);
            out.write(i);
            out.close();
            Files.createSymbolicLink(Paths.get(new File(Environment.getExternalStorageDirectory(), "symlink"+i+".txt").getAbsolutePath()), Paths.get(f.getAbsolutePath()));
        }
    }

    @Test
    public void testNonRoot() throws InterruptedException {
        runVerify(false);
    }

    @Test
    public void testRoot() throws InterruptedException, SecurityException, IllegalArgumentException {
        MainActivity.shellInteractive = new Shell.Builder().setShell("/bin/false").open();
        runVerify(true);
    }

    private void runVerify(boolean root) throws InterruptedException {
        List<String> result = new ArrayList<>();
        CountDownLatch waiter = new CountDownLatch(expected.size());
        RootHelper.getFiles(Environment.getExternalStorageDirectory().getAbsolutePath(), root, true, mode -> {}, file -> {
            if(result.contains(file.getName()))
                fail(file.getName() + " already listed");
            result.add(file.getName());
            waiter.countDown();
        });
        waiter.await();
    }
}
