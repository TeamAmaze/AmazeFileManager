package com.amaze.filemanager.database;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.amaze.filemanager.filesystem.ssh.SshClientUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class UtilsHandlerTest {

    private UtilsHandler utilsHandler;

    @Before
    public void setUp(){
        utilsHandler = new UtilsHandler(InstrumentationRegistry.getTargetContext());
        utilsHandler.onCreate(utilsHandler.getWritableDatabase());
        utilsHandler.getWritableDatabase().execSQL("DELETE FROM sftp;");
    }

    @Test
    public void testEncodeEncryptUri1(){
        performTest("ssh://test:testP@ssw0rd@127.0.0.1:5460");
    }

    @Test
    public void testEncodeEncryptUri2(){
        performTest("ssh://test:testP@##word@127.0.0.1:22");
    }

    @Test
    public void testEncodeEncryptUri3(){
        performTest("ssh://test@example.com:testP@ssw0rd@127.0.0.1:22");
    }

    @Test
    public void testEncodeEncryptUri4(){
        performTest("ssh://test@example.com:testP@ssw0##$@127.0.0.1:22");
    }

    private void performTest(@NonNull final String origPath){
        String encryptedPath = SshClientUtils.encryptSshPathAsNecessary(origPath);
        utilsHandler.addSsh("Test", encryptedPath, "00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff", null, null);

        List<String[]> result = utilsHandler.getSftpList();
        assertEquals(1, result.size());
        assertEquals("Test", result.get(0)[0]);
        assertEquals(origPath, result.get(0)[1]);
        assertEquals("00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff", utilsHandler.getSshHostKey(origPath));
    }
}
