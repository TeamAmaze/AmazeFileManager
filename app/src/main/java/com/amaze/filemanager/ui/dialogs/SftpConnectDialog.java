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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.asynchronous.asynctasks.ssh.GetSshHostFingerprintTask;
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairTask;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;

import static com.amaze.filemanager.filesystem.ssh.SshClientUtils.deriveSftpPathFrom;

/**
 * SSH/SFTP connection setup dialog.
 */
public class SftpConnectDialog extends DialogFragment {
    private static final String TAG = "SftpConnectDialog";

    //Idiotic code
    //FIXME: agree code on
    private static final int SELECT_PEM_INTENT = 0x01010101;

    private UtilitiesProvider utilsProvider;

    private UtilsHandler utilsHandler;

    private Context context;

    private Uri selectedPem = null;

    private KeyPair selectedParsedKeyPair = null;

    private String selectedParsedKeyPairName = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = AppConfig.getInstance().getUtilsProvider();
        utilsHandler = AppConfig.getInstance().getUtilsHandler();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = getActivity();
        final boolean edit=getArguments().getBoolean("edit",false);
        final View v2 = getActivity().getLayoutInflater().inflate(R.layout.sftp_dialog, null);
        final EditText connectionET = v2.findViewById(R.id.connectionET);
        final EditText addressET = v2.findViewById(R.id.ipET);
        final EditText portET = v2.findViewById(R.id.portET);
        final EditText usernameET = v2.findViewById(R.id.usernameET);
        final EditText passwordET = v2.findViewById(R.id.passwordET);
        final Button selectPemBTN = v2.findViewById(R.id.selectPemBTN);

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
                selectedParsedKeyPairName = getArguments().getString("keypairName");
                selectPemBTN.setText(selectedParsedKeyPairName);
            }
        }

        //For convenience, so I don't need to press backspace all the time
        portET.setOnFocusChangeListener((v, hasFocus) -> {
        if(hasFocus)
            portET.selectAll();
        });

        int accentColor = ((ThemedActivity) getActivity()).getAccent();

        //Use system provided action to get Uri to PEM.
        //If MaterialDialog.Builder can be upgraded we may use their file selection dialog too
        selectPemBTN.setOnClickListener(v -> {
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(intent, SELECT_PEM_INTENT);
        });

        //Define action for buttons
        final MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(context);
        dialogBuilder.title((R.string.scp_con));
        dialogBuilder.autoDismiss(false);
        dialogBuilder.customView(v2, true);
        dialogBuilder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        dialogBuilder.negativeText(R.string.cancel);
        dialogBuilder.positiveText(edit ? R.string.update : R.string.create);
        dialogBuilder.positiveColor(accentColor);
        dialogBuilder.negativeColor(accentColor);
        dialogBuilder.neutralColor(accentColor);
        dialogBuilder.onPositive((dialog, which) -> {

            final String connectionName = connectionET.getText().toString();
            final String hostname = addressET.getText().toString();
            final int port = Integer.parseInt(portET.getText().toString());
            final String username = usernameET.getText().toString();
            final String password = passwordET.getText() != null ?
                    passwordET.getText().toString() : null;

            String sshHostKey = utilsHandler.getSshHostKey(deriveSftpPathFrom(hostname, port,
                    username, password, selectedParsedKeyPair));

            if (sshHostKey != null) {
                authenticateAndSaveSetup(connectionName, hostname, port, sshHostKey, username,
                        password, selectedParsedKeyPairName, selectedParsedKeyPair, edit);
            } else {
                new GetSshHostFingerprintTask(hostname, port, taskResult -> {
                    PublicKey hostKey = taskResult.result;
                    if (hostKey != null) {
                        final String hostKeyFingerprint = SecurityUtils.getFingerprint(hostKey);
                        StringBuilder sb = new StringBuilder(hostname);
                        if (port != SshConnectionPool.SSH_DEFAULT_PORT && port > 0)
                            sb.append(':').append(port);

                        final String hostAndPort = sb.toString();

                        new AlertDialog.Builder(context).setTitle(R.string.ssh_host_key_verification_prompt_title)
                                .setMessage(getString(R.string.ssh_host_key_verification_prompt,
                                        hostAndPort, hostKey.getAlgorithm(), hostKeyFingerprint))
                                .setCancelable(true)
                                .setPositiveButton(R.string.yes, (dialog1, which1) -> {
                                    //This closes the host fingerprint verification dialog
                                    dialog1.dismiss();
                                    if (authenticateAndSaveSetup(connectionName, hostname, port,
                                            hostKeyFingerprint, username, password,
                                            selectedParsedKeyPairName, selectedParsedKeyPair, edit)) {
                                        dialog1.dismiss();
                                        Log.d(TAG, "Saved setup");
                                        dismiss();
                                    }
                                }).setNegativeButton(R.string.no, (dialog1, which1) -> dialog1.dismiss()).show();
                    }
                }).execute();
            }
        }).onNegative((dialog, which) -> dialog.dismiss());

        //If we are editing connection settings, give new actions for neutral and negative buttons
        if(edit) {
            dialogBuilder.negativeText(R.string.delete).onNegative((dialog, which) -> {

            final String connectionName = connectionET.getText().toString();
            final String hostname = addressET.getText().toString();
            final int port = Integer.parseInt(portET.getText().toString());
            final String username = usernameET.getText().toString();

            final String path = deriveSftpPathFrom(hostname, port, username,
                    getArguments().getString("password", null), selectedParsedKeyPair);
            int i = DataUtils.getInstance().containsServer(new String[]{connectionName, path});

            if (i != -1) {
                DataUtils.getInstance().removeServer(i);

                AppConfig.runInBackground(() -> {
                    utilsHandler.removeFromDatabase(new OperationData(UtilsHandler.Operation.SFTP,
                            path, connectionName, null, null, null));
                });
                ((MainActivity) getActivity()).getDrawer().refreshDrawer();
            }
            dialog.dismiss();
            }).neutralText(R.string.cancel).onNeutral((dialog, which) -> dialog.dismiss());
        }

        MaterialDialog dialog = dialogBuilder.build();

        // Some validations to make sure the Create/Update button is clickable only when required
        // setting values are given
        final View okBTN = dialog.getActionButton(DialogAction.POSITIVE);
        if(!edit)
            okBTN.setEnabled(false);

        TextWatcher validator = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                int port = portET.getText().toString().length() > 0 ? Integer.parseInt(portET.getText().toString()) : -1;
                okBTN.setEnabled(
                        (connectionET.getText().length() > 0
                     && addressET.getText().length() > 0
                     && port > 0 && port < 65536
                     && usernameET.getText().length() > 0
                     && (passwordET.getText().length() > 0 || selectedParsedKeyPair != null))
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
            selectedPem = data.getData();

            try {
                InputStream selectedKeyContent = context.getContentResolver()
                        .openInputStream(selectedPem);
                new PemToKeyPairTask(selectedKeyContent, result -> {
                    selectedParsedKeyPair = result;
                    selectedParsedKeyPairName = selectedPem.getLastPathSegment()
                            .substring(selectedPem.getLastPathSegment().indexOf('/')+1);
                    MDButton okBTN = ((MaterialDialog)getDialog())
                            .getActionButton(DialogAction.POSITIVE);
                    okBTN.setEnabled(okBTN.isEnabled() || true);

                    Button selectPemBTN = getDialog().findViewById(R.id.selectPemBTN);
                    selectPemBTN.setText(selectedParsedKeyPairName);
                }).execute();

            } catch(FileNotFoundException e) {
                Log.e(TAG, "File not found", e);
            } catch(IOException shouldNotHappen) {}
        }
    }

    private boolean authenticateAndSaveSetup(String connectionName, String hostname,
                                          int port, String hostKeyFingerprint,
                                          String username, String password,
                                          String selectedParsedKeyPairName,
                                          KeyPair selectedParsedKeyPair, boolean isEdit) {

        if(isEdit)
            password = getArguments().getString("password", null);

        final String path = deriveSftpPathFrom(hostname, port, username, password,
                selectedParsedKeyPair);

        final String encryptedPath = SshClientUtils.encryptSshPathAsNecessary(path);

        if(!isEdit) {
            try {
                SSHClient result = SshConnectionPool.getInstance().getConnection(hostname, port,
                        hostKeyFingerprint, username, password, selectedParsedKeyPair);

                if(result != null) {

                    if(DataUtils.getInstance().containsServer(path) == -1) {
                        DataUtils.getInstance().addServer(new String[]{connectionName, path});
                        ((MainActivity) getActivity()).getDrawer().refreshDrawer();

                        utilsHandler.saveToDatabase(new OperationData(UtilsHandler.Operation.SFTP,
                                encryptedPath, connectionName, hostKeyFingerprint,
                                selectedParsedKeyPairName, getPemContents()));

                        MainFragment ma = ((MainActivity)getActivity()).getCurrentMainFragment();
                        ma.loadlist(path, false, OpenMode.SFTP);
                        dismiss();

                    } else {
                        Snackbar.make(getActivity().findViewById(R.id.content_frame),
                                getString(R.string.connection_exists), Snackbar.LENGTH_SHORT).show();
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
        } else {
            DataUtils.getInstance().removeServer(DataUtils.getInstance().containsServer(path));
            DataUtils.getInstance().addServer(new String[]{connectionName, path});
            Collections.sort(DataUtils.getInstance().getServers(), new BookSorter());
            ((MainActivity) getActivity()).getDrawer().refreshDrawer();

            AppConfig.runInBackground(() -> {
                utilsHandler.updateSsh(connectionName,
                        getArguments().getString("name"), encryptedPath,
                        selectedParsedKeyPairName, getPemContents());
            });

            dismiss();
            return true;
        }
    }

    //Read the PEM content from InputStream to String.
    private String getPemContents() {
        if(selectedPem == null)
            return null;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(selectedPem)));
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
