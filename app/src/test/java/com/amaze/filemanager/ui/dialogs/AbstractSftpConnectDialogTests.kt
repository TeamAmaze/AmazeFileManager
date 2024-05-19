package com.amaze.filemanager.ui.dialogs

import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import org.junit.After
import org.junit.Before
import org.mockito.MockedConstruction
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doCallRealMethod

abstract class AbstractSftpConnectDialogTests : AbstractMainActivityTestBase() {
    protected lateinit var mc: MockedConstruction<SftpConnectDialog>

    /**
     * Setups before test.
     */
    @Before
    override fun setUp() {
        super.setUp()
        mc =
            mockConstruction(
                SftpConnectDialog::class.java,
            ) { mock: SftpConnectDialog, _: MockedConstruction.Context? ->
                doCallRealMethod().`when`(mock).arguments = any()
                `when`(mock.arguments).thenCallRealMethod()
            }
    }

    /**
     * Post test cleanups.
     */
    @After
    override fun tearDown() {
        super.tearDown()
        mc.close()
    }

    companion object {
        @JvmStatic
        protected val BUNDLE_KEYS =
            arrayOf(
                "address",
                "port",
                "keypairName",
                "name",
                "username",
                "password",
                "edit",
                "defaultPath",
                "tls",
            )
    }
}
