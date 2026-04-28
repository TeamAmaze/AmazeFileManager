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
import com.openmobilehub.android.auth.plugin.box.mobileweb.presentation.BoxMobileWebAuthClient
import com.openmobilehub.android.auth.plugin.dropbox.mobileweb.presentation.DropboxMobileWebAuthClient
import com.openmobilehub.android.auth.plugin.google.nongms.presentation.OmhAuthFactoryImpl
import com.openmobilehub.android.auth.plugin.microsoft.mobileweb.presentation.MicrosoftMobileWebAuthClient
import com.openmobilehub.android.storage.core.OmhStorageClient
import com.openmobilehub.android.storage.core.OmhStorageProvider
import com.openmobilehub.android.storage.plugin.box.restful.BoxRestfulOmhStorageClientFactory
import com.openmobilehub.android.storage.plugin.dropbox.restful.DropboxRestfulOmhStorageClientFactory
import com.openmobilehub.android.storage.plugin.googledrive.nongms.GoogleDriveNonGmsConstants
import com.openmobilehub.android.storage.plugin.onedrive.restful.OneDriveRestfulOmhStorageClientFactory
import java.io.File
import java.util.EnumMap
import kotlinx.coroutines.runBlocking
import com.openmobilehub.android.storage.core.model.OmhStorageEntity

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
                        1 -> getAuthClient(OpenMode.GDRIVE, cursor)
                        2 -> getAuthClient(OpenMode.DROPBOX, cursor)
                        3 -> getAuthClient(OpenMode.BOX, cursor)
                        4 -> getAuthClient(OpenMode.ONEDRIVE, cursor)
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
                getAuthClient(openMode, cursor)
            }
        }
    }

    /**
     * Retrieves the auth client for the specified [openMode] using the provided [apiKey].
     */
    @JvmStatic
    fun getAuthClient(
        openMode: OpenMode,
        cursor: Cursor,
    ): OmhAuthClient {
        val context = AppConfig.getInstance()
        if (authClients.containsKey(openMode)) {
            return authClients[openMode]!!
        } else {
            synchronized(authClients) {
                val apiKey = cursor.getString(1)
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
                        OpenMode.BOX -> {
                            getBoxAuthClient(context, apiKey, cursor.getString(2))
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
                            OpenMode.BOX -> {
                                getBoxStorageClient(credentials)
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
     * Invalidates cached auth and storage clients for the given [openMode].
     * Should be called after disconnecting a cloud account or after re-authentication
     * so that new client instances are created with fresh credentials on the next access.
     */
    @JvmStatic
    fun invalidateClient(openMode: OpenMode) {
        synchronized(storageClients) {
            storageClients.remove(openMode)
        }
        synchronized(authClients) {
            authClients.remove(openMode)
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
            arrayListOf(
                "User.Read",
                "openid",
                "profile",
                "email",
                "Files.ReadWrite.All",
                "offline_access",
            ).forEach { scope ->
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
        val authClient = getAuthClient(OpenMode.GDRIVE, cursor)
        val storageClientInstance =
            OmhStorageProvider.Builder()
                .addNonGmsPath(GoogleDriveNonGmsConstants.IMPLEMENTATION_PATH)
                .build()
                .provideStorageClient(authClient, context)
        return storageClientInstance
    }

    private fun getBoxAuthClient(
        context: Context,
        clientId: String,
        clientSecret: String,
    ): OmhAuthClient {
        return BoxMobileWebAuthClient.Builder(clientId, clientSecret).also { builder ->
            arrayListOf("root_readonly", "root_readwrite").forEach { scope ->
                builder.addScope(scope)
            }
        }.build(context)
    }

    private fun getBoxStorageClient(cursor: Cursor): OmhStorageClient {
        val authClient = getAuthClient(OpenMode.BOX, cursor)
        return BoxRestfulOmhStorageClientFactory().getStorageClient(authClient)
    }

    // -------------------------------------------------------------------------
    // Blocking Kotlin wrappers — safe to call from Java (e.g. AsyncTask).
    // These keep all coroutine suspension inside Kotlin where the compiler
    // handles COROUTINE_SUSPENDED correctly, avoiding the ClassCastException
    // that occurs when Java lambdas pass the outer Continuation directly into
    // a suspending function.
    // -------------------------------------------------------------------------

    /**
     * Blocking wrapper around [OmhStorageClient.deleteFile].
     * Safe to call from a background Java thread (e.g. [android.os.AsyncTask]).
     */
    @JvmStatic
    fun deleteCloudFile(openMode: OpenMode, fileId: String) {
        val storageClient = getStorageClient(openMode) ?: return
        runBlocking {
            storageClient.deleteFile(fileId)
        }
    }

    /**
     * Blocking wrapper that resolves [remotePath] to an [OmhStorageEntity] id,
     * uploads [localFile] into that folder, then deletes the temp file.
     * Safe to call from a background Java thread.
     */
    @JvmStatic
    fun uploadCloudFile(openMode: OpenMode, localFile: File, remoteFolderPath: String) {
        val storageClient = getStorageClient(openMode) ?: return
        runBlocking {
            val parentFolder: OmhStorageEntity =
                storageClient.resolvePath(remoteFolderPath) ?: return@runBlocking
            storageClient.uploadFile(localFile, parentFolder.id)
            localFile.delete()
        }
    }
}
