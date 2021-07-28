package com.amaze.filemanager.ui.views;

import android.graphics.PointF;
import android.view.View;

import com.amaze.filemanager.utils.Utils;

/**
 * Use this with any widget that should be zoomed when it gains focus
 */
public class CustomZoomFocusChange implements View.OnFocusChangeListener {
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            Utils.zoom(1f, 1f, new PointF(v.getWidth() / 2, v.getHeight() / 2), v);
        } else {
            Utils.zoom(1.2f, 1.2f, new PointF(v.getWidth() / 2, v.getHeight() / 2), v);
        }
    }
}
