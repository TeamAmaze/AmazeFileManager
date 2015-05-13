/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import com.amaze.filemanager.R;

public class IconUtils {
    int LIGHT = 0, DARK = 1, CURRENT, rand;
    Context c;

    public IconUtils(SharedPreferences Sp, Context c) {

        rand = Integer.parseInt(Sp.getString("theme", "0"));
        CURRENT = rand==2 ? PreferenceUtils.hourOfDay() : rand;
        this.c = c;
    }

    public Drawable getCopyDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_content_copy_black_36dp);
        else
            return c.getResources().getDrawable(R.drawable.ic_content_copy_white_36dp);
    }

    public Drawable getCutDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_content_cut_black_36dp);
        else
            return c.getResources().getDrawable(R.drawable.ic_content_cut_white_36dp);
    }
    public Drawable getBookDrawable1() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_not_important_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_not_important);
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
            return c.getResources().getDrawable(R.drawable.ic_delete_black_48dp);
        else
            return c.getResources().getDrawable(R.drawable.ic_delete_white_36dp);
    }

    public Drawable getAllDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_select_all_black_36dp);
        else
            return c.getResources().getDrawable(R.drawable.ic_select_all_white_36dp);
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
    public int getDrawerDrawable() {
        if (CURRENT == LIGHT)
            return (R.drawable.ic_drawer_l);
        else
            return (R.drawable.ic_drawer_l);
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
    public Drawable getGridDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_action_view_as_grid_light);
        else
            return c.getResources().getDrawable(R.drawable.ic_action_view_as_grid);
    }

}
