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

import android.content.Context
import com.amaze.filemanager.utils.SmbUtil
import jcifs.context.SingletonContext
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import org.mockito.Mockito.*
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

@Implements(SmbUtil::class)
@Suppress()
class ShadowSmbUtil {

    companion object {

        /* ktlint-disable max-line-length */
        const val PATH_CANNOT_DELETE_FILE = "smb://user:password@1.2.3.4/access/denied.file"
        const val PATH_CANNOT_MOVE_FILE = "smb://user:password@1.2.3.4/cannot/move.file"
        const val PATH_CANNOT_RENAME_OLDFILE = "smb://user:password@1.2.3.4/cannot/rename.file.old"
        const val PATH_CAN_RENAME_OLDFILE = "smb://user:password@1.2.3.4/rename/old.file"
        const val PATH_CAN_RENAME_NEWFILE = "smb://user:password@1.2.3.4/rename/new.file"

        const val PATH_DOESNT_EXIST = "smb://user:password@5.6.7.8/newfolder/DummyFolder"
        const val PATH_NOT_A_FOLDER = "smb://user:password@5.6.7.8/newfolder/resume.doc"
        const val PATH_EXIST = "smb://user:password@5.6.7.8/newfolder/Documents"
        const val PATH_INVOKE_SMBEXCEPTION_ON_EXISTS = "smb://user:password@5.6.7.8/newfolder/wirebroken.log"
        const val PATH_INVOKE_SMBEXCEPTION_ON_ISFOLDER = "smb://user:password@5.6.7.8/newfolder/failcheck"

        var mockDeleteAccessDenied: SmbFile? = null
        var mockDeleteDifferentNetwork: SmbFile? = null
        var mockCannotRenameOld: SmbFile? = null
        var mockCanRename: SmbFile? = null
        var mockPathDoesNotExist: SmbFile? = null
        var mockPathNotAFolder: SmbFile? = null
        var mockPathExist: SmbFile? = null
        var mockSmbExceptionOnExists: SmbFile? = null
        var mockSmbExceptionOnIsFolder: SmbFile? = null

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

            mockPathDoesNotExist = createInternal(PATH_DOESNT_EXIST).also {
                `when`(it.exists()).thenReturn(false)
            }

            mockPathNotAFolder = createInternal(PATH_NOT_A_FOLDER).also {
                `when`(it.exists()).thenReturn(true)
                `when`(it.isDirectory).thenReturn(false)
            }

            mockPathExist = createInternal(PATH_EXIST).also {
                `when`(it.exists()).thenReturn(true)
                `when`(it.isDirectory).thenReturn(true)
            }

            mockSmbExceptionOnExists = createInternal(PATH_INVOKE_SMBEXCEPTION_ON_EXISTS).also {
                `when`(it.exists()).thenThrow(SmbException())
            }

            mockSmbExceptionOnIsFolder = createInternal(PATH_INVOKE_SMBEXCEPTION_ON_ISFOLDER).also {
                `when`(it.exists()).thenReturn(true)
                `when`(it.isDirectory).thenThrow(SmbException())
            }
        }
        /* ktlint-enable max-line-length */

        /**
         * Delegate to [SmbUtil.getSmbEncryptedPath].
         */
        @JvmStatic @Implementation
        fun getSmbEncryptedPath(context: Context, path: String): String {
            return Shadow.directlyOn(
                SmbUtil::class.java, "getSmbEncryptedPath",
                ReflectionHelpers.ClassParameter(Context::class.java, context),
                ReflectionHelpers.ClassParameter(String::class.java, path)
            )
        }

        /**
         * Delegate to [SmbUtil.getSmbDecryptedPath].
         */
        @JvmStatic @Implementation
        fun getSmbDecryptedPath(context: Context, path: String): String {
            return Shadow.directlyOn(
                SmbUtil::class.java, "getSmbDecryptedPath",
                ReflectionHelpers.ClassParameter(Context::class.java, context),
                ReflectionHelpers.ClassParameter(String::class.java, path)
            )
        }

        /**
         * Delegate to [SmbUtil.checkFolder].
         */
        @JvmStatic @Implementation
        fun checkFolder(path: String): Int {
            return Shadow.directlyOn(
                SmbUtil::class.java, "checkFolder",
                ReflectionHelpers.ClassParameter(String::class.java, path)
            )
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
                PATH_NOT_A_FOLDER -> mockPathNotAFolder!!
                PATH_DOESNT_EXIST -> mockPathDoesNotExist!!
                PATH_EXIST -> mockPathExist!!
                PATH_INVOKE_SMBEXCEPTION_ON_EXISTS -> mockSmbExceptionOnExists!!
                PATH_INVOKE_SMBEXCEPTION_ON_ISFOLDER -> mockSmbExceptionOnIsFolder!!
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
