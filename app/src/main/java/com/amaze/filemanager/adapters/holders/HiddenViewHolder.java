package com.amaze.filemanager.adapters.holders;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 18:38.
 */

public class HiddenViewHolder extends FlexibleViewHolder {
    public final ImageButton image;
    public final TextView txtTitle;
    public final TextView txtDesc;
    public final ConstraintLayout row;

    public HiddenViewHolder(FlexibleAdapter adapter, View view) {
        super(view, adapter);

        txtTitle = view.findViewById(R.id.text1);
        image = view.findViewById(R.id.delete_button);
        txtDesc = view.findViewById(R.id.text2);
        row = view.findViewById(R.id.bookmarkrow);
    }

}