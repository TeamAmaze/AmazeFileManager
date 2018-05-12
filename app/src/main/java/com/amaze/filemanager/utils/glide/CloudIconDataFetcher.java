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

    public CloudIconDataFetcher(Context context, String path) {
        this.context = context;
        this.path = path;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super Bitmap> callback) {
        inputStream = CloudUtil.getThumbnailInputStreamForCloud(context, path);
        Bitmap drawable = BitmapFactory.decodeStream(inputStream);
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
        try {
            if (inputStream != null)
                inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
