package com.amaze.filemanager.utils.omh

import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.openmobilehub.android.storage.core.model.OmhStorageEntity

data class OmhStorageEntityWrapper(
    val openMode: OpenMode,
    val omhStorageEntity: OmhStorageEntity,
)
