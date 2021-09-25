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

package com.amaze.filemanager.ui.fragments.preference_fragments;

import static com.amaze.filemanager.R.string.feedback;
import static com.amaze.filemanager.ui.activities.PreferencesActivity.START_PREFERENCE;
import static com.amaze.filemanager.utils.Utils.EMAIL_SUPPORT;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.ui.activities.AboutActivity;
import com.amaze.filemanager.ui.activities.PreferencesActivity;
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.preference.CheckBox;
import com.amaze.filemanager.utils.Utils;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class PrefFrag extends PreferenceFragmentCompat
    implements Preference.OnPreferenceClickListener {

  private static final String[] PREFERENCE_KEYS = {
    PreferencesConstants.PREFERENCE_GRID_COLUMNS,
    PreferencesConstants.FRAGMENT_THEME,
    PreferencesConstants.PREFERENCE_ROOTMODE,
    PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES,
    PreferencesConstants.FRAGMENT_FEEDBACK,
    PreferencesConstants.FRAGMENT_ABOUT,
    PreferencesConstants.FRAGMENT_COLORS,
    PreferencesConstants.FRAGMENT_FOLDERS,
    PreferencesConstants.FRAGMENT_QUICKACCESSES,
    PreferencesConstants.FRAGMENT_ADVANCED_SEARCH,
    PreferencesConstants.PREFERENCE_ZIP_EXTRACT_PATH,
    PreferencesConstants.PREFERENCE_CLEAR_OPEN_FILE,
    PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE
  };

  private UtilitiesProvider utilsProvider;
  private SharedPreferences sharedPref;
  /** This is a hack see {@link PreferencesActivity#saveListViewState(int, Parcelable)} */
  private ListView listView;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    utilsProvider = ((BasicActivity) getActivity()).getUtilsProvider();

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);

    sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

    for (String PREFERENCE_KEY : PREFERENCE_KEYS) {
      findPreference(PREFERENCE_KEY).setOnPreferenceClickListener(this);
    }

    // crypt master password
    final Preference masterPasswordPreference =
        findPreference(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
        || sharedPref.getBoolean(PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT, false)) {
      // encryption feature not available
      masterPasswordPreference.setEnabled(false);
    }
    masterPasswordPreference.setOnPreferenceClickListener(this);

    CheckBox checkBoxFingerprint =
        (CheckBox) findPreference(PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      // finger print sensor
      FingerprintManager fingerprintManager = null;

      final KeyguardManager keyguardManager =
          (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        fingerprintManager =
            (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        if (fingerprintManager != null && fingerprintManager.isHardwareDetected()) {
          checkBoxFingerprint.setEnabled(true);
        }
      }

      FingerprintManager finalFingerprintManager = fingerprintManager;
      checkBoxFingerprint.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            if (ActivityCompat.checkSelfPermission(
                    getActivity(), Manifest.permission.USE_FINGERPRINT)
                != PackageManager.PERMISSION_GRANTED) {
              Toast.makeText(
                      getActivity(),
                      getResources().getString(R.string.crypt_fingerprint_no_permission),
                      Toast.LENGTH_LONG)
                  .show();
              return false;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && finalFingerprintManager != null
                && !finalFingerprintManager.hasEnrolledFingerprints()) {
              Toast.makeText(
                      getActivity(),
                      getResources().getString(R.string.crypt_fingerprint_not_enrolled),
                      Toast.LENGTH_LONG)
                  .show();
              return false;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && keyguardManager != null
                && !keyguardManager.isKeyguardSecure()) {
              Toast.makeText(
                      getActivity(),
                      getResources().getString(R.string.crypt_fingerprint_no_security),
                      Toast.LENGTH_LONG)
                  .show();
              return false;
            }

            masterPasswordPreference.setEnabled(false);
            return true;
          });
    } else {

      // fingerprint manager class not defined in the framework
      checkBoxFingerprint.setEnabled(false);
    }
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    final String[] sort;
    final String[] dragToMoveArray;
    MaterialDialog.Builder builder;
    MaterialDialog.Builder dragDialogBuilder;

    switch (preference.getKey()) {
      case PreferencesConstants.PREFERENCE_CLEAR_OPEN_FILE:
        OpenFileDialogFragment.Companion.clearPreferences(sharedPref);
        AppConfig.toast(getActivity(), getActivity().getString(R.string.done));
        return true;
      case PreferencesConstants.PREFERENCE_GRID_COLUMNS:
        sort = getResources().getStringArray(R.array.columns);
        builder = new MaterialDialog.Builder(getActivity());
        builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        builder.title(R.string.gridcolumnno);
        int current =
            Integer.parseInt(
                sharedPref.getString(PreferencesConstants.PREFERENCE_GRID_COLUMNS, "-1"));
        current = current == -1 ? 0 : current;
        if (current != 0) current = current - 1;
        builder
            .items(sort)
            .itemsCallbackSingleChoice(
                current,
                (dialog, view, which, text) -> {
                  sharedPref
                      .edit()
                      .putString(
                          PreferencesConstants.PREFERENCE_GRID_COLUMNS,
                          "" + (which != 0 ? sort[which] : "" + -1))
                      .apply();
                  dialog.dismiss();
                  return true;
                });
        builder.build().show();
        return true;
      case PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE:
        dragToMoveArray = getResources().getStringArray(R.array.dragAndDropPreference);
        dragDialogBuilder = new MaterialDialog.Builder(getActivity());
        dragDialogBuilder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        dragDialogBuilder.title(R.string.drag_and_drop_preference);
        int currentDragPreference =
            sharedPref.getInt(
                PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
                PreferencesConstants.PREFERENCE_DRAG_DEFAULT);
        dragDialogBuilder
            .items(dragToMoveArray)
            .itemsCallbackSingleChoice(
                currentDragPreference,
                (dialog, view, which, text) -> {
                  sharedPref
                      .edit()
                      .putInt(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE, which)
                      .apply();
                  sharedPref
                      .edit()
                      .putString(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_REMEMBERED, null)
                      .apply();
                  dialog.dismiss();
                  return true;
                });
        dragDialogBuilder.build().show();
        return true;
      case PreferencesConstants.FRAGMENT_THEME:
        sort = getResources().getStringArray(R.array.theme);
        current = Integer.parseInt(sharedPref.getString(PreferencesConstants.FRAGMENT_THEME, "0"));
        builder = new MaterialDialog.Builder(getActivity());
        // builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        builder
            .items(sort)
            .itemsCallbackSingleChoice(
                current,
                (dialog, view, which, text) -> {
                  utilsProvider.getThemeManager().setAppTheme(AppTheme.getTheme(which));
                  dialog.dismiss();
                  restartPC(getActivity());
                  return true;
                });
        builder.title(R.string.theme);
        builder.build().show();
        return true;
      case PreferencesConstants.FRAGMENT_FEEDBACK:
        Intent emailIntent = Utils.buildEmailIntent(null, EMAIL_SUPPORT);

        PackageManager packageManager = getActivity().getPackageManager();
        List activities =
            packageManager.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        boolean isIntentSafe = activities.size() > 0;

        if (isIntentSafe)
          startActivity(Intent.createChooser(emailIntent, getResources().getString(feedback)));
        else
          Toast.makeText(
                  getActivity(),
                  getResources().getString(R.string.send_email_to) + " " + EMAIL_SUPPORT,
                  Toast.LENGTH_LONG)
              .show();
        return false;
      case PreferencesConstants.FRAGMENT_ABOUT:
        startActivity(new Intent(getActivity(), AboutActivity.class));
        return false;
        /*FROM HERE BE FRAGMENTS*/
      case PreferencesConstants.FRAGMENT_COLORS:
        ((PreferencesActivity) getActivity()).selectItem(PreferencesActivity.COLORS_PREFERENCE);
        return true;
      case PreferencesConstants.FRAGMENT_FOLDERS:
        ((PreferencesActivity) getActivity()).selectItem(PreferencesActivity.FOLDERS_PREFERENCE);
        return true;
      case PreferencesConstants.FRAGMENT_QUICKACCESSES:
        ((PreferencesActivity) getActivity())
            .selectItem(PreferencesActivity.QUICKACCESS_PREFERENCE);
        return true;
      case PreferencesConstants.FRAGMENT_ADVANCED_SEARCH:
        ((PreferencesActivity) getActivity())
            .selectItem(PreferencesActivity.ADVANCEDSEARCH_PREFERENCE);
        return true;
      case PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD:
        MaterialDialog.Builder masterPasswordDialogBuilder =
            new MaterialDialog.Builder(getActivity());
        masterPasswordDialogBuilder.title(
            getResources().getString(R.string.crypt_pref_master_password_title));

        String decryptedPassword = null;
        try {
          String preferencePassword =
              sharedPref.getString(
                  PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                  PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT);
          if (!preferencePassword.equals(
              PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT)) {

            // password is set, try to decrypt
            decryptedPassword = CryptUtil.decryptPassword(getActivity(), preferencePassword);
          } else {
            // no password set in preferences, just leave the field empty
            decryptedPassword = "";
          }
        } catch (GeneralSecurityException | IOException e) {
          e.printStackTrace();
        }

        masterPasswordDialogBuilder.input(
            getResources().getString(R.string.authenticate_password),
            decryptedPassword,
            true,
            (dialog, input) -> {});
        masterPasswordDialogBuilder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
        masterPasswordDialogBuilder.positiveText(getResources().getString(R.string.ok));
        masterPasswordDialogBuilder.negativeText(getResources().getString(R.string.cancel));
        masterPasswordDialogBuilder.positiveColor(((ThemedActivity) getActivity()).getAccent());
        masterPasswordDialogBuilder.negativeColor(((ThemedActivity) getActivity()).getAccent());

        masterPasswordDialogBuilder.onPositive(
            (dialog, which) -> {
              try {

                String inputText = dialog.getInputEditText().getText().toString();
                if (!inputText.equals(
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT)) {

                  sharedPref
                      .edit()
                      .putString(
                          PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                          CryptUtil.encryptPassword(
                              getActivity(), dialog.getInputEditText().getText().toString()))
                      .apply();
                } else {
                  // empty password, remove the preference
                  sharedPref
                      .edit()
                      .putString(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD, "")
                      .apply();
                }
              } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                sharedPref
                    .edit()
                    .putString(
                        PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                        PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT)
                    .apply();
              }
            });

        masterPasswordDialogBuilder.onNegative((dialog, which) -> dialog.cancel());

        masterPasswordDialogBuilder.build().show();
        return true;
        // If there is no directory set, default to storage root (/storage/sdcard...)
      case PreferencesConstants.PREFERENCE_ZIP_EXTRACT_PATH:
        new FolderChooserDialog.Builder(getActivity())
            .tag(PreferencesConstants.PREFERENCE_ZIP_EXTRACT_PATH)
            .goUpLabel(getString(R.string.folder_go_up_one_level))
            .chooseButton(R.string.choose_folder)
            .cancelButton(R.string.cancel)
            .initialPath(
                sharedPref.getString(
                    PreferencesConstants.PREFERENCE_ZIP_EXTRACT_PATH,
                    Environment.getExternalStorageDirectory().getPath()))
            .build()
            .show((PreferencesActivity) getActivity());
        return true;
    }

    return false;
  }

  public static void restartPC(final Activity activity) {
    if (activity == null) return;

    final int enter_anim = android.R.anim.fade_in;
    final int exit_anim = android.R.anim.fade_out;
    activity.overridePendingTransition(enter_anim, exit_anim);
    activity.finish();
    activity.overridePendingTransition(enter_anim, exit_anim);
    activity.startActivity(activity.getIntent());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View v = super.onCreateView(inflater, container, savedInstanceState);
    /** This is a hack see {@link PreferencesActivity#saveListViewState(int, Parcelable)} */
    listView = v.findViewById(android.R.id.list);
    return v;
  }

  @Override
  public void onPause() {
    super.onPause();

    if (listView != null) {
      /** This is a hack see {@link PreferencesActivity#saveListViewState(int, Parcelable)} */
      ((PreferencesActivity) getActivity())
          .saveListViewState(START_PREFERENCE, listView.onSaveInstanceState());
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    if (listView != null) {
      /** This is a hack see {@link PreferencesActivity#saveListViewState(int, Parcelable)} */
      Parcelable restored =
          ((PreferencesActivity) getActivity()).restoreListViewState(START_PREFERENCE);
      if (restored != null) {
        listView.onRestoreInstanceState(restored);
      }
    }
  }
}
