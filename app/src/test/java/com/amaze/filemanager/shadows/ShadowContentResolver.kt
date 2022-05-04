package com.amaze.filemanager.shadows

import android.content.ContentResolver
import android.net.Uri
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowContentResolver
import java.io.OutputStream

/**
 * [ShadowContentResolver] implementation to include
 * [ContentResolver.openOutputStream] with Uri and open mode arguments.
 *
 * @see ContentResolver.openOutputStream(uri, String)
 */
@Implements(ContentResolver::class)
class ShadowContentResolver : ShadowContentResolver() {

    /**
     * Implements [ContentResolver.openOutputStream] with open mode parameter.
     *
     * Simply delegate to one without open mode.
     */
    @Implementation
    fun openOutputStream(uri: Uri, mode: String): OutputStream? = super.openOutputStream(uri)
}
