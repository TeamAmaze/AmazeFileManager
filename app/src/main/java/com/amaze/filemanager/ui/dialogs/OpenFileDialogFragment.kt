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

package com.amaze.filemanager.ui.dialogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.filemanager.GlideApp
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.AppsRecyclerAdapter
import com.amaze.filemanager.adapters.data.AppDataParcelable
import com.amaze.filemanager.adapters.data.OpenFileParcelable
import com.amaze.filemanager.adapters.glide.AppsAdapterPreloadModel
import com.amaze.filemanager.adapters.holders.AppHolder
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.FragmentOpenFileDialogBinding
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity
import com.amaze.filemanager.ui.activities.superclasses.PreferenceActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.base.BaseBottomSheetFragment
import com.amaze.filemanager.ui.fragments.AdjustListViewForTv
import com.amaze.filemanager.ui.icons.MimeTypes
import com.amaze.filemanager.ui.provider.UtilitiesProvider
import com.amaze.filemanager.ui.startActivityCatchingSecurityException
import com.amaze.filemanager.ui.views.ThemedTextView
import com.amaze.filemanager.utils.GlideConstants
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider

class OpenFileDialogFragment : BaseBottomSheetFragment(), AdjustListViewForTv<AppHolder> {

    private var uri: Uri? = null
    private var mimeType: String? = null
    private var useNewStack: Boolean? = null
    private var fragmentOpenFileDialogBinding: FragmentOpenFileDialogBinding? = null
    private val viewBinding get() = fragmentOpenFileDialogBinding!!

    private lateinit var adapter: AppsRecyclerAdapter
    private lateinit var utilsProvider: UtilitiesProvider
    private lateinit var sharedPreferences: SharedPreferences

    companion object {

        private val TAG = OpenFileDialogFragment::class.java.simpleName

        private const val KEY_URI = "uri"
        private const val KEY_MIME_TYPE = "mime_type"
        private const val KEY_USE_NEW_STACK = "use_new_stack"
        private const val KEY_PREFERENCES_DEFAULT = "_DEFAULT"
        const val KEY_PREFERENCES_LAST = "_LAST"

        /**
         * Opens the file using previously set default app or shows a bottom sheet dialog
         */
        fun openFileOrShow(
            uri: Uri,
            mimeType: String,
            useNewStack: Boolean,
            activity: PreferenceActivity,
            forceChooser: Boolean
        ) {
            if (mimeType == MimeTypes.ALL_MIME_TYPES ||
                forceChooser ||
                !getPreferenceAndStartActivity(
                        uri, mimeType, useNewStack, activity
                    )
            ) {
                if (forceChooser) {
                    clearMimeTypePreference(
                        MimeTypes.getMimeType(uri.toString(), false), activity.prefs
                    )
                }
                val openFileDialogFragment = newInstance(uri, mimeType, useNewStack)
                openFileDialogFragment.show(
                    activity.supportFragmentManager, javaClass.simpleName
                )
            }
        }

        private fun newInstance(uri: Uri, mimeType: String, useNewStack: Boolean):
            OpenFileDialogFragment {
                val args = Bundle()

                val fragment = OpenFileDialogFragment()
                args.putParcelable(KEY_URI, uri)
                args.putString(KEY_MIME_TYPE, mimeType)
                args.putBoolean(KEY_USE_NEW_STACK, useNewStack)
                fragment.arguments = args
                return fragment
            }

        private fun startActivity(context: Context, intent: Intent) {
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e(javaClass.simpleName, e.message, e)
                Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
                throw e
            }
        }

        /**
         * Builds an intent which necessary permission flags for external apps to open uri file
         */
        fun buildIntent(
            uri: Uri,
            mimeType: String,
            useNewStack: Boolean,
            className: String?,
            packageName: String?
        ): Intent {
            val chooserIntent = Intent()
            chooserIntent.action = Intent.ACTION_VIEW
            chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            chooserIntent.setDataAndType(uri, mimeType)

            if (useNewStack) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                } else {
                    chooserIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_TASK_ON_HOME
                    )
                }
            }
            className?.run {
                packageName?.run {
                    chooserIntent.setClassName(packageName, className)
                }
            }
            return chooserIntent
        }

        private fun getPreferenceAndStartActivity(
            uri: Uri,
            mimeType: String,
            useNewStack: Boolean,
            activity: PreferenceActivity
        ): Boolean {
            val classAndPackageRaw = activity.prefs.getString(
                mimeType.plus(
                    KEY_PREFERENCES_DEFAULT
                ),
                null
            )
            var result = false
            if (!classAndPackageRaw.isNullOrEmpty()) {
                try {
                    val classNameAndPackageName = classAndPackageRaw.split(" ")
                    val intent = buildIntent(
                        uri,
                        mimeType,
                        useNewStack,
                        classNameAndPackageName[0],
                        classNameAndPackageName[1]
                    )
                    startActivity(activity, intent)
                    result = true
                } catch (e: ActivityNotFoundException) {
                    activity.prefs.edit().putString(
                        mimeType.plus(KEY_PREFERENCES_DEFAULT), null
                    ).apply()
                }
            }
            return result
        }

        /**
         * Sets last open app preference for bottom sheet file chooser.
         * Next time same mime type comes, this app will be shown on top of the list if present
         */
        fun setLastOpenedApp(
            appDataParcelable: AppDataParcelable,
            preferenceActivity: PreferenceActivity
        ) {
            preferenceActivity.prefs.edit().putString(
                appDataParcelable.openFileParcelable?.mimeType.plus(KEY_PREFERENCES_LAST),
                String.format(
                    "%s %s",
                    appDataParcelable.openFileParcelable?.className,
                    appDataParcelable.openFileParcelable?.packageName
                )
            ).apply()
        }

        /**
         * Sets default app for mime type selected using 'Always' button from bottom sheet
         */
        private fun setDefaultOpenedApp(
            appDataParcelable: AppDataParcelable,
            preferenceActivity: PreferenceActivity
        ) {
            preferenceActivity.prefs.edit().putString(
                appDataParcelable.openFileParcelable?.mimeType.plus(KEY_PREFERENCES_DEFAULT),
                String.format(
                    "%s %s",
                    appDataParcelable.openFileParcelable?.className,
                    appDataParcelable.openFileParcelable?.packageName
                )
            ).apply()
        }

        /**
         * Clears all default apps set preferences for mime types
         */
        fun clearPreferences(sharedPreferences: SharedPreferences) {
            AppConfig.getInstance().runInBackground {
                val keys = HashSet<String>()
                sharedPreferences.all.keys.forEach {
                    if (it.endsWith(KEY_PREFERENCES_DEFAULT) ||
                        it.endsWith(KEY_PREFERENCES_LAST)
                    ) {
                        keys.add(it)
                    }
                }
                keys.forEach {
                    sharedPreferences.edit().remove(it).apply()
                }
            }
        }

        private fun clearMimeTypePreference(
            mimeType: String,
            sharedPreferences: SharedPreferences
        ) {
            sharedPreferences.edit().remove(mimeType.plus(KEY_PREFERENCES_DEFAULT)).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = arguments?.getParcelable(KEY_URI)
        mimeType = arguments?.getString(KEY_MIME_TYPE)
        useNewStack = arguments?.getBoolean(KEY_USE_NEW_STACK)
        utilsProvider = (activity as BasicActivity?)!!.utilsProvider
        setStyle(STYLE_NORMAL, R.style.appBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentOpenFileDialogBinding = FragmentOpenFileDialogBinding.inflate(inflater)
        initDialogResources(viewBinding.parent)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentOpenFileDialogBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modelProvider = AppsAdapterPreloadModel(this, true)
        val sizeProvider = ViewPreloadSizeProvider<String>()
        var preloader = RecyclerViewPreloader(
            GlideApp.with(this), modelProvider, sizeProvider, GlideConstants.MAX_PRELOAD_FILES
        )
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val intent = buildIntent(
            uri!!, mimeType!!, useNewStack!!, null, null
        )
        val appDataParcelableList = initAppDataParcelableList(intent)
        val lastClassAndPackageRaw = sharedPreferences
            .getString(mimeType.plus(KEY_PREFERENCES_LAST), null)
        val lastClassAndPackage = lastClassAndPackageRaw?.split(" ")
        val lastAppData: AppDataParcelable = initLastAppData(
            lastClassAndPackage, appDataParcelableList
        ) ?: return

        adapter = AppsRecyclerAdapter(
            this,
            modelProvider,
            true, this, appDataParcelableList
        )
        modelProvider.setItemList(
            appDataParcelableList.map { appDataParcelable ->
                appDataParcelable.packageName
            }
        )
        loadViews(lastAppData)

        viewBinding.appsRecyclerView.addOnScrollListener(preloader)
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    private fun loadViews(lastAppData: AppDataParcelable) {
        lastAppData.let {
            val lastAppIntent = buildIntent(
                it.openFileParcelable?.uri!!,
                it.openFileParcelable?.mimeType!!,
                it.openFileParcelable?.useNewStack!!,
                it.openFileParcelable?.className,
                it.openFileParcelable?.packageName
            )

            viewBinding.run {
                appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                appsRecyclerView.adapter = adapter
                lastAppTitle.text = it.label
                lastAppImage.setImageDrawable(
                    (activity as MainActivity).packageManager.getApplicationIcon(it.packageName)
                )
                justOnceButton.setTextColor((activity as ThemedActivity).accent)
                justOnceButton.setOnClickListener { _ ->
                    setLastOpenedApp(it, activity as PreferenceActivity)
                    requireContext().startActivityCatchingSecurityException(lastAppIntent)
                }
                alwaysButton.setTextColor((activity as ThemedActivity).accent)
                alwaysButton.setOnClickListener { _ ->
                    setDefaultOpenedApp(it, activity as PreferenceActivity)
                    requireContext().startActivityCatchingSecurityException(lastAppIntent)
                }
                openAsButton.setOnClickListener {
                    FileUtils.openWith(uri, activity as PreferenceActivity, useNewStack!!)
                    dismiss()
                }
                ThemedTextView.setTextViewColor(lastAppTitle, requireContext())
                ThemedTextView.setTextViewColor(chooseDifferentAppTextView, requireContext())
            }
        }
    }

    private fun initAppDataParcelableList(intent: Intent): MutableList<AppDataParcelable> {
        val packageManager = requireContext().packageManager
        val appDataParcelableList: MutableList<AppDataParcelable> = ArrayList()
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).forEach {
            val openFileParcelable = OpenFileParcelable(
                uri, mimeType, useNewStack, it.activityInfo.name, it.activityInfo.packageName
            )
            val label = it.loadLabel(packageManager).toString()
            val appDataParcelable =
                AppDataParcelable(
                    if (label.isNotEmpty()) label else it.activityInfo.packageName,
                    "",
                    it.activityInfo.packageName,
                    "",
                    "",
                    0,
                    0, false,
                    openFileParcelable
                )
            appDataParcelableList.add(appDataParcelable)
        }
        return appDataParcelableList
    }

    private fun initLastAppData(
        lastClassAndPackage: List<String>?,
        appDataParcelableList: MutableList<AppDataParcelable>
    ): AppDataParcelable? {
        if (appDataParcelableList.size == 0) {
            AppConfig.toast(requireContext(), requireContext().getString(R.string.no_app_found))
            FileUtils.openWith(uri, activity as PreferenceActivity, useNewStack!!)
            dismiss()
            return null
        }

        var lastAppData: AppDataParcelable? = if (!lastClassAndPackage.isNullOrEmpty()) {
            appDataParcelableList.find {
                it.openFileParcelable?.className == lastClassAndPackage[0]
            }
        } else {
            null
        }
        lastAppData = lastAppData ?: appDataParcelableList[0]
        appDataParcelableList.remove(lastAppData)
        return lastAppData
    }

    override fun adjustListViewForTv(viewHolder: AppHolder, mainActivity: MainActivity) {
        // do nothing
    }
}
