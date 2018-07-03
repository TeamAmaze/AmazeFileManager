package com.amaze.filemanager.filesystem.ssh;

import android.os.Environment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public abstract class AbstractSftpServerTest {

    protected SshServer server;

    protected static TestKeyProvider hostKeyProvider;

    @BeforeClass
    public static void bootstrap() throws Exception {
        hostKeyProvider = new TestKeyProvider();
    }

    @Before
    public void setUp() throws IOException {
        createSshServer(new VirtualFileSystemFactory(Paths.get(Environment.getExternalStorageDirectory().getAbsolutePath())));
        prepareSshConnection();
    }

    @After
    public void tearDown(){
        SshConnectionPool.getInstance().expungeAllConnections();
        if(server != null && server.isOpen())
            server.close(true);
    }

    protected final void prepareSshConnection() {
        String hostFingerprint = KeyUtils.getFingerPrint(hostKeyProvider.getKeyPair().getPublic());
        SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222, hostFingerprint, "testuser", "testpassword", null);
    }

    protected final void createSshServer(FileSystemFactory fileSystemFactory) throws IOException {
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
