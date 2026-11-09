/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters

import android.content.Intent
import android.os.Environment
import android.os.Looper
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.AppDataParcelable
import com.amaze.filemanager.adapters.glide.AppsAdapterPreloadModel
import com.amaze.filemanager.adapters.holders.AppHolder
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.services.CopyService
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.AdjustListViewForTv
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.shadows.ShadowPausedAsyncTask
import org.robolectric.shadows.ShadowPopupMenu
import java.io.File

/**
 * Test the app manager's Backup action through the real file-copy service.
 */
@Suppress("StringLiteralDuplication")
class AppsRecyclerAdapterBackupTest : AbstractMainActivityTestBase() {
    /** Own the synthetic APK inputs without touching installed packages. */
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sourceDirectory by lazy { tempFolder.newFolder("apks") }
    private val baseBytes = byteArrayOf(1, 4, 9, 16, 25)

    /**
     * Test Backup copies every split with its own name and original contents.
     */
    @Test
    fun testBackupIncludesBaseAndSplitApks() {
        val armBytes = byteArrayOf(2, 3, 5, 7)
        val localeBytes = byteArrayOf(11, 13, 17)
        val densityBytes = byteArrayOf(19, 23)
        val splits =
            listOf(
                createApk("split_config.arm64_v8a.apk", armBytes),
                createApk("split_config.en.apk", localeBytes),
                createApk("split_config.XXHDPI.APK", densityBytes),
            )

        backupAndVerify(
            splits,
            linkedMapOf(
                "Sample App_com.example.backup.apk" to baseBytes,
                "Sample App_com.example.backup_arm64_v8a.apk" to armBytes,
                "Sample App_com.example.backup_en.apk" to localeBytes,
                "Sample App_com.example.backup_xxhdpi.apk" to densityBytes,
            ),
        )
    }

    /**
     * Test an app without split metadata still backs up its base APK.
     */
    @Test
    fun testBackupWithNullSplitList() {
        backupAndVerify(
            null,
            mapOf("Sample App_com.example.backup.apk" to baseBytes),
        )
    }

    /**
     * Test an empty split list produces only the base APK backup.
     */
    @Test
    fun testBackupWithEmptySplitList() {
        backupAndVerify(
            emptyList(),
            mapOf("Sample App_com.example.backup.apk" to baseBytes),
        )
    }

    /**
     * Create synthetic APK input bytes without parsing or installing a package.
     */
    private fun createApk(
        name: String,
        bytes: ByteArray,
    ): File = File(sourceDirectory, name).apply { writeBytes(bytes) }

    /**
     * Exercise the actual menu Intent and CopyService, including completion and source preservation.
     */
    private fun backupAndVerify(
        splitFiles: List<File>?,
        expectedOutputs: Map<String, ByteArray>,
    ) {
        val baseApk = createApk("base.apk", baseBytes)
        val sources = listOf(baseApk) + splitFiles.orEmpty()
        val sourceContents = sources.associateWith { it.readBytes() }
        val application = ApplicationProvider.getApplicationContext<AppConfig>()
        val backupDirectory = File(Environment.getExternalStorageDirectory(), "app_backup")
        assertFalse("The backup destination must be isolated for this test", backupDirectory.exists())
        ShadowPausedAsyncTask.overrideExecutor(InlineExecutorService())
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        try {
            shadowOf(Looper.getMainLooper()).idle()
            scenario.moveToState(Lifecycle.State.STARTED)
            lateinit var backupIntent: Intent
            scenario.onActivity { activity ->
                val data =
                    AppDataParcelable(
                        "Sample App",
                        baseApk.absolutePath,
                        splitFiles?.map { it.absolutePath },
                        "com.example.backup",
                        "",
                        "5 B",
                        baseApk.length(),
                        baseApk.lastModified(),
                        false,
                        null,
                    )
                backupIntent = clickBackup(activity, data)
            }

            assertBackupIntent(backupIntent, backupDirectory, sources, expectedOutputs, application)
            runCopyAndVerify(backupIntent, backupDirectory, expectedOutputs, sourceContents, application)
        } finally {
            scenario.close()
            backupDirectory.deleteRecursively()
        }
    }

    /**
     * Verify Backup supplies the real service with its target, mode, source paths and output names.
     */
    private fun assertBackupIntent(
        backupIntent: Intent,
        backupDirectory: File,
        sources: List<File>,
        expectedOutputs: Map<String, ByteArray>,
        application: AppConfig,
    ) {
        assertEquals(CopyService::class.java.name, backupIntent.component?.className)
        assertEquals(
            backupDirectory.path,
            backupIntent.getStringExtra(CopyService.TAG_COPY_TARGET),
        )
        assertEquals(
            OpenMode.UNKNOWN.ordinal,
            backupIntent.getIntExtra(CopyService.TAG_COPY_OPEN_MODE, -1),
        )
        assertFalse(backupIntent.getBooleanExtra(CopyService.TAG_COPY_MOVE, false))
        val requestedSources =
            requireNotNull(
                backupIntent.getParcelableArrayListExtra<HybridFileParcelable>(
                    CopyService.TAG_COPY_SOURCES,
                ),
            )
        assertEquals(sources.map { it.absolutePath }, requestedSources.map { it.path })
        assertEquals(
            expectedOutputs.keys.toList(),
            requestedSources.map { it.getName(application) },
        )
    }

    /**
     * Run the unchanged Backup Intent and verify completion, copied contents and retained sources.
     */
    private fun runCopyAndVerify(
        backupIntent: Intent,
        backupDirectory: File,
        expectedOutputs: Map<String, ByteArray>,
        sourceContents: Map<File, ByteArray>,
        application: AppConfig,
    ) {
        val broadcastsBeforeCopy = shadowOf(application).broadcastIntents.size
        val service = Robolectric.buildService(CopyService::class.java, backupIntent).create()

        try {
            service.startCommand(0, 1)
            shadowOf(Looper.getMainLooper()).idle()
            val broadcasts = shadowOf(application).broadcastIntents.drop(broadcastsBeforeCopy)
            assertTrue(
                "CopyService must publish completion for the backup directory",
                broadcasts.any {
                    it.action == MainActivity.KEY_INTENT_LOAD_LIST &&
                        it.getStringExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE) == backupDirectory.path
                },
            )
            assertFalse(
                "Backup must not report a failed file operation",
                broadcasts.any { it.hasExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS) },
            )
            assertEquals(expectedOutputs.keys, backupDirectory.listFiles()?.map { it.name }?.toSet())
            expectedOutputs.forEach { (name, bytes) ->
                assertArrayEquals(name, bytes, File(backupDirectory, name).readBytes())
            }
            sourceContents.forEach { (file, bytes) ->
                assertTrue("Backup must retain source ${file.name}", file.isFile)
                assertArrayEquals(file.name, bytes, file.readBytes())
            }
        } finally {
            service.destroy()
        }
    }

    /**
     * Bind a real adapter row and select its Backup menu item, retaining the emitted Intent.
     */
    private fun clickBackup(
        activity: MainActivity,
        data: AppDataParcelable,
    ): Intent {
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "backup-test").commitNow()
        val callback =
            object : AdjustListViewForTv<AppHolder> {
                /** No focus scrolling is needed when selecting the Backup menu. */
                override fun adjustListViewForTv(
                    viewHolder: AppHolder,
                    mainActivity: MainActivity,
                ) = Unit
            }
        val adapter =
            AppsRecyclerAdapter(
                fragment,
                mock(AppsAdapterPreloadModel::class.java),
                false,
                callback,
                mutableListOf(data),
            )
        val position =
            (0 until adapter.itemCount).first {
                adapter.getItemViewType(it) == AppsRecyclerAdapter.TYPE_ITEM
            }
        val parent = activity.findViewById<ViewGroup>(android.R.id.content)
        val holder = adapter.onCreateViewHolder(parent, AppsRecyclerAdapter.TYPE_ITEM) as AppHolder
        parent.addView(holder.itemView)
        adapter.onBindViewHolder(holder, position)
        val shadowApplication = shadowOf(activity.application)
        while (shadowApplication.nextStartedService != null) {
            // Discard services started during activity setup before exercising this action.
        }
        assertTrue(holder.about.performClick())
        val popup = requireNotNull(ShadowPopupMenu.getLatestPopupMenu())
        assertTrue(popup.menu.performIdentifierAction(R.id.backup, 0))
        return requireNotNull(shadowApplication.nextStartedService)
    }
}
