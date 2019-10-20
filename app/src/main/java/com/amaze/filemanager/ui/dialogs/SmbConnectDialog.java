package com.amaze.filemanager.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.superclasses.BasicActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.utils.EditTextColorStateUtil;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;

import jcifs.smb.SmbFile;

/**
 * Created by arpitkh996 on 17-01-2016.
 */
public class SmbConnectDialog extends DialogFragment {

    private UtilitiesProvider utilsProvider;

    private static final String TAG = "SmbConnectDialog";

    public interface SmbConnectionListener {

        /**
         * Callback denoting a new connection been added from dialog
         * @param edit whether we edit existing connection or not
         * @param name name of connection as appears in navigation drawer
         * @param path the full path to the server. Includes an un-encrypted password to support
         *             runtime loading without reloading stuff from database.
         * @param encryptedPath the full path to the server. Includes encrypted password to save in
         *                      database. Later be decrypted at every boot when we read from db entry.
         * @param oldname the old name of connection if we're here to edit
         * @param oldPath the old full path (un-encrypted as we read from existing entry in db, which
         *                we decrypted beforehand).
         */
        void addConnection(boolean edit, String name, String path, String encryptedPath,
                           String oldname, String oldPath);

        /**
         * Callback denoting a connection been deleted from dialog
         * @param name name of connection as in navigation drawer and in database entry
         * @param path the full path to server. Includes an un-encrypted password as we decrypted it
         *             beforehand while reading from database before coming here to delete.
         *             We'll later have to encrypt the password back again in order to match entry
         *             from db and to successfully delete it. If we don't want this behaviour,
         *             then we'll have to not allow duplicate connection name, and delete entry based
         *             on the name only. But that is not supported as of now.
         *             See {@link com.amaze.filemanager.database.UtilsHandler#removeSmbPath(String, String)}
         */
        void deleteConnection(String name, String path);
    }

    Context context;
    SmbConnectionListener smbConnectionListener;
    String emptyAddress, emptyName,invalidDomain,invalidUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = ((BasicActivity) getActivity()).getUtilsProvider();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final boolean edit=getArguments().getBoolean("edit",false);
        final String path=getArguments().getString("path");
        final String name=getArguments().getString("name");
        context=getActivity();
        emptyAddress = getString(R.string.cant_be_empty,getString(R.string.ip) );
        emptyName = getString(R.string.cant_be_empty,getString(R.string.connection_name) );
        invalidDomain = getString(R.string.invalid,getString(R.string.domain));
        invalidUsername = getString(R.string.invalid,getString(R.string.username).toLowerCase());
        if(getActivity() instanceof SmbConnectionListener){
            smbConnectionListener=(SmbConnectionListener)getActivity();
        }
        final SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(context);
        final MaterialDialog.Builder ba3 = new MaterialDialog.Builder(context);
        ba3.title((R.string.smb_connection));
        ba3.autoDismiss(false);
        final View v2 = getActivity().getLayoutInflater().inflate(R.layout.smb_dialog, null);
        final TextInputLayout connectionTIL = v2.findViewById(R.id.connectionTIL);
        final TextInputLayout ipTIL = v2.findViewById(R.id.ipTIL);
        final TextInputLayout domainTIL = v2.findViewById(R.id.domainTIL);
        final TextInputLayout usernameTIL = v2.findViewById(R.id.usernameTIL);
        final AppCompatEditText conName = v2.findViewById(R.id.connectionET);

        conName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(conName.getText().toString().length()==0)
                    connectionTIL.setError(emptyName);
                else connectionTIL.setError("");
            }
        });
        final AppCompatEditText ip = v2.findViewById(R.id.ipET);
        ip.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(ip.getText().toString().length()==0)
                    ipTIL.setError(emptyAddress);
                else ipTIL.setError("");
            }
        });
        final AppCompatEditText domain = v2.findViewById(R.id.domainET);
        domain.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(domain.getText().toString().contains(";"))
                    domainTIL.setError(invalidDomain);
                else domainTIL.setError("");
            }
        });
        final AppCompatEditText user = v2.findViewById(R.id.usernameET);
        user.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(user.getText().toString().contains(":"))
                    usernameTIL.setError(invalidUsername);
                else usernameTIL.setError("");
            }
        });

        int accentColor = ((ThemedActivity) getActivity()).getAccent();
        final AppCompatEditText pass = v2.findViewById(R.id.passwordET);
        final AppCompatCheckBox ch = v2.findViewById(R.id.checkBox2);
        TextView help = v2.findViewById(R.id.wanthelp);

        EditTextColorStateUtil.setTint(context, conName, accentColor);
        EditTextColorStateUtil.setTint(context, user, accentColor);
        EditTextColorStateUtil.setTint(context, pass, accentColor);

        Utils.setTint(context, ch, accentColor);
        help.setOnClickListener(v -> {
            int accentColor1 = ((ThemedActivity) getActivity()).getAccent();
            GeneralDialogCreation.showSMBHelpDialog(context, accentColor1);
        });

        ch.setOnClickListener(view -> {
            if (ch.isChecked()) {
                user.setEnabled(false);
                pass.setEnabled(false);
            } else {
                user.setEnabled(true);
                pass.setEnabled(true);

            }
        });

        if (edit) {
            String userp = "", passp = "", ipp = "",domainp = "";
            conName.setText(name);
            try {
                jcifs.Config.registerSmbURLHandler();
                URL a = new URL(path);
                String userinfo = a.getUserInfo();
                if (userinfo != null) {
                    String inf = URLDecoder.decode(userinfo, "UTF-8");
                    int domainDelim = !inf.contains(";") ? 0 : inf.indexOf(';');
                    domainp = inf.substring(0,domainDelim);
                    if(domainp!=null && domainp.length()>0)
                        inf = inf.substring(domainDelim+1);
                    userp = inf.substring(0, inf.indexOf(":"));
                    passp = inf.substring(inf.indexOf(":") + 1, inf.length());
                    domain.setText(domainp);
                    user.setText(userp);
                    pass.setText(passp);
                } else ch.setChecked(true);
                ipp = a.getHost();
                ip.setText(ipp);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        } else if(path!=null && path.length()>0) {
            conName.setText(name);
            ip.setText(path);
            user.requestFocus();
        } else {
            conName.setText(R.string.smb_connection);
            conName.requestFocus();
        }

        ba3.customView(v2, true);
        ba3.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        ba3.neutralText(R.string.cancel);
        ba3.positiveText(R.string.create);
        if (edit) ba3.negativeText(R.string.delete);
        ba3.positiveColor(accentColor).negativeColor(accentColor).neutralColor(accentColor);
        ba3.onPositive((dialog, which) -> {
            String s[];
            String ipa = ip.getText().toString();
            String con_nam=conName.getText().toString();
            String sDomain = domain.getText().toString();
            String username = user.getText().toString();
            TextInputLayout firstInvalidField  = null;
            if(con_nam==null || con_nam.length()==0){
                connectionTIL.setError(emptyName);
                firstInvalidField = connectionTIL;
            }
            if(ipa==null || ipa.length()==0){
                ipTIL.setError(emptyAddress);
                if(firstInvalidField == null)
                    firstInvalidField = ipTIL;
            }
            if(sDomain.contains(";"))
            {
                domainTIL.setError(invalidDomain);
                if(firstInvalidField == null)
                    firstInvalidField = domainTIL;
            }
            if(username.contains(":"))
            {
                usernameTIL.setError(invalidUsername);
                if(firstInvalidField == null)
                    firstInvalidField = usernameTIL;
            }
            if(firstInvalidField != null)
            {
                firstInvalidField.requestFocus();
                return;
            }
            SmbFile smbFile;
            String domaind = domain.getText().toString();
            if (ch.isChecked())
                smbFile = createSMBPath(new String[]{ipa, "", "",domaind}, true);
            else {
                String useraw = user.getText().toString();
                String useru = useraw.replaceAll(" ", "\\ ");
                String passp = pass.getText().toString();
                smbFile = createSMBPath(new String[]{ipa, useru, passp,domaind}, false);
            }

            if (smbFile == null)
                return;

            try {
                s = new String[]{conName.getText().toString(), SmbUtil.getSmbEncryptedPath(getActivity(),
                            smbFile.getPath())};
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), getString(R.string.error), Toast.LENGTH_LONG).show();
                return;
            }

            if(smbConnectionListener!=null) {
                // encrypted path means path with encrypted pass
                smbConnectionListener.addConnection(edit, s[0], smbFile.getPath(), s[1], name, path);
            }
            dismiss();
        });
        ba3.onNegative((dialog, which) -> {
            if(smbConnectionListener!=null){
                smbConnectionListener.deleteConnection(name, path);
            }

            dismiss();
        });
        ba3.onNeutral((dialog, which) -> dismiss());

        return ba3.build();
    }

    private SmbFile createSMBPath(String[] auth, boolean anonym) {
        try {
            String yourPeerIP = auth[0], domain = auth[3];

            String path = "smb://"+(android.text.TextUtils.isEmpty(domain) ? ""
                    :( URLEncoder.encode(domain + ";","UTF-8")) )+ (anonym ? "" :
                    (URLEncoder.encode(auth[1], "UTF-8") + ":" + URLEncoder.encode(auth[2], "UTF-8") + "@")) + yourPeerIP + "/";
            SmbFile smbFile = new SmbFile(path);
            return smbFile;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ColorStateList createEditTextColorStateList(int color) {
        int[][] states = new int[3][];
        int[] colors = new int[3];
        int i = 0;
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = Color.parseColor("#f6f6f6");
        i++;
        states[i] = new int[]{-android.R.attr.state_pressed, -android.R.attr.state_focused};
        colors[i] = Color.parseColor("#666666");
        i++;
        states[i] = new int[]{};
        colors[i] = color;
        return new ColorStateList(states, colors);
    }

    private static void setTint(EditText editText, int color) {
        if (Build.VERSION.SDK_INT >= 21) return;
        ColorStateList editTextColorStateList = createEditTextColorStateList(color);
        if (editText instanceof AppCompatEditText) {
            ((AppCompatEditText) editText).setSupportBackgroundTintList(editTextColorStateList);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editText.setBackgroundTintList(editTextColorStateList);
        }
    }
}
