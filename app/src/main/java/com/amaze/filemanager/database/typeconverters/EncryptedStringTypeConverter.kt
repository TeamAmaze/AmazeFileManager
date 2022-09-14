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

package com.amaze.filemanager.database.typeconverters

import android.util.Log
import androidx.room.TypeConverter
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.models.StringWrapper
import com.amaze.filemanager.utils.PasswordUtil
import com.amaze.filemanager.utils.PasswordUtil.decryptPassword
import com.amaze.filemanager.utils.PasswordUtil.encryptPassword

/**
 * [TypeConverter] for password strings encrypted by [PasswordUtil].
 *
 * @see StringWrapper
 *
 * @see PasswordUtil.encryptPassword
 * @see PasswordUtil.decryptPassword
 */
object EncryptedStringTypeConverter {

    @JvmStatic
    private val TAG = EncryptedStringTypeConverter::class.java.simpleName

    /**
     * Converts value in database to string.
     */
    @JvmStatic
    @TypeConverter
    fun toPassword(encryptedStringEntryInDb: String): StringWrapper {
        return runCatching {
            StringWrapper(
                decryptPassword(AppConfig.getInstance(), encryptedStringEntryInDb)
            )
        }.onFailure {
            Log.e(TAG, "Error decrypting password", it)
        }.getOrElse {
            StringWrapper(encryptedStringEntryInDb)
        }
    }

    /**
     * Encrypt given password in plaintext for storage in database.
     */
    @JvmStatic
    @TypeConverter
    fun fromPassword(unencryptedPasswordString: StringWrapper): String? {
        return runCatching {
            encryptPassword(
                AppConfig.getInstance(),
                unencryptedPasswordString.value
            )
        }.onFailure {
            Log.e(TAG, "Error encrypting password", it)
        }.getOrElse {
            unencryptedPasswordString.value
        }
    }
}
