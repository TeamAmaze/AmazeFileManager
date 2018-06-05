package com.amaze.filemanager.filesystem.ssh;

import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.PathComponents;
import net.schmizz.sshj.sftp.RemoteResourceInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class SshClientUtilsTest {

    // 040755 is special mask for directories. See sshj's unit test for details
    @Test
    public void testDifferentHashCodeByName(){
        PathComponents p1 = new PathComponents(null, "/bin", "/");
        PathComponents p2 = new PathComponents(null, "/lib", "/");
        FileAttributes f1 = new FileAttributes(700, 4096, 1000, 1000, new FileMode(040755), -1, -1, Collections.emptyMap());
        FileAttributes f2 = new FileAttributes(700, 4096, 1000, 1000, new FileMode(040755), -1, -1, Collections.emptyMap());
        RemoteResourceInfo r1 = new RemoteResourceInfo(p1, f1);
        RemoteResourceInfo r2 = new RemoteResourceInfo(p2, f2);

        assertNotEquals(SshClientUtils.hashCode(r1), SshClientUtils.hashCode(r2));
    }

    @Test
    public void testDifferentHashCodeBySize(){
        PathComponents p1 = new PathComponents(null, "/foo/bar/android/test1.txt", "/");
        PathComponents p2 = new PathComponents(null, "/foo/bar/android/test1.txt", "/");
        FileAttributes f1 = new FileAttributes(700, 8132, 1000, 1000, new FileMode(700), -1, -1, Collections.emptyMap());
        FileAttributes f2 = new FileAttributes(700, 8192, 1000, 1000, new FileMode(700), -1, -1, Collections.emptyMap());
        RemoteResourceInfo r1 = new RemoteResourceInfo(p1, f1);
        RemoteResourceInfo r2 = new RemoteResourceInfo(p2, f2);

        assertNotEquals(SshClientUtils.hashCode(r1), SshClientUtils.hashCode(r2));
    }

    @Test
    public void testDifferentHashCodeByType(){
        PathComponents p1 = new PathComponents(null, "/foo/bar/android/test1", "/");
        PathComponents p2 = new PathComponents(null, "/foo/bar/android/test1", "/");
        FileAttributes f1 = new FileAttributes(700, 4096, 1000, 1000, new FileMode(700), -1, -1, Collections.emptyMap());
        FileAttributes f2 = new FileAttributes(700, 4096, 1000, 1000, new FileMode(040755), -1, -1, Collections.emptyMap());
        RemoteResourceInfo r1 = new RemoteResourceInfo(p1, f1);
        RemoteResourceInfo r2 = new RemoteResourceInfo(p2, f2);

        assertNotEquals(SshClientUtils.hashCode(r1), SshClientUtils.hashCode(r2));
    }

    @Test
    public void testIdenticalHashCode(){
        PathComponents p1 = new PathComponents(null, "/foo/bar/android/test1.txt", "/");
        PathComponents p2 = new PathComponents(null, "/foo/bar/android/test1.txt", "/");
        FileAttributes f1 = new FileAttributes(700, 856, 1000, 1000, new FileMode(700), -1, -1, Collections.emptyMap());
        FileAttributes f2 = new FileAttributes(700, 856, 1000, 1000, new FileMode(700), -1, -1, Collections.emptyMap());
        RemoteResourceInfo r1 = new RemoteResourceInfo(p1, f1);
        RemoteResourceInfo r2 = new RemoteResourceInfo(p2, f2);

        assertEquals(SshClientUtils.hashCode(r1), SshClientUtils.hashCode(r2));
    }
}
