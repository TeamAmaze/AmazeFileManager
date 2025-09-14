package com.amaze.filemanager.utils.omh

import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.openmobilehub.android.auth.core.OmhCredentials

data class OmhCredentialsWrapper(
    val openMode: OpenMode,
    val credentials: OmhCredentials,
)
