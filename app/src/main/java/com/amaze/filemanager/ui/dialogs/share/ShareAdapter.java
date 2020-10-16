/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.dialogs.share;

import java.util.ArrayList;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/** Created by Arpit on 01-07-2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com> */
class ShareAdapter extends RecyclerView.Adapter<ShareAdapter.ViewHolder> {

  private ArrayList<Intent> items;
  private MaterialDialog dialog;
  private ArrayList<String> labels;
  private ArrayList<Drawable> drawables;
  private Context context;

  void updateMatDialog(MaterialDialog b) {
    this.dialog = b;
  }

  ShareAdapter(
      Context context,
      ArrayList<Intent> intents,
      ArrayList<String> labels,
      ArrayList<Drawable> arrayList1) {
    items = new ArrayList<>(intents);
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

    private TextView textView;
    private ImageView imageView;

    ViewHolder(View view) {
      super(view);

      rootView = view;

      textView = view.findViewById(R.id.firstline);
      imageView = view.findViewById(R.id.icon);
    }

    void render(final int position) {
      if (drawables.get(position) != null) imageView.setImageDrawable(drawables.get(position));
      textView.setVisibility(View.VISIBLE);
      textView.setText(labels.get(position));
      rootView.setOnClickListener(
          v -> {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            context.startActivity(items.get(position));
          });
    }
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemCount() {
    return items.size();
  }
}
