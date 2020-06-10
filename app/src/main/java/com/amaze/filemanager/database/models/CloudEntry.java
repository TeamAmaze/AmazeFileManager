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

package com.amaze.filemanager.database.models;

import com.amaze.filemanager.utils.OpenMode;

/** Created by vishal on 18/4/17. */
public class CloudEntry {

  private int _id;
  private OpenMode serviceType;
  private String persistData;

  public CloudEntry() {}

  public CloudEntry(OpenMode serviceType, String persistData) {
    this.serviceType = serviceType;
    this.persistData = persistData;
  }

  public void setId(int _id) {
    this._id = _id;
  }

  public int getId() {
    return this._id;
  }

  public void setPersistData(String persistData) {
    this.persistData = persistData;
  }

  public String getPersistData() {
    return this.persistData;
  }

  /** Set the service type Support values from {@link com.amaze.filemanager.utils.OpenMode} */
  public void setServiceType(OpenMode openMode) {
    this.serviceType = openMode;
  }

  /** Returns ordinal value of service from {@link com.amaze.filemanager.utils.OpenMode} */
  public OpenMode getServiceType() {
    return this.serviceType;
  }
}
