package com.amaze.filemanager.fragments.preference_fragments;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 1/1/2018, at 21:16.
 */

public class PreferencesConstants {
    //START fragments
    public static final String FRAGMENT_THEME = "theme";
    public static final String FRAGMENT_COLORS = "colors";
    public static final String FRAGMENT_FOLDERS = "sidebar_folders";
    public static final String FRAGMENT_QUICKACCESSES = "sidebar_quickaccess";
    public static final String FRAGMENT_ADVANCED_SEARCH = "advancedsearch";
    public static final String FRAGMENT_ABOUT = "about";
    public static final String FRAGMENT_FEEDBACK = "feedback";
    //END fragments

    //START preferences.xml constants
    public static final String PREFERENCE_INTELLI_HIDE_TOOLBAR = "intelliHideToolbar";
    public static final String PREFERENCE_SHOW_FILE_SIZE = "showFileSize";
    public static final String PREFERENCE_SHOW_PERMISSIONS = "showPermissions";
    public static final String PREFERENCE_SHOW_DIVIDERS = "showDividers";
    public static final String PREFERENCE_SHOW_HEADERS = "showHeaders";
    public static final String PREFERENCE_SHOW_GOBACK_BUTTON = "goBack_checkbox";
    public static final String PREFERENCE_SHOW_SIDEBAR_FOLDERS = "sidebar_folders_enable";
    public static final String PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES = "sidebar_quickaccess_enable";

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

    public static final String PREFERENCE_CHANGE_DRAWER_BACKGROUND = "changeDrawerBackground";
    //END preferences.xml constants

    //START color_prefs.xml constants
    public static final String PREFERENCE_SKIN = "skin";
    public static final String PREFERENCE_SKIN_TWO = "skin_two";
    public static final String PREFERENCE_ACCENT = "accent_skin";
    public static final String PREFERENCE_ICON_SKIN = "icon_skin";
    public static final String PREFERENCE_COLORIZE_ICONS = "coloriseIcons";
    public static final String PREFERENCE_COLORED_NAVIGATION = "colorednavigation";
    public static final String PREFERENCE_RANDOM_COLOR = "random_checkbox";
    //END color_prefs.xml constants

    //START folders_prefs.xml constants
    public static final String PREFERENCE_SHORTCUT = "add_shortcut";
    //END folders_prefs.xml constants

    //START random preferences
    public static final String PREFERENCE_DIRECTORY_SORT_MODE = "dirontop";
    public static final String PREFERENCE_DRAWER_HEADER_PATH = "drawer_header_path";
    public static final String PREFERENCE_URI = "URI";
    public static final String PREFERENCE_HIDEMODE = "hidemode";
    public static final String PREFERENCE_VIEW = "view";
    public static final String PREFERENCE_NEED_TO_SET_HOME = "needtosethome";

    /**
     * The value is an int with values RANDOM_INDEX, CUSTOM_INDEX, NO_DATA or [0, ...]
     */
    public static final String PREFERENCE_COLOR_CONFIG = "color config";
    //END random preferences
}
