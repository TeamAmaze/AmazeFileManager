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

package com.amaze.filemanager.filesystem.ftp

import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.COLON
import com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX

/**
 * Container object for SSH/FTP/FTPS URL, encapsulating logic for splitting information from given
 * URL. `Uri.parse()` only parse URL that is compliant to RFC2396, but we have to deal with
 * URL that is not compliant, since usernames and/or strong passwords usually have special
 * characters included, like `ssh://user@example.com:P@##w0rd@127.0.0.1:22`.
 *
 * A design decision to keep database schema slim, by the way... -TranceLove
 *
 * @param url URI to break down.
 *
 * For credentials, can be base64 or URL encoded, but if both username and password is provided,
 * must use plain colon character [COLON] as separator.
 *
 * For paths and query strings, **always** use URL encoded paths, or undesired behaviour will
 * occur. No validation is made at this point, so proceed at your own risk.
 */
class NetCopyConnectionInfo(url: String) {

    val prefix: String
    val host: String
    val port: Int
    val username: String
    val password: String?
    var defaultPath: String? = null
        private set
    var queryString: String? = null
        private set
    var arguments: Map<String, String>?
        private set
    var filename: String? = null
        private set

    companion object {
        // Regex taken from https://blog.stevenlevithan.com/archives/parseuri
        // (No, don't break it down to lines)

        /* ktlint-disable max-line-length */
        private const val URI_REGEX = "^(?:(?![^:@]+:[^:@/]*@)([^:/?#.]+):)?(?://)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:/?#]*)(?::(\\d*))?)(((/(?:[^?#](?![^?#/]*\\.[^?#/.]+(?:[?#]|$)))*/?)?([^?#/]*))(?:\\?([^#]*))?(?:#(.*))?)"

        /* ktlint-enable max-line-length */

        const val MULTI_SLASH = "(?<=[^:])(//+)"

        const val AND = '&'
        const val AT = '@'
        const val SLASH = '/'
        const val COLON = ':'
    }

    init {
        require(
            url.startsWith(SSH_URI_PREFIX) or
                url.startsWith(FTP_URI_PREFIX) or
                url.startsWith(FTPS_URI_PREFIX) or
                url.startsWith(SMB_URI_PREFIX)
        ) {
            "Argument is not a supported remote URI: $url"
        }
        val regex = Regex(URI_REGEX)
        val matches = regex.find(url)
        if (matches == null) {
            throw IllegalArgumentException("Unable to parse URI")
        } else {
            matches.groupValues.let {
                prefix = "${it[1]}://"
                host = it[6]
                val credential = it[3]
                if (!credential.contains(COLON)) {
                    username = credential
                    password = null
                } else {
                    username = credential.substringBefore(COLON)
                    password = credential.substringAfter(COLON)
                }
                port = if (it[7].isNotEmpty()) {
                    /*
                     * Invalid string would have been trapped to other branches. Strings fell into
                     * this branch must be integer
                     */
                    it[7].toInt()
                } else {
                    0
                }
                queryString = it[12].ifEmpty { null }
                arguments = if (it[12].isNotEmpty()) {
                    it[12].split(AND).associate { valuePair ->
                        val pair = valuePair.split('=')
                        Pair(
                            pair[0],
                            pair[1].ifEmpty {
                                ""
                            }
                        )
                    }
                } else {
                    null
                }
                defaultPath = (
                    if (it[9].isEmpty()) {
                        null
                    } else if (it[9] == SLASH.toString()) {
                        SLASH.toString()
                    } else if (!it[9].endsWith(SLASH)) {
                        if (it[11].isEmpty()) {
                            it[10]
                        } else {
                            it[10].substringBeforeLast(SLASH)
                        }
                    } else {
                        it[9]
                    }
                    )?.replace(Regex(MULTI_SLASH), SLASH.toString())
                filename = it[11].ifEmpty { null }
            }
        }
    }

    /**
     * Returns the last segment of the URL's path element.
     */
    fun lastPathSegment(): String? {
        return if (filename != null && true == filename?.isNotEmpty()) {
            filename
        } else if (defaultPath != null && true == defaultPath?.isNotEmpty()) {
            defaultPath!!.substringAfterLast(SLASH)
        } else {
            null
        }
    }

    override fun toString(): String {
        return if (username.isNotEmpty()) {
            "$prefix$username@$host${if (port == 0) "" else ":$port"}${defaultPath ?: ""}"
        } else {
            "$prefix$host${if (port == 0) "" else ":$port"}${defaultPath ?: ""}"
        }
    }
}
