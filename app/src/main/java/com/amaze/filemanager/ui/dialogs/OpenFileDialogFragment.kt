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

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MotionEventCompat
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.AppsAdapter
import com.amaze.filemanager.adapters.data.AppDataParcelable
import com.amaze.filemanager.adapters.data.OpenFileParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.FragmentOpenFileDialogBinding
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity
import com.amaze.filemanager.ui.activities.superclasses.PreferenceActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.base.BaseBottomSheetFragment
import com.amaze.filemanager.ui.icons.MimeTypes
import com.amaze.filemanager.ui.provider.UtilitiesProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior

class OpenFileDialogFragment : BaseBottomSheetFragment() {

    private var uri: Uri? = null
    private var mimeType: String? = null
    private var useNewStack: Boolean? = null

    private lateinit var viewBinding: FragmentOpenFileDialogBinding
    private lateinit var adapter: AppsAdapter
    private lateinit var utilsProvider: UtilitiesProvider
    private lateinit var sharedPreferences: SharedPreferences

    companion object {

        private const val KEY_URI = "uri"
        private const val KEY_MIME_TYPE = "mime_type"
        private const val KEY_USE_NEW_STACK = "use_new_stack"
        private const val KEY_PREFERENCES_DEFAULT = "_DEFAULT"
        const val KEY_PREFERENCES_LAST = "_LAST"

        fun openFileOrShow(
            uri: Uri,
            mimeType: String,
            useNewStack: Boolean,
            activity: PreferenceActivity,
            forceChooser: Boolean
        ) {
            if (mimeType == "*/*" || forceChooser || !getPreferenceAndStartActivity(
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
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
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
            uri: Uri, mimeType: String, useNewStack: Boolean, activity: PreferenceActivity
        ): Boolean {
            val classAndPackageRaw = activity.prefs.getString(
                mimeType.plus(
                    KEY_PREFERENCES_DEFAULT
                ), null
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

        fun setLastOpenedApp(
            appDataParcelable: AppDataParcelable, preferenceActivity: PreferenceActivity
        ) {
            preferenceActivity.prefs.edit().putString(
                appDataParcelable.openFileParcelable.mimeType.plus(KEY_PREFERENCES_LAST),
                String.format(
                    "%s %s",
                    appDataParcelable.openFileParcelable.className,
                    appDataParcelable.openFileParcelable.packageName
                )
            ).apply()
        }

        fun setDefaultOpenedApp(
            appDataParcelable: AppDataParcelable, preferenceActivity: PreferenceActivity
        ) {
            preferenceActivity.prefs.edit().putString(
                appDataParcelable.openFileParcelable.mimeType.plus(KEY_PREFERENCES_DEFAULT),
                String.format(
                    "%s %s",
                    appDataParcelable.openFileParcelable.className,
                    appDataParcelable.openFileParcelable.packageName
                )
            ).apply()
        }

        fun clearPreferences(sharedPreferences: SharedPreferences) {
            val keys = HashSet<String>()
            sharedPreferences.all.keys.forEach {
                if (it.endsWith(KEY_PREFERENCES_DEFAULT) || it.endsWith(KEY_PREFERENCES_LAST)) {
                    keys.add(it)
                }
            }
            keys.forEach {
                sharedPreferences.edit().remove(it).apply()
            }
        }

        private fun clearMimeTypePreference(
            mimeType: String, sharedPreferences: SharedPreferences
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentOpenFileDialogBinding.inflate(inflater)
        initDialogResources(viewBinding.parent)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        adapter = AppsAdapter(
            this,
            activity as ThemedActivity?,
            utilsProvider,
            null,
            null,
            R.layout.rowlayout,
            (activity as MainActivity).prefs,
            true
        )

        val packageManager = context!!.packageManager
        val intent = buildIntent(
            uri!!, mimeType!!, useNewStack!!, null, null
        )
        val appDataParcelableList: ArrayList<AppDataParcelable> = ArrayList()
        val lastClassAndPackageRaw = sharedPreferences
            .getString(mimeType.plus(KEY_PREFERENCES_LAST), null)
        val lastClassAndPackage = lastClassAndPackageRaw?.split(" ")
        var lastAppData: AppDataParcelable? = null
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).forEach {
            val openFileParcelable = OpenFileParcelable(
                uri, mimeType, useNewStack, it.activityInfo.name, it.activityInfo.packageName
            )
            val label = it.loadLabel(packageManager).toString()
            val appDataParcelable = AppDataParcelable(
                if (label.isNotEmpty()) label else it.activityInfo.packageName,
                null,
                it.activityInfo.packageName,
                null,
                null,
                0,
                0,
                openFileParcelable
            )
            appDataParcelableList.add(appDataParcelable)
        }
        try {
            lastAppData = if (!lastClassAndPackage.isNullOrEmpty()) {
                appDataParcelableList.find {
                    it.openFileParcelable.className == lastClassAndPackage[0]
                }
            } else {
                appDataParcelableList[0]
            }
            appDataParcelableList.remove(lastAppData)
        } catch (e: Exception) {
            FileUtils.openWith(uri, activity as PreferenceActivity, useNewStack!!)
            dismiss()
        }

        lastAppData?.let {
            val lastAppIntent = buildIntent(
                it.openFileParcelable.uri!!,
                it.openFileParcelable.mimeType!!,
                it.openFileParcelable.useNewStack!!,
                it.openFileParcelable.className,
                it.openFileParcelable.packageName
            )

            viewBinding.run {
                appsListView.adapter = adapter
                lastAppTitle.text = it.label
                lastAppImage.setImageDrawable(
                    (activity as MainActivity).packageManager.getApplicationIcon(it.packageName)
                )
                justOnceButton.setTextColor((activity as ThemedActivity).accent)
                justOnceButton.setOnClickListener { _ ->
                    setLastOpenedApp(it, activity as PreferenceActivity)
                    requireContext().startActivity(lastAppIntent)
                }
                alwaysButton.setTextColor((activity as ThemedActivity).accent)
                alwaysButton.setOnClickListener { _ ->
                    setDefaultOpenedApp(it, activity as PreferenceActivity)
                    requireContext().startActivity(lastAppIntent)
                }
            }
        }
        viewBinding.openAsButton.setOnClickListener {
            FileUtils.openWith(uri, activity as PreferenceActivity, useNewStack!!)
            dismiss()
        }
        adapter.setData(appDataParcelableList)
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }
}
