package com.amaze.filemanager.filesystem.ssh.test;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.amaze.filemanager.database.UtilitiesDatabase;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;

import net.schmizz.sshj.common.SecurityUtils;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;

public class TestUtils {

    public static KeyPair createKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    public static void saveSshConnectionSettings(
            @NonNull KeyPair hostKeyPair,
            @NonNull String validUsername,
            @Nullable String validPassword,
            @Nullable PrivateKey privateKey) {
        UtilitiesDatabase utilitiesDatabase = UtilitiesDatabase.initialize(ApplicationProvider.getApplicationContext());
        UtilsHandler utilsHandler = new UtilsHandler(ApplicationProvider.getApplicationContext(), utilitiesDatabase);

        String privateKeyContents = null;
        if (privateKey != null) {
            StringWriter writer = new StringWriter();
            JcaPEMWriter jw = new JcaPEMWriter(writer);
            try {
                jw.writeObject(privateKey);
                jw.flush();
                jw.close();
            } catch (IOException shallNeverHappen) {
            }
            privateKeyContents = writer.toString();
        }

        StringBuilder fullUri = new StringBuilder().append("ssh://").append(validUsername);

        if (validPassword != null) fullUri.append(':').append(validPassword);

        fullUri.append("@127.0.0.1:22222");

        if (validPassword != null)
            utilsHandler.saveToDatabase(
                    new OperationData(
                            UtilsHandler.Operation.SFTP,
                            SshClientUtils.encryptSshPathAsNecessary(fullUri.toString()),
                            "Test",
                            SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
                            null,
                            null));
        else
            utilsHandler.saveToDatabase(
                    new OperationData(
                            UtilsHandler.Operation.SFTP,
                            SshClientUtils.encryptSshPathAsNecessary(fullUri.toString()),
                            "Test",
                            SecurityUtils.getFingerprint(hostKeyPair.getPublic()),
                            "id_rsa",
                            privateKeyContents));

    }
}
