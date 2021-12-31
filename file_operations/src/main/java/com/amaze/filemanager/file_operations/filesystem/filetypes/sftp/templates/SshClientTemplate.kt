package com.amaze.filemanager.file_operations.filesystem.filetypes.sftp.templates

import androidx.annotation.Nullable
import net.schmizz.sshj.SSHClient
import java.io.IOException

/**
 * Template class for executing actions with [SSHClient] while leave the complexities of
 * handling connection setup/teardown to [SshClientUtils].
 */
abstract class SshClientTemplate <T>
/**
 * Constructor, with closeClientOnFinish set to true (that the connection must close after `
 * execute`.
 *
 * @param url SSH connection URL, in the form of `
 * ssh://<username>:<password>@<host>:<port>` or `
 * ssh://<username>@<host>:<port>`
 */
@JvmOverloads
constructor(@JvmField val iv: String, @JvmField val url: String, @JvmField val closeClientOnFinish: Boolean = true) {

    /**
     * Implement logic here.
     *
     * @param client [SSHClient] instance, with connection opened and authenticated
     * @param <T> Requested return type
     * @return Result of the execution of the type requested
     </T> */
    @Throws(IOException::class)
    @Nullable
    abstract fun execute(client: SSHClient): T
}