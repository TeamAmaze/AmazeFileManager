package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud

import com.cloudrail.si.interfaces.CloudStorage

abstract class Account {
    companion object {
        val accounts = mutableListOf<Account>()
    }

    var account: CloudStorage? = null
        protected set

    fun add(storage: CloudStorage) {
        account = storage
        accounts.add(this)
    }

    fun removeAccount() {
        account = null
        accounts.remove(this)
    }
}