package com.amaze.filemanager.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.RoundedImageView;

/**
 * Check RecyclerAdapter's doc.
 * TODO load everything related to this item here instead of in RecyclerAdapter.
 *
 * @author Emmanuel
 *         on 29/5/2017, at 04:19.
 */

public class ItemViewHolder extends RecyclerView.ViewHolder {
    // each data item is just a string in this case
    public RoundedImageView pictureIcon;
    public ImageView genericIcon, apkIcon;
    public ImageView imageView1;
    public TextView txtTitle;
    public TextView txtDesc;
    public TextView date;
    public TextView perm;
    public View rl;
    public TextView genericText;
    public ImageButton about;
    public ImageView checkImageView;
    public ImageView checkImageViewGrid;

    public ItemViewHolder(View view) {
        super(view);

        txtTitle = (TextView) view.findViewById(R.id.firstline);
        pictureIcon = (RoundedImageView) view.findViewById(R.id.picture_icon);
        rl = view.findViewById(R.id.second);
        perm = (TextView) view.findViewById(R.id.permis);
        date = (TextView) view.findViewById(R.id.date);
        txtDesc = (TextView) view.findViewById(R.id.secondLine);
        apkIcon = (ImageView) view.findViewById(R.id.apk_icon);
        genericText = (TextView) view.findViewById(R.id.generictext);
        imageView1 = (ImageView) view.findViewById(R.id.icon_thumb);
        about = (ImageButton) view.findViewById(R.id.properties);
        checkImageView = (ImageView) view.findViewById(R.id.check_icon);
        genericIcon = (ImageView) view.findViewById(R.id.generic_icon);
        checkImageViewGrid = (ImageView) view.findViewById(R.id.check_icon_grid);
    }


}
