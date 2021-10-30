package com.amaze.filemanager.ui.views.appbar

import android.view.View
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity
import com.google.android.material.bottomappbar.BottomAppBar

class BottomAppBar(mainActivity: MainActivity) {

    var bottomAppBar: BottomAppBar? = null

    init {
        bottomAppBar = mainActivity.findViewById(R.id.bottomAppBar)
        bottomAppBar?.setOnMenuItemClickListener {
            mainActivity.actionModeHelper.mActionModeCallback
                .onActionItemClicked(mainActivity.actionModeHelper.actionMode, it)
        }
    }

    fun showBottomAppBar() {
        bottomAppBar?.visibility = View.VISIBLE
    }

    fun hideBottomAppBar() {
        bottomAppBar?.visibility = View.GONE
    }
}