package com.amaze.filemanager.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Vishal Nehra on 3/27/2018.
 */

public class CloudIconDataFetcher implements DataFetcher<Bitmap> {

    private String path;
    private Context context;
    private InputStream inputStream;
    private int width, height;

    public CloudIconDataFetcher(Context context, String path, int width, int height) {
        this.context = context;
        this.path = path;
        this.width = width;
        this.height = height;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super Bitmap> callback) {
        inputStream = CloudUtil.getThumbnailInputStreamForCloud(context, path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = width;
        options.outHeight = height;
        Bitmap drawable = BitmapFactory.decodeStream(inputStream, null, options);
        callback.onDataReady(drawable);
    }

    @Override
    public void cleanup() {
        try {
            if (inputStream != null)
                inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cancel() {
        // do nothing
    }

    @NonNull
    @Override
    public Class<Bitmap> getDataClass() {
        return Bitmap.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}
