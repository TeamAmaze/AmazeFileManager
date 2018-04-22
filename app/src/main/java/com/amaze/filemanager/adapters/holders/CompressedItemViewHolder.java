package com.amaze.filemanager.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.ui.views.ThemedTextView;

/**
 * @author Emmanuel Messulam<emmanuelbendavid@gmail.com>
 *         on 17/9/2017, at 18:13.
 */

public class CompressedItemViewHolder extends RecyclerView.ViewHolder {
    // each data item is just a string in this case
    public final RoundedImageView pictureIcon;
    public final ImageView genericIcon, apkIcon;
    public final ThemedTextView txtTitle;
    public final TextView txtDesc;
    public final TextView date;
    public final TextView perm;
    public final View rl;
    public final ImageView checkImageView;

    public CompressedItemViewHolder(View view) {
        super(view);
        txtTitle = view.findViewById(R.id.firstline);
        pictureIcon = view.findViewById(R.id.picture_icon);
        genericIcon = view.findViewById(R.id.generic_icon);
        rl = view.findViewById(R.id.second);
        perm = view.findViewById(R.id.permis);
        date = view.findViewById(R.id.date);
        txtDesc = view.findViewById(R.id.secondLine);
        apkIcon = view.findViewById(R.id.apk_icon);
        checkImageView = view.findViewById(R.id.check_icon);
    }
}