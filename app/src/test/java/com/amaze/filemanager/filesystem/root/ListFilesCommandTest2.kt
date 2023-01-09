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
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.exceptions.ShellCommandInvalidException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowNativeOperations
import com.amaze.filemanager.test.TestUtils
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.topjohnwu.superuser.Shell
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.argumentCaptor
import org.robolectric.annotation.Config
import java.io.InputStreamReader

/**
 * Unit test for [ListFilesCommand]. This is to test the case when stat command fails.
 *
 * ls output is captured from busybox, and used as fixed outputs from mocked object
 * to ensure command output.
 *
 * FIXME: add toybox outputs, just to be sure?
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowNativeOperations::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
class ListFilesCommandTest2 {

    val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
    val lsLines =
        InputStreamReader(javaClass.getResourceAsStream("/rootCommands/ls-bin.txt"))
            .readLines()

    /**
     * test setup.
     */
    @Before
    fun setUp() {
        val mockCommand = mock(ListFilesCommand.javaClass)
        `when`(
            mockCommand.listFiles(
                anyString(),
                anyBoolean(),
                anyBoolean(),
                argumentCaptor<(OpenMode) -> Unit>().capture(),
                argumentCaptor<(HybridFileParcelable) -> Unit>().capture()
            )
        ).thenCallRealMethod()
        `when`(
            mockCommand.executeRootCommand(
                anyString(),
                anyBoolean(),
                anyBoolean()
            )
        ).thenCallRealMethod()
        `when`(mockCommand.runShellCommand("pwd")).thenReturn(
            object : Shell.Result() {
                override fun getOut(): MutableList<String> = listOf("/").toMutableList()
                override fun getErr(): MutableList<String> = emptyList<String>().toMutableList()
                override fun getCode(): Int = 0
            }
        )
        `when`(mockCommand.runShellCommandToList("ls -l \"/bin\"")).thenReturn(lsLines)
        `when`(
            mockCommand.runShellCommandToList(
                "stat -c '%A %h %G %U %B %Y %N' /bin/*"
            )
        ).thenThrow(ShellCommandInvalidException("Intentional exception"))
        TestUtils.replaceObjectInstance(ListFilesCommand.javaClass, mockCommand)
    }

    /**
     * Post test cleanup.
     */
    @After
    fun tearDown() {
        TestUtils.replaceObjectInstance(ListFilesCommand.javaClass, null)
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
        ListFilesCommand.listFiles("/bin", true, false, {}, { ++statCount })
        assertEquals(lsLines.size - 1, statCount)

        sharedPreferences.edit()
            .putBoolean(PreferencesConstants.PREFERENCE_ROOT_LEGACY_LISTING, true).commit()
        var lsCount = 0
        ListFilesCommand.listFiles("/bin", true, false, {}, { ++lsCount })
        assertEquals(lsLines.size - 1, lsCount)
    }
}
