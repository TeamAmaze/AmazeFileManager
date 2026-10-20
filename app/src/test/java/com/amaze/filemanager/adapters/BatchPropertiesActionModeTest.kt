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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.adapters

import android.os.Looper
import android.text.format.Formatter
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.android.util.concurrent.PausedExecutorService
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowPausedAsyncTask
import java.io.File

/**
 * Test selected local-file Properties through the real action-mode menu and asynchronous dialog.
 */
class BatchPropertiesActionModeTest : AbstractMainActivityTestBase() {
    /** Own every ordinary synthetic file used by the selection workflow. */
    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * The actual selection menu must expose Properties and include every nested selected folder.
     */
    @Test
    fun testLocalBatchMenuShowsRecursiveTotal() {
        val first = tempFolder.newFolder("first")
        writeFile(first, "one.bin", byteArrayOf(1, 2, 3))
        writeFile(first, "nested/two.bin", byteArrayOf(4, 5, 6, 7, 8))
        val second = tempFolder.newFolder("second")
        writeFile(second, "three.bin", byteArrayOf(9, 10, 11, 12, 13, 14, 15))

        withSelection(listOf(first, second)) { activity, mode ->
            val properties = mode.menu.findItem(R.id.about)
            assertNotNull("The selection menu must contain Properties", properties)
            assertTrue(properties.isVisible)
            assertTrue(mode.menu.performIdentifierAction(R.id.about, 0))
            shadowOf(Looper.getMainLooper()).idle()
            assertBatchContent(activity, 2, 15)
        }
    }

    /**
     * The public action handler must use a snapshot after finishing and clearing the selection.
     */
    @Test
    fun testMixedSelectionSurvivesActionModeFinish() {
        val directory = tempFolder.newFolder("folder")
        writeFile(directory, "nested/folder.bin", byteArrayOf(1, 2, 3, 4))
        val plainFile = writeFile(tempFolder.root, "plain.bin", byteArrayOf(5, 6, 7))

        withSelection(listOf(directory, plainFile)) { activity, mode ->
            val executor = PausedExecutorService()
            ShadowPausedAsyncTask.overrideExecutor(executor)
            assertTrue(dispatchProperties(activity, mode))
            assertTrue(
                requireNotNull(activity.currentMainFragment!!.mainFragmentViewModel).getCheckedItems().isEmpty(),
            )
            assertTrue(executor.hasQueuedTasks())
            executor.runAll()
            shadowOf(Looper.getMainLooper()).idle()
            assertBatchContent(activity, 2, 7)
        }
    }

    /**
     * Empty folders and a zero-byte file still show an explicit zero total for all selected items.
     */
    @Test
    fun testZeroSizedSelectionShowsTotal() {
        val first = tempFolder.newFolder("empty-one")
        val second = tempFolder.newFolder("empty-two")
        val emptyFile = writeFile(tempFolder.root, "empty.bin", byteArrayOf())

        withSelection(listOf(first, second, emptyFile)) { activity, mode ->
            assertTrue(dispatchProperties(activity, mode))
            shadowOf(Looper.getMainLooper()).idle()
            assertBatchContent(activity, 3, 0)
        }
    }

    /**
     * A single selected file retains the established custom Properties dialog and its filename.
     */
    @Test
    fun testSingleSelectionKeepsExistingProperties() {
        val file = writeFile(tempFolder.root, "single.bin", byteArrayOf(1, 3, 5))
        withSelection(listOf(file)) { activity, mode ->
            assertTrue(dispatchProperties(activity, mode))
            val dialog = ShadowDialog.getLatestDialog() as MaterialDialog
            val custom = requireNotNull(dialog.customView)
            assertEquals(file.name, custom.findViewById<AppCompatTextView>(R.id.t5).text.toString())
        }
    }

    /**
     * A stale empty selection hides Properties and is ignored without indexing an absent item.
     */
    @Test
    fun testEmptySelectionIsIgnored() {
        withSelection(emptyList()) { activity, mode ->
            val properties = mode.menu.findItem(R.id.about)
            assertNotNull(properties)
            assertFalse(properties.isVisible)
            val previous = ShadowDialog.getLatestDialog()
            assertFalse(dispatchProperties(activity, mode))
            assertSame(previous, ShadowDialog.getLatestDialog())
        }
    }

    /**
     * Unsupported batch modes are hidden and guarded without trying to measure remote contents.
     */
    @Test
    fun testUnsupportedBatchModesAreHiddenAndIgnored() {
        val first = writeFile(tempFolder.root, "first.bin", byteArrayOf(1))
        val second = writeFile(tempFolder.root, "second.bin", byteArrayOf(2))
        withSelection(listOf(first, second)) { activity, mode ->
            for (openMode in listOf(OpenMode.FTP, OpenMode.DOCUMENT_FILE, OpenMode.SMB)) {
                requireNotNull(activity.currentMainFragment!!.mainFragmentViewModel).openMode = openMode
                mode.invalidate()
                val properties = mode.menu.findItem(R.id.about)
                assertNotNull(properties)
                assertFalse(properties.isVisible)
                val previous = ShadowDialog.getLatestDialog()
                assertFalse(dispatchProperties(activity, mode))
                assertSame(previous, ShadowDialog.getLatestDialog())
            }
        }
    }

    /**
     * The batch restriction must leave the established single-item action visible in other modes.
     */
    @Test
    fun testSingleRemoteSelectionKeepsPropertiesMenu() {
        val file = writeFile(tempFolder.root, "single.bin", byteArrayOf(1))
        withSelection(listOf(file)) { activity, mode ->
            for (openMode in listOf(OpenMode.FTP, OpenMode.DOCUMENT_FILE, OpenMode.SMB)) {
                requireNotNull(activity.currentMainFragment!!.mainFragmentViewModel).openMode = openMode
                mode.invalidate()
                val properties = mode.menu.findItem(R.id.about)
                assertNotNull(properties)
                assertTrue(properties.isVisible)
            }
        }
    }

    /**
     * Dismissing while calculation is queued must discard its result without changing dialog content.
     */
    @Test
    fun testDismissedBatchDoesNotPublishResult() {
        val first = writeFile(tempFolder.root, "first.bin", byteArrayOf(1, 2))
        val second = writeFile(tempFolder.root, "second.bin", byteArrayOf(3, 4))
        withSelection(listOf(first, second)) { activity, mode ->
            val executor = PausedExecutorService()
            ShadowPausedAsyncTask.overrideExecutor(executor)
            assertTrue(dispatchProperties(activity, mode))
            val dialog = ShadowDialog.getLatestDialog() as MaterialDialog
            assertEquals(activity.getString(R.string.calculating), dialog.contentView!!.text.toString())
            dialog.dismiss()
            executor.runAll()
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(dialog.isShowing)
            assertEquals(activity.getString(R.string.calculating), dialog.contentView!!.text.toString())
        }
    }

    /**
     * Create ordinary file bytes, including any nested directories, without external assets.
     */
    private fun writeFile(
        parent: File,
        relativePath: String,
        bytes: ByteArray,
    ): File =
        File(parent, relativePath).apply {
            parentFile!!.mkdirs()
            writeBytes(bytes)
        }

    /**
     * Seed real selection state and open the owner's action mode, preserving every input byte.
     */
    private fun withSelection(
        files: List<File>,
        test: (MainActivity, ActionMode) -> Unit,
    ) {
        val original = fileContents()
        ShadowPausedAsyncTask.overrideExecutor(InlineExecutorService())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            shadowOf(Looper.getMainLooper()).idle()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { activity ->
                selectFiles(activity, files)
                val helper = activity.actionModeHelper
                val mode = requireNotNull(activity.startSupportActionMode(helper))
                helper.actionMode = mode
                try {
                    mode.invalidate()
                    test(activity, mode)
                } finally {
                    mode.finish()
                }
            }
        } finally {
            ShadowDialog.getLatestDialog()?.dismiss()
            scenario.close()
            val remaining = fileContents()
            assertEquals(original.keys, remaining.keys)
            original.forEach { (name, bytes) ->
                assertArrayEquals(name, bytes, remaining[name])
            }
        }
    }

    /**
     * Put selected synthetic entries into the real fragment's ordinary adapter selection model.
     */
    private fun selectFiles(
        activity: MainActivity,
        files: List<File>,
    ) {
        val fragment = requireNotNull(activity.currentMainFragment)
        val model = requireNotNull(fragment.mainFragmentViewModel)
        model.openMode = OpenMode.FILE
        model.adapterListItems = ArrayList(files.map { selectedItem(activity, it) })
        model.folderCount = files.count { it.isDirectory }
        model.fileCount = files.count { it.isFile }
        activity.listItemSelected = files.isNotEmpty()
    }

    /**
     * Use the owner's existing entry type and checked flag for an ordinary local file or folder.
     */
    private fun selectedItem(
        activity: MainActivity,
        file: File,
    ): RecyclerAdapter.ListItem =
        RecyclerAdapter.ListItem(
            LayoutElementParcelable(
                activity,
                file.absolutePath,
                "",
                "",
                Formatter.formatFileSize(activity, file.length()),
                file.length(),
                false,
                "",
                file.isDirectory,
                false,
                OpenMode.FILE,
            ),
        ).apply { setChecked(true) }

    /**
     * Capture contents so that viewing Properties cannot alter or add synthetic files.
     */
    private fun fileContents(): Map<String, ByteArray> =
        tempFolder.root.walkTopDown().filter { it.isFile }.associate { it.path to it.readBytes() }

    /**
     * Invoke the existing public callback with the owner's existing Properties menu item.
     */
    private fun dispatchProperties(
        activity: MainActivity,
        mode: ActionMode,
    ): Boolean {
        val popup = PopupMenu(activity, activity.findViewById(android.R.id.content))
        popup.menuInflater.inflate(R.menu.item_extras, popup.menu)
        return activity.actionModeHelper.onActionItemClicked(mode, popup.menu.findItem(R.id.about))
    }

    /**
     * Check the displayed result against known synthetic bytes, not a copied size algorithm.
     */
    private fun assertBatchContent(
        activity: MainActivity,
        count: Int,
        expectedBytes: Long,
    ) {
        val dialog = ShadowDialog.getLatestDialog() as MaterialDialog
        assertEquals(
            count.toString() + " " + activity.resources.getQuantityString(R.plurals.items, count) +
                "\n" + activity.getString(R.string.total) + " " +
                Formatter.formatFileSize(activity, expectedBytes),
            dialog.contentView?.text?.toString(),
        )
    }
}
