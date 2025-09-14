package com.amaze.filemanager.utils.omh

import android.content.Context
import android.database.Cursor
import android.os.CancellationSignal
import androidx.core.content.ContentResolverCompat
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.CloudContract
import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.utils.cloud.CloudPluginUtil
import com.openmobilehub.android.auth.core.OmhAuthClient
import com.openmobilehub.android.auth.plugin.dropbox.mobileweb.presentation.DropboxMobileWebAuthClient
import com.openmobilehub.android.auth.plugin.google.nongms.presentation.OmhAuthFactoryImpl
import com.openmobilehub.android.auth.plugin.microsoft.mobileweb.presentation.MicrosoftMobileWebAuthClient
import com.openmobilehub.android.storage.core.OmhStorageClient
import com.openmobilehub.android.storage.core.OmhStorageProvider
import com.openmobilehub.android.storage.plugin.dropbox.restful.DropboxRestfulOmhStorageClientFactory
import com.openmobilehub.android.storage.plugin.googledrive.nongms.GoogleDriveNonGmsConstants
import com.openmobilehub.android.storage.plugin.onedrive.restful.OneDriveRestfulOmhStorageClientFactory
import java.util.EnumMap

/**
 * Helper object to manage Open Mobile Hub auth and storage clients for different cloud providers.
 *
 */
object OMHClientHelper {
    const val MULTI_SLASH_FOR_CLOUD = "(?<=[^:])(///+)"

    private val authClients: MutableMap<OpenMode, OmhAuthClient> =
        EnumMap(OpenMode::class.java)

    private val storageClients: MutableMap<OpenMode, OmhStorageClient> =
        EnumMap(OpenMode::class.java)

    /**
     * Initializes all available cloud clients at once. This should be called when the app starts.
     */
    @JvmStatic
    fun initializeClients() {
        AppConfig.getInstance().let { context: Context ->
            val cursor =
                ContentResolverCompat.query(
                    context.contentResolver,
                    CloudContract.URI,
                    CloudContract.PROJECTION,
                    CloudContract.COLUMN_ID,
                    CloudContract.ENABLED_PROVIDER_IDS,
                    null,
                    null as CancellationSignal?,
                )
            if (cursor == null || !cursor.moveToFirst()) {
                throw CloudPluginException()
            } else {
                do {
                    when (cursor.getInt(0)) {
                        1 -> getAuthClient(OpenMode.GDRIVE, cursor.getString(1))
                        2 -> getAuthClient(OpenMode.DROPBOX, cursor.getString(1))
                        3 -> getAuthClient(OpenMode.BOX, cursor.getString(1))
                        4 -> getAuthClient(OpenMode.ONEDRIVE, cursor.getString(1))
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
        }
    }

    /**
     * Retrieves the auth client for the specified [openMode].
     */
    @JvmStatic
    fun getAuthClient(openMode: OpenMode): OmhAuthClient {
        return if (authClients.containsKey(openMode)) {
            authClients[openMode]!!
        } else {
            val cursor = getCloudPluginCredentialsOf(openMode)
            if (cursor == null || !cursor.moveToFirst()) {
                throw CloudPluginException()
            } else {
                getAuthClient(openMode, cursor.getString(1))
            }
        }
    }

    /**
     * Retrieves the auth client for the specified [openMode] using the provided [apiKey].
     */
    @JvmStatic
    fun getAuthClient(
        openMode: OpenMode,
        apiKey: String,
    ): OmhAuthClient {
        val context = AppConfig.getInstance()
        if (authClients.containsKey(openMode)) {
            return authClients[openMode]!!
        } else {
            synchronized(authClients) {
                val authClient =
                    when (openMode) {
                        OpenMode.GDRIVE -> {
                            getGoogleAuthClient(context, apiKey)
                        }
                        OpenMode.DROPBOX -> {
                            getDropboxAuthClient(context, apiKey)
                        }
                        OpenMode.ONEDRIVE -> {
                            getOnedriveAuthClient(context, apiKey)
                        }
                        else -> throw IllegalArgumentException("Unsupported OpenMode $openMode")
                    }
                authClients.put(openMode, authClient)
            }
            return authClients[openMode]!!
        }
    }

    /**
     * Retrieves the storage client for the specified [openMode].
     */
    @JvmStatic
    fun getStorageClient(openMode: OpenMode): OmhStorageClient? {
        if (storageClients.containsKey(openMode)) {
            return storageClients[openMode]!!
        } else {
            synchronized(storageClients) {
                val context = AppConfig.getInstance()
                val credentials = getCloudPluginCredentialsOf(openMode)
                if (credentials != null && credentials.moveToFirst()) {
                    val storageClient =
                        when (openMode) {
                            OpenMode.GDRIVE -> {
                                getGoogleStorageClient(context, credentials)
                            }
                            OpenMode.DROPBOX -> {
                                getDropboxStorageClient(context, credentials)
                            }
                            OpenMode.ONEDRIVE -> {
                                getOnedriveStorageClient(context, credentials)
                            }
                            else -> throw IllegalArgumentException("Unsupported OpenMode $openMode")
                        }
                    credentials.close()
                    storageClients.put(openMode, storageClient)
                } else {
                    throw CloudPluginException("Unable to obtain API secrets from Cloud plugin for $openMode")
                }
            }
            return storageClients[openMode]
        }
    }

    /**
     * Utility method to fetch cloud plugin credentials from the cloud plugin.
     */
    fun getCloudPluginCredentialsOf(openMode: OpenMode): Cursor? {
        val context = AppConfig.getInstance()
        return ContentResolverCompat.query(
            context.contentResolver,
            CloudContract.URI,
            CloudContract.PROJECTION,
            CloudContract.COLUMN_ID,
            arrayOf(CloudPluginUtil.resolvePluginIdFrom(openMode).toString()),
            null,
            null as CancellationSignal?,
        )
    }

    private fun getDropboxAuthClient(
        context: Context,
        apiKey: String,
    ): OmhAuthClient {
        return DropboxMobileWebAuthClient.Builder(apiKey).also { builder ->
            arrayOf(
                "account_info.read",
                "files.metadata.read",
                "files.content.write",
                "files.content.read",
                "sharing.write",
                "sharing.read",
            ).forEach { scope ->
                builder.addScope(scope)
            }
        }.build(context)
    }

    private fun getDropboxStorageClient(
        context: Context,
        cursor: Cursor,
    ): OmhStorageClient {
        val apiKey = cursor.getString(1)
        return DropboxRestfulOmhStorageClientFactory()
            .getStorageClient(getDropboxAuthClient(context, apiKey))
    }

    private fun getOnedriveAuthClient(
        context: Context,
        apiKey: String,
    ): OmhAuthClient {
        return MicrosoftMobileWebAuthClient.Builder(apiKey).also { builder ->
            arrayListOf("User.Read", "openid", "profile", "email").forEach { scope ->
                builder.addScope(scope)
            }
        }.build(context)
    }

    private fun getOnedriveStorageClient(
        context: Context,
        cursor: Cursor,
    ): OmhStorageClient {
        val apiKey = cursor.getString(1)
        return OneDriveRestfulOmhStorageClientFactory()
            .getStorageClient(getOnedriveAuthClient(context, apiKey))
    }

    private fun getGoogleAuthClient(
        context: Context,
        apiKey: String,
    ): OmhAuthClient {
        val authClientInstance =
            OmhAuthFactoryImpl.getAuthClient(
                context,
                listOf(
                    "openid",
                    "email",
                    "profile",
                    "https://www.googleapis.com/auth/drive",
                    "https://www.googleapis.com/auth/drive.file",
                ),
                apiKey,
                null,
            )
        authClientInstance.initialize()
        return authClientInstance
    }

    private fun getGoogleStorageClient(
        context: Context,
        cursor: Cursor,
    ): OmhStorageClient {
        val authClient = getAuthClient(OpenMode.GDRIVE, cursor.getString(1))
        val storageClientInstance =
            OmhStorageProvider.Builder()
                .addNonGmsPath(GoogleDriveNonGmsConstants.IMPLEMENTATION_PATH)
                .build()
                .provideStorageClient(authClient, context)
        return storageClientInstance
    }
}
