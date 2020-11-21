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

package com.amaze.filemanager.shadows

import com.amaze.filemanager.utils.SmbUtil
import jcifs.context.SingletonContext
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import org.mockito.Mockito.*
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
@Implements(SmbUtil::class)
class ShadowSmbUtil {

    companion object {

        const val PATH_CANNOT_DELETE_FILE = "smb://user:password@1.2.3.4/access/denied.file"
        const val PATH_CANNOT_MOVE_FILE = "smb://user:password@1.2.3.4/cannot/move.file"
        const val PATH_CANNOT_RENAME_OLDFILE = "smb://user:password@1.2.3.4/cannot/rename.file.old"
        const val PATH_CAN_RENAME_OLDFILE = "smb://user:password@1.2.3.4/rename/old.file"
        const val PATH_CAN_RENAME_NEWFILE = "smb://user:password@1.2.3.4/rename/new.file"

        var mockDeleteAccessDenied: SmbFile? = null
        var mockDeleteDifferentNetwork: SmbFile? = null
        var mockCannotRenameOld: SmbFile? = null
        var mockCanRename: SmbFile? = null

        init {
            mockDeleteAccessDenied = createInternal(PATH_CANNOT_DELETE_FILE).also {
                `when`(it.delete()).thenThrow(SmbException("Access is denied."))
                `when`(it.exists()).thenReturn(true)
            }

            mockDeleteDifferentNetwork = createInternal(PATH_CANNOT_MOVE_FILE).also {
                `when`(it.delete()).thenThrow(SmbException("Cannot rename between different trees"))
                `when`(it.exists()).thenReturn(true)
            }

            mockCanRename = createInternal(PATH_CAN_RENAME_OLDFILE).also {
                doNothing().`when`(it).renameTo(any())
            }

            mockCannotRenameOld = createInternal(PATH_CANNOT_RENAME_OLDFILE)
            `when`(mockCannotRenameOld!!.renameTo(any()))
                .thenThrow(SmbException("Access is denied."))
            `when`(mockCannotRenameOld!!.exists()).thenReturn(true)
        }

        /**
         * Shadows SmbUtil.create()
         *
         * @see SmbUtil.create
         */
        @JvmStatic @Implementation
        fun create(path: String): SmbFile {

            return when (path) {
                PATH_CANNOT_DELETE_FILE -> mockDeleteAccessDenied!!
                PATH_CANNOT_MOVE_FILE -> mockDeleteDifferentNetwork!!
                PATH_CANNOT_RENAME_OLDFILE -> mockCannotRenameOld!!
                PATH_CAN_RENAME_OLDFILE -> mockCanRename!!
                else -> createInternal(path).also {
                    doNothing().`when`(it).delete()
                    `when`(it.exists()).thenReturn(false)
                }
            }
        }

        private fun createInternal(path: String): SmbFile {
            return mock(SmbFile::class.java).also {
                `when`(it.name).thenReturn(path.substring(path.lastIndexOf('/') + 1))
                `when`(it.path).thenReturn(path)
                `when`(it.context).thenReturn(SingletonContext.getInstance())
            }
        }
    }
}
