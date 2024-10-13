/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.ui.views.drawer;

public final class MenuMetadata {

  public static final int ITEM_ENTRY = 1, ITEM_INTENT = 2;

  public final int type;
  public final String path;
  public final boolean hideFabInMainFragment;
  public final OnClickListener onClickListener;

  public MenuMetadata(String path, boolean hideFabInMainFragment) {
    this.type = ITEM_ENTRY;
    this.path = path;
    this.hideFabInMainFragment = hideFabInMainFragment;
    this.onClickListener = null;
  }

  public MenuMetadata(OnClickListener onClickListener) {
    this.type = ITEM_INTENT;
    this.onClickListener = onClickListener;
    this.hideFabInMainFragment = false;
    this.path = null;
  }

  public interface OnClickListener {
    void onClick();
  }
}
