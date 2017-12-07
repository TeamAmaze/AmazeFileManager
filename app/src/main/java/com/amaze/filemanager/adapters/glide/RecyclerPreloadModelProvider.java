package com.amaze.filemanager.adapters.glide;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.GlideRequest;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.utils.GlideConstants;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;

import java.util.Collections;
import java.util.List;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 6/12/2017, at 15:15.
 */

public class RecyclerPreloadModelProvider implements ListPreloader.PreloadModelProvider<String> {

    private Fragment fragment;
    private List<String> urisToLoad;
    private boolean showThumbs, grid;

    public RecyclerPreloadModelProvider(@NonNull Fragment fragment, @NonNull List<String> uris,
                                        boolean showThumbs, boolean grid) {
        this.fragment = fragment;
        urisToLoad = uris;
        this.showThumbs = showThumbs;
        this.grid = grid;
    }

    @Override
    @NonNull
    public List<String> getPreloadItems(int position) {
        String uri = urisToLoad.get(position);
        if (uri == null) return Collections.emptyList();
        return Collections.singletonList(uri);
    }

    @Override
    @Nullable
    public RequestBuilder getPreloadRequestBuilder(String uri) {
        GlideRequest request;

        if(!showThumbs) {
            request = GlideApp.with(fragment).load(Icons.loadMimeIcon(uri, grid));
        } else {
            int filetype = Icons.getTypeOfFile(uri);
            if (filetype == Icons.PICTURE || filetype == Icons.VIDEO) {
                request = GlideApp.with(fragment).load(uri).fallback(Icons.loadMimeIcon(uri, grid));
            } else if (filetype == Icons.APK) {
                request = GlideApp.with(fragment).load(Icons.loadMimeIcon(uri, grid));// TODO: 6/12/2017 load Icons.getAppDrawable()
            } else {
                request = GlideApp.with(fragment).load(Icons.loadMimeIcon(uri, grid));
            }
        }

        return request.override(GlideConstants.WIDTH, GlideConstants.HEIGHT);
    }

}
