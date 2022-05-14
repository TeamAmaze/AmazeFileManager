/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
package com.amaze.filemanager.ui.icons

import androidx.annotation.DrawableRes
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.compressed.CompressedHelper

object Icons {
    const val NOT_KNOWN = -1
    const val APK = 0
    const val AUDIO = 1
    const val CERTIFICATE = 2
    const val CODE = 3
    const val COMPRESSED = 4
    const val CONTACT = 5
    const val EVENTS = 6
    const val FONT = 7
    const val IMAGE = 8
    const val PDF = 9
    const val PRESENTATION = 10
    const val SPREADSHEETS = 11
    const val DOCUMENTS = 12
    const val TEXT = 13
    const val VIDEO = 14
    const val ENCRYPTED = 15
    const val GIF = 16

    // construct an approximation of the capacity
    private val sMimeIconIds = HashMap<String, Int>(1 + (114 / 0.75).toInt())
    private fun put(mimeType: String, resId: Int) {
        if (sMimeIconIds.put(mimeType, resId) != null) {
            throw RuntimeException("$mimeType already registered!")
        }
    }

    private fun putKeys(resId: Int, vararg mimeTypes: String) {
        for (type in mimeTypes) {
            put(type, resId)
        }
    }

    @JvmStatic
    @DrawableRes
    fun loadMimeIcon(path: String, isDirectory: Boolean): Int {
        if (path == "..") return R.drawable.ic_arrow_left_white_24dp
        if (CompressedHelper.isFileExtractable(path) && !isDirectory) return R.drawable.ic_compressed_white_24dp
        val type = getTypeOfFile(path, isDirectory)
        return when (type) {
            APK -> R.drawable.ic_doc_apk_white
            AUDIO -> R.drawable.ic_doc_audio_am
            IMAGE -> R.drawable.ic_doc_image
            TEXT -> R.drawable.ic_doc_text_am
            VIDEO -> R.drawable.ic_doc_video_am
            PDF -> R.drawable.ic_doc_pdf
            CERTIFICATE -> R.drawable.ic_doc_certificate
            CODE -> R.drawable.ic_doc_codes
            FONT -> R.drawable.ic_doc_font
            ENCRYPTED -> R.drawable.ic_folder_lock_white_36dp
            else -> if (isDirectory) R.drawable.ic_grid_folder_new else {
                R.drawable.ic_doc_generic_am
            }
        }
    }

    @JvmStatic
    fun getTypeOfFile(path: String?, isDirectory: Boolean): Int {
        val mimeType = MimeTypes.getMimeType(path, isDirectory) ?: return NOT_KNOWN
        val type = sMimeIconIds[mimeType]
        return type
            ?: if (checkType(mimeType, "text")) TEXT else if (checkType(
                    mimeType,
                    "image"
                )
            ) IMAGE else if (checkType(
                    mimeType,
                    "video"
                )
            ) VIDEO else if (checkType(
                    mimeType,
                    "audio"
                )
            ) AUDIO else if (checkType(
                    mimeType,
                    "crypt"
                )
            ) ENCRYPTED else NOT_KNOWN
    }

    private fun checkType(mime: String?, check: String): Boolean {
        return mime != null && mime.contains("/") && check == mime.substring(0, mime.indexOf("/"))
    }

    init {
        putKeys(APK, "application/vnd.android.package-archive")
        putKeys(AUDIO, "application/ogg", "application/x-flac")
        putKeys(
            CERTIFICATE,
            "application/pgp-keys",
            "application/pgp-signature",
            "application/x-pkcs12",
            "application/x-pkcs7-certreqresp",
            "application/x-pkcs7-crl",
            "application/x-x509-ca-cert",
            "application/x-x509-user-cert",
            "application/x-pkcs7-certificates",
            "application/x-pkcs7-mime",
            "application/x-pkcs7-signature"
        )
        putKeys(
            CODE,
            "application/rdf+xml",
            "application/rss+xml",
            "application/x-object",
            "application/xhtml+xml",
            "text/css",
            "text/html",
            "text/xml",
            "text/x-c++hdr",
            "text/x-c++src",
            "text/x-chdr",
            "text/x-csrc",
            "text/x-dsrc",
            "text/x-csh",
            "text/x-haskell",
            "text/x-java",
            "text/x-literate-haskell",
            "text/x-pascal",
            "text/x-tcl",
            "text/x-tex",
            "application/x-latex",
            "application/x-texinfo",
            "application/atom+xml",
            "application/ecmascript",
            "application/json",
            "application/javascript",
            "application/xml",
            "text/javascript",
            "application/x-javascript"
        )
        putKeys(
            COMPRESSED,
            "application/mac-binhex40",
            "application/rar",
            "application/zip",
            "application/gzip",
            "application/java-archive",
            "application/x-apple-diskimage",
            "application/x-debian-package",
            "application/x-gtar",
            "application/x-iso9660-image",
            "application/x-lha",
            "application/x-lzh",
            "application/x-lzx",
            "application/x-stuffit",
            "application/x-tar",
            "application/x-webarchive",
            "application/x-webarchive-xml",
            "application/x-gzip",
            "application/x-7z-compressed",
            "application/x-deb",
            "application/x-rar-compressed",
            "application/x-lzma",
            "application/x-xz",
            "application/x-bzip2"
        )
        putKeys(CONTACT, "text/x-vcard", "text/vcard")
        putKeys(EVENTS, "text/calendar", "text/x-vcalendar")
        putKeys(
            FONT,
            "application/x-font",
            "application/font-woff",
            "application/x-font-woff",
            "application/x-font-ttf"
        )
        putKeys(
            IMAGE,
            "application/vnd.oasis.opendocument.graphics",
            "application/vnd.oasis.opendocument.graphics-template",
            "application/vnd.oasis.opendocument.image",
            "application/vnd.stardivision.draw",
            "application/vnd.sun.xml.draw",
            "application/vnd.sun.xml.draw.template",
            "image/jpeg",
            "image/png"
        )
        putKeys(PDF, "application/pdf")
        putKeys(
            PRESENTATION,
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.presentationml.template",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            "application/vnd.stardivision.impress",
            "application/vnd.sun.xml.impress",
            "application/vnd.sun.xml.impress.template",
            "application/x-kpresenter",
            "application/vnd.oasis.opendocument.presentation"
        )
        putKeys(
            SPREADSHEETS,
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.spreadsheet-template",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
            "application/vnd.stardivision.calc",
            "application/vnd.sun.xml.calc",
            "application/vnd.sun.xml.calc.template",
            "application/x-kspread",
            "text/comma-separated-values"
        )
        putKeys(
            DOCUMENTS,
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.text-master",
            "application/vnd.oasis.opendocument.text-template",
            "application/vnd.oasis.opendocument.text-web",
            "application/vnd.stardivision.writer",
            "application/vnd.stardivision.writer-global",
            "application/vnd.sun.xml.writer",
            "application/vnd.sun.xml.writer.global",
            "application/vnd.sun.xml.writer.template",
            "application/x-abiword",
            "application/x-kword",
            "text/markdown"
        )
        putKeys(TEXT, "text/plain")
        putKeys(VIDEO, "application/x-quicktimeplayer", "application/x-shockwave-flash")
        putKeys(ENCRYPTED, "application/octet-stream")
    }
}