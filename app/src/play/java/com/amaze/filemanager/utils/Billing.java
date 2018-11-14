package com.amaze.filemanager.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.superclasses.BasicActivity;
import com.amaze.filemanager.adapters.holders.DonationViewHolder;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.List;

public class Billing extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements PurchasesUpdatedListener {

    private BasicActivity activity;
    private List<String> skuList;
    private LayoutInflater layoutInflater;
    private List<SkuDetails> skuDetails;

    // create new donations client
    private BillingClient billingClient;
    /**
     * True if billing service is connected now.
     */
    private boolean isServiceConnected;


    public Billing(BasicActivity activity) {
        this.activity = activity;

        skuList = new ArrayList<>();
        skuList.add("donations");
        skuList.add("donations_2");
        skuList.add("donations_3");
        skuList.add("donations_4");

        layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        billingClient = BillingClient.newBuilder(activity).setListener(this).build();
        initiatePurchaseFlow();
    }


    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                ConsumeResponseListener listener = (responseCode1, purchaseToken) -> {
                    // we consume the purchase, so that user can perform purchase again
                    Toast.makeText(activity, R.string.donation_thanks, Toast.LENGTH_LONG).show();
                };
                billingClient.consumeAsync(purchase.getPurchaseToken(), listener);
            }
        }
    }

    /**
     * Start a purchase flow
     */
    private void initiatePurchaseFlow() {
        Runnable purchaseFlowRequest = () -> {
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params.build(),
                    (responseCode, skuDetailsList) -> {
                        // Successfully fetched product details
                        skuDetails = skuDetailsList;
                        popProductsList(responseCode, skuDetailsList);
                    });
        };

        executeServiceRequest(purchaseFlowRequest);
    }

    /**
     * Got products list from play store, pop their details
     * @param responseCode
     * @param skuDetailsList
     */
    private void popProductsList(int responseCode, List<SkuDetails> skuDetailsList) {
        if (responseCode == BillingClient.BillingResponse.OK
                && skuDetailsList != null) {
            showPaymentsDialog(activity);
        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rootView = layoutInflater.inflate(R.layout.adapter_donation, parent, false);
        return new DonationViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DonationViewHolder) {
            String titleRaw = skuDetails.get(position).getTitle();
            ((DonationViewHolder) holder).TITLE.setText(titleRaw.subSequence(0, titleRaw.lastIndexOf("(")));
            ((DonationViewHolder) holder).SUMMARY.setText(skuDetails.get(position).getDescription());
            ((DonationViewHolder) holder).PRICE.setText(skuDetails.get(position).getPrice());
            ((DonationViewHolder) holder).ROOT_VIEW.setOnClickListener(v ->
                    purchaseProduct.purchaseItem(skuDetails.get(position)));
        }
    }

    @Override
    public int getItemCount() {
        return skuList.size();
    }

    private interface PurchaseProduct {

        void purchaseItem(SkuDetails skuDetails);
        void purchaseCancel();
    }

    private PurchaseProduct purchaseProduct = new PurchaseProduct() {
        @Override
        public void purchaseItem(SkuDetails skuDetails) {

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build();
            billingClient.launchBillingFlow(activity, billingFlowParams);
        }

        @Override
        public void purchaseCancel() {
            destroyBillingInstance();
        }
    };

    /**
     * We executes a connection request to Google Play
     * @param runnable
     */
    private void executeServiceRequest(Runnable runnable) {
        if (isServiceConnected) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable);
        }
    }

    /**
     * Starts a connection to Google Play services
     * @param executeOnSuccess
     */
    private void startServiceConnection(final Runnable executeOnSuccess) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                Log.d(Billing.this.getClass().getSimpleName(), "Setup finished. Response code: " + billingResponseCode);

                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    isServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isServiceConnected = false;
            }
        });
    }

    public void destroyBillingInstance() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

    private void showPaymentsDialog(final BasicActivity context) {
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.donate);
        builder.adapter(this, null);
        builder.theme(context.getAppTheme().getMaterialDialogTheme());
        builder.cancelListener(dialog -> purchaseProduct.purchaseCancel());
        builder.show();
    }
}
