package com.amaze.filemanager.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OpenMode;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

/**
 * Created by Vishal Nehra on 3/27/2018.
 */

public class CloudIconModelLoader implements ModelLoader<IconDataParcelable, Bitmap> {

    private Context context;

    public CloudIconModelLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<Bitmap> buildLoadData(IconDataParcelable iconDataParcelable, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(iconDataParcelable.getHashCode()),
                new CloudIconDataFetcher(context, iconDataParcelable.path, width, height));
    }

    @Override
    public boolean handles(IconDataParcelable iconDataParcelable) {
        String s = iconDataParcelable.path;
        return s.startsWith(CloudHandler.CLOUD_PREFIX_BOX)
                || s.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)
                || s.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)
                || s.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)
                || s.startsWith("smb:/")
                || s.startsWith("ssh:/");
    }
}
