package com.amaze.filemanager.asynchronous.services.ftp

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES.P
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import com.amaze.filemanager.application.AppConfig
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateExpiredException
import java.security.cert.X509Certificate
import java.security.spec.RSAKeyGenParameterSpec
import java.util.Calendar
import javax.security.auth.x500.X500Principal

/**
 * Provides the FTPS keystore with an overridable factory for testing.
 * Defaults to AndroidKeyStore; tests can inject an in-memory BKS/PKCS12 store.
 * Generates a self-signed RSA-2048 certificate on first use or if the existing
 * entry is missing/corrupt.
 */
@SuppressLint("StaticFieldLeak")
internal object FtpServerSslKeyStoreProvider {
    private val log = LoggerFactory.getLogger(FtpServerSslKeyStoreProvider::class.java)

    internal var FTPS_CERT_ALIAS: String = "${AppConfig.getInstance().packageName}.ftpserver"

    private const val FTPS_KEY_ALGORITHM = "RSA"
    private const val FTPS_KEY_SIZE = 2048
    private const val FTPS_KEY_DN = "CN=ftpserver, OU=Amaze File Manager, O=Team Amaze"
    private const val FTPS_CERT_VALIDITY_DAYS = 7300 // ~20 years

    private val strongBoxSupported
        get() = Build.VERSION.SDK_INT >= P &&
                AppConfig.getInstance().packageManager.hasSystemFeature(
                    PackageManager.FEATURE_STRONGBOX_KEYSTORE
                )

    /**
     * Factory lambda to create a KeyStore instance.
     * Default: AndroidKeyStore with null load.
     * Tests can override this to inject an in-memory BKS/PKCS12 store.
     */
    @VisibleForTesting
    var keyStoreFactory: () -> KeyStore = {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }

    /**
     * Return a configured KeyStore instance using the current factory,
     * ensuring the FTPS certificate exists and is valid.
     */
    fun getKeyStore(): KeyStore {
        val keyStore = keyStoreFactory()

        // Try to use existing certificate; regenerate if missing/corrupt
        try {
            if (keyStore.containsAlias(FTPS_CERT_ALIAS)) {
                val existingEntry = keyStore.getEntry(FTPS_CERT_ALIAS, null)
                if (existingEntry is KeyStore.PrivateKeyEntry) {
                    val cert = existingEntry.certificate as? X509Certificate
                    if (cert != null) {
                        try {
                            cert.checkValidity()
                            return keyStore
                        } catch (e: CertificateExpiredException) {
                            log.warn("Existing FTPS certificate is expired or not yet valid", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.warn("Existing FTPS certificate is corrupt; regenerating", e)
        }

        // Generate new certificate
        try {
            generateFtpsCertificate(keyStore)
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate FTPS certificate", e)
        }

        return keyStore
    }

    /**
     * Reset the factory to the default AndroidKeyStore.
     * Useful for test cleanup.
     */
    fun reset() {
        keyStoreFactory = {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        }
    }

    /**
     * Clear the FTPS certificate and key from the keystore.
     * Useful for test cleanup or resetting state.
     */
    fun clear() {
        getKeyStore().run {
            if (containsAlias(FTPS_CERT_ALIAS)) {
                deleteEntry(FTPS_CERT_ALIAS)
                log.info("Cleared FTPS certificate from keystore")
            }
        }
    }

    /**
     * Generate a self-signed RSA-2048 certificate and store it in the keystore.
     */
    private fun generateFtpsCertificate(keyStore: KeyStore) {
        val keyPair = generateKeyPair()

        // Build self-signed certificate
        val now = Calendar.getInstance().time
        val notAfter =
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, FTPS_CERT_VALIDITY_DAYS)
            }.time

        val serialNumber = BigInteger.valueOf(SecureRandom().nextLong())
        val subject = X500Name(FTPS_KEY_DN)

        val builder =
            JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                now,
                notAfter,
                subject,
                keyPair.public,
            )

        // Add extensions
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        builder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment or KeyUsage.dataEncipherment),
        )

        val signer: ContentSigner =
            JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .build(keyPair.private)

        val bcCert = builder.build(signer)
        val cert: X509Certificate =
            JcaX509CertificateConverter()
                .getCertificate(bcCert)

        // Store in keystore
        val privateKeyEntry = KeyStore.PrivateKeyEntry(keyPair.private, arrayOf(cert))
        keyStore.setEntry(FTPS_CERT_ALIAS, privateKeyEntry, null)

        log.info("Generated new FTPS self-signed certificate for alias: $FTPS_CERT_ALIAS")
    }

    /**
     * Generate an RSA key pair.
     */
    private fun generateKeyPair(): KeyPair {
        // Generate RSA-2048 key pair
        // BouncyCastle provider is loaded at CustomSshJConfig via AppConfig
        val keyPairGenerator = KeyPairGenerator.getInstance(FTPS_KEY_ALGORITHM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = KeyGenParameterSpec.Builder(
                FTPS_CERT_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setAlgorithmParameterSpec(
                    RSAKeyGenParameterSpec(
                        FTPS_KEY_SIZE,
                        RSAKeyGenParameterSpec.F4
                    )
                )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setUserAuthenticationRequired(false)
                .setCertificateSubject(X500Principal(FTPS_KEY_DN))
                .setCertificateSerialNumber(BigInteger.valueOf(SecureRandom().nextLong()))
                .setCertificateNotBefore(Calendar.getInstance().time)
                .setCertificateNotAfter(
                    Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, FTPS_CERT_VALIDITY_DAYS)
                    }.time,
                )
            if (strongBoxSupported) {
                builder.setIsStrongBoxBacked(true)
            }
            keyPairGenerator.initialize(builder.build())
        } else {
            keyPairGenerator.initialize(FTPS_KEY_SIZE, SecureRandom())
        }
        val keyPair: KeyPair = keyPairGenerator.generateKeyPair()
        return keyPair
    }
}
