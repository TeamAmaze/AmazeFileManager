package com.amaze.filemanager.ui.dialogs

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.Assert.assertTrue
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/**
 * Base class for [SftpConnectDialog] UI level tests.
 */
abstract class AbstractSftpConnectDialogUiTests : AbstractMainActivityTestBase() {
    /**
     * Create and display [SftpConnectDialog] with Robolectric and AndroidX test.
     *
     * @param arguments [Bundle] of arguments
     * @param withDialog Lambda performing test
     */
    protected fun doTestWithDialog(
        arguments: Bundle,
        withDialog: (SftpConnectDialog, MaterialDialog) -> Unit,
    ) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            SftpConnectDialog().run {
                this.arguments = arguments
                this.show(activity.supportFragmentManager, SftpConnectDialog.TAG)
                ShadowLooper.runUiThreadTasks()
                assertTrue(ShadowDialog.getLatestDialog().isShowing)
                withDialog.invoke(this, ShadowDialog.getLatestDialog() as MaterialDialog)
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            scenario.close()
        }
    }
}
