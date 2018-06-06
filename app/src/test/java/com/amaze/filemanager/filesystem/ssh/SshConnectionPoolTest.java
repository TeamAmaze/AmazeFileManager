package com.amaze.filemanager.filesystem.ssh;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider;

import net.schmizz.sshj.common.SecurityUtils;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class})
public class SshConnectionPoolTest {

    private static SshServer server;

    private static TestKeyProvider hostKeyProvider, userKeyProvider;

    @BeforeClass
    public static void bootstrap() throws Exception {
        hostKeyProvider = new TestKeyProvider();
        userKeyProvider = new TestKeyProvider();
        createSshServer();
    }

    @AfterClass
    public static void shutdownServer(){
        if(server != null && server.isOpen())
            server.close(true);
    }

    @Test
    public void testGetConnectionWithUsernameAndPassword(){
        assertNotNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "testuser", "testpassword", null));

        assertNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "invaliduser", "invalidpassword", null));
    }

    @Test
    public void testGetConnectionWithUsernameAndKey(){
        assertNotNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "testuser", null, userKeyProvider.getKeyPair()));

        assertNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "invaliduser", null, userKeyProvider.getKeyPair()));
    }

    private static void createSshServer() throws Exception {
        server = SshServer.setUpDefaultServer();
        server.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        server.setPort(22222);
        server.setHost("127.0.0.1");
        server.setKeyPairProvider(hostKeyProvider);
        server.setPasswordAuthenticator(((username, password, session) -> username.equals("testuser") && password.equals("testpassword")));
        server.setPublickeyAuthenticator((username, key, session) -> username.equals("testuser") && key.equals(userKeyProvider.getKeyPair().getPublic()));
        server.start();
    }
}
