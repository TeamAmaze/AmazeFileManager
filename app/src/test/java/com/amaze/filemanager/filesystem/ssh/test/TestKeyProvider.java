package com.amaze.filemanager.filesystem.ssh.test;

import org.apache.sshd.common.keyprovider.KeyPairProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Collections;

public class TestKeyProvider implements KeyPairProvider {

    private KeyPair keyPair;

    public TestKeyProvider() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(1024, new SecureRandom());
        keyPair = keyPairGenerator.generateKeyPair();
    }

    @Override
    public Iterable<KeyPair> loadKeys() {
        return Collections.singleton(keyPair);
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
