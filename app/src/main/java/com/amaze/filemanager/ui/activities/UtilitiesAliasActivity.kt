package com.amaze.filemanager.ui.activities

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.UtilitiesAliasLayoutBinding
import com.amaze.filemanager.ui.refactorAUAlias
import com.amaze.filemanager.utils.Utils

class UtilitiesAliasActivity: AppCompatActivity() {

    private val _binding by lazy(LazyThreadSafetyMode.NONE) {
        UtilitiesAliasLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.downloadButton.setOnClickListener {
            Utils.openURL(
                AboutActivity.URL_AMAZE_UTILS,
                this
            )
        }
        _binding.cancelButton.setOnClickListener {
            finish()
        }
        val isAUInstalled = Utils.appInstalledOrNot(
            AboutActivity.PACKAGE_AMAZE_UTILS,
           packageManager
        )
        if (isAUInstalled) {
            AppConfig.toast(this, R.string.amaze_utils_installed_alias)
            val intent = packageManager.getLaunchIntentForPackage(
                AboutActivity.PACKAGE_AMAZE_UTILS
            )
            try {
                if (intent != null) {
                    this.refactorAUAlias(false)
                    startActivity(intent)
                    finish()
                }
            } catch (e: ActivityNotFoundException) {
                Log.w(javaClass.simpleName, "Amaze utils not installed", e)
            }
        }
    }
}