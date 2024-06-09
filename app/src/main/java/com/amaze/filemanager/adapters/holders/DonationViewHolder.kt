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

package com.amaze.filemanager.adapters.holders

import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R

class DonationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField
    val ROOT_VIEW: LinearLayout = itemView.findViewById(R.id.adapter_donation_root)

    @JvmField
    val TITLE: AppCompatTextView = itemView.findViewById(R.id.adapter_donation_title)

    @JvmField
    val SUMMARY: AppCompatTextView = itemView.findViewById(R.id.adapter_donation_summary)

    @JvmField
    val PRICE: AppCompatTextView = itemView.findViewById(R.id.adapter_donation_price)
}
