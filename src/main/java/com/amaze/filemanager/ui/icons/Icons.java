/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.ui.icons;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.SparseArray;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.CryptUtil;

import java.io.File;
import java.util.HashMap;

public class Icons {
    private static HashMap<String, Integer> sMimeIconIds = new HashMap<>();
    private static SparseArray<Bitmap> sMimeIcons = new SparseArray<>();

    private static void add(String mimeType, int resId) {
        if (sMimeIconIds.put(mimeType, resId) != null) {
            throw new RuntimeException(mimeType + " already registered!");
        }
    }

    private static void add(int resId, String... mimeTypes) {
        for (String type : mimeTypes) {
            add(type, resId);
        }
    }

    static {
        // Package
        add(R.drawable.ic_doc_apk_white,
                "application/vnd.android.package-archive"
        );

// Audio
        add(R.drawable.ic_doc_audio_am,
                "application/ogg",
                "application/x-flac"
        );

// Certificate
        add(R.drawable.ic_doc_certificate,
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
        );

// Source code
        add(R.drawable.ic_doc_codes,
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
        );

// Compressed
        add(R.drawable.ic_zip_box_white_36dp,
                "application/mac-binhex40",
                "application/rar",
                "application/zip",
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
                "application/gzip",
                "application/x-7z-compressed",
                "application/x-deb",
                "application/x-rar-compressed"
        );

// Contact
        add(R.drawable.ic_doc_contact_am,
                "text/x-vcard",
                "text/vcard"
        );

// Event
        add(R.drawable.ic_doc_event_am,
                "text/calendar",
                "text/x-vcalendar"
        );

// Font
        add(R.drawable.ic_doc_font,
                "application/x-font",
                "application/font-woff",
                "application/x-font-woff",
                "application/x-font-ttf"
        );

// Image
        add(R.drawable.ic_doc_image,
                "application/vnd.oasis.opendocument.graphics",
                "application/vnd.oasis.opendocument.graphics-template",
                "application/vnd.oasis.opendocument.image",
                "application/vnd.stardivision.draw",
                "application/vnd.sun.xml.draw",
                "application/vnd.sun.xml.draw.template",
                "image/jpeg",
                "image/png"
        );

// PDF
        add(R.drawable.ic_doc_pdf,
                "application/pdf"
        );

// Presentation
        add(R.drawable.ic_doc_presentation,
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.presentationml.template",
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                "application/vnd.stardivision.impress",
                "application/vnd.sun.xml.impress",
                "application/vnd.sun.xml.impress.template",
                "application/x-kpresenter",
                "application/vnd.oasis.opendocument.presentation"
        );

// Spreadsheet
        add(R.drawable.ic_doc_spreadsheet_am,
                "application/vnd.oasis.opendocument.spreadsheet",
                "application/vnd.oasis.opendocument.spreadsheet-template",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                "application/vnd.stardivision.calc",
                "application/vnd.sun.xml.calc",
                "application/vnd.sun.xml.calc.template",
                "application/x-kspread"
        );

// Doc
        add(R.drawable.ic_doc_doc_am,
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
                "application/x-kword"
        );

// Text
        add(R.drawable.ic_doc_text_am,
                "text/plain"
        );

// Video
        add(R.drawable.ic_doc_video_am,
                "application/x-quicktimeplayer",
                "application/x-shockwave-flash"
        );
    }

    public static boolean isText(String name) {
        String mimeType = MimeTypes.getMimeType(new File(name));

        Integer res = sMimeIconIds.get(mimeType);
        if (res != null && res == R.drawable.ic_doc_text_am) return true;
        if (mimeType != null && mimeType.contains("/")) {
            final String typeOnly = mimeType.split("/")[0];
            if ("text".equals(typeOnly)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVideo(String name) {
        String mimeType = MimeTypes.getMimeType(new File(name));
        Integer res = sMimeIconIds.get(mimeType);
        if (res != null && res == R.drawable.ic_doc_video_am) return true;
        if (mimeType != null && mimeType.contains("/")) {
            final String typeOnly = mimeType.split("/")[0];
            if ("video".equals(typeOnly)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEncrypted(String name) {
        if (name.endsWith(CryptUtil.CRYPT_EXTENSION))
            return true;
        else return false;
    }

    public static boolean isAudio(String name) {
        String mimeType = MimeTypes.getMimeType(new File(name));
        Integer res = sMimeIconIds.get(mimeType);
        if (res != null && res == R.drawable.ic_doc_audio_am) return true;
        if (mimeType != null && mimeType.contains("/")) {
            final String typeOnly = mimeType.split("/")[0];
            if ("audio".equals(typeOnly)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCode(String name) {
        Integer res = sMimeIconIds.get(MimeTypes.getMimeType(new File(name)));
        return res != null && res == R.drawable.ic_doc_codes;
    }

    public static boolean isArchive(String name) {
        Integer res = sMimeIconIds.get(MimeTypes.getMimeType(new File(name)));
        return res != null && res == R.drawable.ic_zip_box_white_36dp;
    }

    public static boolean isApk(String name) {
        Integer res = sMimeIconIds.get(MimeTypes.getMimeType(new File(name)));
        return res != null && res == R.drawable.ic_doc_apk_white;
    }

    public static boolean isPdf(String name) {
        Integer res = sMimeIconIds.get(MimeTypes.getMimeType(new File(name)));
        return res != null && res == R.drawable.ic_doc_pdf;
    }

    public static boolean isPicture(String name) {
        Integer res = sMimeIconIds.get(MimeTypes.getMimeType(new File(name)));
        return res != null && res == R.drawable.ic_doc_image;
    }

    public static boolean isGeneric(String name) {
        String mimeType = MimeTypes.getMimeType(new File(name));
        if (mimeType == null) {
            return true;
        }
        Integer resId = sMimeIconIds.get(mimeType);

        return resId == null;
    }

    public static BitmapDrawable loadMimeIcon(String path, boolean grid, final Resources res) {
        String mimeType = MimeTypes.getMimeType(new File(path));
        if (mimeType == null) {
            /* if(grid) return loadBitmapDrawableById(res, R.drawable.ic_doc_generic_am_grid);*/
            return loadBitmapDrawableById(res, R.drawable.ic_doc_generic_am);
        }

        // Look for exact match first
        Integer resId = sMimeIconIds.get(mimeType);

        if (resId != null) {
            switch (resId) {
                case R.drawable.ic_doc_apk_white:
                    if (grid) resId = R.drawable.ic_doc_apk_grid;
                    break;/*
            case R.drawable.ic_doc_audio_am: if(grid)resId=R.drawable.ic_doc_audio_am_grid;
                break;
            case R.drawable.ic_doc_certificate: if(grid)resId=R.drawable.ic_doc_certificate_grid;
                break;
            case R.drawable.ic_doc_codes: if(grid)resId=R.drawable.ic_doc_codes_grid;
                break;
            case R.drawable.ic_doc_font: if(grid)resId=R.drawable.ic_doc_font_grid;
                break;
            case R.drawable.ic_doc_generic_am: if(grid)resId=R.drawable.ic_doc_generic_am_grid;
                break;
            */
                case R.drawable.ic_doc_image:
                    if (grid) resId = R.drawable.ic_doc_image_grid;
                    break;
            }
            /*case R.drawable.ic_doc_pdf: if(grid)resId=R.drawable.ic_doc_pdf_grid;
                break;
            case R.drawable.ic_doc_video_am: if(grid)resId=R.drawable.ic_doc_video_am_grid;
                break;
            case R.drawable.ic_doc_text_am: if(grid)resId=R.drawable.ic_doc_text_am_grid;
                break;
        }*/
            return loadBitmapDrawableById(res, resId);
        }

        // Otherwise look for partial match
        final String typeOnly = mimeType.split("/")[0];

        if ("audio".equals(typeOnly)) {
           /* if (grid) resId = R.drawable.ic_doc_audio_am_grid; else*/
            resId = R.drawable.ic_doc_audio_am;
        } else if ("image".equals(typeOnly)) {
            if (grid) resId =  R.drawable.ic_doc_image_grid;
            else resId = R.drawable.ic_doc_image;
        } else if ("text".equals(typeOnly)) {
            /*if (grid) resId = R.drawable.ic_doc_text_am_grid; else*/
            resId = R.drawable.ic_doc_text_am;
        } else if ("video".equals(typeOnly)) {
            /*if (grid) resId = R.drawable.ic_doc_video_am_grid; else*/
            resId = R.drawable.ic_doc_video_am;
        } else if (path.endsWith(CryptUtil.CRYPT_EXTENSION)) {
            resId = R.drawable.ic_file_lock_white_36dp;
        }
        if (resId == null) {
            /*if (grid) resId = R.drawable.ic_doc_generic_am_grid; else*/
            resId = R.drawable.ic_doc_generic_am;
        }
        return loadBitmapDrawableById(res, resId);
    }

    private static BitmapDrawable loadBitmapDrawableById(Resources res, int resId) {
        Bitmap bitmap = sMimeIcons.get(resId);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(res, resId);
            sMimeIcons.put(resId, bitmap);
        }
        return new BitmapDrawable(res, bitmap);
    }
}
