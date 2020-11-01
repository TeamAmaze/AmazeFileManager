/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.smb;

import java.util.Properties;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.context.SingletonContext;

public abstract class CifsContextFactory {

  private static final String TAG = CifsContextFactory.class.getSimpleName();

  private static final Properties defaultProperties;

  static {
    defaultProperties = new Properties();
    defaultProperties.setProperty("jcifs.resolveOrder", "BCAST");
    defaultProperties.setProperty("jcifs.smb.client.responseTimeout", "30000");
    defaultProperties.setProperty("jcifs.netbios.retryTimeout", "5000");
    defaultProperties.setProperty("jcifs.netbios.cachePolicy", "-1");
  }

  public static final @NonNull BaseContext createWithDisableIpcSigningCheck(
      boolean disableIpcSigningCheck) {
    if (disableIpcSigningCheck) {
      Properties extraProperties = new Properties();
      extraProperties.put("jcifs.smb.client.ipcSigningEnforced", "false");
      return create(extraProperties);
    } else {
      return create(null);
    }
  }

  public static final @NonNull BaseContext create(@Nullable final Properties extraProperties) {
    return Single.fromCallable(
            () -> {
              try {
                Properties p = new Properties(defaultProperties);
                if (extraProperties != null) p.putAll(extraProperties);
                return new BaseContext(new PropertyConfiguration(p));
              } catch (CIFSException e) {
                Log.e(TAG, "Error initialize jcifs BaseContext, returning default", e);
                return SingletonContext.getInstance();
              }
            })
        .subscribeOn(Schedulers.io())
        .blockingGet();
  }
}
