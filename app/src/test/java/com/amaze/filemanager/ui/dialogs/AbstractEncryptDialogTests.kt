package com.amaze.filemanager.ui.dialogs

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.P
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.test.TestUtils.initializeInternalStorage
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Base class for various tests related to file encryption.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class, ShadowTabHandler::class], sdk = [JELLY_BEAN, KITKAT, P])
abstract class AbstractEncryptDialogTests {

    protected lateinit var scenario: ActivityScenario<MainActivity>

    /**
     * MainActivity setup.
     */
    @Before
    open fun setUp() {
        if (SDK_INT >= N) initializeInternalStorage()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.STARTED)
    }

    /**
     * Post test cleanup.
     */
    @After
    open fun tearDown() {
        scenario.close()
    }
}
