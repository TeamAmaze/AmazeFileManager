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

package com.amaze.filemanager.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.ComputerParcelable;
import com.amaze.filemanager.utils.SubnetScanner;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Created by arpitkh996 on 16-01-2016 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com> */
public class SmbSearchDialog extends DialogFragment {
  private UtilitiesProvider utilsProvider;

  private ListViewAdapter listViewAdapter;
  private ArrayList<ComputerParcelable> computers = new ArrayList<>();
  private int accentColor;
  private SubnetScanner subnetScanner;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    utilsProvider = ((BasicActivity) getActivity()).getUtilsProvider();

    accentColor = ((ThemedActivity) getActivity()).getAccent();
  }

  @Override
  public void dismiss() {
    super.dismiss();
    if (subnetScanner != null) subnetScanner.cancel(true);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
    builder.title(R.string.searching_devices);
    builder.negativeColor(accentColor);
    builder.negativeText(R.string.cancel);
    builder.onNegative(
        (dialog, which) -> {
          if (subnetScanner != null) subnetScanner.cancel(true);
          dismiss();
        });
    builder.onPositive(
        (dialog, which) -> {
          if (subnetScanner != null) subnetScanner.cancel(true);
          if (getActivity() != null && getActivity() instanceof MainActivity) {
            dismiss();
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.showSMBDialog("", "", false);
          }
        });
    builder.positiveText(R.string.use_custom_ip);
    builder.positiveColor(accentColor);
    computers.add(new ComputerParcelable("-1", "-1"));
    listViewAdapter = new ListViewAdapter(getActivity(), computers);
    subnetScanner = new SubnetScanner(getActivity());
    subnetScanner.setObserver(
        new SubnetScanner.ScanObserver() {
          @Override
          public void computerFound(final ComputerParcelable computer) {
            if (getActivity() != null)
              getActivity()
                  .runOnUiThread(
                      () -> {
                        if (!computers.contains(computer)) computers.add(computer);
                        listViewAdapter.notifyDataSetChanged();
                      });
          }

          @Override
          public void searchFinished() {
            if (getActivity() != null) {
              getActivity()
                  .runOnUiThread(
                      () -> {
                        if (computers.size() == 1) {
                          dismiss();
                          Toast.makeText(
                                  getActivity(),
                                  getString(R.string.no_device_found),
                                  Toast.LENGTH_SHORT)
                              .show();
                          MainActivity mainActivity = (MainActivity) getActivity();
                          mainActivity.showSMBDialog("", "", false);
                          return;
                        }
                        computers.remove(computers.size() - 1);
                        listViewAdapter.notifyDataSetChanged();
                      });
            }
          }
        });
    subnetScanner.execute();

    builder.adapter(listViewAdapter, null);
    return builder.build();
  }

  private class ListViewAdapter extends RecyclerView.Adapter<ElementViewHolder> {
    private static final int VIEW_PROGRESSBAR = 1;
    private static final int VIEW_ELEMENT = 2;

    private ArrayList<ComputerParcelable> items;
    private LayoutInflater mInflater;

    ListViewAdapter(Context context, List<ComputerParcelable> objects) {
      items = new ArrayList<>(objects);
      mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    @NonNull
    public ElementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view;
      switch (viewType) {
        case VIEW_PROGRESSBAR:
          view = mInflater.inflate(R.layout.smb_progress_row, parent, false);
          return new ElementViewHolder(view);
        default:
        case VIEW_ELEMENT:
          view = mInflater.inflate(R.layout.smb_computers_row, parent, false);
          return new ElementViewHolder(view);
      }
    }

    @Override
    public void onBindViewHolder(@NonNull ElementViewHolder holder, int position) {
      int viewType = getItemViewType(position);
      if (viewType == VIEW_PROGRESSBAR) {
        return;
      }

      ComputerParcelable f = items.get(position);

      holder.rootView.setOnClickListener(
          v -> {
            if (subnetScanner != null) subnetScanner.cancel(true);
            if (getActivity() != null && getActivity() instanceof MainActivity) {
              dismiss();
              MainActivity mainActivity = (MainActivity) getActivity();
              mainActivity.showSMBDialog(
                  listViewAdapter.items.get(position).getName(),
                  listViewAdapter.items.get(position).getAddr(),
                  false);
            }
          });

      holder.txtTitle.setText(f.getName());
      holder.image.setImageResource(R.drawable.ic_settings_remote_white_48dp);
      if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
        holder.image.setColorFilter(Color.parseColor("#666666"));
      holder.txtDesc.setText(f.getAddr());
    }

    @Override
    public int getItemViewType(int position) {
      ComputerParcelable f = items.get(position);
      if (f.getAddr().equals("-1")) {
        return VIEW_PROGRESSBAR;
      } else {
        return VIEW_ELEMENT;
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

  private static class ElementViewHolder extends RecyclerView.ViewHolder {
    private View rootView;

    private ImageView image;
    private TextView txtTitle;
    private TextView txtDesc;

    ElementViewHolder(View view) {
      super(view);

      rootView = view;

      txtTitle = view.findViewById(R.id.firstline);
      image = view.findViewById(R.id.icon);
      txtDesc = view.findViewById(R.id.secondLine);
    }
  }
}
