package com.amaze.filemanager.utils.omh

import android.content.Context
import android.content.Intent
import com.amaze.filemanager.BuildConfig
import com.openmobilehub.android.auth.core.OmhAuthClient
import com.openmobilehub.android.auth.core.OmhAuthProvider
import com.openmobilehub.android.auth.core.async.OmhCancellable
import com.openmobilehub.android.auth.core.async.OmhTask
import com.openmobilehub.android.auth.core.models.OmhUserProfile
import com.openmobilehub.android.storage.core.OmhStorageClient
import com.openmobilehub.android.storage.core.OmhStorageProvider

class OMHClientHelper {
    companion object {
        @Volatile
        var authClientInstance: OmhAuthClient? = null

        @Volatile
        var storageClientInstance: OmhStorageClient? = null

        @JvmStatic
        public fun getGoogleAuthClient(context: Context): OmhAuthClient {
            if (authClientInstance == null) {
                synchronized(this) {
                    val omhAuthProvider =
                        OmhAuthProvider.Builder()
                            .addNonGmsPath(
                                "com.openmobilehub.android.auth.plugin.google.nongms.presentation.OmhAuthFactoryImpl",
                            )
                            .addGmsPath(
                                "com.openmobilehub.android.auth.plugin.google.gms.OmhAuthFactoryImpl",
                            )
                            .build()

                    authClientInstance =
                        omhAuthProvider
                            .provideAuthClient(
                                scopes =
                                    listOf(
                                        "openid",
                                        "email",
                                        "profile",
                                        "https://www.googleapis.com/auth/drive",
                                        "https://www.googleapis.com/auth/drive.file",
                                    ),
                                clientId = BuildConfig.GOOGLE_CLIENT_ID,
                                context = context,
                            )

                    authClientInstance!!.initialize()
                }
            }

            return authClientInstance!!
        }

        @JvmStatic
        public fun getGoogleStorageClient(context: Context): OmhStorageClient {
            if (storageClientInstance == null) {
                synchronized(this) {
                    val authClient = getGoogleAuthClient(context)

                    storageClientInstance =
                        OmhStorageProvider.Builder()
                            .addGmsPath("com.omh.android.storage.api.drive.gms.OmhGmsStorageFactoryImpl")
                            .addNonGmsPath("com.omh.android.storage.api.drive.nongms.OmhGmsStorageFactoryImpl")
                            .build()
                            .provideStorageClient(authClient, context)
                }
            }

            return storageClientInstance!!
        }
    }
}