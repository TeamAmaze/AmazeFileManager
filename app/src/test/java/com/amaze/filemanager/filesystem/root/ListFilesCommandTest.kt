/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.root

import android.content.SharedPreferences
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowNativeOperations
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.argumentCaptor
import org.robolectric.annotation.Config
import java.io.InputStreamReader

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowNativeOperations::class],
    sdk = [JELLY_BEAN, KITKAT, P]
)
/**
 * Unit test for [ListFilesCommand].
 *
 * stat and ls outputs are captured from busybox or toybox, and used as fixed outputs from
 * mocked object to ensure command output.
 */
class ListFilesCommandTest {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
    private val statLines =
        InputStreamReader(javaClass.getResourceAsStream("/rootCommands/stat-bin.txt"))
            .readLines()
    private val statRootLines =
        InputStreamReader(javaClass.getResourceAsStream("/rootCommands/stat-root.txt"))
            .readLines()
    private val lsLines =
        InputStreamReader(javaClass.getResourceAsStream("/rootCommands/ls-bin.txt"))
            .readLines()
    private val lsRootLines =
        InputStreamReader(javaClass.getResourceAsStream("/rootCommands/ls-root.txt"))
            .readLines()

    /**
     * test setup.
     */
    @Before
    fun setUp() {
        mockkObject(ListFilesCommand)
        every {
            ListFilesCommand.listFiles(
                anyString(),
                anyBoolean(),
                anyBoolean(),
                argumentCaptor<(OpenMode) -> Unit>().capture(),
                argumentCaptor<(HybridFileParcelable) -> Unit>().capture()
            )
        } answers { callOriginal() }
        every {
            ListFilesCommand.executeRootCommand(anyString(), anyBoolean(), anyBoolean())
        } answers { callOriginal() }
        every { ListFilesCommand.runShellCommandToList("ls -l \"/bin\"") } answers { lsLines }
        every { ListFilesCommand.runShellCommandToList("ls -l \"/\"") } answers { lsRootLines }
        every {
            ListFilesCommand.runShellCommandToList("stat -c '%A %h %G %U %B %Y %N' /bin/*")
        } answers { statLines }
        every {
            ListFilesCommand.runShellCommandToList("stat -c '%A %h %G %U %B %Y %N' *")
        } answers { statRootLines }
    }

    /**
     * Test command run.
     *
     * FIXME: Due to some (mysterious) limitations on mocking singletons, have to make both
     * conditions run in one go.
     */
    @Test
    fun testCommandRun() {
        sharedPreferences.edit()
            .putBoolean(PreferencesConstants.PREFERENCE_ROOT_LEGACY_LISTING, false).commit()
        var statCount = 0
        ListFilesCommand.listFiles(
            "/bin",
            root = true,
            showHidden = false,
            openModeCallback = {},
            onFileFoundCallback = { ++statCount }
        )
        assertEquals(statLines.size, statCount)
        statCount = 0
        ListFilesCommand.listFiles(
            "/",
            root = true,
            showHidden = false,
            openModeCallback = {},
            onFileFoundCallback = { ++statCount }
        )
        assertEquals(statRootLines.size, statCount)

        sharedPreferences.edit()
            .putBoolean(PreferencesConstants.PREFERENCE_ROOT_LEGACY_LISTING, true).commit()
        var lsCount = 0
        ListFilesCommand.listFiles(
            "/bin",
            root = true,
            showHidden = false,
            openModeCallback = {},
            onFileFoundCallback = { ++lsCount }
        )
        assertEquals(lsLines.size - 1, lsCount)
        lsCount = 0
        ListFilesCommand.listFiles(
            "/",
            root = true,
            showHidden = false,
            openModeCallback = {},
            onFileFoundCallback = { ++lsCount }
        )
        assertEquals(lsRootLines.size - 1, lsCount)
    }
}
