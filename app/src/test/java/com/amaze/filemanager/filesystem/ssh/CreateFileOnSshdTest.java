package com.amaze.filemanager.filesystem.ssh;

import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.ssh.test.BlockFileCreationFileSystemProvider;
import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;


public class CreateFileOnSshdTest extends AbstractSftpServerTest {

    @Test
    public void testCreateFileNormal() throws Exception {
        createSshServer(new VirtualFileSystemFactory(Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath())));
    }

    @Test
    public void testCreateFilePermissionDenied() throws Exception{
        createSshServer(new VirtualFileSystemFactory(){
            @Override
            public FileSystem createFileSystem(Session session) throws IOException {
            return new BlockFileCreationFileSystemProvider().newFileSystem(Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath()), Collections.emptyMap());
            }
        });
    }
}
