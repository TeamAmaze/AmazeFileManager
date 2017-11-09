package com.amaze.filemanager.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.services.ssh.SFtpClientUtils;
import com.amaze.filemanager.services.ssh.SshConnectionPool;
import com.amaze.filemanager.services.ssh.tasks.SshAuthenticationTask;
import com.amaze.filemanager.services.ssh.tasks.VerifyHostKeyTask;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

import org.apache.ftpserver.ftplet.User;

import org.bouncycastle.openssl.PEMParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class SftpConnectDialog extends DialogFragment
{
    private static final String TAG = "SftpConnectDialog";

    //Idiotic code
    private static final int SELECT_PEM_INTENT = 0x01010101;

    private UtilitiesProviderInterface utilsProvider;

    private UtilsHandler utilsHandler;

    private Context context;

    private Uri selectedPem = null;

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
        final MaterialDialog.Builder ba3 = new MaterialDialog.Builder(context);
        final View v2 = getActivity().getLayoutInflater().inflate(R.layout.sftp_dialog, null);
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

        ba3.title((R.string.scp_con));
        ba3.autoDismiss(false);
        ba3.customView(v2, true);
        ba3.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        ba3.neutralText(R.string.cancel);
        ba3.positiveText(R.string.create);
        if (edit) ba3.negativeText(R.string.delete);
        ba3.positiveColor(accentColor).negativeColor(accentColor).neutralColor(accentColor);

        ba3.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            final String hostname = addressET.getText().toString();
            final int port = Integer.parseInt(portET.getText().toString());
            final String username = usernameET.getText().toString();
            final String password = passwordET.getText() != null ? passwordET.getText().toString() : null;

            String sshHostKey = utilsHandler.getSshHostKey(hostname, port);
            if(sshHostKey != null)
            {
                Log.i(TAG, String.format("%s:%s", username, password));
                authenticateAndSaveSetup(hostname, port, sshHostKey, username, password);
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
                                        authenticateAndSaveSetup(hostname, port, hostKeyFingerprint, username, password);
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
            }
        });
        
        ba3.onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dismiss();
            }
        });

        return ba3.build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(SELECT_PEM_INTENT == requestCode && Activity.RESULT_OK == resultCode)
        {
            selectedPem = data.getData();
            Log.d(TAG, "Selected PEM: " + selectedPem.toString());
            new VerifyPemTask(selectedPem).execute();
        }
    }

    private void authenticateAndSaveSetup(String hostname, int port, String hostKeyFingerprint, String username, String password)
    {
        SSHClient sshClient = null;
        try {
            if(new SshAuthenticationTask(hostname, port, hostKeyFingerprint, username, password).execute().get())
            {
                String path = SmbUtil.getSmbEncryptedPath(context, Uri.parse(String.format("ssh://%s:%s@%s:%d", username, password, hostname, port)).toString());
//                Log.d(TAG, SmbUtil.getSmbEncryptedPath(context, Uri.parse(String.format("ssh://%s@%s:%d", username, hostname, port)).toString()));
//                Log.d(TAG, SmbUtil.getSmbEncryptedPath(context, Uri.parse(String.format("ssh://%s:%s@%s:%d", username, password, hostname, port)).toString()));
                utilsHandler.addSsh(String.format("%s@%s:%d", username, hostname, port), path, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final class VerifyPemTask extends AsyncTask<Void, Void, KeyPair>
    {
        final Uri pemFile;

        VerifyPemTask(Uri pemFile)
        {
            this.pemFile = pemFile;
        }

        @Override
        protected KeyPair doInBackground(Void... voids) {
            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(context.getContentResolver().openInputStream(pemFile));
                PEMParser pemParser = new PEMParser(reader);
                KeyPair keyPair = (KeyPair) pemParser.readObject();
                return keyPair;
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

        @Override
        protected void onPostExecute(KeyPair keyPair) {

        }
    }
}
