package com.amaze.filemanager.utils.omh

import android.content.Context
import android.content.Intent
import com.amaze.filemanager.BuildConfig
import com.omh.android.auth.api.async.OmhCancellable
import com.omh.android.auth.api.async.OmhTask
import com.omh.android.auth.api.models.OmhUserProfile
import com.omh.android.storage.api.OmhStorageClient
import com.omh.android.storage.api.OmhStorageProvider
import com.openmobilehub.android.auth.core.OmhAuthClient
import com.openmobilehub.android.auth.core.OmhAuthProvider

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

                    val authClientMigrator: AuthClientMigrator = AuthClientMigrator(authClient)

                    storageClientInstance =
                        OmhStorageProvider.Builder()
                            .addGmsPath("com.omh.android.storage.api.drive.gms.OmhGmsStorageFactoryImpl")
                            .addNonGmsPath("com.omh.android.storage.api.drive.nongms.OmhGmsStorageFactoryImpl")
                            .build()
                            .provideStorageClient(authClientMigrator, context)
                }
            }

            return storageClientInstance!!
        }
    }
}

class AuthClientMigrator(val omhAuthClient: OmhAuthClient) : com.omh.android.auth.api.OmhAuthClient {
    private lateinit var user: com.openmobilehub.android.auth.core.models.OmhUserProfile

    init {
        omhAuthClient.getUser().addOnSuccess {
            this.user = it
        }
    }

    override fun getAccountFromIntent(data: Intent?): OmhUserProfile {
        return OmhUserProfile(user.name, user.surname, user.email, user.profileImage)
    }

    override fun getCredentials(): Any {
        return omhAuthClient.getCredentials()
    }

    override fun getLoginIntent(): Intent {
        return omhAuthClient.getLoginIntent()
    }

    override fun getUser(): OmhUserProfile {
        return OmhUserProfile(user.name, user.surname, user.email, user.profileImage)
    }

    override fun revokeToken(): OmhTask<Unit> {
        return OmhTaskMigrator(omhAuthClient.revokeToken())
    }

    override fun signOut(): OmhTask<Unit> {
        return OmhTaskMigrator(omhAuthClient.signOut())
    }

    class OmhTaskMigrator<Unit>(private val omhTask: com.openmobilehub.android.auth.core.async.IOmhTask<kotlin.Unit>) :
        OmhTask<Unit>() {
        override fun execute(): OmhCancellableMigrator {
            return OmhCancellableMigrator(omhTask.execute())
        }

        class OmhCancellableMigrator(private val omhCancellable: com.openmobilehub.android.auth.core.async.OmhCancellable) :
            OmhCancellable {
            override fun cancel() {
                omhCancellable.cancel()
            }
        }
    }
}
