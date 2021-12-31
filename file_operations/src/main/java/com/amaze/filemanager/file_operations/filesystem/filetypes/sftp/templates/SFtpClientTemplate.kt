package com.amaze.filemanager.file_operations.filesystem.filetypes.sftp.templates

import net.schmizz.sshj.sftp.SFTPClient
import java.io.IOException

/**
 * Template class for executing actions with [SFTPClient] while leave the complexities of
 * handling connection and session setup/teardown to [SshClientUtils].
 */
abstract class SFtpClientTemplate <T>
/**
 * If closeClientOnFinish is set to true, calling code needs to handle closing of [ ] session.
 *
 * @param url SSH connection URL, in the form of `
 * ssh://<username>:<password>@<host>:<port>` or `
 * ssh://<username>@<host>:<port>`
 */ @JvmOverloads
constructor(@JvmField val iv: String, @JvmField val url: String, @JvmField val closeClientOnFinish: Boolean = true) {

    /**
     * Implement logic here.
     *
     * @param client [SFTPClient] instance, with connection opened and authenticated, and SSH
     * session had been set up.
     * @param <T> Requested return type
     * @return Result of the execution of the type requested
     */
    @Throws(IOException::class)
    abstract fun execute(client: SFTPClient): T
}