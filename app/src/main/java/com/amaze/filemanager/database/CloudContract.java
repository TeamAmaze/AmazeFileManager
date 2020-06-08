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

package com.amaze.filemanager.database;

/** Created by vishal on 19/4/17. */
public class CloudContract {

  public static final String APP_PACKAGE_NAME = "com.filemanager.amazecloud";

  public static final String PROVIDER_AUTHORITY = "com.amaze.cloud.provider";

  public static final String PERMISSION_PROVIDER = "com.amaze.cloud.permission.ACCESS_PROVIDER";

  public static final String DATABASE_NAME = "keys.db";
  public static final String TABLE_NAME = "secret_keys";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_CLIENT_ID = "client_id";
  public static final String COLUMN_CLIENT_SECRET_KEY = "client_secret";
  public static final int DATABASE_VERSION = 1;
}
