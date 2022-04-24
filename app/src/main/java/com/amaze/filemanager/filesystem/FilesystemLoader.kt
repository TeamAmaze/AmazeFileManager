package com.amaze.filemanager.filesystem

import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.SmbAmazeFilesystem
import com.amaze.filemanager.filesystem.files.FileAmazeFilesystem
import com.amaze.filemanager.filesystem.otg.OtgAmazeFilesystem
import com.amaze.filemanager.filesystem.ssh.SshAmazeFilesystem

/**
 * TODO remove this by moving all Filesystem subclasses to file_operations
 */
object FilesystemLoader {
    init {
        AmazeFile //Loads all of the file_operations Filesystem subclasses
        FileAmazeFilesystem
        OtgAmazeFilesystem
        SmbAmazeFilesystem
        SshAmazeFilesystem.INSTANCE
    }
}