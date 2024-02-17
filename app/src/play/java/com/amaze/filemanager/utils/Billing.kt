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

package com.amaze.filemanager.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.holders.DonationViewHolder
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.AdapterDonationBinding
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class Billing(private val activity: BasicActivity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    PurchasesUpdatedListener {

    private val LOG = LoggerFactory.getLogger(Billing::class.java)

    // List of predefined IAP products SKU.
    private val productList: List<Product>

    // List of IAP products query result.
    private lateinit var productDetails: List<ProductDetails>

    // create new donations client
    private lateinit var billingClient: BillingClient

    // True if billing service is connected
    private var isServiceConnected = false

    private lateinit var donationDialog: MaterialDialog

    override fun onPurchasesUpdated(response: BillingResult, purchases: List<Purchase>?) {
        if (response.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                val listener = ConsumeResponseListener { _: BillingResult, _: String -> }
                val consumeParams: ConsumeParams =
                    ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                billingClient.consumeAsync(consumeParams, listener)
            }
            // we consume the purchase, so that user can perform purchase again
            activity.runOnUiThread {
                Toast.makeText(activity, R.string.donation_thanks, Toast.LENGTH_LONG).show()
                donationDialog.dismiss()
            }
        }
    }

    /** Start a purchase flow  */
    private fun initiatePurchaseFlow() {
        val purchaseFlowRequest = Runnable {
            val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

            billingClient.queryProductDetailsAsync(
                params.build()
            ) { responseCode: BillingResult, queryResult: List<ProductDetails> ->
                if (queryResult.isNotEmpty()) {
                    // Successfully fetched product details
                    productDetails = queryResult
                    popProductsList(responseCode, queryResult)
                } else {
                    AppConfig.toast(activity, R.string.error_fetching_google_play_product_list)
                    if (BuildConfig.DEBUG) {
                        /* ktlint-disable max-line-length */
                        LOG.warn(
                            "Error fetching product list - looks like you are running a DEBUG build."
                        )
                        /* ktlint-enable max-line-length */
                    }
                }
            }
        }
        executeServiceRequest(purchaseFlowRequest)
    }

    private fun createProductWith(productId: String) =
        Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

    /**
     * Got products list from play store, pop their details
     *
     * @param response
     * @param productDetailsQueryResult
     */
    private fun popProductsList(
        response: BillingResult,
        productDetailsQueryResult: List<ProductDetails>
    ) {
        if (response.responseCode == BillingClient.BillingResponseCode.OK &&
            productDetailsQueryResult.isNotEmpty()
        ) {
            showPaymentsDialog(activity)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rootView: View = AdapterDonationBinding.inflate(
            LayoutInflater.from(
                activity
            ),
            parent,
            false
        ).root
        return DonationViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DonationViewHolder && productDetails.isNotEmpty()) {
            val titleRaw: String = productDetails[position].title
            holder.TITLE.text = titleRaw.subSequence(
                0,
                titleRaw.lastIndexOf("(")
            )
            holder.SUMMARY.text = productDetails[position].description
            holder.PRICE.text = productDetails[position].oneTimePurchaseOfferDetails?.formattedPrice
            holder.ROOT_VIEW.setOnClickListener {
                purchaseProduct.purchaseItem(
                    productDetails[position]
                )
            }
        }
    }

    override fun getItemCount(): Int = productList.size

    private interface PurchaseProduct {
        fun purchaseItem(productDetails: ProductDetails)
        fun purchaseCancel()
    }

    private val purchaseProduct: PurchaseProduct = object : PurchaseProduct {
        override fun purchaseItem(productDetailsArg: ProductDetails) {
            val billingFlowParams: BillingFlowParams =
                BillingFlowParams.newBuilder().setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetailsArg)
                            .build()
                    )
                ).build()
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }

        override fun purchaseCancel() {
            destroyBillingInstance()
        }
    }

    init {
        productList = listOf(
            createProductWith("donations"),
            createProductWith("donations_2"),
            createProductWith("donations_3"),
            createProductWith("donations_4")
        )
        billingClient =
            BillingClient.newBuilder(activity).setListener(this).enablePendingPurchases().build()
        initiatePurchaseFlow()
    }

    /**
     * We executes a connection request to Google Play
     *
     * @param runnable
     */
    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable)
        }
    }

    /**
     * Starts a connection to Google Play services
     *
     * @param executeOnSuccess
     */
    private fun startServiceConnection(executeOnSuccess: Runnable?) {
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResponse: BillingResult) {
                    LOG.debug("Setup finished. Response code: ${billingResponse.responseCode}")
                    if (billingResponse.responseCode == BillingClient.BillingResponseCode.OK) {
                        isServiceConnected = true
                        executeOnSuccess?.run()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isServiceConnected = false
                }
            }
        )
    }

    /**
     * Drop the [BillingClient] on purchase process cancels.
     */
    fun destroyBillingInstance() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun showPaymentsDialog(context: BasicActivity) {
        /*
     * As of Billing library 4.0, all callbacks are running on background thread.
     * Need to use AppConfig.runInApplicationThread() for UI interactions
     *
     *
     */
        AppConfig.getInstance()
            .runInApplicationThread(
                Callable {
                    val builder: MaterialDialog.Builder = MaterialDialog.Builder(context)
                    builder.title(R.string.donate)
                    builder.adapter(this, null)
                    builder.theme(context.appTheme.getMaterialDialogTheme())
                    builder.cancelListener { purchaseProduct.purchaseCancel() }
                    donationDialog = builder.show()
                    null
                }
            )
    }
}
