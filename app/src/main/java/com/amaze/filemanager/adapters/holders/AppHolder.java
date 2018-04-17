package com.amaze.filemanager.adapters.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.ThemedTextView;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 10/12/2017, at 14:45.
 */

public class AppHolder extends RecyclerView.ViewHolder {

    public final ImageView apkIcon;
    public final ThemedTextView txtTitle;
    public final RelativeLayout rl;
    public final TextView txtDesc;
    public final ImageButton about;

    public AppHolder(View view) {
        super(view);

        txtTitle = view.findViewById(R.id.firstline);
        apkIcon = view.findViewById(R.id.apk_icon);
        rl = view.findViewById(R.id.second);
        txtDesc = view.findViewById(R.id.date);
        about = view.findViewById(R.id.properties);

        apkIcon.setVisibility(View.VISIBLE);
    }
}
