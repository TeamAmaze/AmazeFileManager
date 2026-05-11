package com.amaze.filemanager.asynchronous.asynctasks.cloud

import android.database.Cursor
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.omh.OMHClientHelper
import com.amaze.filemanager.utils.omh.OmhCredentialsWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.util.concurrent.Callable

class CloudLoaderCallable(
    mainActivity: MainActivity,
    private val data: Cursor?,
) : Callable<Boolean> {
    private val mainActivity: WeakReference<MainActivity> = WeakReference(mainActivity)

    companion object {
        @JvmStatic
        private val LOG: Logger = LoggerFactory.getLogger(CloudLoaderCallable::class.java)
    }

    override fun call(): Boolean {
        var hasUpdatedDrawer = false
        if (data == null) {
            return false
        } else if (data.count > 0 && data.moveToFirst()) {
            do {
                val v = data.getInt(0)
                when (v) {
                    1 -> Unit
                    2, 3, 4, 5 -> {
                        val openMode =
                            when (v) {
                                2 -> OpenMode.GDRIVE
                                3 -> OpenMode.DROPBOX
                                4 -> OpenMode.BOX
                                else -> OpenMode.ONEDRIVE
                            }
                        val authClient =
                            OMHClientHelper.getAuthClient(
                                openMode,
                                data,
                            )
                        val credentials = authClient.getCredentials()
                        if (credentials.accessToken != null) {
                            DataUtils.addAccount(
                                OmhCredentialsWrapper(
                                    openMode,
                                    credentials,
                                ),
                            )
                            hasUpdatedDrawer = true
                        } else {
                            mainActivity.get()?.deleteCloudConnection(openMode)
                        }
                    }
                    else -> {
                        AppConfig.toast(mainActivity.get(), R.string.cloud_error_failed_restart)
                        return false
                    }
                }
            } while (data.moveToNext())
        }
        return hasUpdatedDrawer
    }
}
