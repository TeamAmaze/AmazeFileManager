package com.amaze.filemanager.ssh;

import android.support.test.runner.AndroidJUnit4;

import com.amaze.filemanager.filesystem.ssh.SshClientUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SshClientUtilTest {

    @Test
    public void testEncryptDecryptUriWithNoPassword(){
        String uri = "ssh://testuser@127.0.0.1:22";
        assertEquals(uri, SshClientUtils.encryptSshPathAsNecessary(uri));
        assertEquals(uri, SshClientUtils.decryptSshPathAsNecessary(uri));
    }

    @Test
    public void testEncryptDecryptPasswordWithMinusSign1(){
        String uri = "ssh://testuser:abcd-efgh@127.0.0.1:22";
        String result = SshClientUtils.encryptSshPathAsNecessary(uri);
        assertTrue(result.contains("ssh://testuser:"));
        assertTrue(result.contains("@127.0.0.1:22"));
        String verify = SshClientUtils.decryptSshPathAsNecessary(result);
        assertEquals(uri, verify);
    }

    @Test
    public void testEncryptDecryptPasswordWithMinusSign2(){
        String uri = "ssh://testuser:---------------@127.0.0.1:22";
        String result = SshClientUtils.encryptSshPathAsNecessary(uri);
        assertTrue(result.contains("ssh://testuser:"));
        assertTrue(result.contains("@127.0.0.1:22"));
        String verify = SshClientUtils.decryptSshPathAsNecessary(result);
        assertEquals(uri, verify);
    }

    @Test
    public void testEncryptDecryptPasswordWithMinusSign3(){
        String uri = "ssh://testuser:--agdiuhdpost15@127.0.0.1:22";
        String result = SshClientUtils.encryptSshPathAsNecessary(uri);
        assertTrue(result.contains("ssh://testuser:"));
        assertTrue(result.contains("@127.0.0.1:22"));
        String verify = SshClientUtils.decryptSshPathAsNecessary(result);
        assertEquals(uri, verify);
    }

    @Test
    public void testEncryptDecryptPasswordWithMinusSign4(){
        String uri = "ssh://testuser:t-h-i-s-i-s-p-a-s-s-w-o-r-d-@127.0.0.1:22";
        String result = SshClientUtils.encryptSshPathAsNecessary(uri);
        assertTrue(result.contains("ssh://testuser:"));
        assertTrue(result.contains("@127.0.0.1:22"));
        String verify = SshClientUtils.decryptSshPathAsNecessary(result);
        assertEquals(uri, verify);
    }
}
