package com.amaze.filemanager.utils;

import androidx.core.content.FileProvider;

import com.amaze.filemanager.BuildConfig;

/**
 * Created by Vishal on 20-08-2017.
 *
 * Empty class to denote a custom file provider
 */

public class GenericFileProvider extends FileProvider {

    public static final String PROVIDER_NAME = BuildConfig.APPLICATION_ID + ".FILE_PROVIDER";

}
