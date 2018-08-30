package com.amaze.filemanager.adapters.holders;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.amaze.filemanager.R;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.viewholders.FlexibleViewHolder;

public class HistoryViewHolder extends FlexibleViewHolder {
    public final TextView txtTitle;
    public final TextView txtDesc;
    public final ConstraintLayout row;

    public HistoryViewHolder(FlexibleAdapter adapter, View view) {
        super(view, adapter);

        txtTitle = view.findViewById(R.id.text1);
        txtDesc = view.findViewById(R.id.text2);
        row = view.findViewById(R.id.bookmarkrow);
    }

}