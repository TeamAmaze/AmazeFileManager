package com.amaze.filemanager.filesystem.ssh;

import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.ssh.test.BlockFileCreationFileSystemProvider;
import com.amaze.filemanager.filesystem.ssh.test.TestHostKeyProvider;

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

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class CreateFileOnSshdTest {

    private SshServer server;

    private static TestHostKeyProvider hostKeyProvider;

    @BeforeClass
    public static void bootstrap() throws Exception {
        hostKeyProvider = new TestHostKeyProvider();
    }

    @After
    public void tearDown(){
        if(server != null && server.isOpen())
            server.close(true);
    }

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

    private void createSshServer(FileSystemFactory fileSystemFactory) throws Exception {
        server = SshServer.setUpDefaultServer();

        server.setFileSystemFactory(fileSystemFactory);
        server.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        server.setPort(22222);
        server.setHost("127.0.0.1");
        server.setKeyPairProvider(hostKeyProvider);
        server.setCommandFactory(new ScpCommandFactory());
        server.setSubsystemFactories(Arrays.asList(new SftpSubsystemFactory()));
        server.setPasswordAuthenticator(((username, password, session) -> username.equals("testuser") && password.equals("testpassword")));
        server.start();
    }
}
