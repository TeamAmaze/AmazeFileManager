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

package com.amaze.filemanager.asynchronous.asynctasks

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Looper
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.CryptHandler
import com.amaze.filemanager.database.models.explorer.EncryptedEntry
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils
import com.amaze.filemanager.test.TestUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.MainActivityViewModel
import com.amaze.filemanager.utils.CryptUtilTest.Companion.initMockSecretKeygen
import com.amaze.filemanager.utils.ProgressHandler
import com.amaze.filemanager.utils.smb.SmbUtil
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import jcifs.smb.SmbException
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.spy
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import java.io.File

/** Regressions for encrypted-file metadata across destructive and recoverable deletion. */
class DeleteTaskEncryptedMetadataTest : AbstractDeleteTaskTestBase() {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
    private lateinit var directory: File
    private lateinit var original: File
    private lateinit var encrypted: File
    private val contents = "Synthetic content for the recoverable file lifecycle.".toByteArray()
    private val replacementContents = "Different synthetic replacement content.".toByteArray()

    /** Create a real legacy-encrypted file and its password row using existing test key support. */
    @Before
    fun prepareEncryptedFile() {
        // The restore callback runs on Dispatchers.IO, but Android toasts require the main looper.
        RxAndroidPlugins.setMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper()) }
        initMockSecretKeygen()
        directory = File(Environment.getExternalStorageDirectory(), "encrypted-trash-regression")
        assertTrue(directory.mkdirs())
        original = File(directory, "document.txt").also { it.writeBytes(contents) }
        encrypted = File(directory, original.name + CryptUtil.CRYPT_EXTENSION)
        val failures = ArrayList<HybridFile>()
        CryptUtil(
            context,
            HybridFileParcelable(original.absolutePath),
            ProgressHandler(),
            failures,
            encrypted.name,
            false,
            null,
        )
        assertTrue(failures.isEmpty())
        assertTrue(encrypted.isFile)
        assertTrue(original.delete())
        CryptHandler.addEntry(EncryptedEntry(encrypted.absolutePath, TEST_PASSWORD))
        assertNotNull(CryptHandler.findEntry(encrypted.absolutePath))
    }

    /** Remove only this fixture's bin records, password rows and files before closing the database. */
    @After
    fun removeEncryptedFile() {
        try {
            if (::directory.isInitialized) {
                val prefix = directory.absolutePath + File.separator
                val trashBin = AppConfig.getInstance().trashBinInstance
                val ownedEntries = trashBin.listFilesInBin().filter { it.path.startsWith(prefix) }
                val ownedBinPaths =
                    ownedEntries.map { it.getDeletedPath(trashBin.getConfig()) }.toSet()
                ownedEntries.forEach { entry ->
                    val deletedFile = File(entry.getDeletedPath(trashBin.getConfig()))
                    trashElement(deletedFile, entry.fileName).generateBaseFile()
                        .deletePermanentlyFromBin(context)
                }
                CryptHandler.allEntries.filter {
                    it.path.startsWith(prefix) || it.path in ownedBinPaths
                }.forEach { CryptHandler.clear(it.path) }
                directory.deleteRecursively()
            }
        } finally {
            unmockkAll()
        }
    }

    /** Trash and restore must retain the password prompt and decrypt the original file bytes. */
    @Test
    fun testRestoredEncryptedFileCanBeOpened() {
        val entryId = CryptHandler.findEntry(encrypted.absolutePath)!!.id
        val ciphertext = encrypted.readBytes()
        val deleted = DeleteTask(context, false).doInBackground(arrayListOf(localFile()))
        assertTrue(deleted.result)
        assertNull(deleted.exception)
        assertFalse(encrypted.exists())
        val trashBin = AppConfig.getInstance().trashBinInstance
        val trashed = trashBin.listFilesInBin().single { it.path == encrypted.absolutePath }
        val deletedFile = File(trashed.getDeletedPath(trashBin.getConfig()))
        assertArrayEquals(ciphertext, deletedFile.readBytes())
        assertNull(CryptHandler.findEntry(encrypted.absolutePath))
        assertEquals(entryId, CryptHandler.findEntry(deletedFile.absolutePath)!!.id)
        val viewModel = MainActivityViewModel(AppConfig.getInstance())
        viewModel.restoreFromBin(listOf(trashElement(deletedFile, trashed.fileName)))
        finishViewModelWork(viewModel)
        assertTrue(encrypted.exists())
        assertTrue(trashBin.listFilesInBin().none { it.path == encrypted.absolutePath })
        assertArrayEquals(ciphertext, encrypted.readBytes())
        assertNull(CryptHandler.findEntry(deletedFile.absolutePath))
        assertEquals(entryId, CryptHandler.findEntry(encrypted.absolutePath)!!.id)
        assertEquals(TEST_PASSWORD, selectedEntry(encrypted.absolutePath)!!.password.value)
        assertPasswordPrompt()
        assertDecrypts(localFile(), contents, "restored-output")
    }

    /** Permanent local deletion must remove both the ciphertext and its password metadata. */
    @Test
    fun testPermanentLocalDeletionClearsPassword() {
        val result = DeleteTask(context, true).doInBackground(arrayListOf(localFile()))
        assertTrue(result.result)
        assertNull(result.exception)
        assertFalse(encrypted.exists())
        assertNull(CryptHandler.findEntry(encrypted.absolutePath))
        assertTrue(AppConfig.getInstance().trashBinInstance.listFilesInBin().isEmpty())
    }

    /** An SMB delete remains destructive even when the user did not select permanent deletion. */
    @Test
    fun testNonRecyclableDeletionClearsPassword() {
        val remote = remoteFile()
        CryptHandler.addEntry(EncryptedEntry(remote.path, TEST_PASSWORD))
        val result = DeleteTask(context, false).doInBackground(arrayListOf(remote))
        assertTrue(result.result)
        assertNull(result.exception)
        assertNull(CryptHandler.findEntry(remote.path))
        assertTrue(encrypted.exists())
        assertNotNull(CryptHandler.findEntry(encrypted.absolutePath))
    }

    /** A failed non-recyclable deletion must leave its password row available for retry. */
    @Test
    fun testFailedDeletionRetainsPassword() {
        val remote = spy(remoteFile())
        doThrow(SmbException("Synthetic deletion denied"))
            .`when`(remote).delete(context, false)
        CryptHandler.addEntry(EncryptedEntry(remote.path, TEST_PASSWORD))
        val result = DeleteTask(context, false).doInBackground(arrayListOf(remote))
        assertNotNull(result.exception)
        assertNotNull(CryptHandler.findEntry(remote.path))
        assertTrue(encrypted.exists())
    }

    /** Permanently deleting a previously trashed file must clean only its bin-path metadata. */
    @Test
    fun testPermanentTrashDeletionClearsPassword() {
        val entryId = CryptHandler.findEntry(encrypted.absolutePath)!!.id
        val recycled = DeleteTask(context, false).doInBackground(arrayListOf(localFile()))
        assertTrue(recycled.result)
        val trashBin = AppConfig.getInstance().trashBinInstance
        val trashed = trashBin.listFilesInBin().single { it.path == encrypted.absolutePath }
        val deletedFile = File(trashed.getDeletedPath(trashBin.getConfig()))
        assertTrue(deletedFile.exists())
        assertNull(CryptHandler.findEntry(encrypted.absolutePath))
        assertEquals(entryId, CryptHandler.findEntry(deletedFile.absolutePath)!!.id)
        val trashFile = trashElement(deletedFile, trashed.fileName).generateBaseFile()
        val result = DeleteTask(context, true).doInBackground(arrayListOf(trashFile))
        assertTrue(result.result)
        assertNull(result.exception)
        assertFalse(deletedFile.exists())
        assertTrue(trashBin.listFilesInBin().none { it.path == encrypted.absolutePath })
        assertTrue(CryptHandler.allEntries.none { it.id == entryId })
    }

    /** Purging an old trash item must preserve the effective password and bytes of its replacement. */
    @Test
    fun testTrashPurgeRetainsReplacementFilePassword() {
        val oldEntryId = CryptHandler.findEntry(encrypted.absolutePath)!!.id
        val recycled = DeleteTask(context, false).doInBackground(arrayListOf(localFile()))
        assertTrue(recycled.result)
        val trashBin = AppConfig.getInstance().trashBinInstance
        val trashed = trashBin.listFilesInBin().single { it.path == encrypted.absolutePath }
        val deletedFile = File(trashed.getDeletedPath(trashBin.getConfig()))
        assertTrue(deletedFile.exists())
        assertNull(CryptHandler.findEntry(encrypted.absolutePath))
        assertEquals(oldEntryId, CryptHandler.findEntry(deletedFile.absolutePath)!!.id)
        val replacementCiphertext = createReplacement()
        val replacementEntryId = selectedEntry(encrypted.absolutePath)!!.id
        assertEquals(REPLACEMENT_PASSWORD, selectedEntry(encrypted.absolutePath)!!.password.value)
        assertDecrypts(localFile(), replacementContents, "replacement-before-purge")
        val result =
            DeleteTask(context, true).doInBackground(
                arrayListOf(trashElement(deletedFile, trashed.fileName).generateBaseFile()),
            )
        assertTrue(result.result)
        assertNull(result.exception)
        assertFalse(deletedFile.exists())
        assertTrue(trashBin.listFilesInBin().none { it.path == encrypted.absolutePath })
        assertArrayEquals(replacementCiphertext, encrypted.readBytes())
        assertTrue(CryptHandler.allEntries.none { it.id == oldEntryId })
        assertEquals(replacementEntryId, selectedEntry(encrypted.absolutePath)!!.id)
        assertEquals(REPLACEMENT_PASSWORD, selectedEntry(encrypted.absolutePath)!!.password.value)
        assertDecrypts(localFile(), replacementContents, "replacement-after-purge")
    }

    /** A restore collision must retain each file's ciphertext and distinct metadata identity. */
    @Test
    fun testFailedRestorePreservesBothPasswordEntries() {
        val oldEntryId = CryptHandler.findEntry(encrypted.absolutePath)!!.id
        val oldCiphertext = encrypted.readBytes()
        val recycled = DeleteTask(context, false).doInBackground(arrayListOf(localFile()))
        assertTrue(recycled.result)
        val trashBin = AppConfig.getInstance().trashBinInstance
        val trashed = trashBin.listFilesInBin().single { it.path == encrypted.absolutePath }
        val deletedFile = File(trashed.getDeletedPath(trashBin.getConfig()))
        val replacementCiphertext = createReplacement()
        val replacementEntryId = selectedEntry(encrypted.absolutePath)!!.id
        val viewModel = MainActivityViewModel(AppConfig.getInstance())
        viewModel.restoreFromBin(listOf(trashElement(deletedFile, trashed.fileName)))
        finishViewModelWork(viewModel)
        assertArrayEquals(oldCiphertext, deletedFile.readBytes())
        assertArrayEquals(replacementCiphertext, encrypted.readBytes())
        assertTrue(trashBin.listFilesInBin().any { it.path == encrypted.absolutePath })
        assertEquals(oldEntryId, CryptHandler.findEntry(deletedFile.absolutePath)!!.id)
        assertEquals(TEST_PASSWORD, selectedEntry(deletedFile.absolutePath)!!.password.value)
        assertEquals(replacementEntryId, selectedEntry(encrypted.absolutePath)!!.id)
        assertEquals(REPLACEMENT_PASSWORD, selectedEntry(encrypted.absolutePath)!!.password.value)
    }

    /** A stale delete selection whose file has disappeared cannot move its password metadata. */
    @Test
    fun testFailedTrashRenameRetainsOriginalPassword() {
        val entryId = CryptHandler.findEntry(encrypted.absolutePath)!!.id
        assertTrue(encrypted.delete())
        val result = DeleteTask(context, false).doInBackground(arrayListOf(localFile()))
        assertFalse(result.result)
        assertNull(result.exception)
        assertEquals(entryId, CryptHandler.findEntry(encrypted.absolutePath)!!.id)
        assertEquals(TEST_PASSWORD, selectedEntry(encrypted.absolutePath)!!.password.value)
        assertTrue(AppConfig.getInstance().trashBinInstance.listFilesInBin().isEmpty())
    }

    /** The lightweight delete entry point must move metadata with the file and restore it normally. */
    @Test
    fun testLightweightTrashMovePreservesPassword() {
        val entryId = CryptHandler.findEntry(encrypted.absolutePath)!!.id
        val ciphertext = encrypted.readBytes()
        val viewModel = MainActivityViewModel(AppConfig.getInstance())
        viewModel.moveToBinLightWeight(
            listOf(HybridFile(OpenMode.FILE, encrypted.absolutePath).generateLayoutElement(context, false)),
        )
        finishViewModelWork(viewModel)
        assertFalse(encrypted.exists())
        val trashBin = AppConfig.getInstance().trashBinInstance
        val trashed = trashBin.listFilesInBin().single { it.path == encrypted.absolutePath }
        val deletedFile = File(trashed.getDeletedPath(trashBin.getConfig()))
        assertArrayEquals(ciphertext, deletedFile.readBytes())
        assertNull(CryptHandler.findEntry(encrypted.absolutePath))
        assertEquals(entryId, CryptHandler.findEntry(deletedFile.absolutePath)!!.id)
        viewModel.restoreFromBin(listOf(trashElement(deletedFile, trashed.fileName)))
        finishViewModelWork(viewModel)
        assertArrayEquals(ciphertext, encrypted.readBytes())
        assertNull(CryptHandler.findEntry(deletedFile.absolutePath))
        assertEquals(entryId, selectedEntry(encrypted.absolutePath)!!.id)
        assertEquals(TEST_PASSWORD, selectedEntry(encrypted.absolutePath)!!.password.value)
    }

    /** A metadata write failure must roll the real trash rename back without orphaning the file. */
    @Test
    fun testMetadataMoveFailureRestoresOriginalFile() {
        val entryId = CryptHandler.findEntry(encrypted.absolutePath)!!.id
        val ciphertext = encrypted.readBytes()
        mockkObject(CryptHandler)
        try {
            every { CryptHandler.moveEntry(any(), any()) } throws
                IllegalStateException("Synthetic metadata write failure")
            val result = DeleteTask(context, false).doInBackground(arrayListOf(localFile()))
            assertFalse(result.result)
            assertNull(result.exception)
            verify(exactly = 1) { CryptHandler.moveEntry(encrypted.absolutePath, any()) }
            assertTrue(encrypted.exists())
            assertArrayEquals(ciphertext, encrypted.readBytes())
            assertEquals(entryId, CryptHandler.findEntry(encrypted.absolutePath)!!.id)
            assertTrue(AppConfig.getInstance().trashBinInstance.listFilesInBin().isEmpty())
        } finally {
            unmockkObject(CryptHandler)
        }
    }

    /** Purging a plain file must not depend on encrypted-file metadata writes. */
    @Test
    fun testPlainTrashPurgeDoesNotClearEncryptedMetadata() {
        val plain = File(directory, "plain-note.txt").also { it.writeText("Synthetic plain content.") }
        val result =
            DeleteTask(context, false).doInBackground(
                arrayListOf(HybridFileParcelable(plain.absolutePath)),
            )
        assertTrue(result.result)
        assertNull(result.exception)
        assertFalse(plain.exists())
        val trashBin = AppConfig.getInstance().trashBinInstance
        val trashed = trashBin.listFilesInBin().single { it.path == plain.absolutePath }
        val deletedFile = File(trashed.getDeletedPath(trashBin.getConfig()))
        assertTrue(deletedFile.exists())
        mockkObject(CryptHandler)
        try {
            val purged =
                DeleteTask(context, true).doInBackground(
                    arrayListOf(trashElement(deletedFile, trashed.fileName).generateBaseFile()),
                )
            assertTrue(purged.result)
            assertNull(purged.exception)
            assertFalse(deletedFile.exists())
            assertTrue(trashBin.listFilesInBin().none { it.path == plain.absolutePath })
            verify(exactly = 0) { CryptHandler.clear(any()) }
            assertNotNull(CryptHandler.findEntry(encrypted.absolutePath))
        } finally {
            unmockkObject(CryptHandler)
        }
    }

    private fun createReplacement(): ByteArray {
        original.writeBytes(replacementContents)
        val failures = ArrayList<HybridFile>()
        CryptUtil(
            context,
            HybridFileParcelable(original.absolutePath),
            ProgressHandler(),
            failures,
            encrypted.name,
            false,
            null,
        )
        assertTrue(failures.isEmpty())
        assertTrue(original.delete())
        CryptHandler.addEntry(EncryptedEntry(encrypted.absolutePath, REPLACEMENT_PASSWORD))
        return encrypted.readBytes()
    }

    private fun assertDecrypts(
        file: HybridFileParcelable,
        expected: ByteArray,
        outputDirectory: String,
    ) {
        val target = File(directory, outputDirectory)
        assertTrue(target.mkdirs())
        val failures = ArrayList<HybridFile>()
        CryptUtil(context, file, target.absolutePath, ProgressHandler(), failures, null)
        assertTrue(failures.isEmpty())
        assertArrayEquals(expected, File(target, original.name).readBytes())
    }

    private fun finishViewModelWork(viewModel: MainActivityViewModel) {
        runBlocking {
            withTimeout(10_000) {
                viewModel.viewModelScope.coroutineContext[Job]!!.children.toList().joinAll()
            }
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun selectedEntry(path: String): EncryptedEntry? {
        // Exercise decryptFile's actual private resolver without starting another background service.
        val resolver = EncryptDecryptUtils::class.java.getDeclaredMethod("findEncryptedEntry", String::class.java)
        resolver.isAccessible = true
        return resolver.invoke(EncryptDecryptUtils, path) as? EncryptedEntry
    }

    private fun localFile() = HybridFileParcelable(encrypted.absolutePath)

    private fun trashElement(
        deletedFile: File,
        originalName: String,
    ) = LayoutElementParcelable(
        context,
        originalName,
        deletedFile.absolutePath,
        "",
        "",
        "",
        deletedFile.length(),
        false,
        "",
        false,
        false,
        OpenMode.TRASH_BIN,
    )

    private fun remoteFile() = HybridFileParcelable(SmbUtil.create("smb://localhost/synthetic/document.aze"))

    private fun assertPasswordPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) TestUtils.initializeInternalStorage()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED).onActivity { activity ->
                shadowOf(Looper.getMainLooper()).idle()
                val main = activity.currentMainFragment!!
                EncryptDecryptUtils.decryptFile(
                    activity,
                    activity,
                    main,
                    OpenMode.FILE,
                    localFile(),
                    directory.absolutePath,
                    activity.utilsProvider,
                    false,
                )
                val dialog = ShadowDialog.getLatestDialog() as? MaterialDialog
                assertNotNull(dialog)
                assertTrue(dialog!!.isShowing)
                assertEquals(activity.getString(R.string.crypt_decrypt), dialog.titleView.text)
                assertTrue(dialog.findViewById(R.id.singleedittext_input) is AppCompatEditText)
                dialog.dismiss()
            }
        }
    }

    companion object {
        private const val TEST_PASSWORD = "synthetic-trash-password"
        private const val REPLACEMENT_PASSWORD = "synthetic-replacement-password"
    }
}
