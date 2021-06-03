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

package com.amaze.filemanager.ui.fragments.preference_fragments;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 1/1/2018, at 21:16. */
public class PreferencesConstants {
  // START fragments
  public static final String FRAGMENT_THEME = "theme";
  public static final String FRAGMENT_COLORS = "colors";
  public static final String FRAGMENT_FOLDERS = "sidebar_folders";
  public static final String FRAGMENT_QUICKACCESSES = "sidebar_quickaccess";
  public static final String FRAGMENT_ADVANCED_SEARCH = "advancedsearch";
  public static final String FRAGMENT_ABOUT = "about";
  public static final String FRAGMENT_FEEDBACK = "feedback";
  // END fragments

  // START preferences.xml constants
  public static final String PREFERENCE_INTELLI_HIDE_TOOLBAR = "intelliHideToolbar";
  public static final String PREFERENCE_SHOW_FILE_SIZE = "showFileSize";
  public static final String PREFERENCE_SHOW_PERMISSIONS = "showPermissions";
  public static final String PREFERENCE_SHOW_DIVIDERS = "showDividers";
  public static final String PREFERENCE_SHOW_HEADERS = "showHeaders";
  public static final String PREFERENCE_SHOW_GOBACK_BUTTON = "goBack_checkbox";
  public static final String PREFERENCE_SHOW_SIDEBAR_FOLDERS = "sidebar_folders_enable";
  public static final String PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES = "sidebar_quickaccess_enable";
  public static final String PREFERENCE_ENABLE_MARQUEE_FILENAME = "enableMarqueeFilename";
  public static final String PREFERENCE_ROOT_LEGACY_LISTING = "legacyListing";
  public static final String PREFERENCE_DRAG_AND_DROP_PREFERENCE = "dragAndDropPreference";
  public static final String PREFERENCE_DRAG_AND_DROP_REMEMBERED = "dragOperationRemembered";

  public static final String PREFERENCE_CLEAR_OPEN_FILE = "clear_open_file";
  public static final String PREFERENCE_BOOKMARKS_ADDED = "books_added";
  public static final String PREFERENCE_TEXTEDITOR_NEWSTACK = "texteditor_newstack";
  public static final String PREFERENCE_SHOW_HIDDENFILES = "showHidden";
  public static final String PREFERENCE_SHOW_LAST_MODIFIED = "showLastModified";
  public static final String PREFERENCE_USE_CIRCULAR_IMAGES = "circularimages";
  public static final String PREFERENCE_ROOTMODE = "rootmode";
  public static final String PREFERENCE_CHANGEPATHS = "typeablepaths";
  public static final String PREFERENCE_GRID_COLUMNS = "columns";
  public static final String PREFERENCE_SHOW_THUMB = "showThumbs";

  public static final String PREFERENCE_CRYPT_MASTER_PASSWORD = "crypt_password";
  public static final String PREFERENCE_CRYPT_FINGERPRINT = "crypt_fingerprint";
  public static final String PREFERENCE_CRYPT_WARNING_REMEMBER = "crypt_remember";

  public static final String ENCRYPT_PASSWORD_FINGERPRINT = "fingerprint";
  public static final String ENCRYPT_PASSWORD_MASTER = "master";

  public static final String PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT = "";
  public static final boolean PREFERENCE_CRYPT_FINGERPRINT_DEFAULT = false;
  public static final boolean PREFERENCE_CRYPT_WARNING_REMEMBER_DEFAULT = false;

  public static final String PREFERENCE_ZIP_EXTRACT_PATH = "extractpath";
  // END preferences.xml constants

  // START color_prefs.xml constants
  public static final String PREFERENCE_SKIN = "skin";
  public static final String PREFERENCE_SKIN_TWO = "skin_two";
  public static final String PREFERENCE_ACCENT = "accent_skin";
  public static final String PREFERENCE_ICON_SKIN = "icon_skin";
  public static final String PREFERENCE_CURRENT_TAB = "current_tab";
  public static final String PREFERENCE_COLORIZE_ICONS = "coloriseIcons";
  public static final String PREFERENCE_COLORED_NAVIGATION = "colorednavigation";
  public static final String PREFERENCE_RANDOM_COLOR = "random_checkbox";
  // END color_prefs.xml constants

  // START folders_prefs.xml constants
  public static final String PREFERENCE_SHORTCUT = "add_shortcut";
  // END folders_prefs.xml constants

  // START random preferences
  public static final String PREFERENCE_DIRECTORY_SORT_MODE = "dirontop";
  public static final String PREFERENCE_DRAWER_HEADER_PATH = "drawer_header_path";
  public static final String PREFERENCE_URI = "URI";
  public static final String PREFERENCE_HIDEMODE = "hidemode";
  public static final String PREFERENCE_VIEW = "view";
  public static final String PREFERENCE_NEED_TO_SET_HOME = "needtosethome";

  /** The value is an int with values RANDOM_INDEX, CUSTOM_INDEX, NO_DATA or [0, ...] */
  public static final String PREFERENCE_COLOR_CONFIG = "color config";
  // END random preferences

  // START sort preferences
  public static final String PREFERENCE_SORTBY_ONLY_THIS = "sortby_only_this";
  public static final String PREFERENCE_APPLIST_SORTBY = "AppsListFragment.sortBy";
  public static final String PREFERENCE_APPLIST_ISASCENDING = "AppsListFragment.isAscending";
  // END sort preferences

  // drag and drop preferences
  public static final int PREFERENCE_DRAG_DEFAULT = 0;
  public static final int PREFERENCE_DRAG_TO_SELECT = 1;
  public static final int PREFERENCE_DRAG_TO_MOVE_COPY = 2;
  public static final String PREFERENCE_DRAG_REMEMBER_COPY = "copy";
  public static final String PREFERENCE_DRAG_REMEMBER_MOVE = "move";
}
