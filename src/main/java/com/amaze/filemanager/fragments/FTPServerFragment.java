package com.amaze.filemanager.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.services.ftpservice.FTPService;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;

/**
 * Created by yashwanthreddyg on 10-06-2016.
 */
public class FTPServerFragment extends Fragment {

    TextView statusText, warningText, ftpAddrText;
    Button ftpBtn;
    private MainActivity mainActivity;
    private View rootView;

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                warningText.setText("");
            } else {
                stopServer();
                statusText.setText(getResources().getString(R.string.ftp_status_not_running));
                warningText.setText(getResources().getString(R.string.ftp_no_wifi));
                ftpAddrText.setText("");
                ftpBtn.setText(getResources().getString(R.string.start_ftp));
            }
        }
    };
    private BroadcastReceiver ftpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == FTPService.ACTION_STARTED) {
                statusText.setText(getResources().getString(R.string.ftp_status_running));
                warningText.setText("");
                ftpAddrText.setText(getFTPAddressString());
                ftpBtn.setText(getResources().getString(R.string.stop_ftp));
            } else if (action == FTPService.ACTION_FAILEDTOSTART) {
                statusText.setText(getResources().getString(R.string.ftp_status_not_running));
                warningText.setText("Oops! Something went wrong");
                ftpAddrText.setText("");
                ftpBtn.setText(getResources().getString(R.string.start_ftp));
            } else if (action == FTPService.ACTION_STOPPED) {
                statusText.setText(getResources().getString(R.string.ftp_status_not_running));
                ftpAddrText.setText("");
                ftpBtn.setText(getResources().getString(R.string.start_ftp));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        switch (item.getItemId()) {
            case R.id.choose_ftp_port:
                int currentFtpPort = FTPService.getDefaultPortFromPreferences(preferences);

                new MaterialDialog.Builder(getActivity())
                        .input(getString(R.string.ftp_port_edit_menu_title), Integer.toString(currentFtpPort), true, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                            }
                        })
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                EditText editText = dialog.getInputEditText();
                                if (editText != null) {
                                    String name = editText.getText().toString();

                                    int portNumber = Integer.parseInt(name);
                                    if (portNumber < 1024) {
                                        Toast.makeText(getActivity(), R.string.ftp_port_change_error_invalid, Toast.LENGTH_SHORT)
                                                .show();
                                    } else {
                                        FTPService.changeFTPServerPort(preferences, portNumber);
                                        Toast.makeText(getActivity(), R.string.ftp_port_change_success, Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            }
                        })
                        .positiveText(getResources().getString(R.string.change).toUpperCase())
                        .negativeText(R.string.cancel)
                        .build()
                        .show();
                return true;
            case R.id.ftp_path:
                MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(getActivity());
                dialogBuilder.title(getString(R.string.ftp_path));
                dialogBuilder.input(getString(R.string.ftp_path_hint),
                        FTPService.getDefaultPathFromPreferences(preferences),
                        false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                            }
                        });
                dialogBuilder.onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        EditText editText = dialog.getInputEditText();
                        if (editText != null) {
                            String path = editText.getText().toString();

                            File pathFile = new File(path);
                            if (pathFile.exists() && pathFile.isDirectory()) {

                                FTPService.changeFTPServerPath(preferences, pathFile.getPath());
                                Toast.makeText(getActivity(), R.string.ftp_path_change_success,
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                // try to get parent
                                File pathParentFile = new File(pathFile.getParent());
                                if (pathParentFile.exists() && pathParentFile.isDirectory()) {

                                    FTPService.changeFTPServerPath(preferences, pathParentFile.getPath());
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
                    }
                });

                dialogBuilder.positiveText(getResources().getString(R.string.change).toUpperCase())
                        .negativeText(R.string.cancel)
                        .build()
                        .show();
        }

        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mainActivity.getMenuInflater().inflate(R.menu.ftp_server_menu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ftp, container, false);
        statusText =(TextView) rootView.findViewById(R.id.statusText);
        warningText = (TextView) rootView.findViewById(R.id.warningText);
        ftpAddrText = (TextView) rootView.findViewById(R.id.ftpAddressText);
        ftpBtn = (Button) rootView.findViewById(R.id.startStopButton);

        ImageView ftpImage = (ImageView)rootView.findViewById(R.id.ftp_image);

        //light theme
        if (mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
            ftpImage.setImageResource(R.drawable.ic_ftp_light);
        } else {
            //dark
            ftpImage.setImageResource(R.drawable.ic_ftp_dark);
        }

        ftpBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!FTPService.isRunning()) {
                    if (FTPService.isConnectedToWifi(getContext()))
                        startServer();
                    else
                        warningText.setText(getResources().getString(R.string.ftp_no_wifi));
                } else {
                    stopServer();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        mainActivity.setActionBarTitle(getResources().getString(R.string.ftp));
        mainActivity.floatingActionButton.hideMenuButton(true);
        mainActivity.buttonBarFrame.setVisibility(View.GONE);
        mainActivity.supportInvalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Sends a broadcast to start ftp server
     */
    private void startServer() {
        getContext().sendBroadcast(new Intent(FTPService.ACTION_START_FTPSERVER));
    }

    /**
     * Sends a broadcast to stop ftp server
     */
    private void stopServer() {
        getContext().sendBroadcast(new Intent(FTPService.ACTION_STOP_FTPSERVER));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatus();
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getContext().registerReceiver(mWifiReceiver, wifiFilter);
        IntentFilter ftpFilter = new IntentFilter();
        ftpFilter.addAction(FTPService.ACTION_STARTED);
        ftpFilter.addAction(FTPService.ACTION_STOPPED);
        ftpFilter.addAction(FTPService.ACTION_FAILEDTOSTART);
        getContext().registerReceiver(ftpReceiver, ftpFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(mWifiReceiver);
        getContext().unregisterReceiver(ftpReceiver);
    }

    /**
     * Update UI widgets based on connection status
     */
    private void updateStatus() {
        if (FTPService.isRunning()) {
            statusText.setText(getResources().getString(R.string.ftp_status_running));
            ftpBtn.setText(getResources().getString(R.string.stop_ftp));
            ftpAddrText.setText(getFTPAddressString());
        } else {
            statusText.setText(getResources().getString(R.string.ftp_status_not_running));
            ftpBtn.setText(getResources().getString(R.string.start_ftp));
        }
    }

    /**
     * @return address at which server is running
     */
    private String getFTPAddressString() {
        return "ftp://" + FTPService.getLocalInetAddress(getContext()).getHostAddress() + ":" + FTPService.getPort();
    }
}
