package com.amaze.filemanager.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.userauth.UserAuthException;

import org.apache.ftpserver.ftplet.User;

import java.io.IOException;
import java.security.PublicKey;
import java.security.Security;

public class SftpConnectDialog extends DialogFragment
{
    private static final String TAG = "SftpConnectDialog";

    private static final int SSH_DEFAULT_PORT = 22;

    private UtilitiesProviderInterface utilsProvider;

    private UtilsHandler utilsHandler;

    private Context context;

    String emptyAddress, emptyName, invalidUsername;

    static {
        Security.removeProvider("BC");
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), Security.getProviders().length+1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
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

        if(!edit)
            portET.setText(Integer.toString(SSH_DEFAULT_PORT));

        int accentColor = utilsProvider.getColorPreference().getColor(ColorUsage.ACCENT);

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
                String hostname = addressET.getText().toString();
                int port = Integer.parseInt(portET.getText().toString());
                String username = usernameET.getText().toString();
                String password = passwordET.getText() != null ? passwordET.getText().toString() : null;
                try {
                    new VerifyHostKeyTask(hostname, port, username, password).execute();
                } catch(Exception e) {
                    e.printStackTrace();
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

    final class VerifyHostKeyTask extends AsyncTask<Void, Void, AlertDialog.Builder>
    {
        final String hostname;
        final int port;
        final String username;
        final String password;
        final SSHClient sshClient;

        VerifyHostKeyTask(String hostname, int port, String username, String password)
        {
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
            this.sshClient = new SSHClient();
        }

        @Override
        protected void onPostExecute(AlertDialog.Builder builder) {
            if(builder != null)
            {
                builder.show();
            }
            new AuthenticateSshTask(username, password, sshClient).execute();
        }

        @Override
        protected AlertDialog.Builder doInBackground(Void... voids) {
            String sshHostKey = utilsHandler.getSshHostKey(hostname, port);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if(sshHostKey != null)
            {
                sshClient.addHostKeyVerifier(sshHostKey);
            }
            else
            {
                sshClient.addHostKeyVerifier(new HostKeyVerifier() {
                    @Override
                    public boolean verify(String hostname, int port, PublicKey key) {
                        String hostKey = SecurityUtils.getFingerprint(key);
                        StringBuilder sb = new StringBuilder(hostname);
                        if(port != SSH_DEFAULT_PORT && port > 0)
                            sb.append(':').append(port);

                        builder.setTitle(R.string.ssh_host_key_verification_prompt_title)
                        .setMessage(String.format(getResources().getString(R.string.ssh_host_key_verification_prompt), sb.toString(), key.getAlgorithm(), hostKey))
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        //No harm to say accept host verification here, since this connection will be discarded anyway
                        sb = null;
                        return true;
                    }
                });
            }
            try {
                sshClient.connect(hostname, port);
                return builder;

            } catch(IOException e) {
                return new AlertDialog.Builder(context)
                        .setTitle("Connection failed")
                        .setMessage(e.getMessage());
            }
        }
    }

    final class AuthenticateSshTask extends AsyncTask<Void, Void, AlertDialog.Builder>
    {
        final String username;
        final String password;
        final SSHClient sshClient;

        AuthenticateSshTask(String username, String password, SSHClient sshClient)
        {
            this.username = username;
            this.password = password;
            this.sshClient = sshClient;
        }

        @Override
        protected AlertDialog.Builder doInBackground(Void... voids) {
            if(password != null && !"".equals(password))
            {
                try {
                    sshClient.authPassword(username, password);
                    return null;
                } catch (UserAuthException e) {
                    e.printStackTrace();
                    return new AlertDialog.Builder(context)
                            .setTitle("Authentication failure")
                            .setMessage("Authentication failed.\n\n" + e.getMessage());

                } catch (TransportException e) {

                }
                finally {
                    if(sshClient.isConnected())
                    {
                        try {
                            sshClient.disconnect();
                        } catch (IOException e) {
                            Log.w(TAG, "Error closing connection to SSH server", e);
                        }
                    }
                }
            }
            else
            {
                //Perform key-based authentication
            }
            return null;
        }

        @Override
        protected void onPostExecute(AlertDialog.Builder builder) {
            if(builder != null)
                builder.show();
        }
    }
}
