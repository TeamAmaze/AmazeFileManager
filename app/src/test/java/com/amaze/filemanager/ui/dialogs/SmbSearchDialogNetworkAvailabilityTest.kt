/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.Context
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.utils.NetworkUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Public dialog workflows with synthetic network services; no sockets or discovery traffic. */
class SmbSearchDialogNetworkAvailabilityTest : AbstractMainActivityTestBase() {
    /** Shows the local-network requirement without starting discovery when disconnected. */
    @Test
    fun testUnavailableNetworkExplainsRequirement() {
        withSearchDialog(wifi = null, connected = false) { _, dialog, _, nsd ->
            assertEquals(
                "Device search needs a local network connection. You can enter a host address manually.",
                dialog.contentView?.text.toString(),
            )
            assertTrue(dialog.getActionButton(DialogAction.POSITIVE).isEnabled)
            verify(exactly = 0) { nsd.discoverServices(any(), any(), any()) }
        }
    }

    /** Opens manual host entry from the disconnected search dialog without starting discovery. */
    @Test
    fun testManualEntryRemainsAvailableWithoutNetwork() {
        withSearchDialog(wifi = null, connected = false) { activity, dialog, _, nsd ->
            assertTrue(dialog.getActionButton(DialogAction.POSITIVE).performClick())
            ShadowLooper.runUiThreadTasks()
            assertFalse(dialog.isShowing)
            assertTrue(activity.supportFragmentManager.fragments.any { it is SmbConnectDialog })
            assertTrue(ShadowDialog.getLatestDialog().isShowing)
            verify(exactly = 0) { nsd.discoverServices(any(), any(), any()) }
        }
    }

    /** Cancels disconnected search without opening the manual connection dialog. */
    @Test
    fun testUnavailableNetworkCanBeCancelled() {
        withSearchDialog(wifi = null, connected = false) { activity, dialog, _, _ ->
            assertTrue(dialog.getActionButton(DialogAction.NEGATIVE).performClick())
            ShadowLooper.runUiThreadTasks()
            assertFalse(dialog.isShowing)
            assertFalse(activity.supportFragmentManager.fragments.any { it is SmbConnectDialog })
        }
    }

    /** Starts service discovery on a connected local network without an optional Wi-Fi service. */
    @Test
    fun testEthernetDiscoveryDoesNotRequireWifiManager() {
        withSearchDialog(wifi = null, connected = true) { _, dialog, _, nsd ->
            assertTrue(dialog.isShowing)
            verify(exactly = 1) { nsd.discoverServices("_smb._tcp.", NsdManager.PROTOCOL_DNS_SD, any()) }
        }
    }

    /** Acquires the available Wi-Fi multicast lock for discovery and releases it on cancellation. */
    @Test
    fun testWifiDiscoveryAcquiresAndReleasesMulticastLock() {
        val lock = mockk<WifiManager.MulticastLock>(relaxed = true)
        val wifi = mockk<WifiManager>(relaxed = true)
        every { wifi.createMulticastLock(any()) } returns lock
        every { lock.isHeld } returns true
        withSearchDialog(wifi, connected = true) { _, dialog, _, nsd ->
            verify(exactly = 1) { lock.acquire() }
            verify(exactly = 1) { nsd.discoverServices("_smb._tcp.", NsdManager.PROTOCOL_DNS_SD, any()) }
            dialog.getActionButton(DialogAction.NEGATIVE).performClick()
            ShadowLooper.runUiThreadTasks()
            verify(atLeast = 1) { lock.release() }
        }
    }

    private fun withSearchDialog(
        wifi: WifiManager?,
        connected: Boolean,
        assertion: (MainActivity, MaterialDialog, SmbSearchDialog, NsdManager) -> Unit,
    ) {
        val app = AppConfig.getInstance()
        val originalWifi = app.getSystemService(Context.WIFI_SERVICE)
        val originalNsd = app.getSystemService(Context.NSD_SERVICE)
        val nsd = mockk<NsdManager>(relaxed = true)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        try {
            Shadows.shadowOf(app).setSystemService(Context.WIFI_SERVICE, wifi)
            Shadows.shadowOf(app).setSystemService(Context.NSD_SERVICE, nsd)
            mockkStatic(NetworkUtil::class)
            every { NetworkUtil.isConnectedToLocalNetwork(any()) } returns connected
            // The other discovery strategies have no local address and cannot open sockets.
            every { NetworkUtil.getLocalInetAddress(any(), any()) } returns null
            RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
            scenario.onActivity { activity ->
                val fragment = SmbSearchDialog()
                fragment.show(activity.supportFragmentManager, "network-availability-test")
                ShadowLooper.runUiThreadTasks()
                val dialog = ShadowDialog.getLatestDialog() as MaterialDialog
                assertTrue(dialog.isShowing)
                assertion(activity, dialog, fragment, nsd)
                if (fragment.isAdded) fragment.dismiss()
                ShadowLooper.runUiThreadTasks()
            }
        } finally {
            scenario.close()
            unmockkStatic(NetworkUtil::class)
            Shadows.shadowOf(app).setSystemService(Context.WIFI_SERVICE, originalWifi)
            Shadows.shadowOf(app).setSystemService(Context.NSD_SERVICE, originalNsd)
        }
    }
}
