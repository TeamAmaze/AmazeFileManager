package com.amaze.filemanager.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;
import android.widget.LinearLayout;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.CloudConnectDialog;
import com.amaze.filemanager.ui.dialogs.SmbSearchDialog;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.provider.DatabaseContract;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by vishal on 18/2/17.
 *
 * Class represents implementation of a new cloud connection sheet dialog
 */

public class CloudSheetFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private View rootView;
    private LinearLayout mSmbLayout, mDropboxLayout, mBoxLayout, mGoogleDriveLayout, mOnedriveLayout;

    public static final String TAG_FRAGMENT = "cloud_fragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_sheet_cloud, null);
        dialog.setContentView(rootView);

        if (((MainActivity) getActivity()).getAppTheme().equals(AppTheme.DARK)) {
            rootView.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        } else {
            rootView.setBackgroundColor(getResources().getColor(android.R.color.white));
        }

        mSmbLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_smb);
        mBoxLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_box);
        mDropboxLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_dropbox);
        mGoogleDriveLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_google_drive);
        mOnedriveLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_onedrive);

        if (isCloudProviderAvailable(getContext())) {

            mBoxLayout.setVisibility(View.VISIBLE);
            mDropboxLayout.setVisibility(View.VISIBLE);
            mGoogleDriveLayout.setVisibility(View.VISIBLE);
            mOnedriveLayout.setVisibility(View.VISIBLE);
        }

        mSmbLayout.setOnClickListener(this);
        mBoxLayout.setOnClickListener(this);
        mDropboxLayout.setOnClickListener(this);
        mGoogleDriveLayout.setOnClickListener(this);
        mOnedriveLayout.setOnClickListener(this);
    }

    /**
     * Determines whether cloud provider is installed or not
     * @param context
     * @return
     */
    public static final boolean isCloudProviderAvailable(Context context) {

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(DatabaseContract.APP_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onClick(View v) {

        Bundle args = new Bundle();

        switch (v.getId()) {
            case R.id.linear_layout_smb:
                SmbSearchDialog smbDialog=new SmbSearchDialog();
                smbDialog.show(getActivity().getFragmentManager(), "tab");
                return;
            case R.id.linear_layout_box:
                args.putInt(CloudConnectDialog.KEY_TYPE, OpenMode.BOX.ordinal());
                break;
            case R.id.linear_layout_dropbox:
                args.putInt(CloudConnectDialog.KEY_TYPE, OpenMode.DROPBOX.ordinal());
                break;
            case R.id.linear_layout_google_drive:
                args.putInt(CloudConnectDialog.KEY_TYPE, OpenMode.GDRIVE.ordinal());
                break;
            case R.id.linear_layout_onedrive:
                args.putInt(CloudConnectDialog.KEY_TYPE, OpenMode.ONEDRIVE.ordinal());
                break;
        }

        CloudConnectDialog dialog = new CloudConnectDialog();
        dialog.setArguments(args);
        dialog.show(getActivity().getFragmentManager(), CloudConnectDialog.TAG_FRAGMENT);

        // dismiss this sheet dialog
        dismiss();
    }
}
