/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
package com.amaze.filemanager.utils

import android.content.ComponentName
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.AbstractOpenFolderInTerminalTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test [MainActivity.detectInstalledTerminalApps] in extension functions.
 */
@Suppress("StringLiteralDuplication")
class OpenTerminalUtilsExtTest : AbstractOpenFolderInTerminalTestBase() {
    /**
     * Case when no supported Terminal is installed.
     */
    @Test
    fun `Test when there is no terminal app installed`() {
        doTestWithMainActivity { mainActivity ->
            val result = mainActivity.detectInstalledTerminalApps()
            assertNotNull(result)
            assertEquals(0, result.size)
        }
    }

    /**
     * Case when Termux is installed.
     */
    @Test
    fun `Test when there is only Termux installed`() {
        doTestWithMainActivity { mainActivity ->
            // Package name is important. Class name is not... no need to 100% match
            installApp(mainActivity, ComponentName("com.termux", "com.termux.Activity"))

            val result = mainActivity.detectInstalledTerminalApps()
            assertNotNull(result)
            assertEquals(1, result.size)
            assertEquals("com.termux", result.first())
        }
    }

    /**
     * Case when both Termux and Termone plus are installed.
     */
    @Test
    fun `Test when there are both Termux and Termone plus installed`() {
        doTestWithMainActivity { mainActivity ->
            // Package name is important. Class name is not... no need to 100% match
            installApp(mainActivity, ComponentName("com.termux", "com.termux.Activity"))
            installApp(mainActivity, ComponentName("com.termoneplus", "com.termoneplus.Activity"))

            val result = mainActivity.detectInstalledTerminalApps()
            assertNotNull(result)
            assertEquals(2, result.size)
            assertTrue(result.contains("com.termux"))
            assertTrue(result.contains("com.termoneplus"))
        }
    }

    /**
     * Real life situation, when Termux and Termone plus are installed among others.
     */
    @Test
    fun `Test when there are other apps installed, method should filter them out`() {
        doTestWithMainActivity { mainActivity ->
            // Package name is important. Class name is not... no need to 100% match
            installApp(mainActivity, ComponentName("com.termux", "com.termux.Activity"))
            installApp(mainActivity, ComponentName("com.termoneplus", "com.termoneplus.Activity"))
            installApp(mainActivity, ComponentName("com.amaze.filemanager", "com.amaze.filemanager.Activity"))

            val result = mainActivity.detectInstalledTerminalApps()
            assertNotNull(result)
            assertEquals(2, result.size)
            assertTrue(result.contains("com.termux"))
            assertTrue(result.contains("com.termoneplus"))
        }
    }
}
