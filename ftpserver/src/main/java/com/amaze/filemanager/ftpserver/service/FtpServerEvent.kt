package com.amaze.filemanager.ftpserver.service

/**
 * Events broadcast when FTP server state changes.
 */
sealed class FtpServerEvent {
    data object Started : FtpServerEvent()

    data object StartedFromTile : FtpServerEvent()

    data object Stopped : FtpServerEvent()

    data object FailedToStart : FtpServerEvent()
}
