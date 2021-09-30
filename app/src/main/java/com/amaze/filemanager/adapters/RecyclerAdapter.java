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

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.*;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORIZE_ICONS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HEADERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_USE_CIRCULAR_IMAGES;

import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.adapters.glide.RecyclerPreloadModelProvider;
import com.amaze.filemanager.adapters.glide.RecyclerPreloadSizeProvider;
import com.amaze.filemanager.adapters.holders.EmptyViewHolder;
import com.amaze.filemanager.adapters.holders.ItemViewHolder;
import com.amaze.filemanager.adapters.holders.SpecialViewHolder;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.ui.ItemPopupMenu;
import com.amaze.filemanager.ui.activities.superclasses.PreferenceActivity;
import com.amaze.filemanager.ui.colors.ColorUtils;
import com.amaze.filemanager.ui.drag.RecyclerAdapterDragListener;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.utils.AnimUtils;
import com.amaze.filemanager.utils.GlideConstants;
import com.amaze.filemanager.utils.Utils;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This class is the information that serves to load the files into a "list" (a RecyclerView). There
 * are 3 types of item TYPE_ITEM, TYPE_HEADER_FOLDERS and TYPE_HEADER_FILES, EMPTY_LAST_ITEM and
 * TYPE_BACK represeted by ItemViewHolder, SpecialViewHolder and EmptyViewHolder respectively. The
 * showPopup shows the file's popup menu. The 'go to parent' aka '..' button (go to settings to
 * activate it) is just a folder.
 *
 * <p>Created by Arpit on 11-04-2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com> edited
 * by Jens Klingenberg <mail@jensklingenberg.de>
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements RecyclerPreloadSizeProvider.RecyclerPreloadSizeProviderCallback {

  public static final int TYPE_ITEM = 0,
      TYPE_HEADER_FOLDERS = 1,
      TYPE_HEADER_FILES = 2,
      EMPTY_LAST_ITEM = 3,
      TYPE_BACK = 4;
  private static final int VIEW_GENERIC = 0, VIEW_PICTURE = 1, VIEW_APK = 2, VIEW_THUMB = 3;

  public boolean stoppedAnimation = false;

  private PreferenceActivity preferenceActivity;
  @NonNull private final UtilitiesProvider utilsProvider;
  @NonNull private final MainFragment mainFrag;
  @NonNull private final SharedPreferences sharedPrefs;
  private RecyclerViewPreloader<IconDataParcelable> preloader;
  private RecyclerPreloadSizeProvider sizeProvider;
  private RecyclerPreloadModelProvider modelProvider;
  private ArrayList<ListItem> itemsDigested = new ArrayList<>();
  @NonNull private final Context context;
  private LayoutInflater mInflater;
  private float minRowHeight;
  private int grey_color,
      accentColor,
      iconSkinColor,
      goBackColor,
      videoColor,
      audioColor,
      pdfColor,
      codeColor,
      textColor,
      archiveColor,
      genericColor,
      apkColor;
  private int offset = 0;
  private boolean enableMarquee;
  private int dragAndDropPreference;
  private boolean isGrid;

  public RecyclerAdapter(
      PreferenceActivity preferenceActivity,
      @NonNull MainFragment m,
      @NonNull UtilitiesProvider utilsProvider,
      @NonNull SharedPreferences sharedPrefs,
      RecyclerView recyclerView,
      @NonNull List<LayoutElementParcelable> itemsRaw,
      @NonNull Context context,
      boolean isGrid) {
    setHasStableIds(true);

    this.preferenceActivity = preferenceActivity;
    this.mainFrag = m;
    this.utilsProvider = utilsProvider;
    this.context = context;
    this.sharedPrefs = sharedPrefs;
    this.enableMarquee =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME, true);
    this.dragAndDropPreference =
        sharedPrefs.getInt(
            PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
            PreferencesConstants.PREFERENCE_DRAG_DEFAULT);
    this.isGrid = isGrid;

    mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    accentColor = m.getMainActivity().getAccent();
    iconSkinColor = m.getMainActivity().getCurrentColorPreference().getIconSkin();
    goBackColor = Utils.getColor(context, R.color.goback_item);
    videoColor = Utils.getColor(context, R.color.video_item);
    audioColor = Utils.getColor(context, R.color.audio_item);
    pdfColor = Utils.getColor(context, R.color.pdf_item);
    codeColor = Utils.getColor(context, R.color.code_item);
    textColor = Utils.getColor(context, R.color.text_item);
    archiveColor = Utils.getColor(context, R.color.archive_item);
    genericColor = Utils.getColor(context, R.color.generic_item);
    minRowHeight = context.getResources().getDimension(R.dimen.minimal_row_height);
    grey_color = Utils.getColor(context, R.color.grey);
    apkColor = Utils.getColor(context, R.color.apk_item);

    setItems(recyclerView, itemsRaw, false);
  }

  /**
   * called as to toggle selection of any item in adapter
   *
   * @param position the position of the item
   * @param imageView the check {@link CircleGradientDrawable} that is to be animated
   */
  public void toggleChecked(int position, ImageView imageView) {
    if (itemsDigested.size() <= position || position < 0) {
      AppConfig.toast(context, R.string.operation_not_supported);
      return;
    }

    if (itemsDigested.get(position).getChecked() == ListItem.UNCHECKABLE) {
      throw new IllegalArgumentException("You have checked a header");
    }

    if (!stoppedAnimation) mainFrag.stopAnimation();
    if (itemsDigested.get(position).getChecked() == ListItem.CHECKED) {
      // if the view at position is checked, un-check it
      Log.d(
          getClass().getSimpleName(),
          String.format("the view at position %s is checked, un-check it", position));
      itemsDigested.get(position).setChecked(false);

      Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_out);
      if (imageView != null) {
        imageView.clearAnimation();
        imageView.startAnimation(iconAnimation);
      } else {
        // TODO: we don't have the check icon object probably because of config change
      }
    } else {
      // if view is un-checked, check it
      Log.d(
          getClass().getSimpleName(),
          String.format("the view at position %s is unchecked, check it", position));
      itemsDigested.get(position).setChecked(true);

      Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_in);
      if (imageView != null) {
        imageView.clearAnimation();
        imageView.startAnimation(iconAnimation);
      } else {
        // TODO: we don't have the check icon object probably because of config change
      }
      if (mainFrag.mActionMode == null
          || (mainFrag.getMainFragmentViewModel() != null
              && !mainFrag.getMainFragmentViewModel().getSelection())) {
        // start actionmode if not already started
        // null condition if there is config change
        if (mainFrag.getMainFragmentViewModel() != null) {
          mainFrag.getMainFragmentViewModel().setSelection(true);
        }
        mainFrag.mActionMode =
            mainFrag.getMainActivity().startSupportActionMode(mainFrag.mActionModeCallback);
      }
    }

    notifyItemChanged(position);
    invalidateActionMode();
  }

  public void invalidateActionMode() {
    if (mainFrag.mActionMode != null
        && mainFrag.getMainFragmentViewModel() != null
        && mainFrag.getMainFragmentViewModel().getSelection()) {
      // we have the actionmode visible, invalidate it's views
      mainFrag.mActionMode.invalidate();
    }
    if (getCheckedItems().size() == 0) {
      mainFrag.disableActionMode();
    }
  }

  public void toggleChecked(boolean b, String path) {
    int i = path.equals("/") || !getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON) ? 0 : 1;

    for (; i < itemsDigested.size(); i++) {
      ListItem item = itemsDigested.get(i);
      if (b && item.getChecked() != ListItem.CHECKED) {
        item.setChecked(true);
        notifyItemChanged(i);
      } else if (!b && item.getChecked() == ListItem.CHECKED) {
        item.setChecked(false);
        notifyItemChanged(i);
      }
    }
    invalidateActionMode();
  }

  /**
   * called when we would want to toggle check for all items in the adapter
   *
   * @param b if to toggle true or false
   */
  public void toggleChecked(boolean b) {
    for (int i = 0; i < itemsDigested.size(); i++) {
      ListItem item = itemsDigested.get(i);
      if (b && item.getChecked() != ListItem.CHECKED) {
        item.setChecked(true);
        notifyItemChanged(i);
      } else if (!b && item.getChecked() == ListItem.CHECKED) {
        item.setChecked(false);
        notifyItemChanged(i);
      }
    }
    invalidateActionMode();
  }

  public ArrayList<LayoutElementParcelable> getCheckedItems() {
    ArrayList<LayoutElementParcelable> selected = new ArrayList<>();

    for (int i = 0; i < itemsDigested.size(); i++) {
      if (itemsDigested.get(i).getChecked() == ListItem.CHECKED) {
        selected.add(itemsDigested.get(i).elem);
      }
    }

    return selected;
  }

  public ArrayList<ListItem> getItemsDigested() {
    return itemsDigested;
  }

  public boolean areAllChecked(String path) {
    boolean allChecked = true;
    int i = (path.equals("/") || !getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON)) ? 0 : 1;

    for (; i < itemsDigested.size(); i++) {
      if (itemsDigested.get(i).getChecked() == ListItem.NOT_CHECKED) {
        allChecked = false;
      }
    }

    return allChecked;
  }

  public ArrayList<Integer> getCheckedItemsIndex() {
    ArrayList<Integer> checked = new ArrayList<>();

    for (int i = 0; i < itemsDigested.size(); i++) {
      if (itemsDigested.get(i).getChecked() == ListItem.CHECKED) {
        checked.add(i);
      }
    }

    return checked;
  }

  @Override
  public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
    if (holder instanceof ItemViewHolder) {
      ((ItemViewHolder) holder).rl.clearAnimation();
      ((ItemViewHolder) holder).txtTitle.setSelected(false);
      if (dragAndDropPreference != PreferencesConstants.PREFERENCE_DRAG_DEFAULT) {
        ((ItemViewHolder) holder).rl.setOnDragListener(null);
      }
    }
    super.onViewDetachedFromWindow(holder);
  }

  @Override
  public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    boolean enableMarqueeFilename =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME, true);
    if (enableMarqueeFilename && holder instanceof ItemViewHolder) {
      AnimUtils.marqueeAfterDelay(2000, ((ItemViewHolder) holder).txtTitle);
    }
    super.onViewAttachedToWindow(holder);
  }

  @Override
  public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
    ((ItemViewHolder) holder).rl.clearAnimation();
    ((ItemViewHolder) holder).txtTitle.setSelected(false);
    return super.onFailedToRecycleView(holder);
  }

  private void animate(ItemViewHolder holder) {
    holder.rl.clearAnimation();
    Animation localAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_top);
    localAnimation.setStartOffset(this.offset);
    holder.rl.startAnimation(localAnimation);
    this.offset += 30;
  }

  /**
   * Adds item to the end of the list, don't use this unless you are dynamically loading the
   * adapter, after you are finished you must call createHeaders
   */
  public void addItem(LayoutElementParcelable e) {
    // TODO: simplify if condition
    if (mainFrag.getMainFragmentViewModel() != null
        && mainFrag.getMainFragmentViewModel().isList()
        && itemsDigested.size() > 0) {
      itemsDigested.add(itemsDigested.size() - 1, new ListItem(e));
    } else if (mainFrag.getMainFragmentViewModel() != null
        && mainFrag.getMainFragmentViewModel().isList()) {
      itemsDigested.add(new ListItem(e));
      itemsDigested.add(new ListItem(EMPTY_LAST_ITEM));
    } else {
      itemsDigested.add(new ListItem(e));
    }

    notifyItemInserted(getItemCount());
  }

  public void setItems(RecyclerView recyclerView, @NonNull List<LayoutElementParcelable> elements) {
    setItems(recyclerView, elements, true);
  }

  private void setItems(
      RecyclerView recyclerView,
      @NonNull List<LayoutElementParcelable> elements,
      boolean invalidate) {
    if (preloader != null) {
      recyclerView.removeOnScrollListener(preloader);
      preloader = null;
    }

    itemsDigested.clear();
    offset = 0;
    stoppedAnimation = false;

    ArrayList<IconDataParcelable> uris = new ArrayList<>(itemsDigested.size());

    for (LayoutElementParcelable e : elements) {
      itemsDigested.add(new ListItem(e.isBack, e));
      uris.add(e != null ? e.iconData : null);
    }

    if (mainFrag.getMainFragmentViewModel() != null
        && mainFrag.getMainFragmentViewModel().isList()
        && itemsDigested.size() > 0) {
      itemsDigested.add(new ListItem(EMPTY_LAST_ITEM));
      uris.add(null);
    }

    for (int i = 0; i < itemsDigested.size(); i++) {
      itemsDigested.get(i).setAnimate(false);
    }

    if (getBoolean(PREFERENCE_SHOW_HEADERS)) {
      createHeaders(invalidate, uris);
    }

    boolean isItemCircular = !isGrid;

    sizeProvider = new RecyclerPreloadSizeProvider(this);
    modelProvider = new RecyclerPreloadModelProvider(mainFrag, uris, isItemCircular);

    preloader =
        new RecyclerViewPreloader<>(
            GlideApp.with(mainFrag), modelProvider, sizeProvider, GlideConstants.MAX_PRELOAD_FILES);

    recyclerView.addOnScrollListener(preloader);
  }

  public void createHeaders(boolean invalidate, List<IconDataParcelable> uris) {
    boolean[] headers = new boolean[] {false, false};

    for (int i = 0; i < itemsDigested.size(); i++) {

      if (itemsDigested.get(i).elem != null) {
        LayoutElementParcelable nextItem = itemsDigested.get(i).elem;

        if (!headers[0] && nextItem.isDirectory) {
          headers[0] = true;
          itemsDigested.add(i, new ListItem(TYPE_HEADER_FOLDERS));
          uris.add(i, null);
          continue;
        }

        if (!headers[1]
            && !nextItem.isDirectory
            && !nextItem.title.equals(".")
            && !nextItem.title.equals("..")) {
          headers[1] = true;
          itemsDigested.add(i, new ListItem(TYPE_HEADER_FILES));
          uris.add(i, null);
          continue; // leave this continue for symmetry
        }
      }
    }

    if (invalidate) {
      notifyDataSetChanged();
    }
  }

  @Override
  public int getItemCount() {
    return itemsDigested.size();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    if (itemsDigested.get(position).specialType != -1) {
      return itemsDigested.get(position).specialType;
    } else {
      return TYPE_ITEM;
    }
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view;

    switch (viewType) {
      case TYPE_HEADER_FOLDERS:
      case TYPE_HEADER_FILES:
        if (mainFrag.getMainFragmentViewModel() != null
            && mainFrag.getMainFragmentViewModel().isList()) {

          view = mInflater.inflate(R.layout.list_header, parent, false);
        } else {

          view = mInflater.inflate(R.layout.grid_header, parent, false);
        }

        int type =
            viewType == TYPE_HEADER_FOLDERS
                ? SpecialViewHolder.HEADER_FOLDERS
                : SpecialViewHolder.HEADER_FILES;
        return new SpecialViewHolder(context, view, utilsProvider, type);
      case TYPE_ITEM:
      case TYPE_BACK:
        if (mainFrag.getMainFragmentViewModel() != null
            && mainFrag.getMainFragmentViewModel().isList()) {
          view = mInflater.inflate(R.layout.rowlayout, parent, false);
          sizeProvider.addView(VIEW_GENERIC, view.findViewById(R.id.generic_icon));
          sizeProvider.addView(VIEW_PICTURE, view.findViewById(R.id.picture_icon));
          sizeProvider.addView(VIEW_APK, view.findViewById(R.id.apk_icon));
        } else {
          view = mInflater.inflate(R.layout.griditem, parent, false);
          sizeProvider.addView(VIEW_GENERIC, view.findViewById(R.id.generic_icon));
          sizeProvider.addView(VIEW_THUMB, view.findViewById(R.id.icon_thumb));
        }
        sizeProvider.closeOffAddition();

        return new ItemViewHolder(view);
      case EMPTY_LAST_ITEM:
        int totalFabHeight = (int) context.getResources().getDimension(R.dimen.fab_height),
            marginFab = (int) context.getResources().getDimension(R.dimen.fab_margin);
        view = new View(context);
        view.setMinimumHeight(totalFabHeight + marginFab);
        return new EmptyViewHolder(view);
      default:
        throw new IllegalArgumentException("Illegal: " + viewType);
    }
  }

  @Override
  public void onBindViewHolder(final RecyclerView.ViewHolder vholder, int p) {
    if (vholder instanceof ItemViewHolder) {
      final ItemViewHolder holder = (ItemViewHolder) vholder;
      holder.rl.setOnFocusChangeListener(
          (v, hasFocus) -> {
            if (hasFocus) {
              mainFrag.adjustListViewForTv(holder, mainFrag.getMainActivity());
            }
          });
      holder.txtTitle.setEllipsize(
          enableMarquee ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.MIDDLE);
      final boolean isBackButton = itemsDigested.get(p).specialType == TYPE_BACK;
      if (isBackButton) {
        holder.about.setVisibility(View.GONE);
      }
      if (mainFrag.getMainFragmentViewModel() != null
          && mainFrag.getMainFragmentViewModel().isList()) {
        if (p == getItemCount() - 1) {
          holder.rl.setMinimumHeight((int) minRowHeight);
          if (itemsDigested.size() == (getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON) ? 1 : 0))
            holder.txtTitle.setText(R.string.no_files);
          else {
            holder.txtTitle.setText("");
          }
          return;
        }
      }
      if (!this.stoppedAnimation && !itemsDigested.get(p).getAnimating()) {
        animate(holder);
        itemsDigested.get(p).setAnimate(true);
      }
      final LayoutElementParcelable rowItem = itemsDigested.get(p).elem;
      if (dragAndDropPreference != PreferencesConstants.PREFERENCE_DRAG_DEFAULT) {
        holder.rl.setOnDragListener(
            new RecyclerAdapterDragListener(this, holder, dragAndDropPreference, mainFrag));
      }

      holder.rl.setOnLongClickListener(
          p1 -> {
            if (!isBackButton) {
              if (dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_DEFAULT
                  || (dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_TO_MOVE_COPY
                      && itemsDigested.get(vholder.getAdapterPosition()).getChecked()
                          != ListItem.CHECKED)) {
                toggleChecked(
                    vholder.getAdapterPosition(),
                    mainFrag.getMainFragmentViewModel().isList()
                        ? holder.checkImageView
                        : holder.checkImageViewGrid);
              }
              initDragListener(p, p1, holder);
            }
            return true;
          });
      if (mainFrag.getMainFragmentViewModel().isList()) {
        // clear previously cached icon
        GlideApp.with(mainFrag).clear(holder.genericIcon);
        GlideApp.with(mainFrag).clear(holder.pictureIcon);
        GlideApp.with(mainFrag).clear(holder.apkIcon);
        GlideApp.with(mainFrag).clear(holder.rl);

        holder.rl.setOnClickListener(
            v -> {
              mainFrag.onListItemClicked(
                  isBackButton, vholder.getAdapterPosition(), rowItem, holder.checkImageView);
            });

        holder.about.setOnKeyListener(
            (v, keyCode, event) -> {
              if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                  mainFrag.getMainActivity().getFAB().requestFocus();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
                  showPopup(v, rowItem);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                  mainFrag.getMainActivity().onBackPressed();
                } else {
                  return false;
                }
              }
              return true;
            });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          holder.checkImageView.setBackground(
              new CircleGradientDrawable(
                  accentColor,
                  utilsProvider.getAppTheme(),
                  mainFrag.getResources().getDisplayMetrics()));
        } else {
          holder.checkImageView.setBackgroundDrawable(
              new CircleGradientDrawable(
                  accentColor,
                  utilsProvider.getAppTheme(),
                  mainFrag.getResources().getDisplayMetrics()));
        }
        holder.txtTitle.setText(rowItem.title);
        holder.genericText.setText("");

        if (holder.about != null) {
          if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
            holder.about.setColorFilter(grey_color);
          holder.about.setOnClickListener(v -> showPopup(v, rowItem));
        }
        holder.genericIcon.setOnClickListener(
            v -> {
              int id = v.getId();
              if (id == R.id.generic_icon || id == R.id.picture_icon || id == R.id.apk_icon) {
                // TODO: transform icon on press to the properties dialog with animation
                if (!isBackButton) {
                  toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                } else mainFrag.goBack();
              }
            });

        holder.pictureIcon.setOnClickListener(
            view -> {
              if (!isBackButton) {
                toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
              } else mainFrag.goBack();
            });

        holder.apkIcon.setOnClickListener(
            view -> {
              if (!isBackButton) {
                toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
              } else mainFrag.goBack();
            });

        // resetting icons visibility
        holder.genericIcon.setVisibility(View.VISIBLE);
        holder.pictureIcon.setVisibility(View.INVISIBLE);
        holder.apkIcon.setVisibility(View.INVISIBLE);
        holder.checkImageView.setVisibility(View.INVISIBLE);

        // setting icons for various cases
        // apkIcon holder refers to square/non-circular drawable
        // pictureIcon is circular drawable
        switch (rowItem.filetype) {
          case Icons.IMAGE:
          case Icons.VIDEO:
            if (getBoolean(PREFERENCE_SHOW_THUMB)) {
              if (getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)) {
                showThumbnailWithBackground(
                    holder, rowItem.iconData, holder.pictureIcon, rowItem.iconData::setImageBroken);
              } else {
                showThumbnailWithBackground(
                    holder, rowItem.iconData, holder.apkIcon, rowItem.iconData::setImageBroken);
              }
            } else {
              holder.genericIcon.setImageResource(
                  rowItem.filetype == Icons.IMAGE
                      ? R.drawable.ic_doc_image
                      : R.drawable.ic_doc_video_am);
            }
            break;
          case Icons.APK:
            if (getBoolean(PREFERENCE_SHOW_THUMB)) {
              showThumbnailWithBackground(
                  holder, rowItem.iconData, holder.apkIcon, rowItem.iconData::setImageBroken);
            } else {
              holder.genericIcon.setImageResource(R.drawable.ic_doc_apk_white);
            }
            break;
          case Icons.NOT_KNOWN:
            holder.genericIcon.setVisibility(View.VISIBLE);
            // if the file type is any unknown variable
            String ext = !rowItem.isDirectory ? MimeTypes.getExtension(rowItem.title) : null;
            if (ext != null && ext.trim().length() != 0) {
              holder.genericText.setText(ext);
              holder.genericIcon.setImageDrawable(null);
              holder.genericIcon.setVisibility(View.INVISIBLE);
            } else {
              // we could not find the extension, set a generic file type icon probably a directory
              modelProvider.getPreloadRequestBuilder(rowItem.iconData).into(holder.genericIcon);
            }
            break;
          case Icons.ENCRYPTED:
          default:
            holder.genericIcon.setVisibility(View.VISIBLE);
            modelProvider.getPreloadRequestBuilder(rowItem.iconData).into(holder.genericIcon);
            break;
        }

        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
          holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
        } else {
          holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
        }
        holder.rl.setSelected(false);
        if (itemsDigested.get(p).getChecked() == ListItem.CHECKED) {

          if (holder.checkImageView.getVisibility() == View.INVISIBLE)
            holder.checkImageView.setVisibility(View.VISIBLE);
          // making sure the generic icon background color filter doesn't get changed
          // to grey on picture/video/apk/generic text icons when checked
          // so that user can still look at the thumbs even after selection
          if ((rowItem.filetype != Icons.IMAGE
                  && rowItem.filetype != Icons.APK
                  && rowItem.filetype != Icons.VIDEO)
              || !getBoolean(PREFERENCE_SHOW_THUMB)) {
            holder.apkIcon.setVisibility(View.GONE);
            holder.pictureIcon.setVisibility(View.GONE);
            holder.genericIcon.setVisibility(View.VISIBLE);
            GradientDrawable gradientDrawable =
                (GradientDrawable) holder.genericIcon.getBackground();
            gradientDrawable.setColor(goBackColor);
          }
          holder.rl.setSelected(true);
          // holder.genericText.setText("");
        } else {
          holder.checkImageView.setVisibility(View.INVISIBLE);
          if (!((rowItem.filetype == Icons.APK
                  || rowItem.filetype == Icons.IMAGE
                  || rowItem.filetype == Icons.VIDEO)
              && getBoolean(PREFERENCE_SHOW_THUMB))) {
            holder.genericIcon.setVisibility(View.VISIBLE);
            GradientDrawable gradientDrawable =
                (GradientDrawable) holder.genericIcon.getBackground();

            if (getBoolean(PREFERENCE_COLORIZE_ICONS)) {
              if (rowItem.isDirectory) {
                gradientDrawable.setColor(iconSkinColor);
              } else {
                ColorUtils.colorizeIcons(
                    context, rowItem.filetype, gradientDrawable, iconSkinColor);
              }
            } else gradientDrawable.setColor(iconSkinColor);

            if (isBackButton) {
              gradientDrawable.setColor(goBackColor);
            }
          }
        }
        if (getBoolean(PREFERENCE_SHOW_PERMISSIONS)) holder.perm.setText(rowItem.permissions);
        if (getBoolean(PREFERENCE_SHOW_LAST_MODIFIED)) {
          holder.date.setText(rowItem.dateModification);
        } else {
          holder.date.setVisibility(View.GONE);
        }
        if (isBackButton) {
          holder.date.setText(rowItem.size);
          holder.txtDesc.setText("");
        } else if (getBoolean(PREFERENCE_SHOW_FILE_SIZE)) {
          holder.txtDesc.setText(rowItem.size);
        }
      } else {
        // view is a grid view
        // clear previously cached icon
        GlideApp.with(mainFrag).clear(holder.genericIcon);
        GlideApp.with(mainFrag).clear(holder.iconLayout);
        GlideApp.with(mainFrag).clear(holder.imageView1);
        GlideApp.with(mainFrag).clear(holder.rl);

        holder.checkImageViewGrid.setColorFilter(accentColor);
        holder.rl.setOnClickListener(
            v -> {
              mainFrag.onListItemClicked(
                  isBackButton, vholder.getAdapterPosition(), rowItem, holder.checkImageViewGrid);
            });
        holder.txtTitle.setText(rowItem.title);
        holder.imageView1.setVisibility(View.INVISIBLE);
        holder.genericIcon.setVisibility(View.VISIBLE);
        holder.checkImageViewGrid.setVisibility(View.INVISIBLE);

        if (rowItem.filetype == Icons.IMAGE || rowItem.filetype == Icons.VIDEO) {
          if (getBoolean(PREFERENCE_SHOW_THUMB)) {
            holder.imageView1.setVisibility(View.VISIBLE);
            holder.imageView1.setImageDrawable(null);
            if (utilsProvider.getAppTheme().equals(AppTheme.DARK)
                || utilsProvider.getAppTheme().equals(AppTheme.BLACK))
              holder.imageView1.setBackgroundColor(Color.BLACK);
            showRoundedThumbnail(
                holder, rowItem.iconData, holder.imageView1, rowItem.iconData::setImageBroken);
          } else {
            if (rowItem.filetype == Icons.IMAGE)
              holder.genericIcon.setImageResource(R.drawable.ic_doc_image);
            else holder.genericIcon.setImageResource(R.drawable.ic_doc_video_am);
          }
        } else if (rowItem.filetype == Icons.APK) {
          if (getBoolean(PREFERENCE_SHOW_THUMB))
            showRoundedThumbnail(
                holder, rowItem.iconData, holder.genericIcon, rowItem.iconData::setImageBroken);
          else {
            holder.genericIcon.setImageResource(R.drawable.ic_doc_apk_white);
          }
        } else {
          GlideApp.with(mainFrag).load(rowItem.iconData.image).into(holder.genericIcon);
        }

        if (holder.genericIcon.getVisibility() == View.VISIBLE) {
          View iconBackground =
              getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES) ? holder.genericIcon : holder.iconLayout;
          if (rowItem.isDirectory) {
            iconBackground.setBackgroundColor(iconSkinColor);
          } else {
            switch (rowItem.filetype) {
              case Icons.VIDEO:
                if (!getBoolean(PREFERENCE_SHOW_THUMB))
                  iconBackground.setBackgroundColor(videoColor);
                break;
              case Icons.AUDIO:
                iconBackground.setBackgroundColor(audioColor);
                break;
              case Icons.PDF:
                iconBackground.setBackgroundColor(pdfColor);
                break;
              case Icons.CODE:
                iconBackground.setBackgroundColor(codeColor);
                break;
              case Icons.TEXT:
                iconBackground.setBackgroundColor(textColor);
                break;
              case Icons.COMPRESSED:
                iconBackground.setBackgroundColor(archiveColor);
                break;
              case Icons.NOT_KNOWN:
                iconBackground.setBackgroundColor(genericColor);
                break;
              case Icons.APK:
                if (!getBoolean(PREFERENCE_SHOW_THUMB)) iconBackground.setBackgroundColor(apkColor);
                break;
              case Icons.IMAGE:
                if (!getBoolean(PREFERENCE_SHOW_THUMB))
                  iconBackground.setBackgroundColor(videoColor);
                break;
              default:
                iconBackground.setBackgroundColor(iconSkinColor);
                break;
            }
          }

          if (isBackButton) {
            iconBackground.setBackgroundColor(goBackColor);
          }
        }

        if (itemsDigested.get(p).getChecked() == ListItem.CHECKED) {
          if (holder.genericIcon.getVisibility() == View.VISIBLE) {

            if ((rowItem.filetype != Icons.IMAGE
                    && rowItem.filetype != Icons.APK
                    && rowItem.filetype != Icons.VIDEO)
                || !getBoolean(PREFERENCE_SHOW_THUMB)) {
              View iconBackground =
                  getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)
                      ? holder.genericIcon
                      : holder.iconLayout;
              iconBackground.setBackgroundColor(goBackColor);
            }
          }

          holder.checkImageViewGrid.setVisibility(View.VISIBLE);
          holder.rl.setBackgroundColor(Utils.getColor(context, R.color.item_background));
        } else {
          holder.checkImageViewGrid.setVisibility(View.INVISIBLE);
          if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
            holder.rl.setBackgroundResource(R.drawable.item_doc_grid);
          else {
            holder.rl.setBackgroundResource(R.drawable.ic_grid_card_background_dark);
            holder
                .rl
                .findViewById(R.id.icon_frame_grid)
                .setBackgroundColor(Utils.getColor(context, R.color.icon_background_dark));
          }
        }

        if (holder.about != null) {
          if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
            holder.about.setColorFilter(grey_color);
          holder.about.setOnClickListener(v -> showPopup(v, rowItem));
        }

        if (getBoolean(PREFERENCE_SHOW_LAST_MODIFIED)) {
          holder.date.setText(rowItem.dateModification);
        }
        if (isBackButton) {
          holder.date.setText(rowItem.size);
          holder.txtDesc.setText("");
        }
        if (getBoolean(PREFERENCE_SHOW_PERMISSIONS)) holder.perm.setText(rowItem.permissions);
      }
    }
  }

  @Override
  public int getCorrectView(IconDataParcelable item, int adapterPosition) {
    if (mainFrag.getMainFragmentViewModel() != null
        && mainFrag.getMainFragmentViewModel().isList()) {
      if (getBoolean(PREFERENCE_SHOW_THUMB)) {
        int filetype = itemsDigested.get(adapterPosition).elem.filetype;

        if (filetype == Icons.VIDEO || filetype == Icons.IMAGE) {
          if (getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)) {
            return VIEW_PICTURE;
          } else {
            return VIEW_APK;
          }
        } else if (filetype == Icons.APK) {
          return VIEW_APK;
        }
      }

      return VIEW_GENERIC;
    } else {
      if (item.type == IconDataParcelable.IMAGE_FROMFILE) {
        return VIEW_THUMB;
      } else {
        return VIEW_GENERIC;
      }
    }
  }

  private void initDragListener(int position, View view, ItemViewHolder itemViewHolder) {
    if (dragAndDropPreference != PreferencesConstants.PREFERENCE_DRAG_DEFAULT
        && (itemsDigested.get(position).getChecked() == ListItem.CHECKED
            || dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_TO_SELECT)) {
      // toggle drag flag to true for list item due to the fact
      // that we might have set it false in a previous drag event
      if (!itemsDigested.get(position).shouldToggleDragChecked) {
        itemsDigested.get(position).toggleShouldToggleDragChecked();
      }

      View shadowView =
          dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_TO_SELECT
              ? itemViewHolder.dummyView
              : getDragShadow(getCheckedItems().size());
      View.DragShadowBuilder dragShadowBuilder = new View.DragShadowBuilder(shadowView);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        view.startDragAndDrop(null, dragShadowBuilder, null, 0);
      } else {
        view.startDrag(null, dragShadowBuilder, null, 0);
      }
      mainFrag
          .getMainActivity()
          .initCornersDragListener(
              false, dragAndDropPreference != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT);
    }
  }

  private View getDragShadow(int selectionCount) {
    mainFrag.getMainActivity().getTabFragment().getDragPlaceholder().setVisibility(View.VISIBLE);
    String rememberMovePreference =
        sharedPrefs.getString(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_REMEMBERED, "");
    ImageView icon =
        mainFrag.getMainActivity().getTabFragment().getDragPlaceholder().findViewById(R.id.icon);
    View filesCountParent =
        mainFrag
            .getMainActivity()
            .getTabFragment()
            .getDragPlaceholder()
            .findViewById(R.id.files_count_parent);
    TextView filesCount =
        mainFrag
            .getMainActivity()
            .getTabFragment()
            .getDragPlaceholder()
            .findViewById(R.id.files_count);
    icon.setImageDrawable(
        context.getResources().getDrawable(getDragIconReference(rememberMovePreference)));
    GradientDrawable gradientDrawable = (GradientDrawable) icon.getBackground();
    gradientDrawable.setColor(grey_color);
    filesCount.setText(String.valueOf(selectionCount));
    filesCountParent.setBackgroundDrawable(
        new CircleGradientDrawable(
            accentColor, utilsProvider.getAppTheme(), mainFrag.getResources().getDisplayMetrics()));
    return mainFrag.getMainActivity().getTabFragment().getDragPlaceholder();
  }

  private int getDragIconReference(String rememberMovePreference) {
    int iconRef = R.drawable.ic_add_white_24dp;
    if (rememberMovePreference.equalsIgnoreCase(
        PreferencesConstants.PREFERENCE_DRAG_REMEMBER_MOVE)) {
      iconRef = R.drawable.ic_content_cut_white_36dp;
    } else if (rememberMovePreference.equalsIgnoreCase(
        PreferencesConstants.PREFERENCE_DRAG_REMEMBER_COPY)) {
      iconRef = R.drawable.ic_content_copy_white_24dp;
    }
    return iconRef;
  }

  private void showThumbnailWithBackground(
      ItemViewHolder viewHolder,
      IconDataParcelable iconData,
      ImageView view,
      OnImageProcessed errorListener) {
    if (iconData.isImageBroken()) {
      viewHolder.genericIcon.setVisibility(View.VISIBLE);
      GlideApp.with(mainFrag)
          .load(R.drawable.ic_broken_image_white_24dp)
          .into(viewHolder.genericIcon);
      GradientDrawable gradientDrawable = (GradientDrawable) viewHolder.genericIcon.getBackground();
      gradientDrawable.setColor(grey_color);

      errorListener.onImageProcessed(true);
      return;
    }

    viewHolder.genericIcon.setVisibility(View.VISIBLE);
    GlideApp.with(mainFrag).load(iconData.loadingImage).into(viewHolder.genericIcon);
    GradientDrawable gradientDrawable = (GradientDrawable) viewHolder.genericIcon.getBackground();

    RequestListener<Drawable> requestListener =
        new RequestListener<Drawable>() {

          @Override
          public boolean onLoadFailed(
              @Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            new Handler(
                    msg -> {
                      viewHolder.genericIcon.setVisibility(View.VISIBLE);
                      GlideApp.with(mainFrag)
                          .load(R.drawable.ic_broken_image_white_24dp)
                          .into(viewHolder.genericIcon);
                      return false;
                    })
                .obtainMessage()
                .sendToTarget();
            gradientDrawable.setColor(grey_color);

            errorListener.onImageProcessed(true);
            return true;
          }

          @Override
          public boolean onResourceReady(
              Drawable resource,
              Object model,
              Target<Drawable> target,
              DataSource dataSource,
              boolean isFirstResource) {
            viewHolder.genericIcon.setImageDrawable(null);
            viewHolder.genericIcon.setVisibility(View.GONE);
            gradientDrawable.setColor(
                mainFrag.getResources().getColor(android.R.color.transparent));
            view.setVisibility(View.VISIBLE);

            errorListener.onImageProcessed(false);
            return false;
          }
        };
    modelProvider.getPreloadRequestBuilder(iconData).listener(requestListener).into(view);
  }

  private void showRoundedThumbnail(
      ItemViewHolder viewHolder,
      IconDataParcelable iconData,
      ImageView view,
      OnImageProcessed errorListener) {
    if (iconData.isImageBroken()) {
      View iconBackground =
          getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)
              ? viewHolder.genericIcon
              : viewHolder.iconLayout;

      viewHolder.genericIcon.setVisibility(View.VISIBLE);
      iconBackground.setBackgroundColor(grey_color);
      GlideApp.with(mainFrag)
          .load(R.drawable.ic_broken_image_white_24dp)
          .into(viewHolder.genericIcon);
      view.setVisibility(View.INVISIBLE);

      errorListener.onImageProcessed(true);
      return;
    }

    View iconBackground =
        getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES) ? viewHolder.genericIcon : viewHolder.iconLayout;

    viewHolder.genericIcon.setVisibility(View.VISIBLE);
    GlideApp.with(mainFrag).load(iconData.loadingImage).into(viewHolder.genericIcon);
    view.setVisibility(View.INVISIBLE);

    RequestListener<Drawable> requestListener =
        new RequestListener<Drawable>() {
          @Override
          public boolean onLoadFailed(
              @Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            iconBackground.setBackgroundColor(grey_color);
            new Handler(
                    msg -> {
                      GlideApp.with(mainFrag)
                          .load(R.drawable.ic_broken_image_white_24dp)
                          .into(viewHolder.genericIcon);
                      return false;
                    })
                .obtainMessage()
                .sendToTarget();
            errorListener.onImageProcessed(true);
            return true;
          }

          @Override
          public boolean onResourceReady(
              Drawable resource,
              Object model,
              Target<Drawable> target,
              DataSource dataSource,
              boolean isFirstResource) {
            viewHolder.genericIcon.setImageDrawable(null);
            viewHolder.genericIcon.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
            iconBackground.setBackgroundColor(
                mainFrag.getResources().getColor(android.R.color.transparent));
            errorListener.onImageProcessed(false);
            return false;
          }
        };
    modelProvider.getPreloadRequestBuilder(iconData).listener(requestListener).into(view);
  }

  private void showPopup(@NonNull View view, @NonNull final LayoutElementParcelable rowItem) {
    Context currentContext = this.context;
    if (mainFrag.getMainActivity().getAppTheme().getSimpleTheme() == AppTheme.BLACK) {
      currentContext = new ContextThemeWrapper(context, R.style.overflow_black);
    }
    PopupMenu popupMenu =
        new ItemPopupMenu(
            currentContext,
            mainFrag.requireMainActivity(),
            utilsProvider,
            mainFrag,
            rowItem,
            view,
            sharedPrefs);
    popupMenu.inflate(R.menu.item_extras);
    String description = rowItem.desc.toLowerCase();

    if (rowItem.isDirectory) {
      popupMenu.getMenu().findItem(R.id.open_with).setVisible(false);
      popupMenu.getMenu().findItem(R.id.share).setVisible(false);

      if (mainFrag.getMainActivity().mReturnIntent) {
        popupMenu.getMenu().findItem(R.id.return_select).setVisible(true);
      }
    } else {
      popupMenu.getMenu().findItem(R.id.book).setVisible(false);
    }

    if (description.endsWith(fileExtensionZip)
        || description.endsWith(fileExtensionJar)
        || description.endsWith(fileExtensionApk)
        || description.endsWith(fileExtensionApks)
        || description.endsWith(fileExtensionRar)
        || description.endsWith(fileExtensionTar)
        || description.endsWith(fileExtensionGzipTarLong)
        || description.endsWith(fileExtensionGzipTarShort)
        || description.endsWith(fileExtensionBzip2TarLong)
        || description.endsWith(fileExtensionBzip2TarShort)
        || description.endsWith(fileExtensionXz)
        || description.endsWith(fileExtensionLzma)
        || description.endsWith(fileExtension7zip))
      popupMenu.getMenu().findItem(R.id.ex).setVisible(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (description.endsWith(CryptUtil.CRYPT_EXTENSION))
        popupMenu.getMenu().findItem(R.id.decrypt).setVisible(true);
      else popupMenu.getMenu().findItem(R.id.encrypt).setVisible(true);
    }

    popupMenu.show();
  }

  private boolean getBoolean(String key) {
    return preferenceActivity.getBoolean(key);
  }

  @IntDef({TYPE_ITEM, TYPE_HEADER_FOLDERS, TYPE_HEADER_FILES, EMPTY_LAST_ITEM, TYPE_BACK})
  public @interface ListElemType {}

  public static class ListItem {
    public static final int CHECKED = 0, NOT_CHECKED = 1, UNCHECKABLE = 2;

    private LayoutElementParcelable elem;
    private @ListElemType int specialType;
    private boolean checked;
    private boolean animate;
    private boolean shouldToggleDragChecked = true;

    ListItem(LayoutElementParcelable elem) {
      this(false, elem);
    }

    ListItem(boolean isBack, LayoutElementParcelable elem) {
      this.elem = elem;
      specialType = isBack ? TYPE_BACK : TYPE_ITEM;
    }

    ListItem(@ListElemType int specialType) {
      this.specialType = specialType;
    }

    public void setChecked(boolean checked) {
      if (specialType == TYPE_ITEM) this.checked = checked;
    }

    public int getChecked() {
      if (checked) return CHECKED;
      else if (specialType == TYPE_ITEM) return NOT_CHECKED;
      else return UNCHECKABLE;
    }

    public LayoutElementParcelable getElem() {
      return elem;
    }

    public int getSpecialType() {
      return this.specialType;
    }

    public boolean getShouldToggleDragChecked() {
      return !checked && this.shouldToggleDragChecked;
    }

    public void toggleShouldToggleDragChecked() {
      this.shouldToggleDragChecked = !this.shouldToggleDragChecked;
    }

    public void setAnimate(boolean animating) {
      if (specialType == -1) this.animate = animating;
    }

    public boolean getAnimating() {
      return animate;
    }
  }

  public interface OnImageProcessed {
    void onImageProcessed(boolean isImageBroken);
  }
}
