package com.amaze.filemanager.test;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.utils.files.CryptUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.multidex.ShadowMultiDex;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowMultiDex.class, ShadowCryptUtil.class})
public class ShadowCryptUtilTest {

    @Test
    public void testEncryptDecrypt() throws GeneralSecurityException, IOException {
        String text = "test";
        String encrypted = CryptUtil.encryptPassword(RuntimeEnvironment.application, text);
        assertEquals(text, CryptUtil.decryptPassword(RuntimeEnvironment.application, encrypted));
    }

    @Test
    public void testWithUtilsHandler() throws GeneralSecurityException, IOException {
        UtilsHandler utilsHandler = new UtilsHandler(RuntimeEnvironment.application);
        utilsHandler.onCreate(utilsHandler.getWritableDatabase());

        String fingerprint = "00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff";
        String url = "ssh://test:test@127.0.0.1:22";

        utilsHandler.addSsh("Test", SshClientUtils.encryptSshPathAsNecessary(url), fingerprint, null, null);
        assertEquals(fingerprint, utilsHandler.getSshHostKey(url));
    }
}
