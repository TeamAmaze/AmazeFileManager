package com.amaze.filemanager.utils.glide;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

/**
 * Created by Vishal Nehra on 3/27/2018.
 */

public class InputStreamModelFactory implements ModelLoaderFactory<InputStream, InputStream> {

    @Override
    public ModelLoader<InputStream, InputStream> build(MultiModelLoaderFactory multiFactory) {
        return new InputStreamModelLoader();
    }

    @Override
    public void teardown() {

    }
}
