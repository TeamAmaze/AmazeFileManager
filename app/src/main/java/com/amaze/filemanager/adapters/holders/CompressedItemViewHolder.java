package com.amaze.filemanager.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.RoundedImageView;

/**
 * @author Emmanuel Messulam<emmanuelbendavid@gmail.com>
 *         on 17/9/2017, at 18:13.
 */

public class CompressedItemViewHolder extends RecyclerView.ViewHolder {
    // each data item is just a string in this case
    public RoundedImageView pictureIcon;
    public ImageView genericIcon, apkIcon;
    public TextView txtTitle;
    public TextView txtDesc;
    public TextView date;
    public TextView perm;
    public View rl;
    public ImageView checkImageView;

    public CompressedItemViewHolder(View view) {
        super(view);
        txtTitle = (TextView) view.findViewById(R.id.firstline);
        pictureIcon = (RoundedImageView) view.findViewById(R.id.picture_icon);
        genericIcon = (ImageView) view.findViewById(R.id.generic_icon);
        rl = view.findViewById(R.id.second);
        perm = (TextView) view.findViewById(R.id.permis);
        date = (TextView) view.findViewById(R.id.date);
        txtDesc = (TextView) view.findViewById(R.id.secondLine);
        apkIcon = (ImageView) view.findViewById(R.id.apk_icon);
        checkImageView = (ImageView) view.findViewById(R.id.check_icon);
    }
}