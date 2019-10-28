package com.amaze.filemanager.adapters.holders;

import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.amaze.filemanager.R;

/**
 * This is the ViewHolder that formats the hidden files as defined in bookmarkrow.xml.
 *
 * @author Emmanuel on 20/11/2017, at 18:38.
 * @author Bowie Chen on 2019-10-26.
 * @see com.amaze.filemanager.adapters.HiddenAdapter
 */

public class HiddenViewHolder extends RecyclerView.ViewHolder {

    public final ImageButton deleteButton;
    public final TextView textTitle;
    public final TextView textDescription;
    public final LinearLayout row;

    public HiddenViewHolder(View view) {
        super(view);

        textTitle = view.findViewById(R.id.filename);
        deleteButton = view.findViewById(R.id.delete_button);
        textDescription = view.findViewById(R.id.file_path);
        row = view.findViewById(R.id.bookmarkrow);
    }

}