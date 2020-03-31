package com.amaze.filemanager.shadows;

import android.content.Context;

import androidx.multidex.MultiDex;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(MultiDex.class)
public class ShadowMultiDex {

    @Implementation
    public static void install(Context context) {
        // Do nothing since with Robolectric nothing is dexed.
    }
}