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
package com.amaze.filemanager.ui.dialogs

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.adapters.holders.AppHolder
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.OpenFolderInTerminalFragment.Companion.KEY_PREFERENCES_DEFAULT
import com.amaze.filemanager.ui.dialogs.OpenFolderInTerminalFragment.Companion.KEY_PREFERENCES_LAST
import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowToast

/**
 * Tests for [OpenFolderInTerminalFragment].
 */
@Suppress("StringLiteralDuplication", "ComplexMethod", "LongMethod")
class OpenFolderInTerminalFragmentTest : AbstractOpenFolderInTerminalTestBase() {
    @Before
    override fun setUp() {
        super.setUp()
        ShadowToast.reset()
        val application = ApplicationProvider.getApplicationContext<Application>()
        val app: ShadowApplication = shadowOf(application)
        app.grantPermissions(
            "com.termux.permission.RUN_COMMAND",
            "com.termoneplus.permission.RUN_SCRIPT",
            "jackpal.androidterm.permission.RUN_SCRIPT",
        )
    }

    /**
     * Test clearPreferences when no keys are found.
     */
    @Test
    fun testClearPreferencesWhenNoKeyIsSet() {
        doTestWithMainActivity { mainActivity ->
            mainActivity.prefs.let { prefs ->
                prefs.edit().putString("FOO", "BAR").apply()
                assertFalse(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertFalse(prefs.contains(KEY_PREFERENCES_LAST))
                OpenFolderInTerminalFragment.clearPreferences(prefs)
                assertFalse(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertFalse(prefs.contains(KEY_PREFERENCES_LAST))
                assertTrue(prefs.contains("FOO"))
            }
        }
    }

    /**
     * Test clearPreferences when last used key is found.
     */
    @Test
    fun testClearPreferencesWhenLastKeyIsSet() {
        doTestWithMainActivity { mainActivity: MainActivity ->
            mainActivity.prefs.let { prefs ->
                prefs.edit()
                    .putString("FOO", "BAR")
                    .putString(KEY_PREFERENCES_LAST, "com.termoneplus")
                    .apply()
                assertFalse(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertTrue(prefs.contains(KEY_PREFERENCES_LAST))
                OpenFolderInTerminalFragment.clearPreferences(prefs)
                assertFalse(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertFalse(prefs.contains(KEY_PREFERENCES_LAST))
                assertTrue(prefs.contains("FOO"))
            }
        }
    }

    /**
     * Test clearPreferences when default key is found.
     */
    @Test
    fun testClearPreferencesWhenDefaultKeyIsSet() {
        doTestWithMainActivity { mainActivity: MainActivity ->
            mainActivity.prefs.let { prefs ->
                prefs.edit()
                    .putString("FOO", "BAR")
                    .putString(KEY_PREFERENCES_DEFAULT, "com.termoneplus")
                    .apply()
                assertTrue(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertFalse(prefs.contains(KEY_PREFERENCES_LAST))
                OpenFolderInTerminalFragment.clearPreferences(prefs)
                assertFalse(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertFalse(prefs.contains(KEY_PREFERENCES_LAST))
                assertTrue(prefs.contains("FOO"))
            }
        }
    }

    /**
     * Test clearPreferences when both keys are found.
     */
    @Test
    fun testClearPreferencesWhenBothKeysAreSet() {
        doTestWithMainActivity { mainActivity: MainActivity ->
            mainActivity.prefs.let { prefs ->
                prefs.edit()
                    .putString("FOO", "BAR")
                    .putString(KEY_PREFERENCES_DEFAULT, "com.termoneplus")
                    .putString(KEY_PREFERENCES_LAST, "com.termux")
                    .apply()
                assertTrue(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertTrue(prefs.contains(KEY_PREFERENCES_LAST))
                OpenFolderInTerminalFragment.clearPreferences(prefs)
                assertFalse(prefs.contains(KEY_PREFERENCES_DEFAULT))
                assertFalse(prefs.contains(KEY_PREFERENCES_LAST))
                assertTrue(prefs.contains("FOO"))
            }
        }
    }

    /**
     * Test when no terminal app is installed.
     */
    @Test
    fun testOpenOrShowWhenNoTerminalInstalled() {
        doTestWithMainActivity { mainActivity: MainActivity ->
            OpenFolderInTerminalFragment.openTerminalOrShow("/sdcard/tmp", mainActivity)
            assertTrue(ShadowToast.shownToastCount() == 1)
            assertEquals("No Terminal App installed", ShadowToast.getTextOfLatestToast())
        }
    }

    private fun `After install specified app`(
        componentName: ComponentName,
        beforeOpen: ((MainActivity, CapturingSlot<Intent>) -> Unit)? = null,
        nextStep: (MainActivity, CapturingSlot<Intent>) -> Unit,
    ) {
        doTestWithMainActivity { mainActivity: MainActivity ->
            installApp(mainActivity, componentName)
            val capturedIntent = slot<Intent>()
            val capturedCallback = slot<() -> Unit>()
            every {
                mainActivity.requestTerminalPermission(any(), capture(capturedCallback))
            } answers {
                capturedCallback.captured.invoke()
            }
            every {
                mainActivity.startActivity(capture(capturedIntent), any())
            } answers {
                callOriginal()
            }
            beforeOpen?.invoke(mainActivity, capturedIntent)
            OpenFolderInTerminalFragment.openTerminalOrShow("/sdcard/tmp", mainActivity)
            nextStep.invoke(mainActivity, capturedIntent)
        }
    }

    /**
     * Test when only Termone Plus is installed.
     */
    @Test
    fun testOpenTerminalWhenOnlyTermonePlusIsInstalled() {
        `After install specified app`(
            ComponentName("com.termoneplus", "com.termoneplus.Activity"),
        ) { mainActivity, capturedIntent ->
            verify {
                mainActivity.startActivity(capturedIntent.captured, null)
            }

            capturedIntent.captured.let { intent ->
                assertEquals("com.termoneplus.RUN_SCRIPT", intent.action)
                assertEquals("com.termoneplus", intent.component?.packageName)
                assertEquals("jackpal.androidterm.RunScript", intent.component?.className)
                assertEquals(
                    "cd \"/sdcard/tmp\"",
                    intent.getStringExtra("com.termoneplus.Command"),
                )
            }
        }
    }

    /**
     * Test when only Termux is installed.
     */
    @Test
    fun testOpenTerminalWhenOnlyTermuxIsInstalled() {
        `After install specified app`(
            componentName = ComponentName("com.termux", "com.termux.Activity"),
            beforeOpen = { mainActivity, capturedIntent ->
                if (SDK_INT >= 26) {
                    every {
                        mainActivity.startForegroundService(capture(capturedIntent))
                    } answers { callOriginal() }
                } else {
                    every {
                        mainActivity.startService(capture(capturedIntent))
                    } answers {
                        callOriginal()
                    }
                }
            },
        ) { mainActivity, capturedIntent ->
            verify {
                if (SDK_INT >= 26) {
                    mainActivity.startForegroundService(capturedIntent.captured)
                    mainActivity.startService(capturedIntent.captured)?.wasNot(Called)
                } else {
                    mainActivity.startService(capturedIntent.captured)
                }
            }

            capturedIntent.captured.let { intent ->
                assertEquals("com.termux.RUN_COMMAND", intent.action)
                assertEquals("com.termux", intent.component?.packageName)
                assertEquals("com.termux.app.RunCommandService", intent.component?.className)
            }
        }
    }

    private fun `After setup case of both Termux and Termone plus installed`(
        beforeOpen: ((MainActivity) -> Unit)? = null,
        nextStep: (MainActivity, CapturingSlot<Intent>) -> Unit,
    ) {
        doTestWithMainActivity { mainActivity ->
            installApp(mainActivity, ComponentName("com.termoneplus", "com.termoneplus.Activity"))
            installApp(mainActivity, ComponentName("com.termux", "com.termux.Activity"))
            val capturedIntent = slot<Intent>()
            val capturedCallback = slot<() -> Unit>()
            every {
                mainActivity.startActivity(capture(capturedIntent), any())
            } answers {
                callOriginal()
            }
            every {
                mainActivity.requestTerminalPermission(any(), capture(capturedCallback))
            } answers {
                capturedCallback.captured.invoke()
            }
            beforeOpen?.invoke(mainActivity)
            OpenFolderInTerminalFragment.openTerminalOrShow("/sdcard/tmp", mainActivity)
            nextStep.invoke(mainActivity, capturedIntent)
        }
    }

    /**
     * When Termux and Termone plus are installed, but default is set to Termone Plus.
     */
    @Test
    fun `When Termux and Termone plus are installed, but default is set to Termone Plus`() {
        `After setup case of both Termux and Termone plus installed`(
            beforeOpen = { mainActivity: MainActivity ->
                mainActivity.prefs.edit().putString("terminal._DEFAULT", "com.termoneplus").apply()
            },
            nextStep = { mainActivity, capturedIntent ->
                verify {
                    mainActivity.startActivity(capturedIntent.captured, null)
                }
                capturedIntent.captured.let { intent ->
                    assertEquals("com.termoneplus.RUN_SCRIPT", intent.action)
                    assertEquals("com.termoneplus", intent.component?.packageName)
                    assertEquals("jackpal.androidterm.RunScript", intent.component?.className)
                    assertEquals(
                        "cd \"/sdcard/tmp\"",
                        intent.getStringExtra("com.termoneplus.Command"),
                    )
                }
            },
        )
    }

    /**
     * Test Dialog fragment instance.
     */
    @Test
    fun `Display dialog fragment, choosing always use Termone plus`() {
        `After setup case of both Termux and Termone plus installed` { mainActivity, capturedIntent ->
            assertTrue(mainActivity.supportFragmentManager.executePendingTransactions())
            mainActivity.supportFragmentManager.fragments.last().run {
                assertTrue(this is OpenFolderInTerminalFragment)
                (this as OpenFolderInTerminalFragment).let { fragment ->
                    // Because one item had been removed to the last app
                    assertEquals(1, viewBinding.appsRecyclerView.adapter?.itemCount)
                    assertEquals("com.termoneplus", viewBinding.lastAppTitle.text)
                    fragment.viewBinding.alwaysButton.performClick()
                }
            }
            assertEquals(
                "com.termoneplus",
                mainActivity.prefs.getString(KEY_PREFERENCES_DEFAULT, null),
            )

            OpenFolderInTerminalFragment.openTerminalOrShow("/sdcard/tmp", mainActivity)

            verify {
                mainActivity.startActivity(capturedIntent.captured, null)
            }

            capturedIntent.captured.let { intent ->
                assertEquals("com.termoneplus.RUN_SCRIPT", intent.action)
                assertEquals("com.termoneplus", intent.component?.packageName)
                assertEquals("jackpal.androidterm.RunScript", intent.component?.className)
                assertEquals(
                    "cd \"/sdcard/tmp\"",
                    intent.getStringExtra("com.termoneplus.Command"),
                )
            }
        }
    }

    /**
     * Test Dialog fragment instance.
     */
    @Test
    fun `Display dialog fragment, choosing use Termone plus once`() {
        `After setup case of both Termux and Termone plus installed` { mainActivity, capturedIntent ->
            assertTrue(mainActivity.supportFragmentManager.executePendingTransactions())
            mainActivity.supportFragmentManager.fragments.last().run {
                assertTrue(this is OpenFolderInTerminalFragment)
                (this as OpenFolderInTerminalFragment).let { fragment ->
                    // Because one item had been removed to the last app
                    assertEquals(1, viewBinding.appsRecyclerView.adapter?.itemCount)
                    assertEquals("com.termoneplus", viewBinding.lastAppTitle.text)
                    fragment.viewBinding.justOnceButton.performClick()
                }
            }
            assertEquals(
                "com.termoneplus",
                mainActivity.prefs.getString(KEY_PREFERENCES_LAST, null),
            )
        }
    }

    /**
     * Test when both Termone plus and Termux are found, but user choose to use Termux once only.
     */
    @Test
    fun `With both Termux and Termone plus, choose Termux but once only`() {
        `After setup case of both Termux and Termone plus installed` { mainActivity, capturedIntent ->
            assertTrue(mainActivity.supportFragmentManager.executePendingTransactions())
            mainActivity.supportFragmentManager.fragments.last().run {
                assertTrue(this is OpenFolderInTerminalFragment)
                (this as OpenFolderInTerminalFragment).let { fragment ->
                    // Because one item had been removed to the last app
                    assertEquals(1, viewBinding.appsRecyclerView.adapter?.itemCount)
                    assertEquals("com.termoneplus", viewBinding.lastAppTitle.text)
                    assertEquals(1, fragment.viewBinding.appsRecyclerView.adapter?.itemCount)
                    fragment.viewBinding.appsRecyclerView.run {
                        measure(
                            View.MeasureSpec.UNSPECIFIED,
                            View.MeasureSpec.UNSPECIFIED,
                        )
                        layout(0, 0, 1000, 1000)
                        val viewHolder = findViewHolderForAdapterPosition(0)
                        assertNotNull(viewHolder)
                        (viewHolder as AppHolder).run {
                            assertEquals("com.termux", txtTitle.text)
                            rl.performClick()
                        }
                    }
                }
            }
            assertEquals(
                "com.termux",
                mainActivity.prefs.getString(KEY_PREFERENCES_LAST, null),
            )
            OpenFolderInTerminalFragment.openTerminalOrShow("/sdcard/tmp", mainActivity)
            assertTrue(mainActivity.supportFragmentManager.executePendingTransactions())
            mainActivity.supportFragmentManager.fragments.last().run {
                assertTrue(this is OpenFolderInTerminalFragment)
                (this as OpenFolderInTerminalFragment).let { fragment ->
                    // Because one item had been removed to the last app
                    assertEquals(1, viewBinding.appsRecyclerView.adapter?.itemCount)
                    assertEquals("com.termux", viewBinding.lastAppTitle.text)

                    assertEquals(1, fragment.viewBinding.appsRecyclerView.adapter?.itemCount)
                    fragment.viewBinding.appsRecyclerView.run {
                        measure(
                            View.MeasureSpec.UNSPECIFIED,
                            View.MeasureSpec.UNSPECIFIED,
                        )
                        layout(0, 0, 1000, 1000)
                        val viewHolder = findViewHolderForAdapterPosition(0)
                        assertNotNull(viewHolder)
                        (viewHolder as AppHolder).run {
                            assertEquals("com.termoneplus", txtTitle.text)
                            rl.performClick()
                        }
                    }
                }
            }
            assertEquals(
                "com.termoneplus",
                mainActivity.prefs.getString(KEY_PREFERENCES_LAST, null),
            )
            OpenFolderInTerminalFragment.openTerminalOrShow("/sdcard/tmp", mainActivity)
            assertTrue(mainActivity.supportFragmentManager.executePendingTransactions())
            mainActivity.supportFragmentManager.fragments.last().run {
                assertTrue(this is OpenFolderInTerminalFragment)
                (this as OpenFolderInTerminalFragment).let { fragment ->
                    // Because one item had been removed to the last app
                    assertEquals(1, viewBinding.appsRecyclerView.adapter?.itemCount)
                    assertEquals("com.termoneplus", viewBinding.lastAppTitle.text)
                    assertEquals(1, fragment.viewBinding.appsRecyclerView.adapter?.itemCount)
                    fragment.viewBinding.appsRecyclerView.run {
                        measure(
                            View.MeasureSpec.UNSPECIFIED,
                            View.MeasureSpec.UNSPECIFIED,
                        )
                        layout(0, 0, 1000, 1000)
                        val viewHolder = findViewHolderForAdapterPosition(0)
                        assertNotNull(viewHolder)
                        (viewHolder as AppHolder).run {
                            assertEquals("com.termux", txtTitle.text)
                        }
                    }
                    fragment.viewBinding.alwaysButton.performClick()
                }
            }
            assertEquals(
                "com.termoneplus",
                mainActivity.prefs.getString(KEY_PREFERENCES_DEFAULT, null),
            )
            OpenFolderInTerminalFragment.openTerminalOrShow("/sdcard/tmp", mainActivity)

            verify {
                mainActivity.startActivity(capturedIntent.captured, null)
            }

            capturedIntent.captured.let { intent ->
                assertEquals("com.termoneplus.RUN_SCRIPT", intent.action)
                assertEquals("com.termoneplus", intent.component?.packageName)
                assertEquals("jackpal.androidterm.RunScript", intent.component?.className)
                assertEquals(
                    "cd \"/sdcard/tmp\"",
                    intent.getStringExtra("com.termoneplus.Command"),
                )
            }
        }
    }
}
