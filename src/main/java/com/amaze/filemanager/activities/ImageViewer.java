package com.amaze.filemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.AspectRatioImageView;
import com.bumptech.glide.Glide;

public class ImageViewer extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image_viewer);
        AspectRatioImageView imageView = (AspectRatioImageView) findViewById(R.id.image);
        Intent intent = getIntent();
        if(intent!=null){
            String path=intent.getStringExtra("path");
            Glide.with(this).load(path).into(imageView);
        }
    }
}
