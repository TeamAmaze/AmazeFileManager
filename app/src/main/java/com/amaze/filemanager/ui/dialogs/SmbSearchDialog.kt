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

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.provider.UtilitiesProvider
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.ComputerParcelable
import com.amaze.filemanager.utils.smb.SmbDeviceScannerObservable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory

/** Created by arpitkh996 on 16-01-2016 edited by Emmanuel Messulam <emmanuelbendavid></emmanuelbendavid>@gmail.com>  */
class SmbSearchDialog : DialogFragment() {

    private lateinit var utilsProvider: UtilitiesProvider
    private lateinit var listViewAdapter: ListViewAdapter
    private val viewModel = ComputerParcelableViewModel()
    private var accentColor = 0
    private lateinit var subnetScannerObserver: Disposable

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        utilsProvider = AppConfig.getInstance().utilsProvider
        accentColor = (activity as ThemedActivity).accent
    }

    override fun dismiss() {
        super.dismiss()
        if (!subnetScannerObserver.isDisposed) {
            subnetScannerObserver.dispose()
        }
    }

    @Suppress("LongMethod", "LabeledExpression")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialDialog.Builder(requireActivity())
        builder.title(R.string.searching_devices)
        builder.negativeColor(accentColor)
        builder.negativeText(R.string.cancel)
        builder.onNegative { _: MaterialDialog?, _: DialogAction? ->
            if (!subnetScannerObserver.isDisposed) {
                subnetScannerObserver.dispose()
            }
            dismiss()
        }
        builder.onPositive { _: MaterialDialog?, _: DialogAction? ->
            if (!subnetScannerObserver.isDisposed) {
                subnetScannerObserver.dispose()
            }
            if (activity != null && activity is MainActivity) {
                dismiss()
                val mainActivity = activity as MainActivity
                mainActivity.showSMBDialog("", "", false)
            }
        }
        builder.positiveText(R.string.use_custom_ip)
        builder.positiveColor(accentColor)
        viewModel.valHolder.postValue(ComputerParcelable("-1", "-1"))
        listViewAdapter = ListViewAdapter(requireActivity())
        val observable = SmbDeviceScannerObservable()
        subnetScannerObserver = observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnDispose {
                observable.stop()
            }
            .subscribe(
                { computer: ComputerParcelable ->
                    if (!listViewAdapter.contains(computer)) {
                        viewModel.valHolder.postValue(computer)
                    }
                },
                { err: Throwable ->
                    LOG.error("Error searching for devices", err)
                }
            ) {
                subnetScannerObserver.dispose()
                activity?.runOnUiThread {
                    if (listViewAdapter.dummyOnly()) {
                        dismiss()
                        Toast.makeText(
                            activity,
                            getString(R.string.no_device_found),
                            Toast.LENGTH_SHORT
                        ).show()
                        val mainActivity = activity as MainActivity
                        mainActivity.showSMBDialog("", "", false)
                        return@runOnUiThread
                    }
                    listViewAdapter.removeDummy()
                }
            }
        builder.adapter(listViewAdapter, null)
        viewModel.valHolder.observe(this) {
            listViewAdapter.add(it)
        }
        return builder.build()
    }

    private inner class ListViewAdapter(
        context: Context
    ) : RecyclerView.Adapter<ViewHolder>() {
        private val items: MutableList<ComputerParcelable> = ArrayList()
        private val mInflater: LayoutInflater

        init {
            mInflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        /**
         * Called by [ComputerParcelableViewModel], add found computer to list view
         */
        fun add(computer: ComputerParcelable) {
            if (computer.addr == "-1" && computer.name == "-1") {
                items.add(computer)
            } else {
                items.add(items.size - 1, computer)
            }
            notifyDataSetChanged()
        }

        /**
         * Called by Observable when finish probing. If no other computers found, remove first
         * (dummy) host
         */
        fun removeDummy() {
            items.remove(
                items.find {
                    it.addr == "-1" && it.name == "-1"
                }
            )
            notifyDataSetChanged()
        }

        /**
         * Answers if the computer list contains given instance.
         */
        fun contains(computer: ComputerParcelable): Boolean {
            return items.contains(computer)
        }

        /**
         * Answers if the list is empty = only has the dummy [ComputerParcelable] instance
         */
        fun dummyOnly(): Boolean {
            return items.size == 1 && items.last().addr == "-1"
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View
            return when (viewType) {
                VIEW_PROGRESSBAR -> {
                    view = mInflater.inflate(R.layout.smb_progress_row, parent, false)
                    ViewHolder(view)
                }
                else -> {
                    view =
                        mInflater.inflate(R.layout.smb_computers_row, parent, false)
                    ElementViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val viewType = getItemViewType(position)
            if (viewType == Companion.VIEW_PROGRESSBAR) {
                return
            }
            val (addr, name) = items[position]
            holder.rootView.setOnClickListener {
                if (activity != null && activity is MainActivity) {
                    dismiss()
                    val mainActivity = activity as MainActivity
                    mainActivity.showSMBDialog(
                        listViewAdapter.items[position].name,
                        listViewAdapter.items[position].addr,
                        false
                    )
                }
            }
            if (holder is ElementViewHolder) {
                holder.txtTitle.text = name
                holder.image.setImageResource(R.drawable.ic_settings_remote_white_48dp)
                if (utilsProvider.appTheme == AppTheme.LIGHT) {
                    holder.image.setColorFilter(Color.parseColor("#666666"))
                }
                holder.txtDesc.text = addr
            }
        }

        override fun getItemViewType(position: Int): Int {
            val (addr) = items[position]
            return if (addr == "-1") {
                VIEW_PROGRESSBAR
            } else {
                VIEW_ELEMENT
            }
        }

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getItemCount(): Int = items.size
    }

    private open class ViewHolder(val rootView: View) : RecyclerView.ViewHolder(rootView)

    private class ElementViewHolder(rootView: View) :
        ViewHolder(rootView) {
        val image: AppCompatImageView = rootView.findViewById(R.id.icon)
        val txtTitle: AppCompatTextView = rootView.findViewById(R.id.firstline)
        val txtDesc: AppCompatTextView = rootView.findViewById(R.id.secondLine)
    }

    private class ComputerParcelableViewModel : ViewModel() {
        val valHolder = MutableLiveData<ComputerParcelable>()
    }

    companion object {
        private const val VIEW_PROGRESSBAR = 1
        private const val VIEW_ELEMENT = 2
        private val LOG = LoggerFactory.getLogger(SmbSearchDialog::class.java)
    }
}
