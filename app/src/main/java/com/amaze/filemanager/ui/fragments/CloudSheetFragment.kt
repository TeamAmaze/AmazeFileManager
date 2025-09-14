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
package com.amaze.filemanager.ui.fragments

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.R
import com.amaze.filemanager.databinding.FragmentSheetCloudBinding
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog
import com.amaze.filemanager.ui.dialogs.SmbSearchDialog
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.Utils
import com.amaze.filemanager.utils.cloud.CloudPluginUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Created by vishal on 18/2/17.
 *
 *
 * Class represents implementation of a new cloud connection sheet dialog
 */
class CloudSheetFragment : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var rootView: View
    private lateinit var mSmbLayout: LinearLayout
    private lateinit var mScpLayout: LinearLayout
    private lateinit var mDropboxLayout: LinearLayout
    private lateinit var mBoxLayout: LinearLayout
    private lateinit var mGoogleDriveLayout: LinearLayout
    private lateinit var mOnedriveLayout: LinearLayout
    private lateinit var mGetCloudLayout: LinearLayout

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val binding = FragmentSheetCloudBinding.inflate(LayoutInflater.from(requireActivity()))
        rootView = binding.root

        val activity = activity as MainActivity?

        when (activity?.appTheme) {
            AppTheme.DARK -> rootView.setBackgroundColor(Utils.getColor(context, R.color.holo_dark_background))
            AppTheme.BLACK -> rootView.setBackgroundColor(Utils.getColor(context, android.R.color.black))
            else -> rootView.setBackgroundColor(Utils.getColor(context, android.R.color.white))
        }

        mSmbLayout = binding.linearLayoutSmb
        mScpLayout = binding.linearLayoutScp
        mBoxLayout = binding.linearLayoutBox
        mDropboxLayout = binding.linearLayoutDropbox
        mGoogleDriveLayout = binding.linearLayoutGoogleDrive
        mOnedriveLayout = binding.linearLayoutOnedrive
        mGetCloudLayout = binding.linearLayoutGetCloud

        if (CloudPluginUtil.isCloudProviderAvailable(requireContext())) {
            mBoxLayout.visibility = View.VISIBLE
            mDropboxLayout.visibility = View.VISIBLE
            mGoogleDriveLayout.visibility = View.VISIBLE
            mOnedriveLayout.visibility = View.VISIBLE
            mGetCloudLayout.visibility = View.GONE
        }

        if (BuildConfig.IS_VERSION_FDROID) {
            mBoxLayout.visibility = View.GONE
            mDropboxLayout.visibility = View.GONE
            mGoogleDriveLayout.visibility = View.GONE
            mOnedriveLayout.visibility = View.GONE
            mGetCloudLayout.visibility = View.GONE
        }

        mSmbLayout.setOnClickListener(this)
        mScpLayout.setOnClickListener(this)
        mBoxLayout.setOnClickListener(this)
        mDropboxLayout.setOnClickListener(this)
        mGoogleDriveLayout.setOnClickListener(this)
        mOnedriveLayout.setOnClickListener(this)
        mGetCloudLayout.setOnClickListener(this)

        dialog.setContentView(binding.root)
        dialog.setOnShowListener { dialog1: DialogInterface ->
            val d = dialog1 as BottomSheetDialog
            val bottomSheet =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            BottomSheetBehavior.from(bottomSheet!!).setState(BottomSheetBehavior.STATE_EXPANDED)
        }
        return dialog
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.linear_layout_smb -> {
                dismiss()
                val smbDialog = SmbSearchDialog()
                smbDialog.show(requireActivity().supportFragmentManager, "tab")
                return
            }

            R.id.linear_layout_scp -> {
                dismiss()
                val sftpConnectDialog = SftpConnectDialog()
                val args = Bundle()
                args.putBoolean("edit", false)
                sftpConnectDialog.arguments = args
                sftpConnectDialog.show(parentFragmentManager, "tab")
                return
            }

            R.id.linear_layout_box -> requireMainActivity().addCloudConnection(OpenMode.BOX)
            R.id.linear_layout_dropbox -> requireMainActivity().addCloudConnection(OpenMode.DROPBOX)
            R.id.linear_layout_google_drive -> GeneralDialogCreation.showSignInWithGoogleDialog((activity as MainActivity?)!!)
            R.id.linear_layout_onedrive -> requireMainActivity().addCloudConnection(OpenMode.ONEDRIVE)
            R.id.linear_layout_get_cloud -> {
                val cloudPluginIntent = Intent(Intent.ACTION_VIEW)
                cloudPluginIntent.setData(Uri.parse(getString(R.string.cloud_plugin_google_play_uri)))
                try {
                    startActivity(cloudPluginIntent)
                } catch (ifGooglePlayIsNotInstalled: ActivityNotFoundException) {
                    cloudPluginIntent.setData(
                        Uri.parse(getString(R.string.cloud_plugin_google_play_web_uri)),
                    )
                    startActivity(cloudPluginIntent)
                }
            }
        }
        // dismiss this sheet dialog
        dismiss()
    }

    private fun requireMainActivity(): MainActivity = requireActivity() as MainActivity

    interface CloudConnectionCallbacks {
        /**
         * Callback to add a new cloud connection of type [service]
         */
        fun addCloudConnection(service: OpenMode?)

        /**
         * Callback to delete an existing cloud connection of type [service]
         */
        fun deleteCloudConnection(service: OpenMode?)
    }

    companion object {
        const val TAG_FRAGMENT: String = "cloud_fragment"
    }
}
