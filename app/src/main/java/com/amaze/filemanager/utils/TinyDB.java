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

import java.util.ArrayList;
import java.util.Arrays;

//import com.google.gson.Gson;

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

    /**
     * Get parsed ArrayList of T from SharedPreferences at 'key'
     * @param key SharedPreferences key
     * @return ArrayList of T
     */
    public static <T> ArrayList<T> getList(SharedPreferences preferences, Class<T> klazz, String key,
                                           ArrayList<T> defaultValue) {
        String pref = preferences.getString(key, "");

        if(pref.equals("")) {
            return defaultValue;
        }

        String[] myList = TextUtils.split(pref, DIVIDER);
        ArrayList<String> arrayToList = new ArrayList<>(Arrays.asList(myList));
        ArrayList<T> newList = new ArrayList<>();

        for (String item : arrayToList)
            newList.add(valueOf(klazz, item));

        return newList;
    }

    // Put methods

    /**
     * Put ArrayList of T into SharedPreferences with 'key' and save
     * @param key SharedPreferences key
     */
    public static <T> void putList(SharedPreferences preferences, String key, ArrayList<T> list) {
        checkForNullKey(key);
        Object[] myList = list.toArray();
        preferences.edit().putString(key, TextUtils.join(DIVIDER, myList)).apply();
    }

    /**
     * null keys would corrupt the shared pref file and make them unreadable this is a preventive measure
     * @param key the pref key
     */
    private static void checkForNullKey(String key){
        if (key == null){
            throw new NullPointerException();
        }
    }

    private static <T> T valueOf(Class<T> klazz, String arg) {
        Exception cause = null;

        if(klazz != String.class) {
            T ret = null;
            try {
                ret = klazz.cast(klazz.getDeclaredMethod("valueOf", String.class).invoke(null, arg));
            } catch (Exception e) {
                cause = e;
            }

            if (cause == null) {
                return ret;
            } else {
                throw new IllegalArgumentException(cause);
            }
        } else {
            return (T) arg;//Is string but String.valueOf(String) doesn't exist
        }
    }
}