package com.amaze.filemanager.utils.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.RecyclerArrayAdapter;

import java.util.ArrayList;

import static java.security.AccessController.getContext;

/**
 * Created by Arpit on 01-07-2015.
 */

public class ShareAdapter extends RecyclerArrayAdapter<Intent, ShareAdapter.ViewHolder> {
    private MaterialDialog dialog;
    private ArrayList<String> labels;
    private ArrayList<Drawable> drawables;
    private Context context;

    public void updateMatDialog(MaterialDialog b) {
        this.dialog = b;
    }

    public ShareAdapter(Context context,
                        ArrayList<Intent> intents,
                        ArrayList<String> labels,
                        ArrayList<Drawable> arrayList1) {
        addAll(intents);
        this.context = context;
        this.labels = labels;
        this.drawables = arrayList1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.simplerow, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.render(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private View rootView;

        private TextView a;
        private ImageView v;

        ViewHolder(View view) {
            super(view);

            rootView = view;

            TextView a = ((TextView) view.findViewById(R.id.firstline));
            ImageView v = (ImageView) view.findViewById(R.id.icon);
        }

        void render(final int position) {
            if (drawables.get(position) != null)
                v.setImageDrawable(drawables.get(position));
            a.setVisibility(View.VISIBLE);
            a.setText(labels.get(position));
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dialog != null && dialog.isShowing()) dialog.dismiss();
                    context.startActivity(getItem(position));
                }
            });
        }
    }

}
