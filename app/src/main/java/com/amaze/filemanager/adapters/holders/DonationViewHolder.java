package com.amaze.filemanager.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;

public class DonationViewHolder extends RecyclerView.ViewHolder {

    public final LinearLayout ROOT_VIEW;
    public final TextView TITLE, SUMMARY, PRICE;
    public DonationViewHolder(View itemView) {
        super(itemView);
        ROOT_VIEW = itemView.findViewById(R.id.adapter_donation_root);
        TITLE = itemView.findViewById(R.id.adapter_donation_title);
        SUMMARY = itemView.findViewById(R.id.adapter_donation_summary);
        PRICE = itemView.findViewById(R.id.adapter_donation_price);
    }
}
