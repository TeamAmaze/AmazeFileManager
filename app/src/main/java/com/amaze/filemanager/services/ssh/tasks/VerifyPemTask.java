package com.amaze.filemanager.services.ssh.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;

public class VerifyPemTask extends AsyncTask<Void, Void, KeyPair>
{
    private static final String TAG = "VerifyPemTask";

    private final InputStream pemFile;

    public VerifyPemTask(InputStream pemFile)
    {
        this.pemFile = pemFile;
    }

    @Override
    protected KeyPair doInBackground(Void... voids) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(pemFile);
            PEMParser pemParser = new PEMParser(reader);
            PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            KeyPair retval = converter.getKeyPair(keyPair);
            converter = null;
            keyPair = null;
            pemParser = null;
            return retval;
        } catch (FileNotFoundException e){
            Log.e(TAG, "Unable to open PEM for reading", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException reading PEM", e);
        } finally {
            if(reader != null)
            {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
        }
        return null;
    }
}
