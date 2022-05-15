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

package com.amaze.filemanager.ui.fragments.preferencefragments

object PreferencesConstants {
    // appearance_prefs.xml
    const val FRAGMENT_THEME = "theme"
    const val PREFERENCE_USE_CIRCULAR_IMAGES = "circularimages"
    const val PREFERENCE_SHOW_DIVIDERS = "showDividers"
    const val PREFERENCE_SHOW_HEADERS = "showHeaders"
    const val PREFERENCE_COLORIZE_ICONS = "coloriseIcons"
    const val PREFERENCE_COLORED_NAVIGATION = "colorednavigation"
    const val PREFERENCE_SELECT_COLOR_CONFIG = "selectcolorconfig"
    const val PREFERENCE_INTELLI_HIDE_TOOLBAR = "intelliHideToolbar"
    const val PREFERENCE_GRID_COLUMNS = "columnsGrid"
    const val PREFERENCE_GRID_COLUMNS_DEFAULT = "3"
    const val PREFERENCE_ENABLE_MARQUEE_FILENAME = "enableMarqueeFilename"

    // color_prefs.xml
    const val PREFERENCE_SKIN = "skin"
    const val PREFERENCE_SKIN_TWO = "skin_two"
    const val PREFERENCE_ACCENT = "accent_skin"
    const val PREFERENCE_ICON_SKIN = "icon_skin"
    const val PRESELECTED_CONFIGS = "preselectedconfigs"

    /** The value is an int with values RANDOM_INDEX, CUSTOM_INDEX, NO_DATA or [0, ...]  */
    const val PREFERENCE_COLOR_CONFIG = "color config"

    // ui_prefs.xml
    const val PREFERENCE_SHOW_THUMB = "showThumbs"
    const val PREFERENCE_SHOW_FILE_SIZE = "showFileSize"
    const val PREFERENCE_SHOW_PERMISSIONS = "showPermissions"
    const val PREFERENCE_SHOW_GOBACK_BUTTON = "goBack_checkbox"
    const val PREFERENCE_SHOW_HIDDENFILES = "showHidden"
    const val PREFERENCE_SHOW_LAST_MODIFIED = "showLastModified"
    const val PREFERENCE_DRAG_AND_DROP_PREFERENCE = "dragAndDropPreference"
    const val PREFERENCE_DRAG_AND_DROP_REMEMBERED = "dragOperationRemembered"

    // drag and drop
    const val PREFERENCE_DRAG_DEFAULT = 0
    const val PREFERENCE_DRAG_TO_SELECT = 1
    const val PREFERENCE_DRAG_TO_MOVE_COPY = 2
    const val PREFERENCE_DRAG_REMEMBER_COPY = "copy"
    const val PREFERENCE_DRAG_REMEMBER_MOVE = "move" // END drag and drop preferences

    // bookmarks_prefs.xml
    const val PREFERENCE_SHOW_SIDEBAR_FOLDERS = "sidebar_bookmarks_enable"

    // quickaccess_prefs.xml
    const val PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES = "sidebar_quickaccess_enable"

    // behavior_prefs.xml
    const val PREFERENCE_ROOT_LEGACY_LISTING = "legacyListing"
    const val PREFERENCE_ROOTMODE = "rootmode"
    const val PREFERENCE_CHANGEPATHS = "typeablepaths"
    const val PREFERENCE_SAVED_PATHS = "savepaths"
    const val PREFERENCE_ZIP_EXTRACT_PATH = "extractpath"
    const val PREFERENCE_TEXTEDITOR_NEWSTACK = "texteditor_newstack"

    // security_prefs.xml
    const val PREFERENCE_CRYPT_FINGERPRINT = "crypt_fingerprint"
    const val PREFERENCE_CRYPT_MASTER_PASSWORD = "crypt_password"
    const val PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT = ""
    const val PREFERENCE_CRYPT_FINGERPRINT_DEFAULT = false

    const val PREFERENCE_CRYPT_WARNING_REMEMBER = "crypt_remember"
    const val ENCRYPT_PASSWORD_FINGERPRINT = "fingerprint"
    const val ENCRYPT_PASSWORD_MASTER = "master"
    const val PREFERENCE_CRYPT_WARNING_REMEMBER_DEFAULT = false

    // others
    const val PREFERENCE_CURRENT_TAB = ""
    const val PREFERENCE_BOOKMARKS_ADDED = "books_added"
    const val PREFERENCE_DIRECTORY_SORT_MODE = "dirontop"
    const val PREFERENCE_DRAWER_HEADER_PATH = "drawer_header_path"
    const val PREFERENCE_URI = "URI"
    const val PREFERENCE_VIEW = "view"
    const val PREFERENCE_NEED_TO_SET_HOME = "needtosethome"

    const val PREFERENCE_SORTBY_ONLY_THIS = "sortby_only_this"
    const val PREFERENCE_APPLIST_SORTBY = "AppsListFragment.sortBy"
    const val PREFERENCE_APPLIST_ISASCENDING = "AppsListFragment.isAscending"
}
