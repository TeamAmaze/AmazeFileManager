/*
 * VerifyPemTask.java
 *
 * Copyright © 2017 Raymond Lai <airwave209gt at gmail.com>.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.ssh.tasks;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyPair;

/**
 * {@link AsyncTask} to convert given {@link InputStream} into {@link KeyPair} which is requird by
 * sshj, using {@link JcaPEMKeyConverter}.
 *
 * @see JcaPEMKeyConverter
 * @see KeyProvider
 * @see com.amaze.filemanager.filesystem.ssh.SshConnectionPool#create(Uri)
 * @see net.schmizz.sshj.SSHClient#authPublickey(String, KeyProvider...)
 */
public class PemToKeyPairTask extends AsyncTask<Void, Void, KeyPair>
{
    private static final String TAG = "PemToKeyPairTask";

    private final Reader mPemFile;

    public PemToKeyPairTask(@NonNull InputStream pemFile) {
        this.mPemFile = new InputStreamReader(pemFile);
    }

    public PemToKeyPairTask(@NonNull Reader reader) {
        this.mPemFile = reader;
    }

    @Override
    protected KeyPair doInBackground(Void... voids) {
        try {
            PEMParser pemParser = new PEMParser(mPemFile);
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
            try {
                mPemFile.close();
            } catch (IOException ignored) {}
        }
        return null;
    }
}
