/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous;

import java.lang.ref.WeakReference;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.ui.views.drawer.Drawer;
import com.amaze.filemanager.utils.DataUtils;

import androidx.annotation.NonNull;

public class SaveOnDataUtilsChange implements DataUtils.DataChangeListener {
  private final UtilsHandler utilsHandler = AppConfig.getInstance().getUtilsHandler();

  private final WeakReference<Drawer> drawer;

  public SaveOnDataUtilsChange(@NonNull Drawer drawer) {
    this.drawer = new WeakReference<>(drawer);
  }

  @Override
  public void onHiddenFileAdded(String path) {
    utilsHandler.saveToDatabase(new OperationData(UtilsHandler.Operation.HIDDEN, path));
  }

  @Override
  public void onHiddenFileRemoved(String path) {
    utilsHandler.removeFromDatabase(new OperationData(UtilsHandler.Operation.HIDDEN, path));
  }

  @Override
  public void onHistoryAdded(String path) {
    utilsHandler.saveToDatabase(new OperationData(UtilsHandler.Operation.HISTORY, path));
  }

  @Override
  public void onBookAdded(String[] path, boolean refreshdrawer) {
    utilsHandler.saveToDatabase(
        new OperationData(UtilsHandler.Operation.BOOKMARKS, path[0], path[1]));
    if (refreshdrawer) {
      final Drawer drawer = this.drawer.get();
      if (drawer != null) {
        drawer.refreshDrawer();
      }
    }
  }

  @Override
  public void onHistoryCleared() {
    utilsHandler.clearTable(UtilsHandler.Operation.HISTORY);
  }
}
