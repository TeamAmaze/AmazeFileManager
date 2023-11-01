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

package com.amaze.filemanager.ui.fragments.preferencefragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.AboutActivity
import com.amaze.filemanager.utils.Utils

class PrefsFragment : BasePrefsFragment() {
    override val title = R.string.setting

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("appearance")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(AppearancePrefsFragment())
                true
            }

        findPreference<Preference>("ui")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(UiPrefsFragment())
                true
            }

        findPreference<Preference>("behavior")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(BehaviorPrefsFragment())
                true
            }

        findPreference<Preference>("security")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(SecurityPrefsFragment())
                true
            }

        findPreference<Preference>("backup")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(BackupPrefsFragment())
                true
            }

        findPreference<Preference>("about")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(Intent(activity, AboutActivity::class.java))
                false
            }

        findPreference<Preference>("feedback")
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val emailIntent = Utils.buildEmailIntent(requireContext(), null, Utils.EMAIL_SUPPORT)

            val activities = activity.packageManager.queryIntentActivities(
                emailIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            if (activities.isNotEmpty()) {
                startActivity(
                    Intent.createChooser(
                        emailIntent,
                        resources.getString(R.string.feedback)
                    )
                )
            } else {
                Toast.makeText(
                    getActivity(),
                    resources.getString(R.string.send_email_to) + " " + Utils.EMAIL_SUPPORT,
                    Toast.LENGTH_LONG
                )
                    .show()
            }

            false
        }
    }
}
