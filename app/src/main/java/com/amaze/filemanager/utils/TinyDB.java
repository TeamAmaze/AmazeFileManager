/*
 * Copyright 2014 KC Ochibili modified by Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  The "‚‗‚" character is not a comma, it is the SINGLE LOW-9 QUOTATION MARK unicode 201A
 *  and unicode 2017 that are used for separating the items in a list.
 */
package com.amaze.filemanager.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Extract from: https://github.com/kcochibili/TinyDB--Android-Shared-Preferences-Turbo
 * Author: https://github.com/kcochibili
 */
public class TinyDB {
    /*
     *  The "‚" character is not a comma, it is the SINGLE LOW-9 QUOTATION MARK. U-201A
     *  + U-2017 + U-201A are used for separating the items in a list.
     */
    private static final String DIVIDER = "‚‗‚";

    /**
     * Put array of Boolean into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     * @param array array of Booleans to be added
     */
    public static void putBooleanArray(SharedPreferences preferences, String key, Boolean[] array) {
        preferences.edit().putString(key, TextUtils.join(DIVIDER, array)).apply();
    }

    /**
     * Get parsed array of Booleans from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * @return Array of Booleans
     */
    public static Boolean[] getBooleanArray(SharedPreferences preferences, String key, Boolean[] defaultValue) {
        String prefValue = preferences.getString(key, "");
        if(prefValue.equals("")) {
            return defaultValue;
        }

        String[] temp = TextUtils.split(prefValue, DIVIDER);
        Boolean[] newArray = new Boolean[temp.length];
        for(int i = 0; i < temp.length; i++)
            newArray[i] = Boolean.valueOf(temp[i]);
        return newArray;
    }

}