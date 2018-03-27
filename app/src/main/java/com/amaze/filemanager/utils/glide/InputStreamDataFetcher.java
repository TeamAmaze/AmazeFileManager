package com.amaze.filemanager.utils.glide;

import android.support.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Vishal Nehra on 3/27/2018.
 */

public class InputStreamDataFetcher implements DataFetcher<InputStream> {

    private InputStream inputStream;

    public InputStreamDataFetcher(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        callback.onDataReady(inputStream);
    }

    @Override
    public void cleanup() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}
