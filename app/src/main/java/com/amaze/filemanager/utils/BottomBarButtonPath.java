package com.amaze.filemanager.utils;

import androidx.annotation.DrawableRes;

/**
 * This lets BottomBar be independent of the Fragment MainActivity is housing
 *
 * @author Emmanuel
 *         on 20/8/2017, at 13:35.
 */

public interface BottomBarButtonPath {
    void changePath(String path);

    String getPath();

    @DrawableRes int getRootDrawable();
}
