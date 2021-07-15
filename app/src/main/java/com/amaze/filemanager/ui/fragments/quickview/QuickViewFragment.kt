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

package com.amaze.filemanager.ui.fragments.quickview

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.amaze.filemanager.GlideApp
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.fragments.quickview.types.QuickViewImage
import com.amaze.filemanager.ui.fragments.quickview.types.QuickViewType
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.eightbitlab.supportrenderscriptblur.SupportRenderScriptBlur
import eightbitlab.com.blurview.BlurView

class QuickViewFragment : Fragment() {

    companion object {
        private const val VIEW_TYPE_ARGUMENT = "QuickViewFragment.viewTypeArgument"

        fun newInstance(viewType: QuickViewType): QuickViewFragment {
            val arguments = Bundle().also {
                it.putParcelable(VIEW_TYPE_ARGUMENT, viewType)
            }

            return QuickViewFragment().also {
                it.arguments = arguments
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.quick_view_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.frameLayout)
        constraintLayout.setOnClickListener {
            exit()
        }
        // TODO correctly deal with BottomBar visibility

        blur(view)

        val quickViewType = requireArguments().getParcelable<QuickViewType>(VIEW_TYPE_ARGUMENT)

        when (quickViewType) {
            is QuickViewImage -> showImage(quickViewType, constraintLayout)
            else -> TODO()
        }
    }

    private fun blur(view: View) {
        val activity = requireActivity() as MainActivity
        val decorView: View = activity.window.decorView
        val rootView = activity.currentMainFragment!!.requireView() as ViewGroup
        // Set drawable to draw in the beginning of each blurred frame (Optional).
        // Can be used in case your layout has a lot of transparent space and your content
        // gets kinda lost after after blur is applied.
        val windowBackground = decorView.background

        val blurView = view.findViewById<BlurView>(R.id.blurView)
        blurView.setupWith(rootView)
            .setFrameClearDrawable(windowBackground)
            .setBlurAlgorithm(SupportRenderScriptBlur(requireContext()))
            .setBlurRadius(5f)
            // This is a hack to show a blurred FAB in the background of the QuickView
            .setBlurAutoUpdate(false)
            .setHasFixedTransformationMatrix(true)
    }

    private fun showImage(quickViewType: QuickViewImage, constraintLayout: ConstraintLayout) {
        val imageViewContainer = constraintLayout
            .findViewById<ConstraintLayout>(R.id.imageViewContainer)
        applyContainerColor(imageViewContainer)

        val textView = constraintLayout.findViewById<TextView>(R.id.textView)
        textView.text = quickViewType.name
        // TODO correct text color based on background

        runAfterSizeComputed(constraintLayout) {
            val imageView = constraintLayout.findViewById<ImageView>(R.id.imageView)

            val incompleteRequest = GlideApp.with(this)

            val requestBuilder = quickViewType.reference.getPreloadRequestBuilder(incompleteRequest)

            val requestListener = object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    exit()
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    imageViewContainer.visibility = VISIBLE
                    return false
                }
            }

            val width = (constraintLayout.height * 0.90f).toInt()
            val height = (constraintLayout.width * 0.90f).toInt()

            requestBuilder
                .override(width, height)
                .listener(requestListener)
                .into(imageView)
        }
    }

    private fun runAfterSizeComputed(view: View, f: () -> Unit) {
        view.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                    } else {
                        view.viewTreeObserver
                            .removeGlobalOnLayoutListener(this)
                    }

                    f()
                }
            })
    }

    private fun applyContainerColor(container: View) {
        val colorPrimary = (requireActivity() as ThemedActivity).primary

        val background: Drawable = container.background
        when (background) {
            is ShapeDrawable -> {
                background.paint.color = colorPrimary
            }
            is GradientDrawable -> {
                background.mutate()
                background.setColor(colorPrimary)
            }
            is ColorDrawable -> {
                background.color = colorPrimary
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        (requireActivity() as MainActivity).appbar.toolbar.menu
            .setGroupVisible(0, false)

        (requireActivity() as MainActivity).appbar.bottomBar
            .setIsClickEnabled(false)
    }

    fun onBackPressed() {
        exit()
    }

    fun exit() {
        parentFragmentManager
            .beginTransaction()
            .remove(this)
            .commit()

        (requireActivity() as MainActivity)
            .appbar.toolbar.menu.setGroupVisible(0, true)

        (requireActivity() as MainActivity).appbar.bottomBar
            .setIsClickEnabled(true)

        (requireActivity() as MainActivity)
            .fab.mainFab.visibility = VISIBLE
    }
}
