package com.amaze.filemanager.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 18:38.
 */

public class HiddenViewHolder extends RecyclerView.ViewHolder {
    private final ImageButton image;
    private final TextView txtTitle;
    private final TextView txtDesc;
    private final LinearLayout row;

    public HiddenViewHolder(View view) {
        super(view);

        txtTitle = (TextView) view.findViewById(R.id.text1);
        image = (ImageButton) view.findViewById(R.id.delete_button);
        txtDesc = (TextView) view.findViewById(R.id.text2);
        row = (LinearLayout) view.findViewById(R.id.bookmarkrow);
    }

    public ImageButton getImage() {
        return image;
    }

    public TextView getTxtTitle() {
        return txtTitle;
    }

    public TextView getTxtDesc() {
        return txtDesc;
    }

    public LinearLayout getRow() {
        return row;
    }
}