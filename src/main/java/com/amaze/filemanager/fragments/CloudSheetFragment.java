package com.amaze.filemanager.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.SmbSearchDialog;
import com.amaze.filemanager.utils.provider.DatabaseContract;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by vishal on 18/2/17.
 */

public class CloudSheetFragment extends BottomSheetDialogFragment {

    View rootView;
    LinearLayout mSmbLayout, mDropboxLayout, mBoxLayout, mGoogleDriveLayout, mOnedriveLayout;

    public static final String TAG_FRAGMENT = "cloud_fragment";

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.fragment_sheet_cloud, null);
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

        mSmbLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // dismiss this sheet dialog first
                dismiss();
                SmbSearchDialog smbDialog=new SmbSearchDialog();
                smbDialog.show(getActivity().getFragmentManager(), "tab");
            }
        });
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
}
