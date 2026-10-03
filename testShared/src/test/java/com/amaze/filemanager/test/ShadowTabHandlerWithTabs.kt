package com.amaze.filemanager.test

import android.os.Environment
import com.amaze.filemanager.database.TabHandler
import com.amaze.filemanager.database.models.explorer.Tab
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Completable
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(TabHandler::class)
class ShadowTabHandlerWithTabs {
    companion object {
        @JvmStatic @Implementation
        fun getInstance(): TabHandler {
            val retval = mockk<TabHandler>()
            val home = Environment.getExternalStorageDirectory().absolutePath
            val tab1 = Tab(1, home, home)
            val tab2 = Tab(2, home, home)
            every { retval.addTab(any()) } returns Completable.fromCallable { true }
            every { retval.update(any()) } returns Unit
            every { retval.findTab(1) } returns tab1
            every { retval.findTab(2) } returns tab2
            every { retval.allTabs } returns arrayOf(tab1, tab2)
            return retval
        }
    }
}
