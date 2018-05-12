package com.amaze.filemanager.utils.glide;

import android.content.Context;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

/**
 * Created by Vishal Nehra on 3/27/2018.
 */

public class CloudIconModelFactory implements ModelLoaderFactory<String, String> {

    private Context context;

    public CloudIconModelFactory(Context context) {
        this.context = context;
    }

    @Override
    public ModelLoader<String, String> build(MultiModelLoaderFactory multiFactory) {
        return new CloudIconModelLoader(context);
    }

    @Override
    public void teardown() {
    }
}
