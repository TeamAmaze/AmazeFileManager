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

    return LocaleListCompat.forLanguageTags(tagsList.joinToString(","))
}

/**
 * [Context] extension to return a [Map] of [Locale] with its display name as key.
 *
 * For preference drop down convenience.
 */
fun Context.getLangPreferenceDropdownEntries(): Map<String, Locale> {
    val localeList = getLocaleListFromXml()
    val currentLocaleList: List<Locale> = (
        if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.getApplicationLocales()
        } else {
            LocaleListCompat.getDefault()
        }
        ).let { appLocales ->
        ArrayList<Locale>().apply {
            for (x in 0 until appLocales.size()) {
                appLocales.get(x)?.let {
                    this.add(it)
                }
            }
        }
    }
    val map = mutableMapOf<String, Locale>()

    for (a in 0 until localeList.size()) {
        localeList[a].let {
            it?.run {
                val displayName: String = if (currentLocaleList.isEmpty()) {
                    this.getDisplayName(Locale.getDefault())
                } else {
                    this.getDisplayName(
                        currentLocaleList.first { locale ->
                            this.getDisplayName(locale).isNotEmpty()
                        }
                    )
                }
                map.put(displayName, this)
            }
        }
    }
    return map
}

/**
 * [Context] extension to set app locale fluently.
 *
 * Calls [AppCompatDelegate.setApplicationLocales] under the hood.
 */
fun Context.setLocale(langTag: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langTag)
    AppCompatDelegate.setApplicationLocales(appLocale)
}
