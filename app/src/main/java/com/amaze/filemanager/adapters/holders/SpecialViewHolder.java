package com.amaze.filemanager.adapters.holders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Check {@link com.amaze.filemanager.adapters.RecyclerAdapter}'s doc.
 *
 * @author Emmanuel
 *         on 29/5/2017, at 04:22.
 */

public class SpecialViewHolder extends RecyclerView.ViewHolder {
    public static final int HEADER_FILES = 0, HEADER_FOLDERS = 1;
    // each data item is just a string in this case
    public TextView txtTitle;

    private int type;

    public SpecialViewHolder(Context c, View view, UtilitiesProviderInterface utilsProvider,
                             int type) {
        super(view);

        this.type = type;
        txtTitle = (TextView) view.findViewById(R.id.text);

        switch (type) {
            case HEADER_FILES:
                txtTitle.setText(R.string.files);
                break;
            case HEADER_FOLDERS:
                txtTitle.setText(R.string.folders);
                break;
            default:
                throw new IllegalStateException(": " + type);
        }

        //if(utilsProvider.getAppTheme().equals(AppTheme.DARK))
        //    view.setBackgroundResource(R.color.holo_dark_background);

        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
            txtTitle.setTextColor(Utils.getColor(c, R.color.text_light));
        } else {
            txtTitle.setTextColor(Utils.getColor(c, R.color.text_dark));
        }
    }

    public int getType() {
        return type;
    }
}