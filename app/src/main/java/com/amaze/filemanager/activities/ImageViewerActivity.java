/*
 * Copyright (C) 2017 Jens Klingenberg <mail@jensklingenberg.de>
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

package com.amaze.filemanager.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.views.TouchImageView;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;
import java.util.ArrayList;

public class ImageViewerActivity extends BaseActivity {

    public static final String CURRENT_INDEX = "CURRENT_INDEX";
    private final Float ROTATE_LEFT = -90f;
    private final Float ROTATE_RIGHT = 90f;
    private final float NO_ZOOM = 1f;
    private float oldXValue;
    private BaseFile currentFile;
    private TouchImageView ivImage;
    private Toolbar toolbar;
    private LinearLayout llToolbarLayout;
    private ArrayList<BaseFile> baseFiles;
    private int currentIndex;
    private LinearLayout llBottomBar;
    private TextView tvFolderName;
    private String TAG = ImageViewerActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.image_viewer);

        if (getIntent().getData() != null) {
            String filePath = FileUtil.getAbsoluteFilePath(this, getIntent().getData());
            currentFile = new BaseFile(filePath);
        }

        loadImageFileList();
        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(CURRENT_INDEX);
            currentFile = baseFiles.get(currentIndex);
        }

        initIvImageView();
        initSearchViewLayout();
        setAppTheme();
        initToolbar();
        initBottomLayout();
    }

    private void initBottomLayout() {
        llBottomBar = (LinearLayout) findViewById(R.id.linBottomBar);
        tvFolderName = (TextView) llBottomBar.findViewById(R.id.tvFolderName);
        updateTvFolderName();
    }

    private void setAppTheme() {
        if (getAppTheme().equals(AppTheme.DARK)) {
            getWindow().getDecorView()
                    .setBackgroundColor(Utils.getColor(this, R.color.holo_dark_background));
        }
    }

    private void loadImageFileList() {
        baseFiles = new ArrayList<>();
        final int IS_FOLDER = 1;

        if (FileUtil.checkFolder(currentFile.getParent(this), this) == IS_FOLDER) {
            BaseFile parentDir = new BaseFile(currentFile.getParent(this));
            int index = 0;
            for (BaseFile baseFile : parentDir.listFiles(this, parentDir.isRoot())) {
                if (baseFile.getMimeType() != null) {
                    if (baseFile.getPath().equals(currentFile.getPath())) {
                        currentIndex = index;
                    }
                    if (baseFile.getMimeType().contains("image/")) {
                        baseFiles.add(baseFile);
                        index++;
                    }
                }
            }
        }
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initIvImageView() {
        ivImage = (TouchImageView) findViewById(R.id.ivImage);
        loadImage();

        ivImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (ivImage.getCurrentZoom() > NO_ZOOM) {
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        oldXValue = event.getX();
                        break;

                    case MotionEvent.ACTION_UP:
                        if (ivImage.getCurrentZoom() != NO_ZOOM) {
                            return false;
                        }
                        float currentX = event.getX();

                        if (currentX > oldXValue) {
                            //SWIPE RIGHT
                            showPreviousImage();
                        }

                        if (currentX < oldXValue) {
                            //SWIPE LEFT
                            showNextImage();
                        }

                        break;
                }
                return true;
            }
        });

        ivImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (llToolbarLayout.getVisibility() == View.VISIBLE) {
                    setMenuVisibility(View.INVISIBLE);
                } else {
                    setMenuVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setMenuVisibility(int visible) {
        llToolbarLayout.setVisibility(visible);
        llBottomBar.setVisibility(visible);
    }

    private void loadImage() {
        GlideApp.with(ivImage)
                .load(currentFile.getPath())
                .error(R.mipmap.ic_launcher)
                .placeholder(R.color.black_trans)
                .into(ivImage);
        ivImage.setRotation(0f);
    }

    private void showNextImage() {
        if (currentIndex < baseFiles.size() - 1) {
            currentIndex++;
            currentFile = baseFiles.get(currentIndex);
            loadImage();
            updateTvFolderName();
        }
    }

    private void updateTvFolderName() {
        Resources res = getResources();
        String text =
                res.getString(R.string.image_viewer_bottom_bar, getResources().getString(R.string.folder),
                        currentFile.getParentName(), currentIndex + 1, baseFiles.size(), currentFile.getName());

        tvFolderName.setText(text);
    }

    private void showPreviousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            currentFile = baseFiles.get(currentIndex);
            loadImage();
            updateTvFolderName();
        }
    }

    private void initSearchViewLayout() {
        llToolbarLayout = (LinearLayout) findViewById(R.id.linToolbar);
        llToolbarLayout.setBackgroundColor(
                getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_INDEX, currentIndex);
    }

    private void rotateImage(float rotation) {
        ivImage.animate().rotationBy(rotation).start();
    }

    private void setImageAs() {
        Uri uri = Uri.fromFile(new File(currentFile.getPath()));
        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setDataAndType(uri, currentFile.getMimeType());
        intent.putExtra("mimeType", currentFile.getMimeType());
        this.startActivity(
                Intent.createChooser(intent, getResources().getString(R.string.set_image_as)));
    }

    private void shareImage() {
        ArrayList<File> shareFiles = new ArrayList<>();
        int colorAccent = getColorPreference().getColor(ColorUsage.ACCENT);
        shareFiles.add(new File(currentFile.getPath()));
        getFutils().shareFiles(shareFiles, this, getAppTheme(), colorAccent);
    }

    private void showDeleteDialog() {
        ArrayList<BaseFile> baseFiles = new ArrayList<>();
        baseFiles.add(currentFile);
        GeneralDialogCreation.deleteImageDialog(this, baseFiles, getAppTheme());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_viewer_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.properties:
                showDetails();
                break;

            case R.id.delete:
                showDeleteDialog();
                break;

            case R.id.rotate_left:
                rotateImage(ROTATE_LEFT);
                break;

            case R.id.share:
                shareImage();
                break;

            case R.id.setImageAs:
                setImageAs();
                break;

            default:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDetails() {
        new MainActivity().getCurrentMainFragment();
        GeneralDialogCreation.showImagePropertiesDialogForStorage(currentFile, this, getAppTheme());
    }
}

