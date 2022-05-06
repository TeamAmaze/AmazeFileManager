package com.amaze.filemanager.ui.activities

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.UtilitiesAliasLayoutBinding
import com.amaze.filemanager.ui.dialogs.EncryptAuthenticateDialog
import com.amaze.filemanager.ui.updateAUAlias
import com.amaze.filemanager.utils.PackageUtils
import com.amaze.filemanager.utils.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UtilitiesAliasActivity: AppCompatActivity() {
    private val log: Logger = LoggerFactory.getLogger(UtilitiesAliasActivity::class.java)

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
        val isAUInstalled = PackageUtils.appInstalledOrNot(
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
                    this.updateAUAlias(false)
                    startActivity(intent)
                    finish()
                }
            } catch (e: ActivityNotFoundException) {
                log.warn("Amaze utils not installed", e)
            }
        }
    }
}