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
