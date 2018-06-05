package com.amaze.filemanager.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OpenMode;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

/**
 * Created by Vishal Nehra on 3/27/2018.
 */

public class CloudIconModelLoader implements ModelLoader<String, Bitmap> {

    private Context context;

    public CloudIconModelLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<Bitmap> buildLoadData(String s, int width, int height, Options options) {
        // we put key as current time since we're not disk caching the images for cloud,
        // as there is no way to differentiate input streams returned by different cloud services
        // for future instances and they don't expose concrete paths either
        return new LoadData<>(new ObjectKey(System.currentTimeMillis()), new CloudIconDataFetcher(context, s, width, height));
    }

    @Override
    public boolean handles(String s) {
        return s.startsWith(CloudHandler.CLOUD_PREFIX_BOX)
                || s.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)
                || s.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)
                || s.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)
                || s.startsWith("smb:/")
                || s.startsWith("ssh:/");
    }
}
