/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.smb

import android.net.Uri
import android.text.TextUtils
import android.util.Log
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import jcifs.CIFSException
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.context.SingletonContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CifsContexts {

    const val SMB_URI_PREFIX = "smb://"

    private val TAG = CifsContexts::class.java.simpleName

    private val defaultProperties: Properties = Properties().apply {
        setProperty("jcifs.resolveOrder", "BCAST")
        setProperty("jcifs.smb.client.responseTimeout", "30000")
        setProperty("jcifs.netbios.retryTimeout", "5000")
        setProperty("jcifs.netbios.cachePolicy", "-1")
    }

    private val contexts: MutableMap<String, BaseContext> = ConcurrentHashMap()

    @JvmStatic
    fun clearBaseContexts() {
        contexts.forEach {
            try {
                it.value.close()
            } catch (e: CIFSException) {
                Log.w(TAG, "Error closing SMB connection", e)
            }
        }
        contexts.clear()
    }

    @JvmStatic
    fun createWithDisableIpcSigningCheck(
        basePath: String,
        disableIpcSigningCheck: Boolean
    ): BaseContext {
        return if (disableIpcSigningCheck) {
            val extraProperties = Properties()
            extraProperties["jcifs.smb.client.ipcSigningEnforced"] = "false"
            create(basePath, extraProperties)
        } else {
            create(basePath, null)
        }
    }

    @JvmStatic
    fun create(basePath: String, extraProperties: Properties?): BaseContext {
        val basePathKey: String = Uri.parse(basePath).run {
            val prefix = "$scheme://$authority"
            val suffix = if (TextUtils.isEmpty(query)) "" else "?$query"
            "$prefix$suffix"
        }
        return if (contexts.containsKey(basePathKey)) {
            contexts.getValue(basePathKey)
        } else {
            val context = Single.fromCallable {
                try {
                    val p = Properties(defaultProperties)
                    if (extraProperties != null) p.putAll(extraProperties)
                    BaseContext(PropertyConfiguration(p))
                } catch (e: CIFSException) {
                    Log.e(TAG, "Error initialize jcifs BaseContext, returning default", e)
                    SingletonContext.getInstance()
                }
            }.subscribeOn(Schedulers.io())
                .blockingGet()
            contexts[basePathKey] = context
            context
        }
    }
}
