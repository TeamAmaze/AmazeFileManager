package com.amaze.filemanager.utils.omh

import android.content.Context
import com.amaze.filemanager.BuildConfig
import com.openmobilehub.android.auth.core.OmhAuthClient
import com.openmobilehub.android.auth.core.OmhAuthProvider

class OMHClientHelper {

    companion object {

        @Volatile
        var instance: OmhAuthClient? = null

        @JvmStatic
        public fun getGoogleAuthClient(context: Context): OmhAuthClient {
            if (instance == null) {
                synchronized(this) {
                    val omhAuthProvider = OmhAuthProvider.Builder()
                        .addNonGmsPath(
                            "com.openmobilehub.android.auth.plugin.google.nongms.presentation.OmhAuthFactoryImpl"
                        )
                        .addGmsPath(
                            "com.openmobilehub.android.auth.plugin.google.gms.OmhAuthFactoryImpl"
                        )
                        .build()

                    instance = omhAuthProvider
                        .provideAuthClient(
                            scopes = listOf("openid", "email", "profile"),
                            clientId = BuildConfig.GOOGLE_CLIENT_ID,
                            context = context
                        )

                    instance!!.initialize()
                }
            }

            return instance!!
        }
    }
}
