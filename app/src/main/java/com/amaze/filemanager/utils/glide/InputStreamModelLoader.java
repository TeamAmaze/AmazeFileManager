package com.amaze.filemanager.utils.glide;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

/**
 * Created by Vishal Nehra on 3/27/2018.
 */

public class InputStreamModelLoader implements ModelLoader<InputStream, InputStream> {

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(InputStream inputStream, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(System.currentTimeMillis()), new InputStreamDataFetcher(inputStream));
    }

    @Override
    public boolean handles(InputStream inputStream) {
        return true;
    }
}
