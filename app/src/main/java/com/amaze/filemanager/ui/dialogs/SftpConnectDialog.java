package com.amaze.filemanager.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import com.amaze.filemanager.services.ssh.SshConnectionPool;
import com.amaze.filemanager.services.ssh.tasks.SshAuthenticationTask;
import com.amaze.filemanager.services.ssh.tasks.VerifyHostKeyTask;
import com.amaze.filemanager.services.ssh.tasks.VerifyPemTask;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import net.schmizz.sshj.common.SecurityUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.util.concurrent.ExecutionException;

public class SftpConnectDialog extends DialogFragment
{
    private static final String TAG = "SftpConnectDialog";

    //Idiotic code
    private static final int SELECT_PEM_INTENT = 0x01010101;

    private UtilitiesProviderInterface utilsProvider;

    private UtilsHandler utilsHandler;

    private Context context;

    private Uri selectedPem = null;

    private KeyPair selectedParsedKeyPair = null;

    String emptyAddress, emptyName, invalidUsername;

    static {
        Security.removeProvider("BC");
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), Security.getProviders().length+1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = (UtilitiesProviderInterface) getActivity();
        utilsHandler = ((MainActivity) getActivity()).getUtilsHandler();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        context = getActivity();
        final boolean edit=getArguments().getBoolean("edit",false);
        final SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(context);
        final View v2 = getActivity().getLayoutInflater().inflate(R.layout.sftp_dialog, null);
        final EditText connectionET = (EditText) v2.findViewById(R.id.connectionET);
        final EditText addressET = (EditText) v2.findViewById(R.id.ipET);
        final EditText portET = (EditText) v2.findViewById(R.id.portET);
        final EditText usernameET = (EditText) v2.findViewById(R.id.usernameET);
        final EditText passwordET = (EditText) v2.findViewById(R.id.passwordET);
        final Button selectPemBTN = (Button) v2.findViewById(R.id.selectPemBTN);

        if(!edit)
            portET.setText(Integer.toString(SshConnectionPool.SSH_DEFAULT_PORT));

        portET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            if(hasFocus)
                portET.selectAll();
            }
        });

        int accentColor = utilsProvider.getColorPreference().getColor(ColorUsage.ACCENT);

        selectPemBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent intent = new Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(intent, SELECT_PEM_INTENT);
            }
        });

        final MaterialDialog dialog = new MaterialDialog.Builder(context)
            .title((R.string.scp_con))
            .autoDismiss(false)
            .customView(v2, true)
            .theme(utilsProvider.getAppTheme().getMaterialDialogTheme())
            .neutralText(R.string.cancel)
            .positiveText(R.string.create)
            .negativeText(R.string.delete)
            .positiveColor(accentColor).negativeColor(accentColor).neutralColor(accentColor)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
        @Override
        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
        {
            final String hostname = addressET.getText().toString();
            final int port = Integer.parseInt(portET.getText().toString());
            final String username = usernameET.getText().toString();
            final String password = passwordET.getText() != null ? passwordET.getText().toString() : null;

            String sshHostKey = utilsHandler.getSshHostKey(hostname, port);
            if(sshHostKey != null)
            {
                authenticateAndSaveSetup(hostname, port, sshHostKey, username, password, selectedParsedKeyPair);
            }
            else
            {
                try {
                    PublicKey hostKey = new VerifyHostKeyTask(hostname, port).execute().get();
                    if(hostKey != null)
                    {
                        final String hostKeyFingerprint = SecurityUtils.getFingerprint(hostKey);
                        StringBuilder sb = new StringBuilder(hostname);
                        if(port != SshConnectionPool.SSH_DEFAULT_PORT && port > 0)
                            sb.append(':').append(port);

                        final String hostAndPort = sb.toString();

                        new AlertDialog.Builder(context).setTitle(R.string.ssh_host_key_verification_prompt_title)
                            .setMessage(String.format(getResources().getString(R.string.ssh_host_key_verification_prompt), hostAndPort, hostKey.getAlgorithm(), hostKeyFingerprint))
                            .setCancelable(true)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, hostAndPort + ": [" + hostKeyFingerprint + "]");
                                    utilsHandler.addSshHostKey(hostAndPort, hostKeyFingerprint);
                                    dialog.dismiss();
                                    authenticateAndSaveSetup(hostname, port, hostKeyFingerprint, username, password, selectedParsedKeyPair);
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
        }}).onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dismiss();
            }
        }).build();

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
                     && (passwordET.getText().length() > 0 || selectedParsedKeyPair != null)
                );
            }
        };

        addressET.addTextChangedListener(validator);
        portET.addTextChangedListener(validator);
        usernameET.addTextChangedListener(validator);
        passwordET.addTextChangedListener(validator);

        return dialog;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(SELECT_PEM_INTENT == requestCode && Activity.RESULT_OK == resultCode)
        {
            selectedPem = data.getData();
            Log.d(TAG, "Selected PEM: " + selectedPem.toString());
            try {
                InputStream selectedKeyContent = context.getContentResolver().openInputStream(selectedPem);
                KeyPair keypair = new VerifyPemTask(selectedKeyContent).execute().get();
                if(keypair != null)
                {
                    selectedParsedKeyPair = keypair;
                    MDButton okBTN = ((MaterialDialog)getDialog()).getActionButton(DialogAction.POSITIVE);
                    okBTN.setEnabled(okBTN.isEnabled() || true);
                }
            } catch(FileNotFoundException e) {
                Log.e(TAG, "File not found", e);
            } catch(InterruptedException ignored) {

            } catch(ExecutionException e) {

            }
        }
    }

    private void authenticateAndSaveSetup(String hostname, int port, String hostKeyFingerprint, String username, String password, KeyPair selectedParsedKeyPair)
    {
        try {
            if(new SshAuthenticationTask(hostname, port, hostKeyFingerprint, username, password, selectedParsedKeyPair).execute().get())
            {
                String path = SmbUtil.getSmbEncryptedPath(context, Uri.parse(String.format("ssh://%s:%s@%s:%d", username, password, hostname, port)).toString());
                if(selectedParsedKeyPair != null) {
                    path = SmbUtil.getSmbEncryptedPath(context, Uri.parse(String.format("ssh://%s@%s:%d", username, hostname, port)).toString());
                }
//                Log.d(TAG, SmbUtil.getSmbEncryptedPath(context, Uri.parse(String.format("ssh://%s@%s:%d", username, hostname, port)).toString()));
//                Log.d(TAG, SmbUtil.getSmbEncryptedPath(context, Uri.parse(String.format("ssh://%s:%s@%s:%d", username, password, hostname, port)).toString()));
                utilsHandler.addSsh(String.format("%s@%s:%d", username, hostname, port), path, getPemContents());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPemContents()
    {
        if(selectedPem == null)
            return null;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(selectedPem)));
            StringBuilder sb = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine())
            {
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
