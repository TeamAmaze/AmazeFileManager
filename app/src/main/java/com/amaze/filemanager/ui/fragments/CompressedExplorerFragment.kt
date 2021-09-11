/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.fragments

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.CompressedExplorerAdapter
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask
import com.amaze.filemanager.asynchronous.services.ExtractService
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.colors.ColorPreferenceHelper
import com.amaze.filemanager.ui.fragments.data.CompressedExplorerFragmentViewModel
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.ui.views.DividerItemDecoration
import com.amaze.filemanager.ui.views.FastScroller
import com.amaze.filemanager.utils.BottomBarButtonPath
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import com.amaze.filemanager.utils.Utils
import com.github.junrar.exception.UnsupportedRarV5Exception
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

@Suppress("TooManyFunctions")
class CompressedExplorerFragment : Fragment(), BottomBarButtonPath {
    @JvmField
    var compressedFile: File? = null

    private val viewModel: CompressedExplorerFragmentViewModel by viewModels()
    /**
     * files to be deleted from cache with a Map maintaining key - the root of directory created (for
     * deletion purposes after we exit out of here and value - the path of file to open
     */
    @JvmField
    var files: ArrayList<HybridFileParcelable>? = null
    @JvmField
    var selection = false

    // Normally this would be "/" but for pathing issues it isn't
    var relativeDirectory = ""

    @JvmField
    @ColorInt
    var accentColor = 0

    @JvmField
    @ColorInt
    var iconskin = 0
    var compressedExplorerAdapter: CompressedExplorerAdapter? = null
    @JvmField
    var mActionMode: ActionMode? = null
    @JvmField
    var coloriseIcons = false
    @JvmField
    var showSize = false
    @JvmField
    var showLastModified = false
    var gobackitem = false
    var mainActivity: MainActivity? = null
    var listView: RecyclerView? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null
    @JvmField
    var isOpen = false // flag states whether to open file after service extracts it
    private var fastScroller: FastScroller? = null
    private var decompressor: Decompressor? = null
    private var rootView: View? = null
    private var addheader = true
    private var mLayoutManager: LinearLayoutManager? = null
    private var dividerItemDecoration: DividerItemDecoration? = null
    private var showDividers = false
    private var mToolbarContainer: View? = null
    private var stopAnims = true
    private var file = 0
    private var folder = 0
    private var isCachedCompressedFile = false
    private val offsetListenerForToolbar =
        OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
            fastScroller?.updateHandlePosition(verticalOffset, 112)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.main_frag, container, false)
        mainActivity = requireActivity() as MainActivity
        listView = rootView!!.findViewById(R.id.listView)
        listView!!.setOnTouchListener { _: View?, _: MotionEvent? ->
            compressedExplorerAdapter?.apply {
                if (stopAnims && !this.stoppedAnimation) {
                    stopAnim()
                }
                this.stoppedAnimation = true
                stopAnims = false
            }
            false
        }
        swipeRefreshLayout = rootView!!.findViewById(R.id.activity_main_swipe_refresh_layout)
        swipeRefreshLayout!!.apply {
            setOnRefreshListener { refresh() }
            isRefreshing = true
        }
        viewModel.elements.observe(
            viewLifecycleOwner,
            { elements ->
                viewModel.folder?.run {
                    createViews(elements, this)
                    swipeRefreshLayout!!.isRefreshing = false
                    updateBottomBar()
                }
            }
        )
        return rootView
    }

    /**
     * Stop animation at archive file list view.
     */
    fun stopAnim() {
        for (j in 0 until listView!!.childCount) {
            val v = listView!!.getChildAt(j)
            v?.clearAnimation()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sp = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val fileName = prepareCompressedFile(requireArguments().getString(KEY_PATH)!!)
        mToolbarContainer = mainActivity!!.appbar.appbarLayout
        mToolbarContainer!!.setOnTouchListener { _: View?, _: MotionEvent? ->
            if (stopAnims) {
                if (!compressedExplorerAdapter!!.stoppedAnimation) {
                    stopAnim()
                }
                compressedExplorerAdapter!!.stoppedAnimation = true
            }
            stopAnims = false
            false
        }
        listView!!.visibility = View.VISIBLE
        mLayoutManager = LinearLayoutManager(activity)
        listView!!.layoutManager = mLayoutManager
        val utilsProvider = AppConfig.getInstance().utilsProvider
        when (utilsProvider.appTheme) {
            AppTheme.DARK ->
                rootView!!
                    .setBackgroundColor(Utils.getColor(context, R.color.holo_dark_background))
            AppTheme.BLACK ->
                listView!!
                    .setBackgroundColor(Utils.getColor(context, android.R.color.black))
            else ->
                listView!!
                    .setBackgroundColor(Utils.getColor(context, android.R.color.background_light))
        }
        gobackitem = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON, false)
        coloriseIcons = sp.getBoolean(PreferencesConstants.PREFERENCE_COLORIZE_ICONS, true)
        showSize = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE, false)
        showLastModified = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED, true)
        showDividers = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_DIVIDERS, true)
        accentColor = mainActivity!!.accent
        iconskin = mainActivity!!.currentColorPreference.iconSkin

        // mainActivity.findViewById(R.id.buttonbarframe).setBackgroundColor(Color.parseColor(skin));
        if (savedInstanceState == null && compressedFile != null) {
            files = ArrayList()
            // adding a cache file to delete where any user interaction elements will be cached
            val path =
                if (isCachedCompressedFile) {
                    compressedFile!!.absolutePath
                } else {
                    requireActivity().externalCacheDir!!
                        .path + CompressedHelper.SEPARATOR + fileName
                }
            files!!.add(HybridFileParcelable(path))
            decompressor =
                CompressedHelper.getCompressorInstance(requireContext(), compressedFile!!)
            changePath("")
        } else {
            onRestoreInstanceState(savedInstanceState)
        }
        mainActivity!!.supportInvalidateOptionsMenu()
    }

    private fun prepareCompressedFile(pathArg: String): String {
        var fileName: String? = null
        val pathUri = Uri.parse(pathArg)
        if (ContentResolver.SCHEME_CONTENT == pathUri.scheme) {
            requireContext()
                .contentResolver
                .query(
                    pathUri,
                    arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                    null, null, null
                )?.run {
                    try {
                        if (moveToFirst()) {
                            fileName = getString(0)
                            compressedFile = File(requireContext().cacheDir, fileName)
                        } else {
                            // At this point, we know nothing the file the URI represents, we are doing everything
                            // wild guess.
                            compressedFile =
                                File.createTempFile("compressed", null, requireContext().cacheDir)
                                    .also {
                                        fileName = it.name
                                    }
                        }
                        compressedFile!!.deleteOnExit()
                        requireContext().contentResolver.openInputStream(pathUri)!!
                            .copyTo(FileOutputStream(compressedFile), DEFAULT_BUFFER_SIZE)
                        isCachedCompressedFile = true
                    } catch (e: IOException) {
                        Log.e(TAG, "Error opening URI $pathUri for reading", e)
                        AppConfig.toast(
                            requireContext(),
                            requireContext()
                                .getString(
                                    R.string.compressed_explorer_fragment_error_open_uri,
                                    pathUri.toString()
                                )
                        )
                        requireActivity().onBackPressed()
                    } finally {
                        close()
                    }
                }
        } else {
            compressedFile = File(pathUri.path)
            fileName = compressedFile!!.name.substring(0, compressedFile!!.name.lastIndexOf("."))
        }
        return fileName!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(KEY_ELEMENTS, viewModel.elements.value)
        outState.putString(KEY_PATH, relativeDirectory)
        outState.putString(KEY_URI, compressedFile!!.path)
        outState.putParcelableArrayList(KEY_CACHE_FILES, files)
        outState.putBoolean(KEY_OPEN, isOpen)
    }

    private fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        prepareCompressedFile(savedInstanceState!!.getString(KEY_URI)!!)
        files = savedInstanceState.getParcelableArrayList(KEY_CACHE_FILES)
        isOpen = savedInstanceState.getBoolean(KEY_OPEN)
        relativeDirectory = savedInstanceState.getString(KEY_PATH, "")
        decompressor = CompressedHelper.getCompressorInstance(requireContext(), compressedFile!!)
        viewModel.elements.value = savedInstanceState.getParcelableArrayList(KEY_ELEMENTS)
    }

    @JvmField
    var mActionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        private fun hideOption(id: Int, menu: Menu) {
            val item = menu.findItem(id)
            item.isVisible = false
        }

        private fun showOption(id: Int, menu: Menu) {
            val item = menu.findItem(id)
            item.isVisible = true
        }

        var v: View? = null

        // called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater = mode.menuInflater
            v = activity!!.layoutInflater.inflate(R.layout.actionmode, null)
            mode.customView = v
            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu)
            hideOption(R.id.cpy, menu)
            hideOption(R.id.cut, menu)
            hideOption(R.id.delete, menu)
            hideOption(R.id.addshortcut, menu)
            hideOption(R.id.share, menu)
            hideOption(R.id.openwith, menu)
            showOption(R.id.all, menu)
            hideOption(R.id.compress, menu)
            hideOption(R.id.hide, menu)
            showOption(R.id.ex, menu)
            mode.title = getString(R.string.select)
            mainActivity!!.updateViews(
                ColorDrawable(
                    Utils.getColor(
                        context, R.color.holo_dark_action_mode
                    )
                )
            )
            if (Build.VERSION.SDK_INT >= 21) {
                val window = activity!!.window
                if (mainActivity!!
                    .getBoolean(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION)
                ) {
                    window.navigationBarColor =
                        Utils.getColor(context, android.R.color.black)
                }
            }
            if (Build.VERSION.SDK_INT < 19) {
                mainActivity!!.appbar.toolbar.visibility = View.GONE
            }
            return true
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val positions = compressedExplorerAdapter!!.checkedItemPositions
            (v!!.findViewById<View>(R.id.item_count) as TextView).text =
                positions.size.toString() + ""
            menu.findItem(R.id.all)
                .setTitle(
                    if (positions.size == folder + file) {
                        R.string.deselect_all
                    } else {
                        R.string.select_all
                    }
                )
            return false // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.all -> {
                    val positions = compressedExplorerAdapter!!.checkedItemPositions
                    val shouldDeselectAll = positions.size != folder + file
                    compressedExplorerAdapter!!.toggleChecked(shouldDeselectAll)
                    mode.invalidate()
                    item.setTitle(
                        if (shouldDeselectAll) {
                            R.string.deselect_all
                        } else {
                            R.string.select_all
                        }
                    )
                    if (!shouldDeselectAll) {
                        selection = false
                        mActionMode!!.finish()
                        mActionMode = null
                    }
                    return true
                }
                R.id.ex -> {
                    Toast.makeText(activity, getString(R.string.extracting), Toast.LENGTH_SHORT)
                        .show()
                    val dirs = arrayOfNulls<String>(
                        compressedExplorerAdapter!!.checkedItemPositions.size
                    )
                    var i = 0
                    while (i < dirs.size) {
                        dirs[i] =
                            viewModel
                                .elements
                                .value!![compressedExplorerAdapter!!.checkedItemPositions[i]].path
                        i++
                    }
                    decompressor!!.decompress(compressedFile!!.path, dirs)
                    mode.finish()
                    return true
                }
            }
            return false
        }

        override fun onDestroyActionMode(actionMode: ActionMode) {
            if (compressedExplorerAdapter != null) {
                compressedExplorerAdapter!!.toggleChecked(false)
            }
            @ColorInt val primaryColor = ColorPreferenceHelper.getPrimary(
                mainActivity!!.currentColorPreference, MainActivity.currentTab
            )
            selection = false
            mainActivity!!.updateViews(ColorDrawable(primaryColor))
            if (Build.VERSION.SDK_INT >= 21) {
                val window = activity!!.window
                if (mainActivity!!
                    .getBoolean(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION)
                ) {
                    window.navigationBarColor =
                        mainActivity!!.skinStatusBar
                }
            }
            mActionMode = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clearing the touch listeners allows the fragment to
        // be cleaned after it is destroyed, preventing leaks
        mToolbarContainer!!.setOnTouchListener(null)
        (mToolbarContainer as AppBarLayout?)!!.removeOnOffsetChangedListener(
            offsetListenerForToolbar
        )
        mainActivity!!.supportInvalidateOptionsMenu()

        // needed to remove any extracted file from cache, when onResume was not called
        // in case of opening any unknown file inside the zip
        if (true == files?.isNotEmpty() && files!![0].exists()) {
            DeleteTask(requireActivity(), this).execute(files)
        }
        if (isCachedCompressedFile) {
            compressedFile!!.delete()
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivity!!.fab.hide()
        val intent = Intent(activity, ExtractService::class.java)
        requireActivity().bindService(intent, mServiceConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unbindService(mServiceConnection)
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        @Suppress("EmptyFunctionBlock")
        override fun onServiceConnected(name: ComponentName, service: IBinder) {}
        override fun onServiceDisconnected(name: ComponentName) {
            // open file if pending
            if (isOpen) {
                // open most recent entry added to files to be deleted from cache
                val cacheFile = File(files!![files!!.size - 1].path)
                if (cacheFile.exists()) {
                    FileUtils.openFile(cacheFile, mainActivity, mainActivity!!.prefs)
                }
                // reset the flag and cache file, as it's root is already in the list for deletion
                isOpen = false
                files!!.removeAt(files!!.size - 1)
            }
        }
    }

    override fun changePath(folderArg: String?) {
        var folder = folderArg
        if (folder == null) folder = ""
        if (folder.startsWith("/")) folder = folder.substring(1)
        val addGoBackItem = gobackitem && !isRoot(folder)
        if (decompressor != null) {
            decompressor!!
                .changePath(
                    folder,
                    addGoBackItem,
                    object :
                        OnAsyncTaskFinished<AsyncTaskResult<
                                ArrayList<CompressedObjectParcelable>>> {
                        override fun onAsyncTaskFinished(
                            data:
                                AsyncTaskResult<ArrayList<CompressedObjectParcelable>>
                        ) {
                            if (data.exception == null) {
                                viewModel.elements.postValue(data.result)
                                viewModel.folder = folder
                            } else {
                                archiveCorruptOrUnsupportedToast(data.exception)
                            }
                        }
                    }
                )
                .execute()
            swipeRefreshLayout!!.isRefreshing = true
            updateBottomBar()
        } else {
            archiveCorruptOrUnsupportedToast(null)
        }
    }

    override val path: String
        get() = if (!isRootRelativePath) {
            CompressedHelper.SEPARATOR + relativeDirectory
        } else {
            ""
        }

    override val rootDrawable: Int
        get() = R.drawable.ic_compressed_white_24dp

    private fun refresh() {
        changePath(relativeDirectory)
    }

    private fun updateBottomBar() {
        val path =
            if (!isRootRelativePath) {
                compressedFile!!.name + CompressedHelper.SEPARATOR + relativeDirectory
            } else {
                compressedFile!!.name
            }
        mainActivity
            ?.getAppbar()
            ?.bottomBar
            ?.updatePath(path, false, null, OpenMode.FILE, folder, file, this)
    }

    private fun createViews(items: List<CompressedObjectParcelable>?, dir: String) {
        if (compressedExplorerAdapter == null) {
            compressedExplorerAdapter = CompressedExplorerAdapter(
                activity,
                AppConfig.getInstance().utilsProvider,
                items,
                this,
                decompressor,
                PreferenceManager.getDefaultSharedPreferences(activity)
            )
            listView!!.adapter = compressedExplorerAdapter
        } else {
            compressedExplorerAdapter!!.generateZip(items)
        }
        folder = 0
        file = 0
        for (item in items!!) {
            if (item.type == CompressedObjectParcelable.TYPE_GOBACK) continue
            if (item.directory) folder++ else file++
        }
        stopAnims = true
        if (!addheader) {
            listView!!.removeItemDecoration(dividerItemDecoration!!)
            // listView.removeItemDecoration(headersDecor);
            addheader = true
        } else {
            dividerItemDecoration = DividerItemDecoration(activity, true, showDividers)
            listView!!.addItemDecoration(dividerItemDecoration!!)
            // headersDecor = new StickyRecyclerHeadersDecoration(compressedExplorerAdapter);
            // listView.addItemDecoration(headersDecor);
            addheader = false
        }
        fastScroller = rootView!!.findViewById(R.id.fastscroll)
        fastScroller!!.setRecyclerView(listView!!, 1)
        fastScroller!!.setPressedHandleColor(mainActivity!!.accent)
        (mToolbarContainer as AppBarLayout?)!!.addOnOffsetChangedListener(offsetListenerForToolbar)
        listView!!.stopScroll()
        relativeDirectory = dir
        updateBottomBar()
        swipeRefreshLayout!!.isRefreshing = false
    }

    /**
     * Indicator whether navigation through back button is possible.
     */
    fun canGoBack(): Boolean {
        return !isRootRelativePath
    }

    /**
     * Go one level up in the archive hierarchy.
     */
    fun goBack() {
        changePath(File(relativeDirectory).parent)
    }

    private val isRootRelativePath: Boolean
        get() = isRoot(relativeDirectory)

    private fun isRoot(folder: String?): Boolean {
        return folder == null || folder.isEmpty()
    }

    private fun archiveCorruptOrUnsupportedToast(e: Throwable?) {
        @StringRes val msg =
            if (e?.cause != null && UnsupportedRarV5Exception::class.java.isAssignableFrom(
                    e.cause!!.javaClass
                )
            ) R.string.error_unsupported_v5_rar else R.string.archive_unsupported_or_corrupt
        Toast.makeText(
            activity,
            requireContext().getString(msg, compressedFile!!.absolutePath),
            Toast.LENGTH_LONG
        )
            .show()
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }

    companion object {
        const val KEY_PATH = "path"
        private const val KEY_CACHE_FILES = "cache_files"
        private const val KEY_URI = "uri"
        private const val KEY_ELEMENTS = "elements"
        private const val KEY_OPEN = "is_open"
        private val TAG = CompressedExplorerFragment::class.java.simpleName
    }
}
