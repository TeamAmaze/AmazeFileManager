package com.amaze.filemanager.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import java.security.PublicKey;

public class SftpConnectDialog extends DialogFragment
{
    private UtilitiesProviderInterface utilsProvider;

    private UtilsHandler utilsHandler;

    private Context context;

    private static final String TAG = "SftpConnectDialog";

    String emptyAddress, emptyName, invalidUsername;

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
                EditText addressET = (EditText) v2.findViewById(R.id.ipET);
                EditText portET = (EditText) v2.findViewById(R.id.portET);
                String sshHostKey = utilsHandler.getSshHostKey(addressET.getText().toString(), Integer.parseInt(portET.getText().toString()));

                SSHClient client = new SSHClient();
                if(sshHostKey != null)
                {
                    client.addHostKeyVerifier(sshHostKey);
                }
                else
                {
                    client.addHostKeyVerifier(new HostKeyVerifier() {
                        @Override
                        public boolean verify(String hostname, int port, PublicKey key) {
                            String hostKey = SecurityUtils.getFingerprint(key);
                            new AlertDialog.Builder(context).show();
                            return false;
                        }
                    });
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
}
