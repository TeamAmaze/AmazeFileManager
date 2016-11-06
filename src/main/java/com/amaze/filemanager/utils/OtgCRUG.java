package com.amaze.filemanager.utils;

import android.net.Uri;

/**
 * Created by vishal on 7/11/16.
 */

public class OtgCRUG {

    private Uri otgUri = null;

    /**
     * Default constructor
     * @param uri the uri pointing to content provider for OTG
     */
    public OtgCRUG(Uri uri) {
        this.otgUri = uri;
    }
}
