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
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_COLORIZE_ICONS;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HEADERS;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_USE_CIRCULAR_IMAGES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.PasteHelper;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.sort.DirSortBy;
import com.amaze.filemanager.ui.ItemPopupMenu;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.PreferenceActivity;
import com.amaze.filemanager.ui.colors.ColorUtils;
import com.amaze.filemanager.ui.drag.RecyclerAdapterDragListener;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.selection.SelectionPopupMenu;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.utils.AnimUtils;
import com.amaze.filemanager.utils.GlideConstants;
import com.amaze.filemanager.utils.MainActivityActionMode;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
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
  private final Logger LOG = LoggerFactory.getLogger(RecyclerAdapter.class);

  private static final int VIEW_GENERIC = 0, VIEW_PICTURE = 1, VIEW_APK = 2, VIEW_THUMB = 3;

  public boolean stoppedAnimation = false;

  @NonNull private final PreferenceActivity preferenceActivity;
  @NonNull private final UtilitiesProvider utilsProvider;
  @NonNull private final MainFragment mainFragment;
  @NonNull private final SharedPreferences sharedPrefs;
  private RecyclerViewPreloader<IconDataParcelable> preloader;
  private RecyclerPreloadSizeProvider sizeProvider;
  private RecyclerPreloadModelProvider modelProvider;
  @NonNull private final Context context;
  private final LayoutInflater mInflater;
  private final float minRowHeight;
  private final int grey_color;
  private final int accentColor;
  private final int iconSkinColor;
  private final int goBackColor;
  private final int videoColor;
  private final int audioColor;
  private final int pdfColor;
  private final int codeColor;
  private final int textColor;
  private final int archiveColor;
  private final int genericColor;
  private final int apkColor;
  private int offset = 0;
  private final boolean enableMarquee;
  private final int dragAndDropPreference;
  private final boolean isGrid;

  @IntDef({VIEW_GENERIC, VIEW_PICTURE, VIEW_APK, VIEW_THUMB})
  public @interface ViewType {}

  @IntDef({TYPE_ITEM, TYPE_HEADER_FOLDERS, TYPE_HEADER_FILES, EMPTY_LAST_ITEM, TYPE_BACK})
  public @interface ListElemType {}

  public RecyclerAdapter(
      @NonNull PreferenceActivity preferenceActivity,
      @NonNull MainFragment mainFragment,
      @NonNull UtilitiesProvider utilsProvider,
      @NonNull SharedPreferences sharedPrefs,
      @NonNull RecyclerView recyclerView,
      @NonNull List<LayoutElementParcelable> itemsRaw,
      @NonNull Context context,
      boolean isGrid) {
    setHasStableIds(true);

    this.preferenceActivity = preferenceActivity;
    this.mainFragment = mainFragment;
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
    accentColor = mainFragment.getMainActivity().getAccent();
    iconSkinColor = mainFragment.getMainActivity().getCurrentColorPreference().getIconSkin();
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
  public void toggleChecked(int position, AppCompatImageView imageView) {
    if (getItemsDigested().size() <= position || position < 0) {
      AppConfig.toast(context, R.string.operation_not_supported);
      return;
    }

    if (getItemsDigested().get(position).getChecked() == ListItem.UNCHECKABLE) {
      throw new IllegalArgumentException("You have checked a header");
    }

    if (!stoppedAnimation) {
      mainFragment.stopAnimation();
    }

    if (getItemsDigested().get(position).getChecked() == ListItem.CHECKED) {
      // if the view at position is checked, un-check it
      LOG.debug("the view at position {} is checked, un-check it", position);
      getItemsDigested().get(position).setChecked(false);

      Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_out);
      if (imageView != null) {
        imageView.clearAnimation();
        imageView.startAnimation(iconAnimation);
      } else {
        // TODO: we don't have the check icon object probably because of config change
      }
    } else {
      // if view is un-checked, check it
      LOG.debug("the view at position {} is unchecked, check it", position);
      getItemsDigested().get(position).setChecked(true);

      Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_in);
      if (imageView != null) {
        imageView.clearAnimation();
        imageView.startAnimation(iconAnimation);
      } else {
        // TODO: we don't have the check icon object probably because of config change
      }
    }

    invalidateSelection();
    notifyItemChanged(position);
  }

  private void invalidateSelection() {
    if (mainFragment.getMainFragmentViewModel() != null) {
      mainFragment
          .getMainActivity()
          .setListItemSelected(
              mainFragment.getMainFragmentViewModel().getCheckedItems().size() != 0);
    }
  }

  public void invalidateActionMode() {
    if (mainFragment.getMainFragmentViewModel() != null) {
      // we have the actionmode visible, invalidate it's views
      if (mainFragment.getMainActivity().getListItemSelected()) {
        if (mainFragment.getMainActivity().getActionModeHelper().getActionMode() == null) {
          ActionMode.Callback mActionModeCallback =
              mainFragment.getMainActivity().getActionModeHelper();
          mainFragment
              .getMainActivity()
              .getActionModeHelper()
              .setActionMode(
                  mainFragment.getMainActivity().startSupportActionMode(mActionModeCallback));
        } else {
          mainFragment.getMainActivity().getActionModeHelper().getActionMode().invalidate();
        }
      } else {
        if (mainFragment.getMainActivity().getActionModeHelper().getActionMode() != null) {
          mainFragment.getMainActivity().getActionModeHelper().getActionMode().finish();
          mainFragment.getMainActivity().getActionModeHelper().setActionMode(null);
        }
      }
    }
  }

  public void toggleChecked(boolean selectAll, String path) {
    int i = path.equals("/") || !getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON) ? 0 : 1;
    for (; i < getItemsDigested().size(); i++) {
      ListItem item = getItemsDigested().get(i);
      if (selectAll && item.getChecked() != ListItem.CHECKED) {
        item.setChecked(true);
        notifyItemChanged(i);
      } else if (!selectAll && item.getChecked() == ListItem.CHECKED) {
        item.setChecked(false);
        notifyItemChanged(i);
      }
    }
    invalidateSelection();
    invalidateActionMode();
  }

  public void toggleInverse(String path) {
    int i = path.equals("/") || !getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON) ? 0 : 1;

    for (; i < getItemsDigested().size(); i++) {
      ListItem item = getItemsDigested().get(i);
      if (item.getChecked() != ListItem.CHECKED) {
        item.setChecked(true);
        notifyItemChanged(i);
      } else if (item.getChecked() == ListItem.CHECKED) {
        item.setChecked(false);
        notifyItemChanged(i);
      }
    }
  }

  public void toggleSameTypes() {
    ArrayList<Integer> checkedItemsIndexes = getCheckedItemsIndex();
    for (int i = 0; i < checkedItemsIndexes.size(); i++) {
      ListItem selectedItem = getItemsDigested().get(checkedItemsIndexes.get(i));

      if (!selectedItem.specialTypeHasFile()) {
        continue;
      }

      LayoutElementParcelable selectedElement = selectedItem.requireLayoutElementParcelable();
      for (int z = 0; z < getItemsDigested().size(); z++) {
        ListItem currentItem = getItemsDigested().get(z);
        if (!currentItem.specialTypeHasFile()) {
          // header type list item ('Files' / 'Folders')
          continue;
        }

        LayoutElementParcelable currentElement = currentItem.requireLayoutElementParcelable();
        if (selectedElement.isDirectory || currentElement.isDirectory) {
          if (selectedElement.isDirectory && currentElement.isDirectory) {
            if (currentItem.getChecked() != ListItem.CHECKED) {
              currentItem.setChecked(true);
              notifyItemChanged(z);
            }
          }
        } else {
          String mimeTypeCurrentItem = MimeTypes.getExtension(currentElement.desc);
          String mimeTypeSelectedElement = MimeTypes.getExtension(selectedElement.desc);
          if (mimeTypeCurrentItem.equalsIgnoreCase(mimeTypeSelectedElement)
              && currentItem.getChecked() != ListItem.CHECKED) {
            currentItem.setChecked(true);
            notifyItemChanged(z);
          }
        }
      }
    }
  }

  public void toggleSameDates() {
    ArrayList<Integer> checkedItemsIndexes = getCheckedItemsIndex();
    for (int i = 0; i < checkedItemsIndexes.size(); i++) {
      ListItem selectedItem = getItemsDigested().get(checkedItemsIndexes.get(i));

      if (!selectedItem.specialTypeHasFile()) {
        continue;
      }

      LayoutElementParcelable selectedElement = selectedItem.requireLayoutElementParcelable();
      for (int y = 0; y < getItemsDigested().size(); y++) {
        ListItem currentItem = getItemsDigested().get(y);
        if (!currentItem.specialTypeHasFile()) {
          // header type list item ('Files' / 'Folders')
          continue;
        }
        String dateModifiedCurrentItem =
            currentItem.requireLayoutElementParcelable().dateModification.split("\\|")[0];
        String dateModifiedSelectedElement = selectedElement.dateModification.split("\\|")[0];
        if (dateModifiedCurrentItem.trim().equalsIgnoreCase(dateModifiedSelectedElement.trim())
            && currentItem.getChecked() != ListItem.CHECKED) {
          currentItem.setChecked(true);
          notifyItemChanged(y);
        }
      }
    }
  }

  public void toggleFill() {
    ArrayList<Integer> checkedItemsIndexes = getCheckedItemsIndex();
    Collections.sort(checkedItemsIndexes);
    if (checkedItemsIndexes.size() >= 2) {
      for (int i = checkedItemsIndexes.get(0);
          i < checkedItemsIndexes.get(checkedItemsIndexes.size() - 1);
          i++) {
        Objects.requireNonNull(getItemsDigested()).get(i).setChecked(true);
        notifyItemChanged(i);
      }
    }
  }

  public void toggleSimilarNames() {
    ArrayList<Integer> checkedItemsIndexes = getCheckedItemsIndex();
    for (int i = 0; i < checkedItemsIndexes.size(); i++) {
      ListItem selectedItem = getItemsDigested().get(checkedItemsIndexes.get(i));

      if (!selectedItem.specialTypeHasFile()) {
        continue;
      }

      LayoutElementParcelable selectedElement = selectedItem.requireLayoutElementParcelable();
      int fuzzinessFactor = selectedElement.title.length() / SelectionPopupMenu.FUZZYNESS_FACTOR;
      if (fuzzinessFactor >= 1) {
        for (int z = 0; z < getItemsDigested().size(); z++) {
          ListItem currentItem = getItemsDigested().get(z);
          if (!currentItem.specialTypeHasFile()) {
            // header type list item ('Files' / 'Folders')
            continue;
          }
          int remainingFuzzyness = fuzzinessFactor;

          char[] currentItemName = currentItem.requireLayoutElementParcelable().title.toCharArray();
          char[] selectedElementName = selectedElement.title.toCharArray();
          boolean isSimilar = true;
          for (int j = 0; j < Math.min(currentItemName.length, selectedElementName.length); j++) {
            if (currentItemName[j] != selectedElementName[j] && remainingFuzzyness-- < 0) {
              isSimilar = false;
              break;
            }
          }
          if (isSimilar
              && Math.abs(currentItemName.length - selectedElementName.length)
                  <= remainingFuzzyness) {
            if (currentItem.getChecked() != ListItem.CHECKED) {
              currentItem.setChecked(true);
              notifyItemChanged(z);
            }
          }
        }
      }
    }
  }

  /**
   * called when we would want to toggle check for all items in the adapter
   *
   * @param b if to toggle true or false
   */
  public void toggleChecked(boolean b) {
    for (int i = 0; i < getItemsDigested().size(); i++) {
      ListItem item = getItemsDigested().get(i);
      if (b && item.getChecked() != ListItem.CHECKED) {
        item.setChecked(true);
        notifyItemChanged(i);
      } else if (!b && item.getChecked() == ListItem.CHECKED) {
        item.setChecked(false);
        notifyItemChanged(i);
      }
    }
    invalidateSelection();
    invalidateActionMode();
  }

  @NonNull
  public ArrayList<LayoutElementParcelable> getCheckedItems() {
    return mainFragment.getMainFragmentViewModel().getCheckedItems();
  }

  @Nullable
  public ArrayList<ListItem> getItemsDigested() {
    return mainFragment.getMainFragmentViewModel() != null
        ? mainFragment.getMainFragmentViewModel().getAdapterListItems()
        : null;
  }

  public boolean isItemsDigestedNullOrEmpty() {
    return getItemsDigested() == null || getItemsDigested().isEmpty();
  }

  public boolean areAllChecked(String path) {
    int i = (path.equals("/") || !getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON)) ? 0 : 1;

    for (; i < getItemsDigested().size(); i++) {
      if (getItemsDigested().get(i).getChecked() == ListItem.NOT_CHECKED) {
        return false;
      }
    }
    return true;
  }

  public ArrayList<Integer> getCheckedItemsIndex() {
    ArrayList<Integer> checked = new ArrayList<>();

    for (int i = 0; i < getItemsDigested().size(); i++) {
      if (getItemsDigested().get(i).getChecked() == ListItem.CHECKED) {
        checked.add(i);
      }
    }

    return checked;
  }

  @Override
  public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
    if (holder instanceof ItemViewHolder) {
      ((ItemViewHolder) holder).baseItemView.clearAnimation();
      ((ItemViewHolder) holder).txtTitle.setSelected(false);
      if (dragAndDropPreference != PreferencesConstants.PREFERENCE_DRAG_DEFAULT) {
        ((ItemViewHolder) holder).baseItemView.setOnDragListener(null);
      }
    }
    super.onViewDetachedFromWindow(holder);
  }

  @Override
  public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    boolean enableMarqueeFilename =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME, true);
    if (enableMarqueeFilename && holder instanceof ItemViewHolder) {
      AnimUtils.marqueeAfterDelay(2000, ((ItemViewHolder) holder).txtTitle);
    }
    super.onViewAttachedToWindow(holder);
  }

  @Override
  public boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder) {
    ((ItemViewHolder) holder).baseItemView.clearAnimation();
    ((ItemViewHolder) holder).txtTitle.setSelected(false);
    return super.onFailedToRecycleView(holder);
  }

  private void animate(ItemViewHolder holder) {
    holder.baseItemView.clearAnimation();
    Animation localAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_top);
    localAnimation.setStartOffset(this.offset);
    holder.baseItemView.startAnimation(localAnimation);
    this.offset += 30;
  }

  /**
   * Adds item to the end of the list, don't use this unless you are dynamically loading the
   * adapter, after you are finished you must call createHeaders
   */
  public void addItem(@NonNull LayoutElementParcelable element) {
    // TODO: simplify if condition
    if (mainFragment.getMainFragmentViewModel() != null
        && mainFragment.getMainFragmentViewModel().isList()
        && getItemsDigested().size() > 0) {
      getItemsDigested().add(getItemsDigested().size() - 1, new ListItem(element));
    } else if (mainFragment.getMainFragmentViewModel() != null
        && mainFragment.getMainFragmentViewModel().isList()) {
      getItemsDigested().add(new ListItem(element));
      getItemsDigested().add(new ListItem(EMPTY_LAST_ITEM));
    } else {
      getItemsDigested().add(new ListItem(element));
    }

    notifyItemInserted(getItemCount());
  }

  public void setItems(
      @NonNull RecyclerView recyclerView, @NonNull List<LayoutElementParcelable> elements) {
    setItems(recyclerView, elements, true);
  }

  private void setItems(
      @NonNull RecyclerView recyclerView,
      @NonNull List<LayoutElementParcelable> elements,
      boolean invalidate) {
    if (preloader != null) {
      recyclerView.removeOnScrollListener(preloader);
      preloader = null;
    }

    if (getItemsDigested() != null && invalidate) {
      getItemsDigested().clear();
      if (mainFragment.getMainFragmentViewModel().getIconList() != null) {
        mainFragment.getMainFragmentViewModel().getIconList().clear();
      }
    }

    offset = 0;
    stoppedAnimation = false;

    ArrayList<IconDataParcelable> uris = new ArrayList<>();
    ArrayList<ListItem> listItems = new ArrayList<>();

    for (LayoutElementParcelable e : elements) {
      if (invalidate || isItemsDigestedNullOrEmpty()) {
        if (e != null) {
          listItems.add(new ListItem(e.isBack, e));
        }
        uris.add(e != null ? e.iconData : null);
      }
    }

    if (mainFragment.getMainFragmentViewModel() != null
        && mainFragment.getMainFragmentViewModel().isList()
        && listItems.size() > 0
        && (invalidate || isItemsDigestedNullOrEmpty())) {
      listItems.add(new ListItem(EMPTY_LAST_ITEM));
      uris.add(null);
    }

    if (invalidate || isItemsDigestedNullOrEmpty()) {
      mainFragment.getMainFragmentViewModel().setAdapterListItems(listItems);
      mainFragment.getMainFragmentViewModel().setIconList(uris);

      if (getBoolean(PREFERENCE_SHOW_HEADERS)) {
        createHeaders(invalidate, mainFragment.getMainFragmentViewModel().getIconList());
      }
    }

    boolean isItemCircular = !isGrid;

    sizeProvider = new RecyclerPreloadSizeProvider(this);
    modelProvider =
        new RecyclerPreloadModelProvider(
            mainFragment, mainFragment.getMainFragmentViewModel().getIconList(), isItemCircular);

    preloader =
        new RecyclerViewPreloader<>(
            GlideApp.with(mainFragment),
            modelProvider,
            sizeProvider,
            GlideConstants.MAX_PRELOAD_FILES);

    recyclerView.addOnScrollListener(preloader);
  }

  public void createHeaders(boolean invalidate, List<IconDataParcelable> uris) {
    if ((mainFragment.getMainFragmentViewModel() != null
            && mainFragment.getMainFragmentViewModel().getDsort() == DirSortBy.NONE_ON_TOP)
        || getItemsDigested() == null
        || getItemsDigested().isEmpty()) {
      return;
    } else {
      boolean[] headers = new boolean[] {false, false};

      for (int i = 0; i < getItemsDigested().size(); i++) {

        if (getItemsDigested().get(i).layoutElementParcelable != null) {
          LayoutElementParcelable nextItem = getItemsDigested().get(i).layoutElementParcelable;

          if (nextItem != null) {
            if (!headers[0] && nextItem.isDirectory) {
              headers[0] = true;
              getItemsDigested().add(i, new ListItem(TYPE_HEADER_FOLDERS));
              uris.add(i, null);
              continue;
            }

            if (!headers[1]
                && !nextItem.isDirectory
                && !nextItem.title.equals(".")
                && !nextItem.title.equals("..")) {
              headers[1] = true;
              getItemsDigested().add(i, new ListItem(TYPE_HEADER_FILES));
              uris.add(i, null);
              continue; // leave this continue for symmetry
            }
          }
        }
      }

      if (invalidate) {
        notifyDataSetChanged();
      }
    }
  }

  @Override
  public int getItemCount() {
    return getItemsDigested().size();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    if (getItemsDigested().get(position).specialType != -1) {
      return getItemsDigested().get(position).specialType;
    } else {
      return TYPE_ITEM;
    }
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view;

    switch (viewType) {
      case TYPE_HEADER_FOLDERS:
      case TYPE_HEADER_FILES:
        if (mainFragment.getMainFragmentViewModel() != null
            && mainFragment.getMainFragmentViewModel().isList()) {

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
        if (mainFragment.getMainFragmentViewModel() != null
            && mainFragment.getMainFragmentViewModel().isList()) {
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
  public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder vholder, int position) {
    if (!(vholder instanceof ItemViewHolder)) {
      return;
    }

    @NonNull final ItemViewHolder holder = (ItemViewHolder) vholder;

    holder.baseItemView.setOnFocusChangeListener(
        (v, hasFocus) -> {
          if (hasFocus) {
            mainFragment.adjustListViewForTv(holder, mainFragment.getMainActivity());
          }
        });
    holder.txtTitle.setEllipsize(
        enableMarquee ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.MIDDLE);

    final boolean isBackButton = getItemsDigested().get(position).specialType == TYPE_BACK;
    if (isBackButton) {
      holder.about.setVisibility(View.GONE);
    }

    if (!this.stoppedAnimation && !getItemsDigested().get(position).getAnimating()) {
      animate(holder);
      getItemsDigested().get(position).setAnimate(true);
    }

    if (dragAndDropPreference != PreferencesConstants.PREFERENCE_DRAG_DEFAULT) {
      holder.baseItemView.setOnDragListener(
          new RecyclerAdapterDragListener(this, holder, dragAndDropPreference, mainFragment));
    }

    if (mainFragment.getMainFragmentViewModel().isList()) {
      bindViewHolderList(holder, position);
    } else {
      bindViewHolderGrid(holder, position);
    }

    invalidateActionMode();
  }

  private void bindViewHolderList(@NonNull final ItemViewHolder holder, int position) {
    final boolean isBackButton = getItemsDigested().get(position).specialType == TYPE_BACK;
    @Nullable
    final LayoutElementParcelable rowItem =
        getItemsDigested().get(position).layoutElementParcelable;

    if (mainFragment.getMainFragmentViewModel() != null && position == getItemCount() - 1) {
      holder.baseItemView.setMinimumHeight((int) minRowHeight);
      if (getItemsDigested().size() == (getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON) ? 1 : 0))
        holder.txtTitle.setText(R.string.no_files);
      else {
        holder.txtTitle.setText("");
      }
      return;
    }

    holder.baseItemView.setOnLongClickListener(
        p1 -> {
          if (hasPendingPasteOperation()) return false;
          if (!isBackButton) {
            if (dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_DEFAULT
                || (dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_TO_MOVE_COPY
                    && getItemsDigested().get(holder.getAdapterPosition()).getChecked()
                        != ListItem.CHECKED)) {
              mainFragment.registerListItemChecked(
                  holder.getAdapterPosition(), holder.checkImageView);
            }
            initDragListener(position, p1, holder);
          }
          return true;
        });

    // clear previously cached icon
    GlideApp.with(mainFragment).clear(holder.genericIcon);
    GlideApp.with(mainFragment).clear(holder.pictureIcon);
    GlideApp.with(mainFragment).clear(holder.apkIcon);
    GlideApp.with(mainFragment).clear(holder.baseItemView);

    holder.baseItemView.setOnClickListener(
        v -> {
          mainFragment.onListItemClicked(
              isBackButton, holder.getAdapterPosition(), rowItem, holder.checkImageView);
        });

    holder.about.setOnKeyListener(
        (v, keyCode, event) -> {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
              mainFragment.getMainActivity().getFAB().requestFocus();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
              showPopup(v, rowItem);
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
              mainFragment.getMainActivity().onBackPressed();
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
              mainFragment.getResources().getDisplayMetrics()));
    } else {
      holder.checkImageView.setBackgroundDrawable(
          new CircleGradientDrawable(
              accentColor,
              utilsProvider.getAppTheme(),
              mainFragment.getResources().getDisplayMetrics()));
    }
    holder.txtTitle.setText(rowItem.title);
    holder.genericText.setText("");

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
      holder.about.setColorFilter(grey_color);
    }
    holder.about.setOnClickListener(v -> showPopup(v, rowItem));
    holder.genericIcon.setOnClickListener(
        v -> {
          int id = v.getId();
          if (id == R.id.generic_icon || id == R.id.picture_icon || id == R.id.apk_icon) {
            // TODO: transform icon on press to the properties dialog with animation
            if (!isBackButton) {
              toggleChecked(holder.getAdapterPosition(), holder.checkImageView);
            } else mainFragment.goBack();
          }
        });

    holder.pictureIcon.setOnClickListener(
        view -> {
          if (!isBackButton) {
            toggleChecked(holder.getAdapterPosition(), holder.checkImageView);
          } else mainFragment.goBack();
        });

    holder.apkIcon.setOnClickListener(
        view -> {
          if (!isBackButton) {
            toggleChecked(holder.getAdapterPosition(), holder.checkImageView);
          } else mainFragment.goBack();
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
        if (getBoolean(PREFERENCE_SHOW_THUMB) && rowItem.getMode() != OpenMode.FTP) {
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
      holder.baseItemView.setBackgroundResource(R.drawable.safr_ripple_white);
    } else {
      holder.baseItemView.setBackgroundResource(R.drawable.safr_ripple_black);
    }
    holder.baseItemView.setSelected(false);
    if (getItemsDigested().get(position).getChecked() == ListItem.CHECKED) {

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
        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();
        gradientDrawable.setColor(goBackColor);
      }
      holder.baseItemView.setSelected(true);
      // holder.genericText.setText("");
    } else {
      holder.checkImageView.setVisibility(View.INVISIBLE);
      if (!((rowItem.filetype == Icons.APK
              || rowItem.filetype == Icons.IMAGE
              || rowItem.filetype == Icons.VIDEO)
          && getBoolean(PREFERENCE_SHOW_THUMB))) {
        holder.genericIcon.setVisibility(View.VISIBLE);
        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();

        if (getBoolean(PREFERENCE_COLORIZE_ICONS)) {
          if (rowItem.isDirectory) {
            gradientDrawable.setColor(iconSkinColor);
          } else {
            ColorUtils.colorizeIcons(context, rowItem.filetype, gradientDrawable, iconSkinColor);
          }
        } else {
          gradientDrawable.setColor(iconSkinColor);
        }

        if (isBackButton) {
          gradientDrawable.setColor(goBackColor);
        }
      }
    }
    if (getBoolean(PREFERENCE_SHOW_PERMISSIONS)) {
      holder.perm.setText(rowItem.permissions);
    }
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
  }

  private void bindViewHolderGrid(@NonNull final ItemViewHolder holder, int position) {
    final boolean isBackButton = getItemsDigested().get(position).specialType == TYPE_BACK;
    @Nullable
    final LayoutElementParcelable rowItem =
        getItemsDigested().get(position).layoutElementParcelable;

    holder.baseItemView.setOnLongClickListener(
        p1 -> {
          if (hasPendingPasteOperation()) return false;
          if (!isBackButton) {
            if (dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_DEFAULT
                || (dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_TO_MOVE_COPY
                    && getItemsDigested().get(holder.getAdapterPosition()).getChecked()
                        != ListItem.CHECKED)) {
              mainFragment.registerListItemChecked(
                  holder.getAdapterPosition(), holder.checkImageViewGrid);
            }
            initDragListener(position, p1, holder);
          }
          return true;
        });

    // view is a grid view
    // clear previously cached icon
    GlideApp.with(mainFragment).clear(holder.genericIcon);
    GlideApp.with(mainFragment).clear(holder.iconLayout);
    GlideApp.with(mainFragment).clear(holder.imageView1);
    GlideApp.with(mainFragment).clear(holder.baseItemView);

    holder.checkImageViewGrid.setColorFilter(accentColor);
    holder.baseItemView.setOnClickListener(
        v -> {
          mainFragment.onListItemClicked(
              isBackButton, holder.getAdapterPosition(), rowItem, holder.checkImageViewGrid);
        });
    holder.txtTitle.setText(rowItem.title);
    holder.imageView1.setVisibility(View.INVISIBLE);
    holder.genericIcon.setVisibility(View.VISIBLE);
    holder.checkImageViewGrid.setVisibility(View.INVISIBLE);

    if (rowItem.filetype == Icons.IMAGE || rowItem.filetype == Icons.VIDEO) {
      if (getBoolean(PREFERENCE_SHOW_THUMB) && rowItem.getMode() != OpenMode.FTP) {
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
      GlideApp.with(mainFragment).load(rowItem.iconData.image).into(holder.genericIcon);
    }

    if (holder.genericIcon.getVisibility() == View.VISIBLE) {
      View iconBackground =
          getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES) ? holder.genericIcon : holder.iconLayout;
      if (rowItem.isDirectory) {
        iconBackground.setBackgroundColor(iconSkinColor);
      } else {
        switch (rowItem.filetype) {
          case Icons.VIDEO:
            if (!getBoolean(PREFERENCE_SHOW_THUMB)) iconBackground.setBackgroundColor(videoColor);
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
            if (!getBoolean(PREFERENCE_SHOW_THUMB)) iconBackground.setBackgroundColor(videoColor);
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

    if (getItemsDigested().get(position).getChecked() == ListItem.CHECKED) {
      if (holder.genericIcon.getVisibility() == View.VISIBLE) {

        if ((rowItem.filetype != Icons.IMAGE
                && rowItem.filetype != Icons.APK
                && rowItem.filetype != Icons.VIDEO)
            || !getBoolean(PREFERENCE_SHOW_THUMB)) {
          View iconBackground =
              getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES) ? holder.genericIcon : holder.iconLayout;
          iconBackground.setBackgroundColor(goBackColor);
        }
      }

      holder.checkImageViewGrid.setVisibility(View.VISIBLE);
      holder.baseItemView.setBackgroundColor(Utils.getColor(context, R.color.item_background));
    } else {
      holder.checkImageViewGrid.setVisibility(View.INVISIBLE);
      if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
        holder.baseItemView.setBackgroundResource(R.drawable.item_doc_grid);
      } else {
        holder.baseItemView.setBackgroundResource(R.drawable.ic_grid_card_background_dark);
        holder
            .baseItemView
            .findViewById(R.id.icon_frame_grid)
            .setBackgroundColor(Utils.getColor(context, R.color.icon_background_dark));
      }
    }

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
      holder.about.setColorFilter(grey_color);
    }
    holder.about.setOnClickListener(v -> showPopup(v, rowItem));

    if (getBoolean(PREFERENCE_SHOW_LAST_MODIFIED)) {
      holder.date.setText(rowItem.dateModification);
    }
    if (isBackButton) {
      holder.date.setText(rowItem.size);
      holder.txtDesc.setText("");
    }
    if (getBoolean(PREFERENCE_SHOW_PERMISSIONS)) {
      holder.perm.setText(rowItem.permissions);
    }
  }

  @Override
  @ViewType
  public int getCorrectView(IconDataParcelable item, int adapterPosition) {
    int specialType = getItemsDigested().get(adapterPosition).specialType;

    if (specialType != TYPE_ITEM && specialType != TYPE_BACK) { // These have no icons
      throw new IllegalStateException("Setting view type to wrong item");
    }

    if (mainFragment.getMainFragmentViewModel() != null
        && mainFragment.getMainFragmentViewModel().isList()) {
      if (getBoolean(PREFERENCE_SHOW_THUMB)) {
        int filetype =
            getItemsDigested().get(adapterPosition).requireLayoutElementParcelable().filetype;

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
        && (getItemsDigested().get(position).getChecked() == ListItem.CHECKED
            || dragAndDropPreference == PreferencesConstants.PREFERENCE_DRAG_TO_SELECT)) {
      // toggle drag flag to true for list item due to the fact
      // that we might have set it false in a previous drag event
      if (!getItemsDigested().get(position).shouldToggleDragChecked) {
        getItemsDigested().get(position).toggleShouldToggleDragChecked();
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
      mainFragment
          .getMainActivity()
          .initCornersDragListener(
              false, dragAndDropPreference != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT);
    }
  }

  private View getDragShadow(int selectionCount) {
    mainFragment
        .getMainActivity()
        .getTabFragment()
        .getDragPlaceholder()
        .setVisibility(View.VISIBLE);
    String rememberMovePreference =
        sharedPrefs.getString(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_REMEMBERED, "");
    AppCompatImageView icon =
        mainFragment
            .getMainActivity()
            .getTabFragment()
            .getDragPlaceholder()
            .findViewById(R.id.icon);
    View filesCountParent =
        mainFragment
            .getMainActivity()
            .getTabFragment()
            .getDragPlaceholder()
            .findViewById(R.id.files_count_parent);
    AppCompatTextView filesCount =
        mainFragment
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
            accentColor,
            utilsProvider.getAppTheme(),
            mainFragment.getResources().getDisplayMetrics()));
    return mainFragment.getMainActivity().getTabFragment().getDragPlaceholder();
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
      AppCompatImageView view,
      OnImageProcessed errorListener) {
    if (iconData.isImageBroken()) {
      viewHolder.genericIcon.setVisibility(View.VISIBLE);
      GlideApp.with(mainFragment)
          .load(R.drawable.ic_broken_image_white_24dp)
          .into(viewHolder.genericIcon);
      GradientDrawable gradientDrawable = (GradientDrawable) viewHolder.genericIcon.getBackground();
      gradientDrawable.setColor(grey_color);

      errorListener.onImageProcessed(true);
      return;
    }

    viewHolder.genericIcon.setVisibility(View.VISIBLE);
    GlideApp.with(mainFragment).load(iconData.loadingImage).into(viewHolder.genericIcon);
    GradientDrawable gradientDrawable = (GradientDrawable) viewHolder.genericIcon.getBackground();

    RequestListener<Drawable> requestListener =
        new RequestListener<Drawable>() {

          @Override
          public boolean onLoadFailed(
              @Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            new Handler(
                    msg -> {
                      viewHolder.genericIcon.setVisibility(View.VISIBLE);
                      GlideApp.with(mainFragment)
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
                mainFragment.getResources().getColor(android.R.color.transparent));
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
      AppCompatImageView view,
      OnImageProcessed errorListener) {
    if (iconData.isImageBroken()) {
      View iconBackground =
          getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)
              ? viewHolder.genericIcon
              : viewHolder.iconLayout;

      viewHolder.genericIcon.setVisibility(View.VISIBLE);
      iconBackground.setBackgroundColor(grey_color);
      GlideApp.with(mainFragment)
          .load(R.drawable.ic_broken_image_white_24dp)
          .into(viewHolder.genericIcon);
      view.setVisibility(View.INVISIBLE);

      errorListener.onImageProcessed(true);
      return;
    }

    View iconBackground =
        getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES) ? viewHolder.genericIcon : viewHolder.iconLayout;

    viewHolder.genericIcon.setVisibility(View.VISIBLE);
    GlideApp.with(mainFragment).load(iconData.loadingImage).into(viewHolder.genericIcon);
    view.setVisibility(View.INVISIBLE);

    RequestListener<Drawable> requestListener =
        new RequestListener<Drawable>() {
          @Override
          public boolean onLoadFailed(
              @Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            iconBackground.setBackgroundColor(grey_color);
            new Handler(
                    msg -> {
                      GlideApp.with(mainFragment)
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
                mainFragment.getResources().getColor(android.R.color.transparent));
            errorListener.onImageProcessed(false);
            return false;
          }
        };
    modelProvider.getPreloadRequestBuilder(iconData).listener(requestListener).into(view);
  }

  private void showPopup(@NonNull View view, @NonNull final LayoutElementParcelable rowItem) {
    if (hasPendingPasteOperation()) return;
    Context currentContext = this.context;
    if (mainFragment.getMainActivity().getAppTheme() == AppTheme.BLACK) {
      currentContext = new ContextThemeWrapper(context, R.style.overflow_black);
    }
    PopupMenu popupMenu =
        new ItemPopupMenu(
            currentContext,
            mainFragment.requireMainActivity(),
            utilsProvider,
            mainFragment,
            rowItem,
            view,
            sharedPrefs);
    popupMenu.inflate(R.menu.item_extras);
    String description = rowItem.desc.toLowerCase();

    if (rowItem.isDirectory) {
      popupMenu.getMenu().findItem(R.id.open_with).setVisible(false);
      popupMenu.getMenu().findItem(R.id.share).setVisible(false);

      if (mainFragment.getMainActivity().mReturnIntent) {
        popupMenu.getMenu().findItem(R.id.return_select).setVisible(true);
      }
    } else {
      popupMenu.getMenu().findItem(R.id.book).setVisible(false);
      popupMenu.getMenu().findItem(R.id.compress).setVisible(true);

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
          || description.endsWith(fileExtensionTarXz)
          || description.endsWith(fileExtensionTarLzma)
          || description.endsWith(fileExtension7zip)
          || description.endsWith(fileExtensionGz)
          || description.endsWith(fileExtensionBzip2)
          || description.endsWith(fileExtensionLzma)
          || description.endsWith(fileExtensionXz)) {
        popupMenu.getMenu().findItem(R.id.ex).setVisible(true);
        popupMenu.getMenu().findItem(R.id.compress).setVisible(false);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (description.endsWith(CryptUtil.CRYPT_EXTENSION)
          || description.endsWith(CryptUtil.AESCRYPT_EXTENSION)) {
        popupMenu.getMenu().findItem(R.id.decrypt).setVisible(true);
      } else {
        popupMenu.getMenu().findItem(R.id.encrypt).setVisible(true);
      }
    }

    popupMenu.show();
  }

  /**
   * Helps in deciding whether to allow file modification or not, depending on the state of the
   * copy/paste operation.
   *
   * @return true if there is an unfinished copy/paste operation, false otherwise.
   */
  private boolean hasPendingPasteOperation() {
    MainActivity mainActivity = mainFragment.getMainActivity();
    if (mainActivity == null) return false;
    MainActivityActionMode mainActivityActionMode = mainActivity.mainActivityActionMode;
    PasteHelper pasteHelper = mainActivityActionMode.getPasteHelper();

    if (pasteHelper != null
        && pasteHelper.getSnackbar() != null
        && pasteHelper.getSnackbar().isShown()) {
      Toast.makeText(
              mainFragment.requireContext(),
              mainFragment.getString(R.string.complete_paste_warning),
              Toast.LENGTH_LONG)
          .show();
      return true;
    }
    return false;
  }

  private boolean getBoolean(String key) {
    return preferenceActivity.getBoolean(key);
  }

  public static class ListItem {
    public static final int CHECKED = 0, NOT_CHECKED = 1, UNCHECKABLE = 2;

    /** Not null if {@link ListItem#specialTypeHasFile()} returns true */
    @Nullable private final LayoutElementParcelable layoutElementParcelable;

    private final @ListElemType int specialType;
    private boolean checked;
    private boolean animate;
    private boolean shouldToggleDragChecked = true;

    ListItem(@NonNull LayoutElementParcelable layoutElementParcelable) {
      this(false, layoutElementParcelable);
    }

    ListItem(boolean isBack, @NonNull LayoutElementParcelable layoutElementParcelable) {
      this.layoutElementParcelable = layoutElementParcelable;
      specialType = isBack ? TYPE_BACK : TYPE_ITEM;
    }

    ListItem(@ListElemType int specialType) {
      this.specialType = specialType;
      this.layoutElementParcelable = null;
    }

    public void setChecked(boolean checked) {
      if (specialType == TYPE_ITEM) this.checked = checked;
    }

    public int getChecked() {
      if (checked) return CHECKED;
      else if (specialType == TYPE_ITEM) return NOT_CHECKED;
      else return UNCHECKABLE;
    }

    @Nullable
    public LayoutElementParcelable getLayoutElementParcelable() {
      return layoutElementParcelable;
    }

    /**
     * Check that {@link ListItem#specialTypeHasFile()} returns true, if it does this method doesn't
     * return null.
     */
    @NonNull
    public LayoutElementParcelable requireLayoutElementParcelable() {
      if (!specialTypeHasFile()) {
        throw new IllegalStateException(
            "Type of item " + specialType + " has no LayoutElementParcelable!");
      }
      return layoutElementParcelable;
    }

    public int getSpecialType() {
      return this.specialType;
    }

    /**
     * This method effectively has a contract that allows {@link
     * ListItem#requireLayoutElementParcelable} afterwards without crashing.
     */
    public boolean specialTypeHasFile() {
      return specialType == TYPE_ITEM || specialType == TYPE_BACK;
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
