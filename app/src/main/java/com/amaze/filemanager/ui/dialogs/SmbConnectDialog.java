/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.dialogs;

import static android.util.Base64.URL_SAFE;
import static com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.AT;
import static com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.COLON;
import static com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.SLASH;
import static com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX;
import static com.amaze.filemanager.utils.SmbUtil.PARAM_DISABLE_IPC_SIGNING_CHECK;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.databinding.SmbDialogBinding;
import com.amaze.filemanager.filesystem.smb.CifsContexts;
import com.amaze.filemanager.ui.ExtensionsKt;
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.EditTextColorStateUtil;
import com.amaze.filemanager.utils.PasswordUtil;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.textfield.TextInputLayout;

import android.app.Dialog;
import android.content.Context;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;

import jcifs.smb.SmbFile;
import kotlin.text.Charsets;

public class SmbConnectDialog extends DialogFragment {

  // Dialog tag.
  public static final String TAG = "smbdialog";

  public static final String ARG_NAME = "name";

  public static final String ARG_PATH = "path";

  public static final String ARG_EDIT = "edit";

  private static final Logger LOG = LoggerFactory.getLogger(SmbConnectDialog.class);

  private UtilitiesProvider utilsProvider;

  private SmbConnectionListener smbConnectionListener;

  private SmbDialogBinding binding;
  private String emptyAddress;
  private String emptyName;
  private String invalidDomain;
  private String invalidUsername;

  public interface SmbConnectionListener {

    /**
     * Callback denoting a new connection been added from dialog
     *
     * @param edit whether we edit existing connection or not
     * @param name name of connection as appears in navigation drawer
     * @param encryptedPath the full path to the server. Includes encrypted password to save in
     *     database. Later be decrypted at every boot when we read from db entry.
     * @param oldname the old name of connection if we're here to edit
     * @param oldPath the old full path (un-encrypted as we read from existing entry in db, which we
     *     decrypted beforehand).
     */
    void addConnection(
        boolean edit,
        @NonNull String name,
        @NonNull String encryptedPath,
        @Nullable String oldname,
        @Nullable String oldPath);

    /**
     * Callback denoting a connection been deleted from dialog
     *
     * @param name name of connection as in navigation drawer and in database entry
     * @param path the full path to server. Includes an un-encrypted password as we decrypted it
     *     beforehand while reading from database before coming here to delete. We'll later have to
     *     encrypt the password back again in order to match entry from db and to successfully
     *     delete it. If we don't want this behaviour, then we'll have to not allow duplicate
     *     connection name, and delete entry based on the name only. But that is not supported as of
     *     now. See {@link com.amaze.filemanager.database.UtilsHandler#removeSmbPath(String,
     *     String)}
     */
    void deleteConnection(String name, String path);
  }

  @VisibleForTesting
  public void setSmbConnectionListener(SmbConnectionListener smbConnectionListener) {
    this.smbConnectionListener = smbConnectionListener;
  }

  @VisibleForTesting
  public SmbConnectionListener getSmbConnectionListener() {
    return smbConnectionListener;
  }

  @VisibleForTesting
  public SmbDialogBinding getBinding() {
    return binding;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    utilsProvider = ((BasicActivity) getActivity()).getUtilsProvider();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final boolean edit = getArguments().getBoolean(ARG_EDIT, false);
    final String path = getArguments().getString(ARG_PATH);
    final String name = getArguments().getString(ARG_NAME);
    Context context = requireActivity();
    emptyAddress = getString(R.string.cant_be_empty, getString(R.string.ip));
    emptyName = getString(R.string.cant_be_empty, getString(R.string.connection_name));
    invalidDomain = getString(R.string.invalid, getString(R.string.domain));
    invalidUsername = getString(R.string.invalid, getString(R.string.username).toLowerCase());
    if (requireActivity() instanceof SmbConnectionListener && smbConnectionListener == null) {
      smbConnectionListener = (SmbConnectionListener) getActivity();
    }
    final MaterialDialog.Builder ba3 = new MaterialDialog.Builder(context);
    ba3.title((R.string.smb_connection));
    ba3.autoDismiss(false);
    binding = SmbDialogBinding.inflate(LayoutInflater.from(context));
    final TextInputLayout connectionTIL = binding.connectionTIL;
    final TextInputLayout ipTIL = binding.ipTIL;
    final TextInputLayout domainTIL = binding.domainTIL;
    final TextInputLayout usernameTIL = binding.usernameTIL;
    final TextInputLayout passwordTIL = binding.passwordTIL;
    final AppCompatEditText conName = binding.connectionET;

    ExtensionsKt.makeRequired(connectionTIL);
    ExtensionsKt.makeRequired(ipTIL);
    ExtensionsKt.makeRequired(usernameTIL);
    ExtensionsKt.makeRequired(passwordTIL);

    conName.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable s) {
            if (conName.getText().toString().length() == 0) connectionTIL.setError(emptyName);
            else connectionTIL.setError("");
          }
        });
    final AppCompatEditText ip = binding.ipET;
    ip.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable s) {
            if (ip.getText().toString().length() == 0) ipTIL.setError(emptyAddress);
            else ipTIL.setError("");
          }
        });
    final AppCompatEditText share = binding.shareET;
    final AppCompatEditText domain = binding.domainET;
    domain.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable s) {
            if (domain.getText().toString().contains(";")) domainTIL.setError(invalidDomain);
            else domainTIL.setError("");
          }
        });
    final AppCompatEditText user = binding.usernameET;
    user.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable s) {
            if (user.getText().toString().contains(String.valueOf(COLON)))
              usernameTIL.setError(invalidUsername);
            else usernameTIL.setError("");
          }
        });

    int accentColor = ((ThemedActivity) getActivity()).getAccent();
    final AppCompatEditText pass = binding.passwordET;
    final AppCompatCheckBox chkSmbAnonymous = binding.chkSmbAnonymous;
    final AppCompatCheckBox chkSmbDisableIpcSignature = binding.chkSmbDisableIpcSignature;
    TextView help = binding.wanthelp;

    EditTextColorStateUtil.setTint(context, conName, accentColor);
    EditTextColorStateUtil.setTint(context, user, accentColor);
    EditTextColorStateUtil.setTint(context, pass, accentColor);

    Utils.setTint(context, chkSmbAnonymous, accentColor);
    help.setOnClickListener(
        v -> {
          int accentColor1 = ((ThemedActivity) getActivity()).getAccent();
          GeneralDialogCreation.showSMBHelpDialog(context, accentColor1);
        });

    chkSmbAnonymous.setOnClickListener(
        v -> {
          if (chkSmbAnonymous.isChecked()) {
            user.setEnabled(false);
            pass.setEnabled(false);
          } else {
            user.setEnabled(true);
            pass.setEnabled(true);
          }
        });

    if (edit) {
      String userp = "";
      String passp = "";
      String ipp = "";
      String domainp = "";
      String sharep = "";

      conName.setText(name);
      try {
        URL a = new URL(path);
        String userinfo = a.getUserInfo();
        if (userinfo != null) {
          String inf = decode(userinfo, Charsets.UTF_8.name());
          int domainDelim = !inf.contains(";") ? 0 : inf.indexOf(';');
          domainp = inf.substring(0, domainDelim);
          if (domainp != null && domainp.length() > 0) inf = inf.substring(domainDelim + 1);
          userp = inf.substring(0, inf.indexOf(":"));
          try {
            passp =
                PasswordUtil.INSTANCE.decryptPassword(
                    context, inf.substring(inf.indexOf(COLON) + 1), URL_SAFE);
            passp = decode(passp, Charsets.UTF_8.name());
          } catch (GeneralSecurityException | IOException e) {
            LOG.warn("Error decrypting password", e);
            passp = "";
          }
          domain.setText(domainp);
          user.setText(userp);
          pass.setText(passp);
        } else {
          chkSmbAnonymous.setChecked(true);
        }
        ipp = a.getHost();
        sharep = a.getPath().replaceFirst("/", "").replaceAll("/$", "");
        ip.setText(ipp);
        share.setText(sharep);

        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(path);
        if (sanitizer.hasParameter(PARAM_DISABLE_IPC_SIGNING_CHECK)) {
          chkSmbDisableIpcSignature.setChecked(
              Boolean.parseBoolean(sanitizer.getValue(PARAM_DISABLE_IPC_SIGNING_CHECK)));
        }
      } catch (UnsupportedEncodingException | IllegalArgumentException e) {
        LOG.warn("failed to load smb dialog info for path {}", path, e);
      } catch (MalformedURLException e) {
        LOG.warn("failed to load smb dialog info", e);
      }

    } else if (path != null && path.length() > 0) {
      conName.setText(name);
      ip.setText(path);
      user.requestFocus();
    } else {
      conName.setText(R.string.smb_connection);
      conName.requestFocus();
    }

    ba3.customView(binding.getRoot(), true);
    ba3.theme(utilsProvider.getAppTheme().getMaterialDialogTheme(context));
    ba3.neutralText(android.R.string.cancel);
    ba3.positiveText(edit ? R.string.update : R.string.create);
    if (edit) ba3.negativeText(R.string.delete);
    ba3.positiveColor(accentColor).negativeColor(accentColor).neutralColor(accentColor);
    ba3.onPositive(
        (dialog, which) -> {
          String[] s;
          String ipa = ip.getText().toString();
          String con_nam = conName.getText().toString();
          String sDomain = domain.getText().toString();
          String sShare = share.getText().toString();
          String username = user.getText().toString();
          TextInputLayout firstInvalidField = null;
          if (con_nam == null || con_nam.length() == 0) {
            connectionTIL.setError(emptyName);
            firstInvalidField = connectionTIL;
          }
          if (ipa == null || ipa.length() == 0) {
            ipTIL.setError(emptyAddress);
            if (firstInvalidField == null) firstInvalidField = ipTIL;
          }
          if (sDomain.contains(";")) {
            domainTIL.setError(invalidDomain);
            if (firstInvalidField == null) firstInvalidField = domainTIL;
          }
          if (username.contains(":")) {
            usernameTIL.setError(invalidUsername);
            if (firstInvalidField == null) firstInvalidField = usernameTIL;
          }
          if (firstInvalidField != null) {
            firstInvalidField.requestFocus();
            return;
          }
          SmbFile smbFile;
          String domaind = domain.getText().toString();
          if (chkSmbAnonymous.isChecked())
            smbFile = createSMBPath(new String[] {ipa, "", "", domaind, sShare}, true, false);
          else {
            String useraw = user.getText().toString();
            String useru = useraw.replaceAll(" ", "\\ ");
            String passp = pass.getText().toString();
            smbFile =
                createSMBPath(new String[] {ipa, useru, passp, domaind, sShare}, false, false);
          }

          if (smbFile == null) return;

          StringBuilder extraParams = new StringBuilder();
          if (chkSmbDisableIpcSignature.isChecked())
            extraParams.append(PARAM_DISABLE_IPC_SIGNING_CHECK).append('=').append(true);

          try {
            s =
                new String[] {
                  conName.getText().toString(),
                  SmbUtil.getSmbEncryptedPath(getActivity(), smbFile.getPath())
                };
          } catch (Exception e) {
            LOG.warn("failed to load smb dialog info", e);
            Toast.makeText(getActivity(), getString(R.string.error), Toast.LENGTH_LONG).show();
            return;
          }

          if (smbConnectionListener != null) {
            // encrypted path means path with encrypted pass
            String qs = extraParams.length() > 0 ? extraParams.insert(0, '?').toString() : "";
            smbConnectionListener.addConnection(edit, s[0], s[1] + qs, name, path);
          }
          dismiss();
        });
    ba3.onNegative(
        (dialog, which) -> {
          if (smbConnectionListener != null) {
            smbConnectionListener.deleteConnection(name, path);
          }

          dismiss();
        });
    ba3.onNeutral((dialog, which) -> dismiss());

    return ba3.build();
  }

  // Begin URL building, hence will need to URL encode credentials here, to begin with.
  private SmbFile createSMBPath(String[] auth, boolean anonymous, boolean disableIpcSignCheck) {
    try {
      String yourPeerIP = auth[0];
      String domain = auth[3];
      String share = auth[4];

      StringBuilder sb = new StringBuilder(SMB_URI_PREFIX);
      if (!TextUtils.isEmpty(domain)) sb.append(encode(domain + ";", Charsets.UTF_8.name()));
      if (!anonymous)
        sb.append(encode(auth[1], Charsets.UTF_8.name()))
            .append(COLON)
            .append(encode(auth[2], Charsets.UTF_8.name()))
            .append(AT);
      sb.append(yourPeerIP).append(SLASH);
      if (!TextUtils.isEmpty(share)) {
        sb.append(share).append(SLASH);
      }
      return new SmbFile(
          sb.toString(),
          CifsContexts.createWithDisableIpcSigningCheck(sb.toString(), disableIpcSignCheck));
    } catch (MalformedURLException e) {
      LOG.warn("failed to load smb path", e);
    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
      LOG.warn("Failed to load smb path", e);
    }
    return null;
  }
}
