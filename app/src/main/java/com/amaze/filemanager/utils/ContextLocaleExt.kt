/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.N
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.amaze.filemanager.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Locale

/**
 * [Context] extension to return app's available locales, from locales_config.xml.
 */
fun Context.getLocaleListFromXml(): LocaleListCompat {
    val tagsList = mutableListOf<CharSequence>()
    try {
        val xpp: XmlPullParser = resources.getXml(R.xml.locales_config)
        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
            if (xpp.eventType == XmlPullParser.START_TAG) {
                if (xpp.name == "locale") {
                    tagsList.add(xpp.getAttributeValue(0))
                }
            }
            xpp.next()
        }
    } catch (e: XmlPullParserException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    // Remove locale tags that would produce same locale on Android N or above
    if (Build.VERSION.SDK_INT >= N) {
        tagsList.remove("id")
        tagsList.remove("he")
    }

    return try {
        val seenLocales = LinkedHashSet<Locale>()
        val dedupedTags = mutableListOf<String>()

        for (tag in tagsList) {
            val resolvedLocale = Locale.forLanguageTag(tag.toString())
            // add() returns false if an equal Locale was already seen -
            // this mirrors exactly what LocaleList's own dedupe check does,
            // so anything that would crash it gets filtered out here first.
            if (seenLocales.add(resolvedLocale)) {
                dedupedTags.add(tag.toString())
            }
        }

        LocaleListCompat.forLanguageTags(dedupedTags.joinToString(","))
    } catch (e: IllegalArgumentException) {
        // Last-resort safety net for any OEM ICU quirk we didn't anticipate above.
        e.printStackTrace()
        LocaleListCompat.getEmptyLocaleList()
    }
}

/**
 * [Context] extension to return a [Map] of [Locale] with its display name as key.
 *
 * For preference drop down convenience.
 */
fun Context.getLangPreferenceDropdownEntries(): Map<String, Locale> {
    val appLocales =
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            LocaleListCompat.getDefault()
        } else {
            AppCompatDelegate.getApplicationLocales()
        }

    val currentLocaleList = ArrayList<Locale>()

    for (i in 0 until appLocales.size()) {
        val appLocale = appLocales.get(i) ?: continue
        currentLocaleList.add(appLocale)
    }

    val xmlLocales = getLocaleListFromXml()
    val map = mutableMapOf<String, Locale>()

    for (i in 0 until xmlLocales.size()) {
        val xmlLocale = xmlLocales[i] ?: continue
        val displayName: String =
            if (currentLocaleList.isEmpty()) {
                xmlLocale.getDisplayName(Locale.getDefault())
            } else {
                val nameInCurrentLocale =
                    currentLocaleList.firstOrNull { locale ->
                        xmlLocale.getDisplayName(locale).isNotEmpty()
                    } ?: Locale.getDefault()

                xmlLocale.getDisplayName(nameInCurrentLocale)
            }

        map[displayName] = xmlLocale
    }

    return map
}