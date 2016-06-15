package com.amaze.filemanager.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.AppsAdapter;
import com.amaze.filemanager.services.ftpservice.FTPService;
import com.amaze.filemanager.ui.icons.IconHolder;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;

import org.w3c.dom.Text;

/**
<<<<<<< HEAD
 * Created by yashwanthreddyg on 10-06-2016.
=======
 * Created by KH9151 on 10-06-2016.
>>>>>>> e5a0d0bd685bba3a1322d7c0ff416f1334451f9b
 */
public class FTPServerFragment extends Fragment {

    TextView statusText,warningText,ftpAddrText;
    Button ftpBtn;
    Futils utils = new Futils();
    private MainActivity mainActivity;
    public String fabSkin;
    private View rootView;
    private BroadcastReceiver mWifiReceiver = new  BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI){
                warningText.setText("");
            }
            else{
                stopServer();
                statusText.setText(utils.getString(getContext(),R.string.ftp_status_not_running));
                warningText.setText(utils.getString(getContext(),R.string.ftp_no_wifi));
                ftpAddrText.setText("");
                ftpBtn.setText(utils.getString(getContext(),R.string.start_ftp));
            }
        }
    };
    private BroadcastReceiver ftpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == FTPService.ACTION_STARTED) {
                statusText.setText(utils.getString(getContext(), R.string.ftp_status_running));
                warningText.setText("");
                ftpAddrText.setText("ftp:/"+FTPService.getLocalInetAddress(getContext())+":"+FTPService.getPort());
                ftpBtn.setText(utils.getString(getContext(),R.string.stop_ftp));
            }
            else if(action == FTPService.ACTION_FAILEDTOSTART){
                statusText.setText(utils.getString(getContext(),R.string.ftp_status_not_running));
                warningText.setText("Oops! Something went wrong");
                ftpAddrText.setText("");
                ftpBtn.setText(utils.getString(getContext(),R.string.start_ftp));
            }
            else if(action == FTPService.ACTION_STOPPED){
                statusText.setText(utils.getString(getContext(),R.string.ftp_status_not_running));
                ftpAddrText.setText("");
                ftpBtn.setText(utils.getString(getContext(),R.string.start_ftp));
            }
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ftp,container,false);
//        return inflater.inflate(R.layout.article_view, container, false);
        statusText =(TextView) rootView.findViewById(R.id.statusText);
        warningText = (TextView) rootView.findViewById(R.id.warningText);
        ftpAddrText = (TextView) rootView.findViewById(R.id.ftpAddressText);
        ftpBtn = (Button) rootView.findViewById(R.id.startStopButton);

        ftpBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(!FTPService.isRunning()){
                    if(FTPService.isConnectedToWifi(getContext()))
                        startServer();
                    else
                        warningText.setText(utils.getString(getContext(),R.string.ftp_no_wifi));
                }
                else{
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
        mainActivity=(MainActivity)getActivity();
        mainActivity.setActionBarTitle(utils.getString(getActivity(), R.string.ftp));
        mainActivity.floatingActionButton.hideMenuButton(true);
        mainActivity.buttonBarFrame.setVisibility(View.GONE);
        mainActivity.supportInvalidateOptionsMenu();
        fabSkin = mainActivity.fabskin;
    }
    @Override
    public  void onDestroy(){
        super.onDestroy();
    }

    private void startServer() {
        getContext().sendBroadcast(new Intent(FTPService.ACTION_START_FTPSERVER));
    }

    private void stopServer() {
        getContext().sendBroadcast(new Intent(FTPService.ACTION_STOP_FTPSERVER));
    }

    @Override
    public void onResume(){
        super.onResume();
        updateStatus();
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getContext().registerReceiver(mWifiReceiver,wifiFilter);
        IntentFilter ftpFilter = new IntentFilter();
        ftpFilter.addAction(FTPService.ACTION_STARTED);
        ftpFilter.addAction(FTPService.ACTION_STOPPED);
        ftpFilter.addAction(FTPService.ACTION_FAILEDTOSTART);
        getContext().registerReceiver(ftpReceiver,ftpFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
        getContext().unregisterReceiver(mWifiReceiver);
        getContext().unregisterReceiver(ftpReceiver);
    }
    private void updateStatus(){
        if(FTPService.isRunning()){
            statusText.setText(utils.getString(getContext(),R.string.ftp_status_running));
            ftpBtn.setText(utils.getString(getContext(),R.string.stop_ftp));
        }
        else{
            statusText.setText(utils.getString(getContext(),R.string.ftp_status_not_running));
            ftpBtn.setText(utils.getString(getContext(),R.string.start_ftp));
        }
    }
}
