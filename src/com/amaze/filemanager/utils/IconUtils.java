package com.amaze.filemanager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Display;

import com.amaze.filemanager.R;

public class IconUtils {
    int LIGHT = 0, DARK = 1, CURRENT;
    Context c;

    public IconUtils(SharedPreferences Sp, Context c) {
        CURRENT = Integer.parseInt(Sp.getString("theme", "0"));
        this.c = c;

    }

    public Drawable getCopyDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_copy);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_copy);
    }

    public Drawable getCutDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_cut);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_cut);
    }

    public Drawable getPasteDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_paste);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_paste);
    }

    public Drawable getBookDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_not_important);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_not_important);
    }
    public Drawable getBookDrawable1() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_not_important_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_not_important);
    }
    public Drawable getBackDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_back);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_back);
    }

    public Drawable getHomeDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.home);
        else
            return c.getResources().getDrawable(R.drawable.home);
    }

    public Drawable getRefreshDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_refresh);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_refresh);
    }

    public Drawable getCancelDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_cancel_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_cancel);
    }

    public Drawable getNewDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_new_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_new_light);
    }

    public Drawable getSearchDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_search);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_search);
    }

    public Drawable getDeleteDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_delete);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_delete);
    }

    public Drawable getAllDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_select_all);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_select_all);
    }

    public Drawable getAboutDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_about);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_about);
    }

    public Drawable getRootDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.root);
        else
            return c.getResources().getDrawable(R.drawable.root);
    }

    public Drawable getSdDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_sd_storage);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_sd_storage);
    }
    public Drawable getSdDrawable1() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_sd_storage_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_sd_storage);
    }
    public Drawable getSettingDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_settings_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_settings);
    }

    public Drawable getImageDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_picture_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_picture);
    }

    public Drawable getGridDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_view_as_grid_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_view_as_grid);
    }

    public int getDrawerDrawable() {
        if (CURRENT == LIGHT)
            return (R.drawable.ic_drawer_light);
        else
            return (R.drawable.ic_drawer_light);
    }
}
