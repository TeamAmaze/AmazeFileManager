package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

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
            return c.getResources().getDrawable(R.drawable.ic_action_copy_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_copy);
    }

    public Drawable getCutDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_cut_light);
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
            return c.getResources().getDrawable(R.drawable.ic_action_cancel);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_cancel);
    }

    public Drawable getNewDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_new);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_new);
    }

    public Drawable getSearchDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_search);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_search);
    }

    public Drawable getDeleteDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_delete_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_delete);
    }

    public Drawable getAllDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_select_all_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_select_all);
    }

    public Drawable getAboutDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_about_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_about);
    }

    public Drawable getRootDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.root_light);
        else
            return c.getResources().getDrawable(R.drawable.root);
    }

    public Drawable getSdDrawable() {
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
