/*
 * SftpConnectDialog.java
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

package com.amaze.filemanager.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.services.ssh.SshClientUtils;
import com.amaze.filemanager.services.ssh.SshConnectionPool;
import com.amaze.filemanager.services.ssh.tasks.AsyncTaskResult;
import com.amaze.filemanager.services.ssh.tasks.PemToKeyPairTask;
import com.amaze.filemanager.services.ssh.tasks.SshAuthenticationTask;
import com.amaze.filemanager.services.ssh.tasks.GetSshHostFingerprintTask;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.ExecutionException;

/**
 * SSH/SFTP connection setup dialog.
 */
public class SftpConnectDialog extends DialogFragment {
    private static final String TAG = "SftpConnectDialog";

    //Idiotic code
    //FIXME: agree code on
    private static final int SELECT_PEM_INTENT = 0x01010101;

    private UtilitiesProviderInterface mUtilsProvider;

    private UtilsHandler nUtilsHandler;

    private Context mContext;

    private Uri mSelectedPem = null;

    private KeyPair mSelectedParsedKeyPair = null;

    private String mSelectedParsedKeyPairName = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUtilsProvider = (UtilitiesProviderInterface) getActivity();
        nUtilsHandler = AppConfig.getInstance().getUtilsHandler();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = getActivity();
        final boolean edit=getArguments().getBoolean("edit",false);
        final View v2 = getActivity().getLayoutInflater().inflate(R.layout.sftp_dialog, null);
        final EditText connectionET = (EditText) v2.findViewById(R.id.connectionET);
        final EditText addressET = (EditText) v2.findViewById(R.id.ipET);
        final EditText portET = (EditText) v2.findViewById(R.id.portET);
        final EditText usernameET = (EditText) v2.findViewById(R.id.usernameET);
        final EditText passwordET = (EditText) v2.findViewById(R.id.passwordET);
        final Button selectPemBTN = (Button) v2.findViewById(R.id.selectPemBTN);

        // If it's new connection setup, set some default values
        // Otherwise, use given Bundle instance for filling in the blanks
        if(!edit) {
            connectionET.setText(R.string.scp_con);
            portET.setText(Integer.toString(SshConnectionPool.SSH_DEFAULT_PORT));
        } else {
            connectionET.setText(getArguments().getString("name"));
            addressET.setText(getArguments().getString("address"));
            portET.setText(getArguments().getString("port"));
            usernameET.setText(getArguments().getString("username"));
            if(getArguments().getBoolean("hasPassword")) {
                passwordET.setHint(R.string.password_unchanged);
            } else {
                mSelectedParsedKeyPairName = getArguments().getString("keypairName");
                selectPemBTN.setText(mSelectedParsedKeyPairName);
            }
        }

        //For convenience, so I don't need to press backspace all the time
        portET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            if(hasFocus)
                portET.selectAll();
            }
        });

        int accentColor = mUtilsProvider.getColorPreference().getColor(ColorUsage.ACCENT);

        //Use system provided action to get Uri to PEM.
        //If MaterialDialog.Builder can be upgraded we may use their file selection dialog too
        selectPemBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent intent = new Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(intent, SELECT_PEM_INTENT);
            }
        });

        //Define action for buttons
        final MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(mContext)
            .title((R.string.scp_con))
            .autoDismiss(false)
            .customView(v2, true)
            .theme(mUtilsProvider.getAppTheme().getMaterialDialogTheme())
            .negativeText(R.string.cancel)
            .positiveText(R.string.create)
            .positiveColor(accentColor).negativeColor(accentColor).neutralColor(accentColor)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
        {

            final String connectionName = connectionET.getText().toString();
            final String hostname = addressET.getText().toString();
            final int port = Integer.parseInt(portET.getText().toString());
            final String username = usernameET.getText().toString();
            final String password = passwordET.getText() != null ?
                    passwordET.getText().toString() : null;

            String sshHostKey = nUtilsHandler.getSshHostKey(deriveSftpPathFrom(hostname,port,
                    username, password, mSelectedParsedKeyPair));
            Log.d("DEBUG", "sshHostKey? [" + sshHostKey + "]");
            if(sshHostKey != null) {
                authenticateAndSaveSetup(connectionName, hostname, port, sshHostKey, username,
                        password, mSelectedParsedKeyPairName, mSelectedParsedKeyPair);
            } else {
                try {
                    AsyncTaskResult<PublicKey> taskResult = new GetSshHostFingerprintTask(hostname, port).execute().get();
                    PublicKey hostKey = taskResult.getResult();
                    if(hostKey != null) {
                        final String hostKeyFingerprint = SecurityUtils.getFingerprint(hostKey);
                        StringBuilder sb = new StringBuilder(hostname);
                        if(port != SshConnectionPool.SSH_DEFAULT_PORT && port > 0)
                            sb.append(':').append(port);

                        final String hostAndPort = sb.toString();

                        new AlertDialog.Builder(mContext).setTitle(R.string.ssh_host_key_verification_prompt_title)
                            .setMessage(String.format(getResources().getString(R.string.ssh_host_key_verification_prompt),
                                    hostAndPort, hostKey.getAlgorithm(), hostKeyFingerprint))
                            .setCancelable(true)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(!edit) {
                                        Log.d(TAG, hostAndPort + ": [" + hostKeyFingerprint + "]");
                                        //This closes the host fingerprint verification dialog
                                        dialog.dismiss();
                                        if(authenticateAndSaveSetup(connectionName, hostname, port,
                                                hostKeyFingerprint, username, password,
                                                mSelectedParsedKeyPairName, mSelectedParsedKeyPair))
                                        {
                                            dialog.dismiss();
                                            Log.d(TAG, "Saved setup");
                                            dismiss();
                                        }
                                    } else {
                                        //TODO: update connection settings
                                    }
                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            }
                        }).show();
                    }
                } catch(InterruptedException e) {
                    e.printStackTrace();
                } catch(ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }}).onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            dialog.dismiss();
            }
        });

        //If we are editing connection settings, give new actions for neutral and negative buttons
        if(edit) {
            Log.d(TAG, "Edit? " + edit);
            dialogBuilder.negativeText(R.string.delete).onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                final String connectionName = connectionET.getText().toString();
                final String hostname = addressET.getText().toString();
                final int port = Integer.parseInt(portET.getText().toString());
                final String username = usernameET.getText().toString();

                final String path = deriveSftpPathFrom(hostname, port, username,
                        getArguments().getString("password", null), mSelectedParsedKeyPair);
                int i = DataUtils.getInstance().containsServer(new String[]{connectionName, path});

                if (i != -1) {
                    DataUtils.getInstance().removeServer(i);

                    AppConfig.runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            nUtilsHandler.removeSftpPath(connectionName, path);
                        }
                    });
                    ((MainActivity) getActivity()).refreshDrawer();
                }
                dialog.dismiss();
                }
            }).neutralText(R.string.cancel).onNeutral(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {dialog.dismiss();
                }
            });
        }

        MaterialDialog dialog = dialogBuilder.build();

        // Some validations to make sure the Create/Update button is clickable only when required
        // setting values are given
        final View okBTN = dialog.getActionButton(DialogAction.POSITIVE);
        okBTN.setEnabled(false);

        TextWatcher validator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                okBTN.setEnabled(
                        connectionET.getText().length() > 0
                     && addressET.getText().length() > 0
                     && portET.getText().length() > 0
                     && usernameET.getText().length() > 0
                     && (passwordET.getText().length() > 0 || mSelectedParsedKeyPair != null)
                );
            }
        };

        addressET.addTextChangedListener(validator);
        portET.addTextChangedListener(validator);
        usernameET.addTextChangedListener(validator);
        passwordET.addTextChangedListener(validator);

        return dialog;
    }

    /**
     * Set the PEM key for authentication when the Intent to browse file returned.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(SELECT_PEM_INTENT == requestCode && Activity.RESULT_OK == resultCode)
        {
            mSelectedPem = data.getData();
            Log.d(TAG, "Selected PEM: " + mSelectedPem.toString() + "/ "
                    + mSelectedPem.getLastPathSegment());
            
            try {
                InputStream selectedKeyContent = mContext.getContentResolver()
                        .openInputStream(mSelectedPem);
                KeyPair keypair = new PemToKeyPairTask(selectedKeyContent).execute().get();
                if(keypair != null)
                {
                    mSelectedParsedKeyPair = keypair;
                    mSelectedParsedKeyPairName = mSelectedPem.getLastPathSegment()
                            .substring(mSelectedPem.getLastPathSegment().indexOf('/')+1);
                    MDButton okBTN = ((MaterialDialog)getDialog())
                            .getActionButton(DialogAction.POSITIVE);
                    okBTN.setEnabled(okBTN.isEnabled() || true);

                    Button selectPemBTN = (Button) getDialog().findViewById(R.id.selectPemBTN);
                    selectPemBTN.setText(mSelectedParsedKeyPairName);
                }
            } catch(FileNotFoundException e) {
                Log.e(TAG, "File not found", e);
            } catch(InterruptedException ignored) {

            } catch(ExecutionException e) {

            }
        }
    }

    private boolean authenticateAndSaveSetup(final String connectionName, final String hostname,
                                          final int port, final String hostKeyFingerprint,
                                          final String username, final String password,
                                          final String selectedParsedKeyPairName,
                                          final KeyPair selectedParsedKeyPair) {
        try {
            AsyncTaskResult<SSHClient> taskResult = new SshAuthenticationTask(hostname, port, hostKeyFingerprint, username, password,
                    selectedParsedKeyPair).execute().get();
            SSHClient result = taskResult.getResult();
            if(result != null) {
                SshClientUtils.tryDisconnect(result);

                final String path = deriveSftpPathFrom(hostname, port, username, password,
                        selectedParsedKeyPair);

                final String encryptedPath = (password.length() > 0) ?
                        SmbUtil.getSmbEncryptedPath(mContext, path): path;

                if(DataUtils.getInstance().containsServer(path) == -1) {
                    AppConfig.runInBackground(new Runnable() {
                        @Override
                        public void run() {
                        nUtilsHandler.addSsh(connectionName, encryptedPath, hostKeyFingerprint,
                                selectedParsedKeyPairName, getPemContents());
                        }
                    });
                    DataUtils.getInstance().addServer(new String[]{connectionName, path});
                    ((MainActivity) getActivity()).refreshDrawer();

                    TabFragment fragment = ((MainActivity) getActivity()).getFragment();
                    if (fragment != null) {
                        Fragment fragment1 = fragment.getTab();
                        if (fragment1 != null) {
                            final MainFragment ma = (MainFragment) fragment1;
                            ma.loadlist(path, false, OpenMode.UNKNOWN);
                        }
                    }
                    dismiss();

                } else {
                    Snackbar.make(getActivity().findViewById(R.id.content_frame),
                            getResources().getString(R.string.connection_exists), Snackbar.LENGTH_SHORT).show();
                    dismiss();
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //Decide the SSH URL depends on password/selected KeyPair
    private String deriveSftpPathFrom(String hostname, int port, String username, String password,
                                      KeyPair selectedParsedKeyPair) {
        return (selectedParsedKeyPair != null || password == null) ?
                String.format("ssh://%s@%s:%d", username, hostname, port) :
                String.format("ssh://%s:%s@%s:%d", username, password, hostname, port);
    }

    //Read the PEM content from InputStream to String.
    private String getPemContents() {
        if(mSelectedPem == null)
            return null;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(mContext.getContentResolver().openInputStream(mSelectedPem)));
            StringBuilder sb = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch(FileNotFoundException e){
            return null;
        } catch(IOException e) {
            return null;
        }
    }
}
