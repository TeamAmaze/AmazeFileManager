package com.amaze.filemanager.adapters.glide;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.GlideRequest;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

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

    public RecyclerPreloadModelProvider(@NonNull Fragment fragment, @NonNull List<IconDataParcelable> uris,
                                        boolean showThumbs) {
        this.fragment = fragment;
        urisToLoad = uris;
        this.showThumbs = showThumbs;
        request = GlideApp.with(fragment).asDrawable().centerCrop();
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
                requestBuilder = request.load(iconData.path);
            } else if (iconData.type == IconDataParcelable.IMAGE_FROMCLOUD) {
                requestBuilder = request.load(iconData.path).diskCacheStrategy(DiskCacheStrategy.NONE);
            } else {
                requestBuilder = request.load(iconData.image);
            }
        }
        return requestBuilder;
    }
}
