package com.amaze.filemanager.ftpserver.commands

/**
 * Interface for providing localized error messages for custom FTP commands.
 */
interface FtpCommandMessageProvider {
    /**
     * Provides a localized message for the given command and subId.
     *
     * @param command The FTP command for which the message is requested.
     * @param subId A specific identifier for the message, allowing for more granular messages.
     * @param fileName An optional filename that can be included in the message if relevant.
     * @return A localized message string.
     */
    fun getMessage(
        command: String,
        fileName: String? = null,
    ): String
}
