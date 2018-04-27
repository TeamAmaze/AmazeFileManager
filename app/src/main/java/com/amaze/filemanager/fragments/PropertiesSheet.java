package com.amaze.filemanager.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.NestedScrollView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.Utils;

import java.io.File;

/**
 * Created by vishal on 14/12/16.
 */

public class PropertiesSheet extends BottomSheetDialogFragment {

    private Bundle mBundle;
    private HybridFileParcelable mFile;
    private String mPermission;
    private boolean mIsRoot;
    private CollapsingToolbarLayout mToolbar;
    private AppBarLayout mAppBarLayout;
    private View rootView;
    private TextView mFileNameTextView, mFileTypeTextView, mFileSizeTextView, mFileLocationTextView;
    private TextView mFileAccessedTextView, mFileModifiedTextView;
    private NestedScrollView mNestedScrollView;
    private BottomSheetBehavior mBottomSheetBehavior;

    private static PropertiesSheet staticPropertiesSheet;

    public static final String KEY_FILE = "file";
    public static final String KEY_PERMISSION = "permission";
    public static final String KEY_ROOT = "root";
    public static final String TAG_FRAGMENT = "properties";

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        rootView = View.inflate(getContext(), R.layout.fragment_sheet_properties, null);
        dialog.setContentView(rootView);

        mBundle = getArguments();
        mFile = mBundle.getParcelable(KEY_FILE);
        mPermission = mBundle.getString(KEY_PERMISSION);
        mIsRoot = mBundle.getBoolean(KEY_ROOT);

        mToolbar = rootView.findViewById(R.id.collapsing_toolbar_layout);
        mAppBarLayout = rootView.findViewById(R.id.appBarLayout);
        mToolbar.setTitle(getString(R.string.properties));
        mToolbar.setCollapsedTitleTextAppearance(R.style.collapsed_appbar);
        mToolbar.setExpandedTitleTextAppearance(R.style.expanded_appbar);

        mFileNameTextView = rootView.findViewById(R.id.text_view_file_name);
        mFileNameTextView.setText(mFile.getName());
        mFileTypeTextView = rootView.findViewById(R.id.text_view_file_type);
        mFileTypeTextView.setText(mFile.isDirectory() ? getString(R.string.folder) : mFile.getName().substring(mFile.getName().lastIndexOf(".")));
        mFileSizeTextView = rootView.findViewById(R.id.text_view_file_size);
        mFileSizeTextView.setText(Formatter.formatFileSize(dialog.getContext(), mFile.isDirectory() ? FileUtils.folderSize(new File(mFile.getPath()), null) : mFile.getSize()));
        mFileLocationTextView = rootView.findViewById(R.id.text_view_file_location);
        mFileLocationTextView.setText(mFile.getPath());
        mFileAccessedTextView = rootView.findViewById(R.id.text_view_file_accessed);
        mFileAccessedTextView.setText(Utils.getDate(mFile.getDate()));
        mFileModifiedTextView = rootView.findViewById(R.id.text_view_file_modified);
        mFileModifiedTextView.setText(Utils.getDate(mFile.getDate()));

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) ((View) rootView.getParent()).getLayoutParams();

        mBottomSheetBehavior = (BottomSheetBehavior) layoutParams.getBehavior();
        mBottomSheetBehavior.setBottomSheetCallback(mCallback);

        mNestedScrollView = rootView.findViewById(R.id.nested_view);

        mNestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                Log.d(getClass().getSimpleName(), "ScrollY: " + scrollY + " oldScrollY: " + oldScrollY);
                if (scrollY==0) {
                    // we're at the top
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });


        /*Bitmap bitmap = BitmapFactory.decodeFile(mFile.getPath());
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                mToolbarLayout.setContentScrimColor(palette.getMutedColor());
                mToolbarLayout.setStatusBarScrimColor(palette.getMutedColor());
            }
        });*/
    }

    /*public static PropertiesSheet from(PropertiesSheet propertiesSheet) {
        return staticPropertiesSheet = propertiesSheet;
    }*/

    private void generate(BottomSheetBehavior.BottomSheetCallback callback) {
        //if (staticPropertiesSheet==null) return;

    }

    private BottomSheetBehavior.BottomSheetCallback mCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState==BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    };
}
