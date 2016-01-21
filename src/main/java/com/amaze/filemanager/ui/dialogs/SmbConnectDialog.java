package com.amaze.filemanager.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import jcifs.smb.SmbFile;

/**
 * Created by arpitkh996 on 17-01-2016.
 */
public class SmbConnectDialog extends DialogFragment {

    public interface SmbConnectionListener{
        void addConnection(boolean edit,String name,String path,String oldname,String oldPath);
        void deleteConnection(String name,String path);
    }
    Context context;
    SmbConnectionListener smbConnectionListener;
    String s1,s2;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final boolean edit=getArguments().getBoolean("edit",false);
        final String path=getArguments().getString("path");
        final String name=getArguments().getString("name");
        context=getActivity();
        s1= String.format(getString(R.string.cantbeempty),getString(R.string.ip) );
        s2= String.format(getString(R.string.cantbeempty),getString(R.string.connectionname) );
        if(getActivity() instanceof SmbConnectionListener){
            smbConnectionListener=(SmbConnectionListener)getActivity();
        }
        final SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(context);
        final MaterialDialog.Builder ba3 = new MaterialDialog.Builder(context);
        ba3.title((R.string.smb_con));
        ba3.autoDismiss(false);
        final View v2 = getActivity().getLayoutInflater().inflate(R.layout.smb_dialog, null);
        final TextInputLayout t1=(TextInputLayout)v2.findViewById(R.id.t1);
        final TextInputLayout t2=(TextInputLayout)v2.findViewById(R.id.t2);
        final AppCompatEditText con_name = (AppCompatEditText) v2.findViewById(R.id.editText4);

        con_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            if(con_name.getText().toString().length()==0)
                t1.setError(s2);
            else t1.setError("");
            }
        });
        final AppCompatEditText ip = (AppCompatEditText) v2.findViewById(R.id.editText);
        ip.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(ip.getText().toString().length()==0)
                    t2.setError(s1);
                else t2.setError("");
            }
        });
        int color = Color.parseColor(PreferenceUtils.getAccentString(sharedPreferences));
        final AppCompatEditText user = (AppCompatEditText) v2.findViewById(R.id.editText3);
        final AppCompatEditText pass = (AppCompatEditText) v2.findViewById(R.id.editText2);
        final AppCompatCheckBox ch = (AppCompatCheckBox) v2.findViewById(R.id.checkBox2);
        TextView help = (TextView) v2.findViewById(R.id.wanthelp);
        Futils futils=new Futils();
        futils.setTint(con_name,color);
        futils.setTint(user,color);
        futils.setTint(pass,color);
        futils.setTint(ch,color);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Futils().showSMBHelpDialog(context,PreferenceUtils.getAccentString(sharedPreferences));
            }
        });
        ch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ch.isChecked()) {
                    user.setEnabled(false);
                    pass.setEnabled(false);
                } else {
                    user.setEnabled(true);
                    pass.setEnabled(true);

                }
            }
        });
        if (edit) {
            String userp = "", passp = "", ipp = "";
            con_name.setText(name);
            try {
                jcifs.Config.registerSmbURLHandler();
                URL a = new URL(path);
                String userinfo = a.getUserInfo();
                if (userinfo != null) {
                    String inf = URLDecoder.decode(userinfo, "UTF-8");
                    userp = inf.substring(0, inf.indexOf(":"));
                    passp = inf.substring(inf.indexOf(":") + 1, inf.length());
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

        }else if(path!=null && path.length()>0){
            con_name.setText(name);
            ip.setText(path);
            user.requestFocus();
        }
        else {
            con_name.setText(R.string.smb_con);
            con_name.requestFocus();
        }
        ba3.customView(v2, true);
        if (PreferenceUtils.getTheme(sharedPreferences) == 1) ba3.theme(Theme.DARK);
        ba3.neutralText(R.string.cancel);
        ba3.positiveText(R.string.create);
        if (edit) ba3.negativeText(R.string.delete);
        ba3.positiveColor(color).negativeColor(color).neutralColor(color);
        ba3.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                String s[];
                String ipa = ip.getText().toString();
                String con_nam=con_name.getText().toString();
                if(ipa==null || ipa.length()==0){
                    t2.setError(s1);
                    t2.requestFocus();
                    return;
                }
                if(con_nam==null || con_nam.length()==0){
                    t1.setError(s2);
                    t1.requestFocus();
                    return;
                }
                SmbFile smbFile;
                if (ch.isChecked())
                    smbFile = connectingWithSmbServer(new String[]{ipa, "", ""}, true);
                else {
                    String useru = user.getText().toString();
                    String passp = pass.getText().toString();
                    smbFile = connectingWithSmbServer(new String[]{ipa, useru, passp}, false);
                }
                if (smbFile == null) return;
                s = new String[]{con_name.getText().toString(), smbFile.getPath()};
                if(smbConnectionListener!=null){
                    smbConnectionListener.addConnection(edit,s[0],s[1],name,path);
                }
                dismiss();
            }
        });
        ba3.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                if(smbConnectionListener!=null){
                    smbConnectionListener.deleteConnection(name,path);
                dismiss();
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

    public SmbFile connectingWithSmbServer(String[] auth, boolean anonym) {
        try {
            String yourPeerIP = auth[0], domain = "";
            String path = "smb://" + (anonym ? "" : (URLEncoder.encode(auth[1] + ":" + auth[2], "UTF-8") + "@")) + yourPeerIP + "/";
            SmbFile smbFile = new SmbFile(path);
            return smbFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    }
