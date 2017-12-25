/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 *                      Emmanuel Messulam <emmanuelbendavid@gmail.com>
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

package com.amaze.filemanager.fragments.preference_fragments;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.AboutActivity;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.ui.views.preference.CheckBox;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;
import static com.amaze.filemanager.R.string.feedback;

public class PrefFrag extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final int START_PREFERENCE_CHANGE_DRAWER_BACKGROUND = 1;

    private static final String[] PREFERENCE_KEYS = {PreferencesConstants.PREFERENCE_GRID_COLUMNS,
            PreferencesConstants.FRAGMENT_THEME, PreferencesConstants.PREFERENCE_ROOTMODE,
            PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES,
            PreferencesConstants.PREFERENCE_CHANGE_DRAWER_BACKGROUND, PreferencesConstants.FRAGMENT_FEEDBACK,
            PreferencesConstants.FRAGMENT_ABOUT, PreferencesConstants.FRAGMENT_COLORS,
            PreferencesConstants.FRAGMENT_FOLDERS, PreferencesConstants.FRAGMENT_QUICKACCESSES,
            PreferencesConstants.FRAGMENT_ADVANCED_SEARCH};

    private UtilitiesProviderInterface utilsProvider;
    private SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = (UtilitiesProviderInterface) getActivity();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        for (String PREFERENCE_KEY : PREFERENCE_KEYS) {
            findPreference(PREFERENCE_KEY).setOnPreferenceClickListener(this);
        }

        // crypt master password
        final Preference masterPasswordPreference = findPreference(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 ||
                sharedPref.getBoolean(PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT, false)) {
            // encryption feature not available
            masterPasswordPreference.setEnabled(false);
        }
        masterPasswordPreference.setOnPreferenceClickListener(this);

        CheckBox checkBoxFingerprint = (CheckBox) findPreference(PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT);

        try {

            // finger print sensor
            final FingerprintManager fingerprintManager = (FingerprintManager)
                    getActivity().getSystemService(Context.FINGERPRINT_SERVICE);

            final KeyguardManager keyguardManager = (KeyguardManager)
                    getActivity().getSystemService(Context.KEYGUARD_SERVICE);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && fingerprintManager.isHardwareDetected()) {

                checkBoxFingerprint.setEnabled(true);
            }

            checkBoxFingerprint.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    if (ActivityCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.crypt_fingerprint_no_permission),
                                Toast.LENGTH_LONG).show();
                        return false;
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            !fingerprintManager.hasEnrolledFingerprints()) {
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.crypt_fingerprint_not_enrolled),
                                Toast.LENGTH_LONG).show();
                        return false;
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            !keyguardManager.isKeyguardSecure()) {
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.crypt_fingerprint_no_security),
                                Toast.LENGTH_LONG).show();
                        return false;
                    }

                    masterPasswordPreference.setEnabled(false);
                    return true;
                }
            });
        } catch (NoClassDefFoundError error) {
            error.printStackTrace();

            // fingerprint manager class not defined in the framework
            checkBoxFingerprint.setEnabled(false);
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String[] sort;
        MaterialDialog.Builder builder;

        switch (preference.getKey()) {
            case PreferencesConstants.PREFERENCE_GRID_COLUMNS:
                sort = getResources().getStringArray(R.array.columns);
                builder = new MaterialDialog.Builder(getActivity());
                builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
                builder.title(R.string.gridcolumnno);
                int current = Integer.parseInt(sharedPref.getString(PreferencesConstants.PREFERENCE_GRID_COLUMNS, "-1"));
                current = current == -1 ? 0 : current;
                if (current != 0) current = current - 1;
                builder.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString(PreferencesConstants.PREFERENCE_GRID_COLUMNS, "" + (which != 0 ? sort[which] : "" + -1)).commit();
                        dialog.dismiss();
                        return true;
                    }
                });
                builder.build().show();
                return true;
            case PreferencesConstants.FRAGMENT_THEME:
                sort = getResources().getStringArray(R.array.theme);
                current = Integer.parseInt(sharedPref.getString(PreferencesConstants.FRAGMENT_THEME, "0"));
                builder = new MaterialDialog.Builder(getActivity());
                //builder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
                builder.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        utilsProvider.getThemeManager().setAppTheme(AppTheme.getTheme(which));
                        dialog.dismiss();
                        restartPC(getActivity());
                        return true;
                    }
                });
                builder.title(R.string.theme);
                builder.build().show();
            case PreferencesConstants.PREFERENCE_CHANGE_DRAWER_BACKGROUND:
                String action = SDK_INT < Build.VERSION_CODES.KITKAT? Intent.ACTION_GET_CONTENT:Intent.ACTION_OPEN_DOCUMENT;
                Intent intent = new Intent(action);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, START_PREFERENCE_CHANGE_DRAWER_BACKGROUND);
                return true;
            case PreferencesConstants.FRAGMENT_FEEDBACK:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "vishalmeham2@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback : Amaze File Manager");

                PackageManager packageManager = getActivity().getPackageManager();
                List activities = packageManager.queryIntentActivities(emailIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                boolean isIntentSafe = activities.size() > 0;

                if (isIntentSafe)
                    startActivity(Intent.createChooser(emailIntent, getResources().getString(feedback)));
                else
                    Toast.makeText(getActivity(), getResources().getString(R.string.send_email_to)
                            + " vishalmeham2@gmail.com", Toast.LENGTH_LONG).show();
                return false;
            case PreferencesConstants.FRAGMENT_ABOUT:
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return false;
            /*FROM HERE BE FRAGMENTS*/
            case PreferencesConstants.FRAGMENT_COLORS:
                ((PreferencesActivity) getActivity())
                        .selectItem(PreferencesActivity.COLORS_PREFERENCE);
                return true;
            case PreferencesConstants.FRAGMENT_FOLDERS:
                ((PreferencesActivity) getActivity())
                        .selectItem(PreferencesActivity.FOLDERS_PREFERENCE);
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
                MaterialDialog.Builder masterPasswordDialogBuilder = new MaterialDialog.Builder(getActivity());
                masterPasswordDialogBuilder.title(getResources().getString(R.string.crypt_pref_master_password_title));

                String decryptedPassword = null;
                try {
                    String preferencePassword = sharedPref.getString(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                            PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT);
                    if (!preferencePassword.equals(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT)) {

                        // password is set, try to decrypt
                        decryptedPassword = CryptUtil.decryptPassword(getActivity(), preferencePassword);
                    } else {
                        // no password set in preferences, just leave the field empty
                        decryptedPassword = "";
                    }
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }

                masterPasswordDialogBuilder.input(getResources().getString(R.string.authenticate_password),
                        decryptedPassword, false,
                        new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                            }
                        });
                masterPasswordDialogBuilder.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
                masterPasswordDialogBuilder.positiveText(getResources().getString(R.string.ok));
                masterPasswordDialogBuilder.negativeText(getResources().getString(R.string.cancel));
                masterPasswordDialogBuilder.positiveColor(utilsProvider.getColorPreference().getColor(ColorUsage.ACCENT));
                masterPasswordDialogBuilder.negativeColor(utilsProvider.getColorPreference().getColor(ColorUsage.ACCENT));

                masterPasswordDialogBuilder.onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {

                            String inputText = dialog.getInputEditText().getText().toString();
                            if (!inputText.equals(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT)) {

                                sharedPref.edit().putString(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                                        CryptUtil.encryptPassword(getActivity(),
                                                dialog.getInputEditText().getText().toString())).apply();
                            } else {
                                // empty password, remove the preference
                                sharedPref.edit().putString(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                                        "").apply();
                            }
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                            sharedPref.edit().putString(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT).apply();
                        }
                    }
                });

                masterPasswordDialogBuilder.onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.cancel();
                    }
                });

                masterPasswordDialogBuilder.build().show();
                return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case START_PREFERENCE_CHANGE_DRAWER_BACKGROUND:
                if (sharedPref != null && data != null && data.getData() != null) {
                    if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getActivity().getContentResolver().takePersistableUriPermission(data.getData(),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    sharedPref.edit()
                            .putString(PreferencesConstants.PREFERENCE_DRAWER_HEADER_PATH, data.getData().toString())
                            .apply();
                }
                break;
        }
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

}
