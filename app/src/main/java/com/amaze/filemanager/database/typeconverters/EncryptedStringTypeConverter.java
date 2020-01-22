/*
 * EncryptedStringTypeConverter.java
 *
 * Copyright (C) 2020 Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.database.typeconverters;

import androidx.room.TypeConverter;

import com.amaze.filemanager.database.models.StringWrapper;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.files.CryptUtil;

public class EncryptedStringTypeConverter {

    private static final String TAG = EncryptedStringTypeConverter.class.getSimpleName();

    @TypeConverter
    public static StringWrapper toPassword(String encryptedStringEntryInDb) {
        try {
            return new StringWrapper(CryptUtil.decryptPassword(AppConfig.getInstance(), encryptedStringEntryInDb));
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error decrypting password", e);
            return null;
        }
    }

    @TypeConverter
    public static String fromPassword(StringWrapper unencryptedPasswordString) {
        try {
            return CryptUtil.encryptPassword(AppConfig.getInstance(), unencryptedPasswordString.value);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error encrypting password", e);
            return null;
        }
    }
}
