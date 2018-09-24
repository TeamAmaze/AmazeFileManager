/*
 * FTPServerFragment.java
 *
 * Copyright © 2016-2018 Yashwanth Reddy Gondi, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.asynchronous.services.ftp.FtpService;
import com.amaze.filemanager.utils.OneCharacterCharSequence;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.files.CryptUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;

/**
 * Created by yashwanthreddyg on 10-06-2016.
 * Edited by Luca D'Amico (Luca91) on 25 Jul 2017 (Fixed FTP Server while usi
 */
public class FtpServerFragment extends Fragment {

    private MainActivity mainActivity;

    private TextView statusText, url, username, password, port, sharedPath;
    private AppCompatEditText usernameEditText, passwordEditText;
    private TextInputLayout usernameTextInput, passwordTextInput;
    private AppCompatCheckBox mAnonymousCheckBox, mSecureCheckBox;
    private Button ftpBtn;
    private int accentColor;
    private Spanned spannedStatusNoConnection, spannedStatusConnected, spannedStatusUrl;
    private Spanned spannedStatusSecure, spannedStatusNotRunning;
    private ImageButton ftpPasswordVisibleButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ftp, container, false);
        statusText = rootView.findViewById(R.id.text_view_ftp_status);
        url = rootView.findViewById(R.id.text_view_ftp_url);
        username = rootView.findViewById(R.id.text_view_ftp_username);
        password = rootView.findViewById(R.id.text_view_ftp_password);
        port = rootView.findViewById(R.id.text_view_ftp_port);
        sharedPath = rootView.findViewById(R.id.text_view_ftp_path);
        ftpBtn = rootView.findViewById(R.id.startStopButton);
        View startDividerView = rootView.findViewById(R.id.divider_ftp_start);
        View statusDividerView = rootView.findViewById(R.id.divider_ftp_status);
        ftpPasswordVisibleButton = rootView.findViewById(R.id.ftp_password_visible);
        accentColor = mainActivity.getAccent();

        updateSpans();
        updateStatus();

        switch (mainActivity.getAppTheme().getSimpleTheme()) {
            case LIGHT:
                startDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider));
                statusDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider));
                break;
            case DARK:
            case BLACK:
                startDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider_dark_card));
                statusDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider_dark_card));
                break;
        }

        ftpBtn.setOnClickListener(v -> {
            if (!FtpService.isRunning()) {
                if (FtpService.isConnectedToWifi(getContext())
                        || FtpService.isConnectedToLocalNetwork(getContext())
                        || FtpService.isEnabledWifiHotspot(getContext()))
                    startServer();
                else {
                    // no wifi and no eth, we shouldn't be here in the first place, because of broadcast
                    // receiver, but just to be sure
                    statusText.setText(spannedStatusNoConnection);
                }
            } else {
                stopServer();
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        mainActivity.getAppbar().setTitle(R.string.ftp);
        mainActivity.floatingActionButton.getMenuButton().hide();
        mainActivity.getAppbar().getBottomBar().setVisibility(View.GONE);
        mainActivity.supportInvalidateOptionsMenu();

        int skin_color = mainActivity.getCurrentColorPreference().primaryFirstTab;
        int skinTwoColor = mainActivity.getCurrentColorPreference().primarySecondTab;

        mainActivity.updateViews(new ColorDrawable(MainActivity.currentTab==1 ?
                skinTwoColor : skin_color));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.choose_ftp_port:
                int currentFtpPort = getDefaultPortFromPreferences();

                new MaterialDialog.Builder(getContext())
                        .input(getString(R.string.ftp_port_edit_menu_title), Integer.toString(currentFtpPort), true, (dialog, input) -> {})
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .onPositive((dialog, which) -> {
                            EditText editText = dialog.getInputEditText();
                            if (editText != null) {
                                String name = editText.getText().toString();

                                int portNumber = Integer.parseInt(name);
                                if (portNumber < 1024) {
                                    Toast.makeText(getActivity(), R.string.ftp_port_change_error_invalid, Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    changeFTPServerPort(portNumber);
                                    Toast.makeText(getActivity(), R.string.ftp_port_change_success, Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        })
                        .positiveText(getString(R.string.change).toUpperCase())
                        .negativeText(R.string.cancel)
                        .build()
                        .show();
                return true;
            case R.id.ftp_path:
                MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(getContext());
                dialogBuilder.title(getString(R.string.ftp_path));
                dialogBuilder.input(getString(R.string.ftp_path_hint),
                        getDefaultPathFromPreferences(),
                        false, (dialog, input) -> {});
                dialogBuilder.onPositive((dialog, which) -> {
                    EditText editText = dialog.getInputEditText();
                    if (editText != null) {
                        String path = editText.getText().toString();

                        File pathFile = new File(path);
                        if (pathFile.exists() && pathFile.isDirectory()) {

                            changeFTPServerPath(pathFile.getPath());

                            Toast.makeText(getActivity(), R.string.ftp_path_change_success,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            // try to get parent
                            File pathParentFile = new File(pathFile.getParent());
                            if (pathParentFile.exists() && pathParentFile.isDirectory()) {

                                changeFTPServerPath(pathParentFile.getPath());
                                Toast.makeText(getActivity(), R.string.ftp_path_change_success,
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                // don't have access, print error

                                Toast.makeText(getActivity(), R.string.ftp_path_change_error_invalid,
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                });

                dialogBuilder.positiveText(getString(R.string.change).toUpperCase())
                        .negativeText(R.string.cancel)
                        .build()
                        .show();
                return true;
            case R.id.ftp_login:
                MaterialDialog.Builder loginDialogBuilder = new MaterialDialog.Builder(getContext());

                LayoutInflater inflater = getActivity().getLayoutInflater();
                View rootView = inflater.inflate(R.layout.dialog_ftp_login, null);
                initLoginDialogViews(rootView);

                loginDialogBuilder.customView(rootView, true);

                loginDialogBuilder.title(getString(R.string.ftp_login));

                loginDialogBuilder.onPositive((dialog, which) -> {
                    if (mAnonymousCheckBox.isChecked()) {

                        // remove preferences
                        setFTPUsername("");
                        setFTPPassword("");
                    } else {

                        if (passwordEditText.getText().toString().equals("")) {
                            passwordTextInput.setError(getString(R.string.field_empty));
                        } else if (usernameEditText.getText().toString().equals("")) {
                            usernameTextInput.setError(getString(R.string.field_empty));
                        } else {

                            // password and username field not empty, let's set them to preferences
                            setFTPUsername(usernameEditText.getText().toString());
                            setFTPPassword(passwordEditText.getText().toString());
                        }
                    }

                    if (mSecureCheckBox.isChecked()) {
                        setSecurePreference(true);
                    } else setSecurePreference(false);
                });

                loginDialogBuilder.positiveText(getString(R.string.set).toUpperCase())
                        .negativeText(getString(R.string.cancel))
                        .build()
                        .show();

                return true;
            case R.id.ftp_timeout:
                MaterialDialog.Builder timeoutBuilder = new MaterialDialog.Builder(getActivity());

                timeoutBuilder.title(getString(R.string.ftp_timeout) + " (" +
                        getResources().getString(R.string.ftp_seconds) + ")");
                timeoutBuilder.input(String.valueOf(FtpService.DEFAULT_TIMEOUT + " " +
                                getResources().getString(R.string.ftp_seconds)), String.valueOf(getFTPTimeout()),
                        true, (dialog, input) -> {
                            boolean isInputInteger;
                            try {
                                // try parsing for integer check
                                Integer.parseInt(input.toString());
                                isInputInteger = true;
                            } catch (NumberFormatException e) {
                                isInputInteger = false;
                            }

                            if (input.length()==0 || !isInputInteger)
                                setFTPTimeout(FtpService.DEFAULT_TIMEOUT);
                            else
                                setFTPTimeout(Integer.valueOf(input.toString()));
                        });
                timeoutBuilder.positiveText(getResources().getString(R.string.set).toUpperCase())
                        .negativeText(getResources().getString(R.string.cancel))
                        .build()
                        .show();
                return true;
        }

        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mainActivity.getMenuInflater().inflate(R.menu.ftp_server_menu, menu);
    }

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if ((netInfo != null && (netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET))
                    || FtpService.isEnabledWifiHotspot(getContext())) {
                // connected to wifi or eth
                ftpBtn.setEnabled(true);
            } else {
                // wifi or eth connection lost
                stopServer();
                statusText.setText(spannedStatusNoConnection);
                ftpBtn.setEnabled(true);
                ftpBtn.setEnabled(false);
                ftpBtn.setText(getResources().getString(R.string.start_ftp).toUpperCase());
            }
        }
    };

    private BroadcastReceiver ftpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSpans();
            switch (intent.getAction()) {
                case FtpService.ACTION_STARTED:
                    if (getSecurePreference()) {
                        statusText.setText(spannedStatusSecure);
                    } else {
                        statusText.setText(spannedStatusConnected);
                    }
                    url.setText(spannedStatusUrl);
                    ftpBtn.setText(getResources().getString(R.string.stop_ftp).toUpperCase());
                    break;
                case FtpService.ACTION_FAILEDTOSTART:
                    statusText.setText(spannedStatusNotRunning);

                    Toast.makeText(getContext(),
                            getResources().getString(R.string.unknown_error), Toast.LENGTH_LONG).show();

                    ftpBtn.setText(getResources().getString(R.string.start_ftp).toUpperCase());
                    url.setText("URL: ");
                    break;
                case FtpService.ACTION_STOPPED:
                    statusText.setText(spannedStatusNotRunning);
                    url.setText("URL: ");
                    ftpBtn.setText(getResources().getString(R.string.start_ftp).toUpperCase());
                    break;
            }
        }
    };

    /**
     * Sends a broadcast to start ftp server
     */
    private void startServer() {
        getContext().sendBroadcast(new Intent(FtpService.ACTION_START_FTPSERVER).setPackage(getContext().getPackageName()));
    }

    /**
     * Sends a broadcast to stop ftp server
     */
    private void stopServer() {
        getContext().sendBroadcast(new Intent(FtpService.ACTION_STOP_FTPSERVER).setPackage(getContext().getPackageName()));
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getContext().registerReceiver(mWifiReceiver, wifiFilter);
        IntentFilter ftpFilter = new IntentFilter();
        ftpFilter.addAction(FtpService.ACTION_STARTED);
        ftpFilter.addAction(FtpService.ACTION_STOPPED);
        ftpFilter.addAction(FtpService.ACTION_FAILEDTOSTART);
        getContext().registerReceiver(ftpReceiver, ftpFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mWifiReceiver);
        getContext().unregisterReceiver(ftpReceiver);
    }

    /**
     * Update UI widgets after change in shared preferences
     */
    private void updateStatus() {

        if (!FtpService.isRunning()) {
            if (!FtpService.isConnectedToWifi(getContext())
                    && !FtpService.isConnectedToLocalNetwork(getContext())
                    && !FtpService.isEnabledWifiHotspot(getContext())) {
                statusText.setText(spannedStatusNoConnection);
                ftpBtn.setEnabled(false);
            } else {
                statusText.setText(spannedStatusNotRunning);
                ftpBtn.setEnabled(true);
            }
            url.setText("URL: ");
            ftpBtn.setText(getResources().getString(R.string.start_ftp).toUpperCase());

        } else {
            accentColor = mainActivity.getAccent();
            url.setText(spannedStatusUrl);
            statusText.setText(spannedStatusConnected);
            ftpBtn.setEnabled(true);
            ftpBtn.setText(getResources().getString(R.string.stop_ftp).toUpperCase());
        }

        final String passwordDecrypted = getPasswordFromPreferences();
        final CharSequence passwordBulleted = new OneCharacterCharSequence('\u25CF', passwordDecrypted.length());

        username.setText(getResources().getString(R.string.username) + ": " + getUsernameFromPreferences());
        password.setText(getResources().getString(R.string.password) + ": " + passwordBulleted);

        ftpPasswordVisibleButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_grey600_24dp));

        if (passwordDecrypted.equals("")) {
            ftpPasswordVisibleButton.setVisibility(View.GONE);
        } else {
            ftpPasswordVisibleButton.setVisibility(View.VISIBLE);
        }

        ftpPasswordVisibleButton.setOnClickListener(v -> {
            if (password.getText().toString().contains("\u25CF")) {
                // password was not visible, let's make it visible
                password.setText(getResources().getString(R.string.password) + ": " +
                        passwordDecrypted);
                ftpPasswordVisibleButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_off_grey600_24dp));
            } else {
                // password was visible, let's hide it
                password.setText(getResources().getString(R.string.password) + ": " + passwordBulleted);
                ftpPasswordVisibleButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_grey600_24dp));
            }
        });

        port.setText(getResources().getString(R.string.ftp_port) + ": " +
                getDefaultPortFromPreferences());
        sharedPath.setText(getResources().getString(R.string.ftp_path) + ": " +
                getDefaultPathFromPreferences());
    }

    /**
     * Updates the status spans
     */
    private void updateSpans() {

        String ftpAddress = getFTPAddressString();

        if(ftpAddress == null) {
            ftpAddress = "";
            Toast.makeText(getContext(), getResources().getString(R.string.local_inet_addr_error), Toast.LENGTH_SHORT).show();
        }

        String statusHead = getResources().getString(R.string.ftp_status_title) + ": ";

        spannedStatusConnected = Html.fromHtml(statusHead + "<b>&nbsp;&nbsp;" +
                "<font color='" + accentColor + "'>"
                + getResources().getString(R.string.ftp_status_running) + "</font></b>");
        spannedStatusUrl = Html.fromHtml("URL:&nbsp;" + ftpAddress);
        spannedStatusNoConnection = Html.fromHtml(statusHead + "<b>&nbsp;&nbsp;&nbsp;&nbsp;" +
                "<font color='" + Utils.getColor(getContext(), android.R.color.holo_red_light) + "'>"
                + getResources().getString(R.string.ftp_status_no_connection) + "</font></b>");

        spannedStatusNotRunning = Html.fromHtml(statusHead + "<b>&nbsp;&nbsp;&nbsp;&nbsp;" +
                getResources().getString(R.string.ftp_status_not_running) + "</b>");
        spannedStatusSecure = Html.fromHtml(statusHead + "<b>&nbsp;&nbsp;&nbsp;&nbsp;" +
                "<font color='" + Utils.getColor(getContext(), android.R.color.holo_green_light) + "'>"
                + getResources().getString(R.string.ftp_status_secure_connection) + "</font></b>");
        spannedStatusUrl = Html.fromHtml("URL:&nbsp;" + ftpAddress);
    }

    private void initLoginDialogViews(View loginDialogView) {

        usernameEditText = loginDialogView.findViewById(R.id.edit_text_dialog_ftp_username);
        passwordEditText = loginDialogView.findViewById(R.id.edit_text_dialog_ftp_password);
        usernameTextInput = loginDialogView.findViewById(R.id.text_input_dialog_ftp_username);
        passwordTextInput = loginDialogView.findViewById(R.id.text_input_dialog_ftp_password);
        mAnonymousCheckBox = loginDialogView.findViewById(R.id.checkbox_ftp_anonymous);
        mSecureCheckBox = loginDialogView.findViewById(R.id.checkbox_ftp_secure);

        mAnonymousCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                usernameEditText.setEnabled(false);
                passwordEditText.setEnabled(false);
            } else {
                usernameEditText.setEnabled(true);
                passwordEditText.setEnabled(true);
            }
        });

        // init dialog views as per preferences
        if (getUsernameFromPreferences().equals(FtpService.DEFAULT_USERNAME)) {
            mAnonymousCheckBox.setChecked(true);
        } else {

            usernameEditText.setText(getUsernameFromPreferences());
            passwordEditText.setText(getPasswordFromPreferences());
        }

        if (getSecurePreference()) {
            mSecureCheckBox.setChecked(true);
        } else mSecureCheckBox.setChecked(false);
    }

    /**
     * @return address at which server is running
     */
    @Nullable
    private String getFTPAddressString() {
        InetAddress ia = FtpService.getLocalInetAddress(getContext());
        if(ia == null) return null;

        return (getSecurePreference() ? FtpService.INITIALS_HOST_SFTP : FtpService.INITIALS_HOST_FTP)
                + ia.getHostAddress()  + ":" + getDefaultPortFromPreferences();
    }

    private int getDefaultPortFromPreferences() {
        return mainActivity.getPrefs().getInt(FtpService.PORT_PREFERENCE_KEY, FtpService.DEFAULT_PORT);
    }

    private String getUsernameFromPreferences() {
        return mainActivity.getPrefs().getString(FtpService.KEY_PREFERENCE_USERNAME, FtpService.DEFAULT_USERNAME);
    }

    private String getPasswordFromPreferences() {
        try {
            String encryptedPassword = mainActivity.getPrefs().getString(FtpService.KEY_PREFERENCE_PASSWORD, "");

            if (encryptedPassword.equals("")) {
                return "";
            } else {
                return CryptUtil.decryptPassword(getContext(), encryptedPassword);
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();

            Toast.makeText(getContext(), getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            // can't decrypt the password saved in preferences, remove the preference altogether
            mainActivity.getPrefs().edit().putString(FtpService.KEY_PREFERENCE_PASSWORD, "").apply();
            return "";
        }
    }

    private String getDefaultPathFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        return preferences.getString(FtpService.KEY_PREFERENCE_PATH, FtpService.DEFAULT_PATH);
    }

    private void changeFTPServerPort(int port) {
        mainActivity.getPrefs().edit()
                .putInt(FtpService.PORT_PREFERENCE_KEY, port)
                .apply();

        // first update spans which will point to an updated status
        updateSpans();
        updateStatus();
    }

    private void changeFTPServerPath(String path) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.edit().putString(FtpService.KEY_PREFERENCE_PATH, path).apply();

        updateStatus();

    }

    private void setFTPUsername(String username) {
        mainActivity.getPrefs().edit().putString(FtpService.KEY_PREFERENCE_USERNAME, username).apply();
        updateStatus();
    }

    private void setFTPPassword(String password) {
        try {
            mainActivity.getPrefs().edit().putString(FtpService.KEY_PREFERENCE_PASSWORD, CryptUtil.encryptPassword(getContext(), password)).apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), getResources().getString(R.string.error), Toast.LENGTH_LONG).show();
        }
        updateStatus();
    }

    /**
     * Returns timeout from preferences
     * @return timeout in seconds
     */
    private int getFTPTimeout() {
        return mainActivity.getPrefs().getInt(FtpService.KEY_PREFERENCE_TIMEOUT, FtpService.DEFAULT_TIMEOUT);
    }

    private void setFTPTimeout(int seconds) {
        mainActivity.getPrefs().edit().putInt(FtpService.KEY_PREFERENCE_TIMEOUT, seconds).apply();
    }

    private boolean getSecurePreference() {
        return mainActivity.getPrefs().getBoolean(FtpService.KEY_PREFERENCE_SECURE, FtpService.DEFAULT_SECURE);
    }

    private void setSecurePreference(boolean isSecureEnabled) {
        mainActivity.getPrefs().edit().putBoolean(FtpService.KEY_PREFERENCE_SECURE, isSecureEnabled).apply();
    }
}
