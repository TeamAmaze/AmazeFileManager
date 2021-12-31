package com.amaze.filemanager.asynchronous.asynctasks.ssh.pem

import androidx.annotation.WorkerThread
import com.amaze.filemanager.exceptions.SshKeyInvalidPassphrase
import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile
import net.schmizz.sshj.userauth.password.PasswordFinder
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.security.KeyPair
import java.util.concurrent.Callable

/**
 * [Callable] to convert given [InputStream] into [KeyPair] which is required by
 * sshj, using [JcaPEMKeyConverter].
 *
 * @see JcaPEMKeyConverter
 *
 * @see KeyProvider
 *
 * @see OpenSSHKeyV1KeyFile
 *
 * @see PuTTYKeyFile
 *
 * @see com.amaze.filemanager.filesystem.ssh.SshConnectionPool.create
 * @see net.schmizz.sshj.SSHClient.authPublickey
 */
class PemToKeyPairCallable(private val pemFile: ByteArray, private val passwordFinder: PasswordFinder?): Callable<KeyPair> {
    private val converters = arrayOf(
            JcaPemToKeyPairConverter(),
            OpenSshPemToKeyPairConverter(),
            OpenSshV1PemToKeyPairConverter(),
            PuttyPrivateKeyToKeyPairConverter()
    )

    @WorkerThread
    @kotlin.jvm.Throws(Exception::class)
    override fun call(): KeyPair {
        for (converter in converters) {
            val keyPair = converter.convert(String(pemFile))
            if (keyPair != null) {
                return keyPair
            }
        }

        if (passwordFinder != null) {
            throw SshKeyInvalidPassphrase()
        }

        throw IOException("No converter available to parse selected PEM")
    }

    private abstract inner class PemToKeyPairConverter {
        fun convert(source: String?): KeyPair? = runCatching {
            throwingConvert(source)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()

        @Throws(Exception::class)
        protected abstract fun throwingConvert(source: String?): KeyPair?
    }

    private inner class JcaPemToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        override fun throwingConvert(source: String?): KeyPair? {
            val pemParser = PEMParser(StringReader(source))
            val keyPair = pemParser.readObject() as PEMKeyPair
            val converter = JcaPEMKeyConverter()
            return converter.getKeyPair(keyPair)
        }
    }

    private inner class OpenSshPemToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        public override fun throwingConvert(source: String?): KeyPair {
            val converter = OpenSSHKeyFile()
            converter.init(StringReader(source), passwordFinder)
            return KeyPair(converter.public, converter.private)
        }
    }

    private inner class OpenSshV1PemToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        public override fun throwingConvert(source: String?): KeyPair {
            val converter = OpenSSHKeyV1KeyFile()
            converter.init(StringReader(source), passwordFinder)
            return KeyPair(converter.public, converter.private)
        }
    }

    private inner class PuttyPrivateKeyToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        public override fun throwingConvert(source: String?): KeyPair {
            val converter = PuTTYKeyFile()
            converter.init(StringReader(source), passwordFinder)
            return KeyPair(converter.public, converter.private)
        }
    }
}