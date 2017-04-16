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

package com.amaze.filemanager.ui.icons;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.PreferenceUtils;

public class IconUtils {
    private int LIGHT = 0, DARK = 1, CURRENT, rand;
    private Context c;

    public IconUtils(SharedPreferences sharedPrefs, Context c) {
        rand = Integer.parseInt(sharedPrefs.getString("theme", "0"));
        CURRENT = rand==2 ? PreferenceUtils.hourOfDay() : rand;
        this.c = c;
    }

    public Drawable getCopyDrawable() {
        return c.getResources().getDrawable(R.drawable.ic_content_copy_white_36dp);
    }

    public Drawable getCutDrawable() {
            return c.getResources().getDrawable(R.drawable.ic_content_cut_white_36dp);
    }
    public Drawable getRootDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.root);
        else
            return c.getResources().getDrawable(R.drawable.root);
    }
    public Drawable getSdDrawable() {
        if (CURRENT == LIGHT)
            return c.getResources().getDrawable(R.drawable.ic_sd_storage_white_56dp);
        else
            return c.getResources().getDrawable(R.drawable.ic_sd_storage_white_56dp);
    }

}
