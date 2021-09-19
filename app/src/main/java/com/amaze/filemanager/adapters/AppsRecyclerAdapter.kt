/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.AppDataParcelable
import com.amaze.filemanager.adapters.glide.AppsAdapterPreloadModel
import com.amaze.filemanager.adapters.holders.AppHolder
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.asynchronous.services.CopyService
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.PreferenceActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment.Companion.buildIntent
import com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment.Companion.setLastOpenedApp
import com.amaze.filemanager.ui.fragments.AdjustListViewForTv
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import com.amaze.filemanager.ui.startActivityCatchingSecurityException
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.AnimUtils.marqueeAfterDelay
import com.amaze.filemanager.utils.Utils
import com.amaze.filemanager.utils.safeLet
import java.io.File
import java.util.*

class AppsRecyclerAdapter(
    private val fragment: Fragment,
    private val modelProvider: AppsAdapterPreloadModel,
    private val isBottomSheet: Boolean,
    private val adjustListViewCallback: AdjustListViewForTv<AppHolder>,
    private val appDataParcelableList: MutableList<AppDataParcelable>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val myChecked = SparseBooleanArray()
    private val mInflater: LayoutInflater get() = fragment.requireActivity()
        .getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(R.layout.rowlayout, parent, false)
        return AppHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppHolder) {
            val rowItem: AppDataParcelable = appDataParcelableList[position]
            adjustListViewCallback.adjustListViewForTv(
                holder,
                fragment.requireActivity() as MainActivity
            )
            if (isBottomSheet) {
                holder.about.visibility = View.GONE
                holder.txtDesc.text = rowItem.openFileParcelable?.className
                holder.txtDesc.isSingleLine = true
                holder.txtDesc.ellipsize = TextUtils.TruncateAt.MIDDLE
                modelProvider.loadApkImage(rowItem.packageName, holder.apkIcon)
            } else {
                modelProvider.loadApkImage(rowItem.path, holder.apkIcon)
            }
            if (holder.about != null && !isBottomSheet) {
                if ((fragment.requireActivity() as MainActivity).appTheme == AppTheme.LIGHT) {
                    holder.about.setColorFilter(
                        Color.parseColor(
                            "#ff666666"
                        )
                    )
                }
                showPopup(holder.about, rowItem)
            }
            holder.txtTitle.text = rowItem.label
            val enableMarqueeFilename =
                (fragment.requireActivity() as MainActivity)
                    .getBoolean(PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME)
            if (enableMarqueeFilename) {
                holder.txtTitle.ellipsize =
                    if (enableMarqueeFilename) {
                        TextUtils.TruncateAt.MARQUEE
                    } else {
                        TextUtils.TruncateAt.MIDDLE
                    }
                marqueeAfterDelay(2000, holder.txtTitle)
            }

            // 	File f = new File(rowItem.getDesc());
            if (!isBottomSheet) {
                holder.txtDesc.text = rowItem.fileSize
            }
            holder.rl.isClickable = true
            holder.rl.nextFocusRightId = holder.about.id
            holder.rl.setOnClickListener {
                startActivityForRowItem(
                    rowItem
                )
            }
            if (myChecked[position]) {
                holder.rl.setBackgroundColor(
                    Utils.getColor(fragment.context, R.color.appsadapter_background)
                )
            } else {
                if ((fragment.requireActivity() as MainActivity).appTheme == AppTheme.LIGHT) {
                    holder.rl.setBackgroundResource(R.drawable.safr_ripple_white)
                } else {
                    holder.rl.setBackgroundResource(R.drawable.safr_ripple_black)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return appDataParcelableList.size
    }

    fun setData(data: List<AppDataParcelable>, showSystemApps: Boolean) {
        appDataParcelableList.run {
            clear()
            val list = if (!showSystemApps) data.filter { !it.isSystemApp } else data
            addAll(list)
            modelProvider.setItemList(list.map { it.path })
            notifyDataSetChanged()
        }
    }

    private fun startActivityForRowItem(rowItem: AppDataParcelable?) {
        rowItem?.run {
            if (isBottomSheet) {
                val openFileParcelable = rowItem.openFileParcelable
                openFileParcelable?.let {
                    safeLet(
                        openFileParcelable.uri, openFileParcelable.mimeType,
                        openFileParcelable.useNewStack
                    ) {
                        uri, mimeType, useNewStack ->
                        val intent = buildIntent(
                            uri, mimeType, useNewStack,
                            openFileParcelable.className,
                            openFileParcelable.packageName
                        )
                        setLastOpenedApp(
                            rowItem,
                            fragment.requireActivity() as PreferenceActivity
                        )
                        fragment.requireContext().startActivityCatchingSecurityException(intent)
                    }
                }
            } else {
                val i1 = fragment.requireContext().packageManager.getLaunchIntentForPackage(
                    rowItem.packageName
                )
                if (i1 != null) {
                    fragment.startActivity(i1)
                } else {
                    Toast.makeText(
                        fragment.context, fragment.getString(R.string.not_allowed),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    // TODO: Implement this method
                }
            }
        }
    }

    private fun showPopup(v: View, rowItem: AppDataParcelable?) {
        v.setOnClickListener { view: View? ->
            val popupMenu = PopupMenu(
                fragment.context, view
            )
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                val themedActivity: MainActivity = fragment.requireActivity() as MainActivity
                val colorAccent = themedActivity.accent
                when (item.itemId) {
                    R.id.open -> {
                        val i1 = fragment
                            .context
                            ?.packageManager
                            ?.getLaunchIntentForPackage(rowItem!!.packageName)
                        if (i1 != null) fragment.startActivity(i1) else Toast.makeText(
                            fragment.context,
                            fragment.getString(R.string.not_allowed),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.share -> {
                        val arrayList2 =
                            ArrayList<File>()
                        arrayList2.add(File(rowItem!!.path))
                        themedActivity.colorPreference
                        FileUtils.shareFiles(
                            arrayList2,
                            fragment.activity,
                            themedActivity.utilsProvider.appTheme,
                            colorAccent
                        )
                        return@setOnMenuItemClickListener true
                    }
                    R.id.unins -> {
                        val f1 = HybridFileParcelable(rowItem!!.path)
                        f1.mode = OpenMode.ROOT
                        if (rowItem.isSystemApp) {
                            // system package
                            if ((fragment.requireActivity() as MainActivity).getBoolean(
                                    PreferencesConstants.PREFERENCE_ROOTMODE
                                )
                            ) {
                                showDeleteSystemAppDialog(themedActivity, colorAccent, f1)
                            } else {
                                Toast.makeText(
                                    fragment.context,
                                    fragment.getString(R.string.enablerootmde),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        } else {
                            FileUtils.uninstallPackage(
                                rowItem.packageName,
                                fragment.context
                            )
                        }
                        return@setOnMenuItemClickListener true
                    }
                    R.id.play -> {
                        val intent1 =
                            Intent(Intent.ACTION_VIEW)
                        try {
                            intent1.data = Uri.parse(
                                String.format(
                                    "market://details?id=%s",
                                    rowItem!!.packageName
                                )
                            )
                            fragment.startActivity(intent1)
                        } catch (ifPlayStoreNotInstalled: ActivityNotFoundException) {
                            intent1.data = Uri.parse(
                                String.format(
                                    "https://play.google.com/store/apps/details?id=%s",
                                    rowItem!!.packageName
                                )
                            )
                            fragment.startActivity(intent1)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    R.id.properties -> {
                        fragment.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse(
                                    String.format(
                                        "package:%s",
                                        rowItem!!.packageName
                                    )
                                )
                            )
                        )
                        return@setOnMenuItemClickListener true
                    }
                    R.id.backup -> {
                        Toast.makeText(
                            fragment.context,
                            fragment.getString(R.string.copyingapk) +
                                Environment.getExternalStorageDirectory().path +
                                "/app_backup",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        val f = File(rowItem!!.path)
                        val ab =
                            ArrayList<HybridFileParcelable>()
                        val dst = File(
                            Environment.getExternalStorageDirectory()
                                .path + "/app_backup"
                        )
                        if (!dst.exists() || !dst.isDirectory) dst.mkdirs()
                        val intent = Intent(
                            fragment.context,
                            CopyService::class.java
                        )
                        val baseFile =
                            RootHelper.generateBaseFile(f, true)
                        val startIndex = rowItem.packageName.indexOf("_")
                        val subString = rowItem.packageName.substring(startIndex + 1)
                        baseFile.name = (rowItem.label + "_$subString.apk")
                        ab.add(baseFile)
                        intent.putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, ab)
                        intent.putExtra(CopyService.TAG_COPY_TARGET, dst.path)
                        intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, 0)
                        ServiceWatcherUtil.runService(fragment.context, intent)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            popupMenu.inflate(R.menu.app_options)
            popupMenu.show()
        }
    }

    private fun showDeleteSystemAppDialog(
        themedActivity: ThemedActivity,
        colorAccent: Int,
        f1: HybridFileParcelable
    ) {
        val builder1 =
            MaterialDialog.Builder(fragment.requireContext())
        builder1
            .theme(
                themedActivity.appTheme.materialDialogTheme
            )
            .content(fragment.getString(R.string.unin_system_apk))
            .title(fragment.getString(R.string.warning))
            .negativeColor(colorAccent)
            .positiveColor(colorAccent)
            .negativeText(fragment.getString(R.string.no))
            .positiveText(fragment.getString(R.string.yes))
            .onNegative { dialog: MaterialDialog, _: DialogAction? -> dialog.cancel() }
            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                val files =
                    ArrayList<HybridFileParcelable>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val parent =
                        f1.getParent(fragment.context)
                    if (parent != "app" && parent != "priv-app") {
                        val baseFile =
                            HybridFileParcelable(
                                f1.getParent(fragment.context)
                            )
                        baseFile.mode =
                            OpenMode.ROOT
                        files.add(baseFile)
                    } else files.add(f1)
                } else {
                    files.add(f1)
                }
                DeleteTask(fragment.requireContext()).execute(files)
            }
            .build()
            .show()
    }
}
