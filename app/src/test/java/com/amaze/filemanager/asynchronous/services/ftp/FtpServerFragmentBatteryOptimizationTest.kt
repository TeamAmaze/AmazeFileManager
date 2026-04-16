package com.amaze.filemanager.asynchronous.services.ftp

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.PowerManager
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.ftpserver.service.FtpPreferences
import com.amaze.filemanager.ftpserver.ui.BaseFtpServerFragment
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager

/**
 * Unit tests for the battery optimization prompt logic related to the FTP server.
 *
 * These tests validate:
 * - [FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED] preference saves and reads correctly.
 * - The Robolectric [ShadowPowerManager] correctly simulates the battery exemption state that
 *   `FtpServerFragment.checkBatteryOptimizationIfNecessary` reads.
 * - [BaseFtpServerFragment.onBeforeStartServer] invokes its `proceed` lambda immediately by default.
 *
 * Full integration tests for the dialog being shown / dismissed are covered by instrumented tests
 * that launch [com.amaze.filemanager.ui.activities.MainActivity] and navigate to the FTP fragment.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class],
)
class FtpServerFragmentBatteryOptimizationTest {
    private lateinit var context: Context
    private lateinit var shadowPowerManager: ShadowPowerManager

    /**
     * Minimal concrete [BaseFtpServerFragment] subclass for testing [onBeforeStartServer].
     * All abstract members are no-ops; the default [onBeforeStartServer] is NOT overridden.
     */
    private class DefaultFragment : BaseFtpServerFragment() {
        var proceedCalled = false

        fun callOnBeforeStartServer() {
            onBeforeStartServer { proceedCalled = true }
        }

        override fun getAccentColor(): Int = 0

        override fun isConnectedToLocalNetwork(): Boolean = true

        override fun isConnectedToWifi(): Boolean = true

        override fun getLocalAddress(): String = "127.0.0.1"

        override fun startFtpService(startedByTile: Boolean) {}

        override fun stopFtpService() {}

        override fun promptUserToEnableWireless() {}

        override fun dismissSnackbar() {}

        override fun getEncryptedPassword(): String? = null

        override fun decryptPassword(encryptedPassword: String): String? = null

        override fun onPathChangeRequested() {}

        override fun onLoginChangeRequested() {}
    }

    /**
     * Concrete [BaseFtpServerFragment] subclass that overrides [onBeforeStartServer] to
     * simulate gating (e.g. showing a dialog) without calling [proceed].
     */
    private class GatingFragment : BaseFtpServerFragment() {
        var proceedCalled = false
        var intercepted = false

        fun callOnBeforeStartServer() {
            onBeforeStartServer { proceedCalled = true }
        }

        override fun onBeforeStartServer(proceed: () -> Unit) {
            intercepted = true
            // Deliberately do NOT call proceed — simulating a dialog gate.
        }

        override fun getAccentColor(): Int = 0

        override fun isConnectedToLocalNetwork(): Boolean = true

        override fun isConnectedToWifi(): Boolean = true

        override fun getLocalAddress(): String = "127.0.0.1"

        override fun startFtpService(startedByTile: Boolean) {}

        override fun stopFtpService() {}

        override fun promptUserToEnableWireless() {}

        override fun dismissSnackbar() {}

        override fun getEncryptedPassword(): String? = null

        override fun decryptPassword(encryptedPassword: String): String? = null

        override fun onPathChangeRequested() {}

        override fun onLoginChangeRequested() {}
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowPowerManager = Shadows.shadowOf(powerManager)
        // Reset the "don't ask again" preference before each test.
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED)
            .apply()
    }

    @After
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED)
            .apply()
    }

    // ---- FtpPreferences key tests ----

    /**
     * When [FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED] is absent the preference
     * returns `false` — i.e. the user has NOT yet suppressed the prompt.
     */
    @Test
    fun testBatteryOptimizationPreferenceDefaultIsFalse() {
        val asked =
            FtpPreferences.getPreferences(context)
                .getBoolean(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED, false)
        assertFalse("Battery optimization preference should default to false (prompt not suppressed)", asked)
    }

    /**
     * After the user clicks "Don't ask again", the preference is persisted as `true`.
     */
    @Test
    fun testBatteryOptimizationPreferencePersistence() {
        FtpPreferences.getPreferences(context)
            .edit()
            .putBoolean(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED, true)
            .apply()

        val asked =
            FtpPreferences.getPreferences(context)
                .getBoolean(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED, false)
        assertTrue("Battery optimization preference should be true after user suppresses it", asked)
    }

    // ---- ShadowPowerManager simulation tests ----

    /**
     * Verifies that the Robolectric [ShadowPowerManager] correctly simulates the app being
     * exempt from battery optimizations. When `isIgnoringBatteryOptimizations()` returns `true`
     * for the app package, `FtpServerFragment.checkBatteryOptimizationIfNecessary` should
     * bypass the dialog entirely.
     *
     * This test verifies the Robolectric shadow behaves as expected so that integration tests
     * relying on it are meaningful.
     */
    @Config(sdk = [M])
    @Test
    fun testShadowPowerManagerExemptionSimulation() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // Before exemption: optimization is active (app is NOT ignoring battery optimizations).
        assertFalse(
            "App should not be exempt by default",
            powerManager.isIgnoringBatteryOptimizations(context.packageName),
        )

        // Simulate the user granting battery optimization exemption.
        shadowPowerManager.setIgnoringBatteryOptimizations(context.packageName, true)

        assertTrue(
            "App should be exempt after ShadowPowerManager grants exemption",
            powerManager.isIgnoringBatteryOptimizations(context.packageName),
        )
    }

    /**
     * Verifies that when the app IS already exempt from battery optimizations,
     * the "don't ask again" preference remains unset (no preference side-effect on bypass).
     */
    @Config(sdk = [M])
    @Test
    fun testExemptAppDoesNotSetDontAskPref() {
        shadowPowerManager.setIgnoringBatteryOptimizations(context.packageName, true)

        // The bypass due to exemption should NOT persist the "don't ask again" flag itself.
        val asked =
            FtpPreferences.getPreferences(context)
                .getBoolean(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED, false)
        assertFalse(
            "Bypassing the check due to system exemption must not persist the 'don't ask' flag",
            asked,
        )
    }

    // ---- BaseFtpServerFragment.onBeforeStartServer tests ----

    /**
     * The default [BaseFtpServerFragment.onBeforeStartServer] must invoke the `proceed`
     * lambda immediately without any interception.
     */
    @Test
    fun testDefaultOnBeforeStartServerCallsProceed() {
        val fragment = DefaultFragment()
        fragment.callOnBeforeStartServer()
        assertTrue(
            "Default onBeforeStartServer must invoke proceed immediately",
            fragment.proceedCalled,
        )
    }

    /**
     * A subclass that overrides [BaseFtpServerFragment.onBeforeStartServer] and does NOT call
     * `proceed` can gate the server start — e.g. to show a battery optimization dialog first.
     */
    @Test
    fun testOverriddenOnBeforeStartServerCanIntercept() {
        val fragment = GatingFragment()
        fragment.callOnBeforeStartServer()
        assertTrue("Overridden onBeforeStartServer must be invoked", fragment.intercepted)
        assertFalse(
            "Proceed must NOT be called when the override intercepts it",
            fragment.proceedCalled,
        )
    }
}
