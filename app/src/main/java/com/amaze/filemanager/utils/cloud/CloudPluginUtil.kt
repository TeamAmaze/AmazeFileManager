package com.amaze.filemanager.utils.cloud

import android.content.Context
import android.content.pm.PackageManager
import com.amaze.filemanager.database.CloudContract.APP_PACKAGE_NAME
import com.amaze.filemanager.database.CloudContract.ENABLED_PROVIDERS
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.omh.OMHClientHelper
import com.amaze.filemanager.utils.omh.OmhCredentialsWrapper
import com.openmobilehub.android.auth.core.utils.EncryptedSharedPreferences

/**
 * Utility object for cloud plugin related operations.
 */
object CloudPluginUtil {
    /**
     * Initializes DataUtils with accounts from enabled cloud providers, if exist.
     */
    @JvmStatic
    fun initializeDataUtils(context: Context) {
        ENABLED_PROVIDERS.forEach {
            val prefs = EncryptedSharedPreferences.getEncryptedSharedPrefs(context, resolveOmhProviderNameFrom(it))
            if (prefs.getString("email", null) != null) {
                DataUtils.addAccount(
                    OmhCredentialsWrapper(
                        openMode = it,
                        credentials = OMHClientHelper.getAuthClient(it).getCredentials(),
                    ),
                )
            }
        }
    }

    /**
     * Resolves the plugin ID from the given [OpenMode].
     *
     */
    @JvmStatic
    fun resolvePluginIdFrom(openMode: OpenMode): Int {
        return when (openMode) {
            OpenMode.GDRIVE -> 1
            OpenMode.BOX -> 3
            OpenMode.DROPBOX -> 2
            OpenMode.ONEDRIVE -> 4
            else -> throw IllegalArgumentException("Invalid open mode: $openMode")
        }
    }

    /**
     * Resolves the Open Mobile Hub provider name from the given [OpenMode].
     */
    @JvmStatic
    fun resolveOmhProviderNameFrom(openMode: OpenMode): String {
        return when (openMode) {
            OpenMode.GDRIVE -> "google"
            OpenMode.BOX -> "box"
            OpenMode.DROPBOX -> "dropbox"
            OpenMode.ONEDRIVE -> "microsoft"
            else -> throw IllegalArgumentException("Invalid open mode: $openMode")
        }
    }

    /** Determines whether cloud provider is installed or not  */
    @JvmStatic
    fun isCloudProviderAvailable(context: Context): Boolean {
        val pm = context.packageManager
        try {
            pm.getPackageInfo(APP_PACKAGE_NAME, PackageManager.GET_ACTIVITIES)
            return true
        } catch (_: PackageManager.NameNotFoundException) {
            return false
        }
    }
}
