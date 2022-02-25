package com.amaze.filemanager.filesystem.ftpserver

import org.apache.ftpserver.ftplet.FileSystemFactory
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.User

class RootFileSystemFactory(
    private val fileFactory: RootFileSystemView.SuFileFactory =
        RootFileSystemView.DefaultSuFileFactory()
) : FileSystemFactory {

    override fun createFileSystemView(user: User): FileSystemView =
        RootFileSystemView(user, fileFactory)
}
