package com.amaze.filemanager.filesystem

import androidx.annotation.IntDef

const val DOESNT_EXIST = 0
const val WRITABLE_OR_ON_SDCARD = 1

// For Android 5
const val CAN_CREATE_FILES = 2

@IntDef(DOESNT_EXIST, WRITABLE_OR_ON_SDCARD, CAN_CREATE_FILES)
annotation class FolderState