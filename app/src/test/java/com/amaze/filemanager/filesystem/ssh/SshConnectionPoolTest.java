package com.amaze.filemanager.filesystem.ssh;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider;
import com.amaze.filemanager.test.ShadowCryptUtil;

import net.schmizz.sshj.common.SecurityUtils;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class, ShadowCryptUtil.class})
public class SshConnectionPoolTest {

    private SshServer server;

    private UtilsHandler utilsHandler;

    private static TestKeyProvider hostKeyProvider, userKeyProvider;

    @BeforeClass
    public static void bootstrap() throws Exception {
        hostKeyProvider = new TestKeyProvider();
        userKeyProvider = new TestKeyProvider();
    }

    @After
    public void tearDown(){
        if(server != null && server.isOpen())
            server.close(true);
    }

    @Test
    public void testGetConnectionWithUsernameAndPassword() throws IOException {
        createSshServer("testuser", "testpassword");
        assertNotNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "testuser", "testpassword", null));

        assertNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "invaliduser", "invalidpassword", null));
    }

    @Test
    public void testGetConnectionWithUsernameAndKey() throws IOException {
        createSshServer("testuser", null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "testuser", null, userKeyProvider.getKeyPair()));

        assertNull(SshConnectionPool.getInstance().getConnection("127.0.0.1", 22222,
                SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()),
                "invaliduser", null, userKeyProvider.getKeyPair()));
    }

    @Test
    public void testGetConnectionWithUrl() throws IOException {
        String validPassword = "testpassword";
        createSshServer("testuser", validPassword);
        saveSshConnectionSettings("testuser", validPassword, null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://testuser:testpassword@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
    }

    @Test
    public void testGetConnectionWithUrlAndKeyAuth() throws IOException {
        createSshServer("testuser", null);
        saveSshConnectionSettings("testuser", null, userKeyProvider.getKeyPair().getPrivate());
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://testuser@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser@127.0.0.1:22222"));
    }

    @Test
    public void testGetConnectionWithUrlHavingComplexPassword1() throws IOException {
        String validPassword = "testP@ssw0rd";
        createSshServer("testuser", validPassword);
        saveSshConnectionSettings("testuser", validPassword, null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://testuser:testP@ssw0rd@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
    }

    @Test
    public void testGetConnectionWithUrlHavingComplexPassword2() throws IOException {
        String validPassword = "testP@##word";
        createSshServer("testuser", validPassword);
        saveSshConnectionSettings("testuser", validPassword, null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
    }

    @Test
    public void testGetConnectionWithUrlHavingComplexCredential1() throws IOException {
        String validPassword = "testP@##word";
        createSshServer("testuser", validPassword);
        saveSshConnectionSettings("testuser", validPassword, null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
    }

    @Test
    public void testGetConnectionWithUrlHavingComplexCredential2() throws IOException {
        String validPassword = "testP@##word";
        createSshServer("testuser", validPassword);
        saveSshConnectionSettings("testuser", validPassword, null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://testuser:testP@##word@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
    }

    @Test
    public void testGetConnectionWithUrlHavingComplexCredential3() throws IOException {
        String validUsername = "test@example.com";
        String validPassword = "testP@ssw0rd";
        createSshServer(validUsername, validPassword);
        saveSshConnectionSettings(validUsername, validPassword, null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://test@example.com:testP@ssw0rd@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
    }

    @Test
    public void testGetConnectionWithUrlHavingComplexCredential4() throws IOException {
        String validUsername = "test@example.com";
        String validPassword = "testP@ssw0##$";
        createSshServer(validUsername, validPassword);
        saveSshConnectionSettings(validUsername, validPassword, null);
        assertNotNull(SshConnectionPool.getInstance().getConnection("ssh://test@example.com:testP@ssw0##$@127.0.0.1:22222"));
        assertNull(SshConnectionPool.getInstance().getConnection("ssh://invaliduser:invalidpassword@127.0.0.1:22222"));
    }

    private void createSshServer(@NonNull String validUsername, @Nullable String validPassword) throws IOException {
        server = SshServer.setUpDefaultServer();
        server.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        server.setPort(22222);
        server.setHost("127.0.0.1");
        server.setKeyPairProvider(hostKeyProvider);
        if(validPassword != null)
            server.setPasswordAuthenticator(((username, password, session) -> username.equals(validUsername) && password.equals(validPassword)));
        server.setPublickeyAuthenticator((username, key, session) -> username.equals(validUsername) && key.equals(userKeyProvider.getKeyPair().getPublic()));
        server.start();
    }

    private void saveSshConnectionSettings(@NonNull String validUsername, @Nullable String validPassword, @Nullable PrivateKey privateKey) {
        utilsHandler = new UtilsHandler(RuntimeEnvironment.application);
        utilsHandler.onCreate(utilsHandler.getWritableDatabase());

        //FIXME: privateKeyContents created this way cannot be parsed back in PemToKeyPairTask
        String privateKeyContents = null;
        if(privateKey != null){
            StringWriter writer = new StringWriter();
            JcaPEMWriter jw = new JcaPEMWriter(writer);
            try {
                jw.writeObject(userKeyProvider.getKeyPair().getPrivate());
                jw.flush();
                jw.close();
            } catch(IOException shallNeverHappen){}
            privateKeyContents = writer.toString();
        }

        StringBuilder fullUri = new StringBuilder()
            .append("ssh://").append(validUsername);

        if(validPassword != null)
            fullUri.append(':').append(validPassword);

        fullUri.append("@127.0.0.1:22222");

        if(validPassword != null)
            utilsHandler.addSsh("test", SshClientUtils.encryptSshPathAsNecessary(fullUri.toString()), SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()), null, null);
        else
            utilsHandler.addSsh("test", fullUri.toString(), SecurityUtils.getFingerprint(hostKeyProvider.getKeyPair().getPublic()), "id_rsa", privateKeyContents);
    }
}
