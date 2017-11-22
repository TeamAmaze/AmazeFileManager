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
import com.amaze.filemanager.services.ssh.SshConnectionPool;
import com.amaze.filemanager.services.ssh.tasks.SshAuthenticationTask;
import com.amaze.filemanager.services.ssh.tasks.VerifyHostKeyTask;
import com.amaze.filemanager.services.ssh.tasks.VerifyPemTask;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
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

    private String selectedParsedKeyPairName = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = (UtilitiesProviderInterface) getActivity();
        utilsHandler = AppConfig.getInstance().getUtilsHandler();
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
        {
            connectionET.setText(R.string.scp_con);
            portET.setText(Integer.toString(SshConnectionPool.SSH_DEFAULT_PORT));
        }
        else
        {
            connectionET.setText(getArguments().getString("name"));
            addressET.setText(getArguments().getString("address"));
            portET.setText(getArguments().getString("port"));
            usernameET.setText(getArguments().getString("username"));
            Log.d("DEBUG", getArguments().getString("password"));
            if(getArguments().getBoolean("hasPassword"))
            {
                passwordET.setHint(R.string.password_unchanged);
            }
            else
            {
                selectedParsedKeyPairName = getArguments().getString("keypairName");
                selectPemBTN.setText(selectedParsedKeyPairName);
            }
        }

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

        final MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(context)
            .title((R.string.scp_con))
            .autoDismiss(false)
            .customView(v2, true)
            .theme(utilsProvider.getAppTheme().getMaterialDialogTheme())
            .neutralText(R.string.cancel)
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
            final String password = passwordET.getText() != null ? passwordET.getText().toString() : null;

            String sshHostKey = utilsHandler.getSshHostKey(deriveSftpPathFrom(hostname, port, username, password, selectedParsedKeyPair));
            Log.d("DEBUG", "sshHostKey? [" + sshHostKey + "]");
            if(sshHostKey != null)
            {
                authenticateAndSaveSetup(connectionName, hostname, port, sshHostKey, username, password, selectedParsedKeyPairName, selectedParsedKeyPair);
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
                                    if(!edit)
                                    {
                                        Log.d(TAG, hostAndPort + ": [" + hostKeyFingerprint + "]");
                                        //This closes the host fingerprint verification dialog
                                        dialog.dismiss();
                                        authenticateAndSaveSetup(connectionName, hostname, port, hostKeyFingerprint, username, password, selectedParsedKeyPairName, selectedParsedKeyPair);
                                        Log.d(TAG, "Saved setup");
                                    }
                                    else
                                    {

                                    }
                                    dismiss();
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

        if(edit) {
            Log.d(TAG, "Edit? " + edit);
            dialogBuilder.negativeText(R.string.delete).onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                final String connectionName = connectionET.getText().toString();
                final String hostname = addressET.getText().toString();
                final int port = Integer.parseInt(portET.getText().toString());
                final String username = usernameET.getText().toString();

                final String path = deriveSftpPathFrom(hostname, port, username, getArguments().getString("password", null), selectedParsedKeyPair);
                int i = DataUtils.getInstance().containsServer(new String[]{connectionName, path});

                if (i != -1) {
                    DataUtils.getInstance().removeServer(i);

                    AppConfig.runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            utilsHandler.removeSftpPath(connectionName, path);
                        }
                    });
                    ((MainActivity) getActivity()).refreshDrawer();
                }
                dialog.dismiss();
                }
            }).onNeutral(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    dialog.dismiss();
                }
            });
        }

        MaterialDialog dialog = dialogBuilder.build();

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
            Log.d(TAG, "Selected PEM: " + selectedPem.toString() + "/ " + selectedPem.getLastPathSegment());
            try {
                InputStream selectedKeyContent = context.getContentResolver().openInputStream(selectedPem);
                KeyPair keypair = new VerifyPemTask(selectedKeyContent).execute().get();
                if(keypair != null)
                {
                    selectedParsedKeyPair = keypair;
                    selectedParsedKeyPairName = selectedPem.getLastPathSegment();
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

    private void authenticateAndSaveSetup(final String connectionName, final String hostname,
                                          final int port, final String hostKeyFingerprint,
                                          final String username, final String password,
                                          final String selectedParsedKeyPairName,
                                          final KeyPair selectedParsedKeyPair)
    {
        try {
            if(new SshAuthenticationTask(hostname, port, hostKeyFingerprint, username, password, selectedParsedKeyPair).execute().get())
            {
                final String path = deriveSftpPathFrom(hostname, port, username, password, selectedParsedKeyPair);

                final String encryptedPath = SmbUtil.getSmbEncryptedPath(context, path);

                if(DataUtils.getInstance().containsServer(path) == -1) {
                    AppConfig.runInBackground(new Runnable() {
                        @Override
                        public void run() {
                        utilsHandler.addSsh(connectionName, encryptedPath, hostKeyFingerprint, selectedParsedKeyPairName, getPemContents());
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
                    Snackbar.make(getActivity().findViewById(R.id.content_frame), getResources().getString(R.string.connection_exists), Snackbar.LENGTH_SHORT).show();
                    dismiss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String deriveSftpPathFrom(String hostname, int port, String username, String password, KeyPair selectedParsedKeyPair)
    {
        return (selectedParsedKeyPair != null) ?
                String.format("ssh://%s@%s:%d", username, hostname, port) :
                String.format("ssh://%s:%s@%s:%d", username, password, hostname, port);
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
