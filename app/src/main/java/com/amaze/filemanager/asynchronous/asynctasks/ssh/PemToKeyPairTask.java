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

package com.amaze.filemanager.asynchronous.asynctasks.ssh;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.ui.views.WarnableTextInputLayout;
import com.amaze.filemanager.ui.views.WarnableTextInputValidator;
import com.amaze.filemanager.utils.application.AppConfig;
import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile;

import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyPair;

/**
 * {@link AsyncTask} to convert given {@link InputStream} into {@link KeyPair} which is requird by
 * sshj, using {@link JcaPEMKeyConverter}.
 *
 * @see JcaPEMKeyConverter
 * @see KeyProvider
 * @see OpenSSHKeyV1KeyFile
 * @see PuTTYKeyFile
 * @see com.amaze.filemanager.filesystem.ssh.SshConnectionPool#create(String)
 * @see net.schmizz.sshj.SSHClient#authPublickey(String, KeyProvider...)
 */
public class PemToKeyPairTask extends AsyncTask<Void, IOException, KeyPair>
{
    private final PemToKeyPairConverter[] converters = {
        new JcaPemToKeyPairConverter(),
        new OpenSshPemToKeyPairConverter(),
        new OpenSshV1PemToKeyPairConverter(),
        new PuttyPrivateKeyToKeyPairConverter()
    };

    private boolean paused = false;

    private PasswordFinder passwordFinder;

    private String errorMessage;

    private final byte[] pemFile;

    private final AsyncTaskResult.Callback<KeyPair> callback;

    public PemToKeyPairTask(@NonNull InputStream pemFile, AsyncTaskResult.Callback<KeyPair> callback) throws IOException {
        this(IOUtils.readFully(pemFile).toByteArray(), callback);
    }

    public PemToKeyPairTask(@NonNull String pemContent, AsyncTaskResult.Callback<KeyPair> callback) {
        this(pemContent.getBytes(), callback);
    }

    private PemToKeyPairTask(@NonNull byte[] pemContent, AsyncTaskResult.Callback<KeyPair> callback) {
        this.pemFile = pemContent;
        this.callback = callback;
    }

    @Override
    protected KeyPair doInBackground(Void... voids) {
        while(true) {
            if (isCancelled()) return null;
            if (paused) continue;

            for (PemToKeyPairConverter converter : converters) {
                KeyPair keyPair = converter.convert(new String(pemFile));
                if (keyPair != null) {
                    paused = false;
                    return keyPair;
                }
            }

            if (this.passwordFinder != null) {
                this.errorMessage = AppConfig.getInstance().getString(R.string.ssh_key_invalid_passphrase);
            }

            paused = true;
            publishProgress(new IOException("No converter available to parse selected PEM"));
        }
    }

    @Override
    protected void onProgressUpdate(IOException... values) {
        super.onProgressUpdate(values);
        if (values.length < 1) return;

        IOException result = values[0];
        MaterialDialog.Builder builder = new MaterialDialog.Builder(AppConfig.getInstance().getMainActivityContext());
        View dialogLayout = View.inflate(AppConfig.getInstance().getMainActivityContext(), R.layout.dialog_singleedittext, null);
        WarnableTextInputLayout wilTextfield = dialogLayout.findViewById(R.id.singleedittext_warnabletextinputlayout);
        EditText textfield = dialogLayout.findViewById(R.id.singleedittext_input);
        textfield.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        builder.customView(dialogLayout, false)
                .autoDismiss(false)
                .title(R.string.ssh_key_prompt_passphrase)
                .positiveText(R.string.ok)
                .onPositive(((dialog, which) -> {
                    this.passwordFinder = new PasswordFinder() {
                        @Override
                        public char[] reqPassword(Resource<?> resource) {
                            return textfield.getText().toString().toCharArray();
                        }

                        @Override
                        public boolean shouldRetry(Resource<?> resource) {
                            return false;
                        }
                    };
                    this.paused = false;
                    dialog.dismiss();
                })).negativeText(R.string.cancel)
                .onNegative(((dialog, which) -> {
                    dialog.dismiss();
                    toastOnParseError(result);
                    cancel(true);
                }));

        MaterialDialog dialog = builder.show();

        new WarnableTextInputValidator(AppConfig.getInstance().getMainActivityContext(), textfield,
                wilTextfield, dialog.getActionButton(DialogAction.POSITIVE), (text) -> {
            if (text.length() < 1) {
                return new WarnableTextInputValidator.ReturnState(WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
            }
            return new WarnableTextInputValidator.ReturnState();
        });

        if (errorMessage != null) {
            wilTextfield.setError(errorMessage);
            textfield.selectAll();
        }
    }

    @Override
    protected void onPostExecute(KeyPair result) {
        if(callback != null) {
            callback.onResult(result);
        }
    }

    private void toastOnParseError(IOException result){
        Toast.makeText(AppConfig.getInstance().getMainActivityContext(),
                AppConfig.getInstance().getResources().getString(R.string.ssh_pem_key_parse_error,
                        result.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    private abstract class PemToKeyPairConverter {
        KeyPair convert(String source) {
            try {
                return throwingConvert(source);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected abstract KeyPair throwingConvert(String source) throws Exception;
    }

    private class JcaPemToKeyPairConverter extends PemToKeyPairConverter {
        @Override
        public KeyPair throwingConvert(String source) throws Exception {
            PEMParser pemParser = new PEMParser(new StringReader(source));
            PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getKeyPair(keyPair);
        }
    }

    private class OpenSshPemToKeyPairConverter extends PemToKeyPairConverter {
        @Override
        public KeyPair throwingConvert(String source) throws Exception {
            OpenSSHKeyFile converter = new OpenSSHKeyFile();
            converter.init(new StringReader(source), passwordFinder);
            return new KeyPair(converter.getPublic(), converter.getPrivate());
        }
    }

    private class OpenSshV1PemToKeyPairConverter extends PemToKeyPairConverter {
        @Override
        public KeyPair throwingConvert(String source) throws Exception {
            OpenSSHKeyV1KeyFile converter = new OpenSSHKeyV1KeyFile();
            converter.init(new StringReader(source), passwordFinder);
            return new KeyPair(converter.getPublic(), converter.getPrivate());
        }
    }

    private class PuttyPrivateKeyToKeyPairConverter extends PemToKeyPairConverter {
        @Override
        public KeyPair throwingConvert(String source) throws Exception {
            PuTTYKeyFile converter = new PuTTYKeyFile();
            converter.init(new StringReader(source), passwordFinder);
            return new KeyPair(converter.getPublic(), converter.getPrivate());
        }
    }
}
