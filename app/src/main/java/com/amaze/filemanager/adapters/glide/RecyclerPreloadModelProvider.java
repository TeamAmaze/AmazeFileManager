package com.amaze.filemanager.adapters.glide;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.GlideRequest;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.utils.IconLoaderUtil;
import com.amaze.filemanager.utils.application.AppConfig;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.Target;

import java.util.Collections;
import java.util.List;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 6/12/2017, at 15:15.
 */

public class RecyclerPreloadModelProvider implements ListPreloader.PreloadModelProvider<IconDataParcelable> {

    private Fragment fragment;
    private List<IconDataParcelable> urisToLoad;
    private boolean showThumbs;
    private GlideRequest<Drawable> request;
    private int imageWidthPixels, imageHeightPixels;

    public RecyclerPreloadModelProvider(@NonNull Fragment fragment, @NonNull List<IconDataParcelable> uris,
                                        boolean showThumbs) {
        this.fragment = fragment;
        urisToLoad = uris;
        this.showThumbs = showThumbs;
        request = GlideApp.with(fragment).asDrawable().centerCrop();
        imageHeightPixels = AppConfig.getInstance().getScreenUtils().convertDbToPx(40);
        imageWidthPixels = AppConfig.getInstance().getScreenUtils().convertDbToPx(40);
    }

    @Override
    @NonNull
    public List<IconDataParcelable> getPreloadItems(int position) {
        IconDataParcelable iconData = urisToLoad.get(position);
        if (iconData == null) return Collections.emptyList();
        return Collections.singletonList(iconData);
    }

    @Override
    @Nullable
    public RequestBuilder<Drawable> getPreloadRequestBuilder(IconDataParcelable iconData) {
        RequestBuilder<Drawable> requestBuilder;
        if(!showThumbs) {
            requestBuilder = GlideApp.with(fragment).asDrawable().fitCenter().load(iconData.image);
        } else {
            if (iconData.type == IconDataParcelable.IMAGE_FROMFILE) {
                requestBuilder = request.load(iconData.path)
                        .override(imageWidthPixels, imageHeightPixels);
            } else if (iconData.type == IconDataParcelable.IMAGE_FROMCLOUD) {
                requestBuilder = request.load(IconLoaderUtil.getThumbnailInputStreamForCloud(fragment.getContext(),
                        iconData)).thumbnail(0.25f)
                        .override(imageWidthPixels, imageHeightPixels).diskCacheStrategy(DiskCacheStrategy.NONE);
            } else {
                requestBuilder = request.load(iconData.image);
            }
        }
        return requestBuilder;
    }
}
