package com.amaze.filemanager.shadows

import org.robolectric.annotation.Implements
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.SmbAmazeFilesystem
import jcifs.context.SingletonContext
import jcifs.smb.SmbFile
import org.mockito.Mockito.*
import org.robolectric.annotation.Implementation

@Implements(SmbAmazeFilesystem::class)
@Suppress
class ShadowSmbAmazeFilesystem {

    companion object {
        /**
         * Shadows SmbAmazeFileSystem.create()
         *
         * @see SmbAmazeFilesystem.create
         */
        @JvmStatic
        @Implementation
        fun create(path: String): SmbFile {
            return when (path) {
                ShadowSmbUtil.PATH_CANNOT_DELETE_FILE -> ShadowSmbUtil.mockDeleteAccessDenied
                ShadowSmbUtil.PATH_CANNOT_MOVE_FILE -> ShadowSmbUtil.mockDeleteDifferentNetwork
                ShadowSmbUtil.PATH_CANNOT_RENAME_OLDFILE -> ShadowSmbUtil.mockCannotRenameOld
                ShadowSmbUtil.PATH_CAN_RENAME_OLDFILE -> ShadowSmbUtil.mockCanRename
                ShadowSmbUtil.PATH_NOT_A_FOLDER -> ShadowSmbUtil.mockPathNotAFolder
                ShadowSmbUtil.PATH_DOESNT_EXIST -> ShadowSmbUtil.mockPathDoesNotExist
                ShadowSmbUtil.PATH_EXIST -> ShadowSmbUtil.mockPathExist
                ShadowSmbUtil.PATH_INVOKE_SMBEXCEPTION_ON_EXISTS -> ShadowSmbUtil.mockSmbExceptionOnExists
                ShadowSmbUtil.PATH_INVOKE_SMBEXCEPTION_ON_ISFOLDER -> ShadowSmbUtil.mockSmbExceptionOnIsFolder
                else -> createInternal(path).also {
                    doNothing().`when`(it).delete()
                    `when`(it.exists()).thenReturn(false)
                }
            }
        }

        @JvmStatic
        private fun createInternal(path: String): SmbFile {
            return mock(SmbFile::class.java).also {
                `when`(it.name).thenReturn(path.substring(path.lastIndexOf('/') + 1))
                `when`(it.path).thenReturn(path)
                `when`(it.canonicalPath).thenReturn("$path/")
                `when`(it.context).thenReturn(SingletonContext.getInstance())
            }
        }
    }
}