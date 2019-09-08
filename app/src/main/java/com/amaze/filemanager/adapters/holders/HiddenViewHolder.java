package com.amaze.filemanager.adapters.holders;

import androidx.recyclerview.widget.RecyclerView;
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
    public final ImageButton image;
    public final TextView txtTitle;
    public final TextView txtDesc;
    public final LinearLayout row;

    public HiddenViewHolder(View view) {
        super(view);

        txtTitle = view.findViewById(R.id.text1);
        image = view.findViewById(R.id.delete_button);
        txtDesc = view.findViewById(R.id.text2);
        row = view.findViewById(R.id.bookmarkrow);
    }

}