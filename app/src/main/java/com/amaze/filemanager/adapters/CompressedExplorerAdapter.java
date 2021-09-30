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

package com.amaze.filemanager.adapters;

import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.adapters.holders.CompressedItemViewHolder;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.ui.colors.ColorUtils;
import com.amaze.filemanager.ui.fragments.CompressedExplorerFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.utils.AnimUtils;
import com.amaze.filemanager.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

/** Created by Arpit on 25-01-2015 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com> */
public class CompressedExplorerAdapter extends RecyclerView.Adapter<CompressedItemViewHolder> {

  private static final int TYPE_HEADER = 0, TYPE_ITEM = 1;

  public boolean stoppedAnimation = false;

  private Context context;
  private UtilitiesProvider utilsProvider;
  private Drawable folder;
  private List<CompressedObjectParcelable> items;
  private CompressedExplorerFragment compressedExplorerFragment;
  private Decompressor decompressor;
  private LayoutInflater mInflater;
  private boolean[] itemsChecked;
  private int offset = 0;
  private SharedPreferences sharedPrefs;

  public CompressedExplorerAdapter(
      Context c,
      UtilitiesProvider utilsProvider,
      List<CompressedObjectParcelable> items,
      CompressedExplorerFragment compressedExplorerFragment,
      Decompressor decompressor,
      SharedPreferences sharedPrefs) {
    setHasStableIds(true);

    this.utilsProvider = utilsProvider;
    this.items = items;
    this.decompressor = decompressor;

    itemsChecked = new boolean[items.size()];

    context = c;
    if (c == null) return;
    folder = c.getResources().getDrawable(R.drawable.ic_grid_folder_new);
    this.compressedExplorerFragment = compressedExplorerFragment;
    mInflater = (LayoutInflater) c.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    this.sharedPrefs = sharedPrefs;
  }

  public void toggleChecked(boolean check) {
    int k = 0;

    for (int i = k; i < items.size(); i++) {
      itemsChecked[i] = check;
      notifyItemChanged(i);
    }
  }

  public ArrayList<Integer> getCheckedItemPositions() {
    ArrayList<Integer> checkedItemPositions = new ArrayList<>();

    for (int i = 0; i < itemsChecked.length; i++) {
      if (itemsChecked[i]) {
        (checkedItemPositions).add(i);
      }
    }

    return checkedItemPositions;
  }

  /**
   * called as to toggle selection of any item in adapter
   *
   * @param position the position of the item
   * @param imageView the circular {@link CircleGradientDrawable} that is to be animated
   */
  private void toggleChecked(int position, ImageView imageView) {
    compressedExplorerFragment.stopAnim();
    stoppedAnimation = true;

    Animation animation;
    if (itemsChecked[position]) {
      animation = AnimationUtils.loadAnimation(context, R.anim.check_out);
    } else {
      animation = AnimationUtils.loadAnimation(context, R.anim.check_in);
    }

    if (imageView != null) {
      imageView.setAnimation(animation);
    } else {
      // TODO: we don't have the check icon object probably because of config change
    }

    itemsChecked[position] = !itemsChecked[position];

    notifyDataSetChanged();
    if (!compressedExplorerFragment.selection || compressedExplorerFragment.mActionMode == null) {
      compressedExplorerFragment.selection = true;
      /*compressedExplorerFragment.mActionMode = compressedExplorerFragment.getActivity().startActionMode(
      compressedExplorerFragment.mActionModeCallback);*/
      compressedExplorerFragment.mActionMode =
          compressedExplorerFragment
              .requireMainActivity()
              .getAppbar()
              .getToolbar()
              .startActionMode(compressedExplorerFragment.mActionModeCallback);
    }
    compressedExplorerFragment.mActionMode.invalidate();
    if (getCheckedItemPositions().size() == 0) {
      compressedExplorerFragment.selection = false;
      compressedExplorerFragment.mActionMode.finish();
      compressedExplorerFragment.mActionMode = null;
    }
  }

  private void animate(CompressedItemViewHolder holder) {
    holder.rl.clearAnimation();
    Animation localAnimation =
        AnimationUtils.loadAnimation(compressedExplorerFragment.getActivity(), R.anim.fade_in_top);
    localAnimation.setStartOffset(this.offset);
    holder.rl.startAnimation(localAnimation);
    this.offset = (30 + this.offset);
  }

  public void generateZip(List<CompressedObjectParcelable> arrayList) {
    offset = 0;
    stoppedAnimation = false;
    items = arrayList;
    notifyDataSetChanged();
    itemsChecked = new boolean[items.size()];
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    if (isPositionHeader(position)) return TYPE_HEADER;

    return TYPE_ITEM;
  }

  @Override
  public CompressedItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      View v = mInflater.inflate(R.layout.rowlayout, parent, false);
      v.findViewById(R.id.picture_icon).setVisibility(View.INVISIBLE);
      return new CompressedItemViewHolder(v);
    } else if (viewType == TYPE_ITEM) {
      View v = mInflater.inflate(R.layout.rowlayout, parent, false);
      CompressedItemViewHolder vh = new CompressedItemViewHolder(v);
      ImageButton about = v.findViewById(R.id.properties);
      about.setVisibility(View.INVISIBLE);
      return vh;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void onBindViewHolder(final CompressedItemViewHolder holder, int position) {
    if (!stoppedAnimation) {
      animate(holder);
    }

    boolean enableMarquee =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME, true);
    holder.txtTitle.setEllipsize(
        enableMarquee ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.MIDDLE);

    final CompressedObjectParcelable rowItem = items.get(position);
    GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      holder.checkImageView.setBackground(
          new CircleGradientDrawable(
              compressedExplorerFragment.accentColor,
              utilsProvider.getAppTheme(),
              compressedExplorerFragment.getResources().getDisplayMetrics()));
    } else
      holder.checkImageView.setBackgroundDrawable(
          new CircleGradientDrawable(
              compressedExplorerFragment.accentColor,
              utilsProvider.getAppTheme(),
              compressedExplorerFragment.getResources().getDisplayMetrics()));

    if (rowItem.type == CompressedObjectParcelable.TYPE_GOBACK) {
      GlideApp.with(compressedExplorerFragment)
          .load(R.drawable.ic_arrow_left_white_24dp)
          .into(holder.genericIcon);
      gradientDrawable.setColor(Utils.getColor(context, R.color.goback_item));
      holder.txtTitle.setText("..");
      holder.txtDesc.setText("");
      holder.date.setText(R.string.goback);
    } else {
      GlideApp.with(compressedExplorerFragment)
          .load(rowItem.iconData.image)
          .into(holder.genericIcon);

      if (compressedExplorerFragment.showLastModified)
        holder.date.setText(Utils.getDate(context, rowItem.date));
      if (rowItem.directory) {
        holder.genericIcon.setImageDrawable(folder);
        gradientDrawable.setColor(compressedExplorerFragment.iconskin);
        holder.txtTitle.setText(rowItem.name);
      } else {
        if (compressedExplorerFragment.showSize)
          holder.txtDesc.setText(Formatter.formatFileSize(context, rowItem.size));
        holder.txtTitle.setText(rowItem.path.substring(rowItem.path.lastIndexOf("/") + 1));
        if (compressedExplorerFragment.coloriseIcons) {
          ColorUtils.colorizeIcons(
              context, rowItem.filetype, gradientDrawable, compressedExplorerFragment.iconskin);
        } else gradientDrawable.setColor(compressedExplorerFragment.iconskin);
      }
    }

    holder.rl.setOnLongClickListener(
        view -> {
          if (rowItem.type != CompressedObjectParcelable.TYPE_GOBACK) {
            toggleChecked(position, holder.checkImageView);
          }
          return true;
        });
    holder.genericIcon.setOnClickListener(
        view -> {
          if (rowItem.type != CompressedObjectParcelable.TYPE_GOBACK) {
            toggleChecked(position, holder.checkImageView);
          } else {
            compressedExplorerFragment.goBack();
          }
        });
    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
      holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
    } else {
      holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
    }
    holder.rl.setSelected(false);
    if (itemsChecked[position]) {
      // holder.genericIcon.setImageDrawable(compressedExplorerFragment.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
      holder.checkImageView.setVisibility(View.VISIBLE);
      gradientDrawable.setColor(Utils.getColor(context, R.color.goback_item));
      holder.rl.setSelected(true);
    } else holder.checkImageView.setVisibility(View.INVISIBLE);

    holder.rl.setOnClickListener(
        p1 -> {
          if (rowItem.type == CompressedObjectParcelable.TYPE_GOBACK)
            compressedExplorerFragment.goBack();
          else {
            if (compressedExplorerFragment.selection) {
              toggleChecked(position, holder.checkImageView);
            } else {
              if (rowItem.directory) {
                String newPath =
                    (rowItem.path.endsWith("/"))
                        ? rowItem.path.substring(0, rowItem.path.length() - 1)
                        : rowItem.path;
                compressedExplorerFragment.changePath(newPath);
              } else {

                String fileName =
                    CompressedHelper.getFileName(
                        compressedExplorerFragment.compressedFile.getName());
                String archiveCacheDirPath =
                    compressedExplorerFragment.getActivity().getExternalCacheDir().getPath()
                        + CompressedHelper.SEPARATOR
                        + fileName;

                HybridFileParcelable file =
                    new HybridFileParcelable(
                        archiveCacheDirPath
                            + CompressedHelper.SEPARATOR
                            + rowItem.path.replaceAll("\\\\", CompressedHelper.SEPARATOR));
                file.setMode(OpenMode.FILE);
                // this file will be opened once service finishes up it's extraction
                compressedExplorerFragment.files.add(file);
                // setting flag for binder to know
                compressedExplorerFragment.isOpen = true;

                Toast.makeText(
                        compressedExplorerFragment.getContext(),
                        compressedExplorerFragment.getContext().getString(R.string.please_wait),
                        Toast.LENGTH_SHORT)
                    .show();
                decompressor.decompress(
                    compressedExplorerFragment.getActivity().getExternalCacheDir().getPath(),
                    new String[] {rowItem.path});
              }
            }
          }
        });
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public void onViewDetachedFromWindow(CompressedItemViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    holder.rl.clearAnimation();
    holder.txtTitle.setSelected(false);
  }

  @Override
  public void onViewAttachedToWindow(CompressedItemViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    boolean enableMarqueeFilename =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME, true);
    if (enableMarqueeFilename) {
      AnimUtils.marqueeAfterDelay(2000, holder.txtTitle);
    }
  }

  @Override
  public boolean onFailedToRecycleView(CompressedItemViewHolder holder) {
    holder.rl.clearAnimation();
    holder.txtTitle.setSelected(false);
    return super.onFailedToRecycleView(holder);
  }

  private boolean isPositionHeader(int position) {
    return false; // TODO:
  }
}
