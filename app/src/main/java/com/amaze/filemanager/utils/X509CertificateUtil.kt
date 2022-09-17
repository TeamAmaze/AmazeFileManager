/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.utils

import net.schmizz.sshj.common.ByteArrayUtils
import org.json.JSONObject
import java.security.MessageDigest
import java.util.*

object X509CertificateUtil {

    const val SUBJECT = "subject"
    const val ISSUER = "issuer"
    const val SERIAL = "serial"
    const val FINGERPRINT = "sha256Fingerprint"

    private fun colonSeparatedHex(array: ByteArray) =
        ByteArrayUtils.toHex(array).chunked(2).joinToString(":")

    /**
     * Parse a [javax.security.cert.X509Certificate] and return part of its information in a JSON object.
     *
     * Includes the certificate's subject, issuer, serial number and SHA-256 fingerprint.
     *
     * @param certificate [javax.security.cert.X509Certificate]
     * @return [JSONObject]
     */
    fun parse(certificate: javax.security.cert.X509Certificate): Map<String, String> {
        val retval = WeakHashMap<String, String>()
        retval[SUBJECT] = certificate.subjectDN.name
        retval[ISSUER] = certificate.issuerDN.name
        retval[SERIAL] = colonSeparatedHex(certificate.serialNumber.toByteArray())
        retval[FINGERPRINT] = MessageDigest.getInstance("sha-256").run {
            colonSeparatedHex(digest(certificate.encoded))
        }
        return retval
    }

    /**
     * Parse a [java.security.cert.X509Certificate] and return part of its information in a JSON object.
     *
     * Includes the certificate's subject, issuer, serial number and SHA-256 fingerprint.
     *
     * @param certificate [java.security.cert.X509Certificate]
     * @return [JSONObject]
     */
    fun parse(certificate: java.security.cert.X509Certificate): Map<String, String> {
        val retval = WeakHashMap<String, String>()
        retval[SUBJECT] = certificate.subjectDN.name
        retval[ISSUER] = certificate.issuerDN.name
        retval[SERIAL] = colonSeparatedHex(certificate.serialNumber.toByteArray())
        retval[FINGERPRINT] = MessageDigest.getInstance("sha-256").run {
            colonSeparatedHex(digest(certificate.encoded))
        }
        return retval
    }
}
