/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.AppsRecyclerAdapter
import com.amaze.filemanager.adapters.data.AppDataParcelable
import com.amaze.filemanager.adapters.glide.AppsAdapterPreloadModel
import com.amaze.filemanager.adapters.holders.AppHolder
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.FragmentOpenFileDialogBinding
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.PermissionsActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.base.BaseBottomSheetFragment
import com.amaze.filemanager.ui.fragments.AdjustListViewForTv
import com.amaze.filemanager.utils.ANDROID_TERM
import com.amaze.filemanager.utils.GlideConstants
import com.amaze.filemanager.utils.TERMONE_PLUS
import com.amaze.filemanager.utils.TERMUX
import com.amaze.filemanager.utils.detectInstalledTerminalApps
import com.amaze.filemanager.utils.getApplicationInfoCompat
import com.amaze.filemanager.utils.getPackageInfoCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import org.slf4j.LoggerFactory

/**
 * Bottom sheet fragment for open folder in terminal app actions.
 *
 * Supports Termux and Termone plus (and possibly its predecessor, Jack Palovich's terminal app).
 */
class OpenFolderInTerminalFragment : BaseBottomSheetFragment(), AdjustListViewForTv<AppHolder> {
    private var fragmentOpenFileDialogBinding: FragmentOpenFileDialogBinding? = null

    @VisibleForTesting
    internal val viewBinding get() = fragmentOpenFileDialogBinding!!

    private lateinit var path: String
    private lateinit var installedTerminals: Array<String>
    private lateinit var adapter: AppsRecyclerAdapter
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private val logger = LoggerFactory.getLogger(OpenFileDialogFragment::class.java)

        const val KEY_PREFERENCES_DEFAULT = "terminal._DEFAULT"
        const val KEY_PREFERENCES_LAST = "terminal._LAST"

        private const val TERMONE_PLUS_PERMISSION = "com.termoneplus.permission.RUN_SCRIPT"
        private const val ANDROID_TERM_PERMISSION = "jackpal.androidterm.permission.RUN_SCRIPT"
        private const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"

        @SuppressLint("SdCardPath")
        private const val TERMUX_SHELL_LOCATION = "/data/data/com.termux/files/usr/bin/bash"

        /**
         * Public facing method. Opens this sheet fragment for user to choose the terminal app.
         *
         * Supports Termux, Jack Palovich's terminal app and Termone plus.
         */
        fun openTerminalOrShow(
            path: String,
            activity: MainActivity,
        ) {
            val installedTerminals = activity.detectInstalledTerminalApps()
            if (installedTerminals.isEmpty()) {
                AppConfig.toast(activity, "No Terminal App installed")
            } else if (installedTerminals.size == 1) {
                startActivity(activity, buildIntent(installedTerminals.first(), path))
            } else {
                val packageName = activity.prefs.getString(KEY_PREFERENCES_DEFAULT, null)
                if (true == packageName?.isNotEmpty()) {
                    startActivity(activity, buildIntent(packageName, path))
                } else {
                    newInstance(path, installedTerminals).show(
                        activity.supportFragmentManager,
                        OpenFolderInTerminalFragment::class.java.simpleName,
                    )
                }
            }
        }

        private fun newInstance(
            path: String,
            installedTerminals: Array<String>,
        ): OpenFolderInTerminalFragment {
            val retval = OpenFolderInTerminalFragment()
            retval.path = path
            retval.installedTerminals = installedTerminals
            retval.arguments =
                Bundle().also {
                    it.putString("path", path)
                }
            return retval
        }

        private fun startActivity(
            context: PermissionsActivity,
            intent: Intent,
        ) {
            if (TERMUX == intent.component?.packageName) {
                context.requestTerminalPermission(TERMUX_PERMISSION) {
                    ContextCompat.startForegroundService(context, intent)
                }
            } else if (TERMONE_PLUS == intent.component?.packageName) {
                context.requestTerminalPermission(TERMONE_PLUS_PERMISSION) {
                    ContextCompat.startActivity(context, intent, null)
                }
            } else if (ANDROID_TERM == intent.component?.packageName) {
                context.requestTerminalPermission(ANDROID_TERM_PERMISSION) {
                    ContextCompat.startActivity(context, intent, null)
                }
            } else {
                logger.error(
                    "Invalid intent - intent.component is null or package name supported: ${intent.component?.packageName}",
                )
            }
        }

        private fun buildIntent(
            packageName: String,
            path: String,
        ): Intent {
            return when (packageName) {
                TERMONE_PLUS -> {
                    Intent().also {
                        it.action = "$TERMONE_PLUS.RUN_SCRIPT"
                        it.setClassName(TERMONE_PLUS, "$ANDROID_TERM.RunScript")
                        it.putExtra("$TERMONE_PLUS.Command", "cd \"$path\"")
                    }
                }

                ANDROID_TERM -> {
                    Intent().also {
                        it.action = "$ANDROID_TERM.RUN_SCRIPT"
                        it.setClassName(ANDROID_TERM, "$ANDROID_TERM.RunScript")
                        it.putExtra("$ANDROID_TERM.iInitialCommand", "cd \"$path\"")
                    }
                }

                TERMUX -> {
                    Intent().also {
                        it.setClassName(TERMUX, "$TERMUX.app.RunCommandService")
                        it.setAction("$TERMUX.RUN_COMMAND")
                        it.putExtra(
                            "$TERMUX.RUN_COMMAND_PATH",
                            TERMUX_SHELL_LOCATION,
                        )
                        it.putExtra("$TERMUX.RUN_COMMAND_WORKDIR", path)
                    }
                }
                else -> throw IllegalArgumentException("Unsupported package: $packageName")
            }
        }

        /**
         * Sets last open app preference for bottom sheet file chooser.
         * Next time same mime type comes, this app will be shown on top of the list if present
         */
        fun setLastOpenedApp(
            appDataParcelable: AppDataParcelable,
            sharedPreferences: SharedPreferences,
        ) {
            sharedPreferences.edit().putString(
                KEY_PREFERENCES_LAST,
                appDataParcelable.packageName,
            ).apply()
        }

        /**
         * Sets default app for mime type selected using 'Always' button from bottom sheet
         */
        private fun setDefaultOpenedApp(
            appDataParcelable: AppDataParcelable,
            sharedPreferences: SharedPreferences,
        ) {
            sharedPreferences.edit().putString(
                KEY_PREFERENCES_DEFAULT,
                appDataParcelable.packageName,
            ).apply()
        }

        /**
         * Clears all default apps set preferences for mime types
         */
        fun clearPreferences(sharedPreferences: SharedPreferences) {
            AppConfig.getInstance().runInBackground {
                arrayOf(KEY_PREFERENCES_DEFAULT, KEY_PREFERENCES_LAST).forEach {
                    sharedPreferences.edit().remove(it).apply()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.appBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        fragmentOpenFileDialogBinding = FragmentOpenFileDialogBinding.inflate(inflater)
        initDialogResources(viewBinding.parent)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentOpenFileDialogBinding = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val modelProvider = AppsAdapterPreloadModel(this, true)
        val sizeProvider = ViewPreloadSizeProvider<String>()
        val preloader =
            RecyclerViewPreloader(
                Glide.with(this),
                modelProvider,
                sizeProvider,
                GlideConstants.MAX_PRELOAD_TERMINAL_APPS,
            )
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val appDataParcelableList = initList()
        val lastClassAndPackage =
            sharedPreferences
                .getString(KEY_PREFERENCES_LAST, null)
        val lastAppData: AppDataParcelable =
            initLastAppData(
                lastClassAndPackage,
                appDataParcelableList,
            ) ?: return

        adapter =
            AppsRecyclerAdapter(
                this,
                modelProvider,
                true,
                this,
                appDataParcelableList,
            ) { rowItem ->
                setLastOpenedApp(rowItem, sharedPreferences)
                startActivity(
                    requireActivity() as PermissionsActivity,
                    buildIntent(rowItem.packageName, path),
                )
                dismiss()
            }
        loadViews(lastAppData)
        viewBinding.appsRecyclerView.addOnScrollListener(preloader)
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    private fun initList(): MutableList<AppDataParcelable> {
        val packageManager = requireContext().packageManager
        val appDataParcelableList: MutableList<AppDataParcelable> = ArrayList()
        for (pkg in installedTerminals) {
            kotlin.runCatching {
                packageManager.getPackageInfoCompat(pkg, 0)
            }.onFailure {
                logger.error("Error getting package info for $pkg", it)
            }.getOrNull()?.run {
                packageManager.getApplicationInfoCompat(pkg, 0).let { applicationInfo ->
                    appDataParcelableList.add(
                        AppDataParcelable(
                            packageManager.getApplicationLabel(applicationInfo).toString(),
                            "",
                            null,
                            this.packageName,
                            "",
                            "",
                            0,
                            0, false,
                            null,
                        ),
                    )
                }
            }
        }
        return appDataParcelableList
    }

    private fun initLastAppData(
        lastClassAndPackage: String?,
        appDataParcelableList: MutableList<AppDataParcelable>,
    ): AppDataParcelable? {
        if (appDataParcelableList.size == 0) {
            AppConfig.toast(requireContext(), "No terminal apps available")
            dismiss()
            return null
        }

        if (appDataParcelableList.size == 1) {
            startActivity(buildIntent(appDataParcelableList.first().packageName, path))
        }

        var lastAppData: AppDataParcelable? =
            if (!lastClassAndPackage.isNullOrEmpty()) {
                appDataParcelableList.find {
                    it.packageName == lastClassAndPackage
                }
            } else {
                null
            }
        lastAppData = lastAppData ?: appDataParcelableList[0]
        appDataParcelableList.remove(lastAppData)
        return lastAppData
    }

    private fun loadViews(lastAppData: AppDataParcelable) {
        lastAppData.let {
            val lastAppIntent = buildIntent(lastAppData.packageName, path)

            viewBinding.run {
                appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                appsRecyclerView.adapter = adapter

                lastAppTitle.text = it.label
                lastAppImage.setImageDrawable(
                    requireActivity().packageManager.getApplicationIcon(it.packageName),
                )

                justOnceButton.setTextColor((activity as ThemedActivity).accent)
                justOnceButton.setOnClickListener { _ ->
                    setLastOpenedApp(it, sharedPreferences)
                    startActivity(requireActivity() as PermissionsActivity, lastAppIntent)
                    dismiss()
                }
                alwaysButton.setTextColor((activity as ThemedActivity).accent)
                alwaysButton.setOnClickListener { _ ->
                    setDefaultOpenedApp(it, sharedPreferences)
                    startActivity(requireActivity() as PermissionsActivity, lastAppIntent)
                    dismiss()
                }
            }
        }
    }

    override fun adjustListViewForTv(
        viewHolder: AppHolder,
        mainActivity: MainActivity,
    ) {
        // do nothing
    }
}
