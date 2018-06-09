package com.amaze.filemanager.fragments;

import android.view.View;
import android.widget.LinearLayout;
import com.amaze.filemanager.R;

public class CloudSheetFragmentFacade {
    private LinearLayout mSmbLayout, mScpLayout, mDropboxLayout, mBoxLayout, mGoogleDriveLayout, mOnedriveLayout
            , mGetCloudLayout;

    public CloudSheetFragmentFacade(View rootView) {
        mSmbLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_smb);
        mScpLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_scp);
        mBoxLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_box);
        mDropboxLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_dropbox);
        mGoogleDriveLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_google_drive);
        mOnedriveLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_onedrive);
        mGetCloudLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_get_cloud);
    }

    public void cloudSetVisibility() {
        mBoxLayout.setVisibility(View.VISIBLE);
        mDropboxLayout.setVisibility(View.VISIBLE);
        mGoogleDriveLayout.setVisibility(View.VISIBLE);
        mOnedriveLayout.setVisibility(View.VISIBLE);
        mGetCloudLayout.setVisibility(View.GONE);
    }

    public void cloudSetOnClickListener(CloudSheetFragment cloudSheetFragment) {
        mSmbLayout.setOnClickListener(cloudSheetFragment);
        mScpLayout.setOnClickListener(cloudSheetFragment);
        mBoxLayout.setOnClickListener(cloudSheetFragment);
        mDropboxLayout.setOnClickListener(cloudSheetFragment);
        mGoogleDriveLayout.setOnClickListener(cloudSheetFragment);
        mOnedriveLayout.setOnClickListener(cloudSheetFragment);
        mGetCloudLayout.setOnClickListener(cloudSheetFragment);
    }


}
