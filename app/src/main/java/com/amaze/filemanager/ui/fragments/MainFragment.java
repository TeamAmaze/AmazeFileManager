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

package com.amaze.filemanager.ui.fragments;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.Q;
import static com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_URI_PREFIX;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_DIVIDERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.adapters.holders.ItemViewHolder;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.asynctasks.LoadFilesListTask;
import com.amaze.filemanager.asynchronous.handlers.FileHandler;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.models.explorer.Tab;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.file_operations.filesystem.smbstreamer.Streamer;
import com.amaze.filemanager.filesystem.CustomFileObserver;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.PasteHelper;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils;
import com.amaze.filemanager.filesystem.files.FileListSorter;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.drag.RecyclerAdapterDragListener;
import com.amaze.filemanager.ui.drag.TabFragmentBottomDragListener;
import com.amaze.filemanager.ui.fragments.data.MainFragmentViewModel;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.CustomScrollGridLayoutManager;
import com.amaze.filemanager.ui.views.CustomScrollLinearLayoutManager;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.ui.views.FastScroller;
import com.amaze.filemanager.ui.views.WarnableTextInputValidator;
import com.amaze.filemanager.utils.BottomBarButtonPath;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MainFragment extends Fragment
    implements BottomBarButtonPath,
        ViewTreeObserver.OnGlobalLayoutListener,
        AdjustListViewForTv<ItemViewHolder> {

  public ActionMode mActionMode;

  public SwipeRefreshLayout mSwipeRefreshLayout;

  public RecyclerAdapter adapter;
  private SharedPreferences sharedPref;
  private Resources res;

  // ATTRIBUTES FOR APPEARANCE AND COLORS
  private LinearLayoutManager mLayoutManager;
  private GridLayoutManager mLayoutManagerGrid;
  private DividerItemDecoration dividerItemDecoration;
  private AppBarLayout mToolbarContainer;
  private SwipeRefreshLayout nofilesview;

  private RecyclerView listView;
  private UtilitiesProvider utilsProvider;
  private HashMap<String, Bundle> scrolls = new HashMap<>();
  private View rootView;
  private View actionModeView;
  private FastScroller fastScroller;
  private CustomFileObserver customFileObserver;

  // defines the current visible tab, default either 0 or 1
  // private int mCurrentTab;

  private MainFragmentViewModel mainFragmentViewModel;

  private ActivityResultLauncher<Intent> handleDocumentUriForRestrictedDirectories =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            if (SDK_INT >= Q) {
              getContext()
                  .getContentResolver()
                  .takePersistableUriPermission(
                      result.getData().getData(),
                      Intent.FLAG_GRANT_READ_URI_PERMISSION
                          | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
              SafRootHolder.setUriRoot(result.getData().getData());
              loadlist(result.getData().getDataString(), false, OpenMode.DOCUMENT_FILE);
            }
          });

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mainFragmentViewModel = new ViewModelProvider(this).get(MainFragmentViewModel.class);

    utilsProvider = getMainActivity().getUtilsProvider();
    sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
    res = getResources();
    mainFragmentViewModel.initBundleArguments(getArguments());
    mainFragmentViewModel.initIsList();
    mainFragmentViewModel.initColumns(sharedPref);
    mainFragmentViewModel.initSortModes(
        SortHandler.getSortType(getContext(), getCurrentPath()), sharedPref);
    mainFragmentViewModel.setAccentColor(getMainActivity().getAccent());
    mainFragmentViewModel.setPrimaryColor(
        getMainActivity().getCurrentColorPreference().getPrimaryFirstTab());
    mainFragmentViewModel.setPrimaryTwoColor(
        getMainActivity().getCurrentColorPreference().getPrimarySecondTab());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = inflater.inflate(R.layout.main_frag, container, false);
    return rootView;
  }

  @Override
  @SuppressWarnings("PMD.NPathComplexity")
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mainFragmentViewModel = new ViewModelProvider(this).get(MainFragmentViewModel.class);
    listView = rootView.findViewById(R.id.listView);
    mToolbarContainer = getMainActivity().getAppbar().getAppbarLayout();
    fastScroller = rootView.findViewById(R.id.fastscroll);
    fastScroller.setPressedHandleColor(mainFragmentViewModel.getAccentColor());
    View.OnTouchListener onTouchListener =
        (view1, motionEvent) -> {
          if (adapter != null && mainFragmentViewModel.getStopAnims()) {
            stopAnimation();
            mainFragmentViewModel.setStopAnims(false);
          }
          return false;
        };
    listView.setOnTouchListener(onTouchListener);
    //    listView.setOnDragListener(new MainFragmentDragListener());
    mToolbarContainer.setOnTouchListener(onTouchListener);

    mSwipeRefreshLayout = rootView.findViewById(R.id.activity_main_swipe_refresh_layout);

    mSwipeRefreshLayout.setOnRefreshListener(
        () ->
            loadlist(
                (mainFragmentViewModel.getCurrentPath()),
                false,
                mainFragmentViewModel.getOpenMode()));

    // String itemsstring = res.getString(R.string.items);// TODO: 23/5/2017 use or delete
    mToolbarContainer.setBackgroundColor(
        MainActivity.currentTab == 1
            ? mainFragmentViewModel.getPrimaryTwoColor()
            : mainFragmentViewModel.getPrimaryColor());

    //   listView.setPadding(listView.getPaddingLeft(), paddingTop, listView.getPaddingRight(),
    // listView.getPaddingBottom());

    setHasOptionsMenu(false);
    initNoFileLayout();
    HybridFile f = new HybridFile(OpenMode.UNKNOWN, mainFragmentViewModel.getCurrentPath());
    f.generateMode(getActivity());
    getMainActivity().getAppbar().getBottomBar().setClickListener();

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT) && !mainFragmentViewModel.isList()) {
      listView.setBackgroundColor(Utils.getColor(getContext(), R.color.grid_background_light));
    } else {
      listView.setBackgroundDrawable(null);
    }
    listView.setHasFixedSize(true);
    if (mainFragmentViewModel.isList()) {
      mLayoutManager = new CustomScrollLinearLayoutManager(getContext());
      listView.setLayoutManager(mLayoutManager);
    } else {
      if (mainFragmentViewModel.getColumns() == -1 || mainFragmentViewModel.getColumns() == 0)
        mLayoutManagerGrid = new CustomScrollGridLayoutManager(getActivity(), 3);
      else
        mLayoutManagerGrid =
            new CustomScrollGridLayoutManager(getActivity(), mainFragmentViewModel.getColumns());
      setGridLayoutSpanSizeLookup(mLayoutManagerGrid);
      listView.setLayoutManager(mLayoutManagerGrid);
    }
    // use a linear layout manager
    // View footerView = getActivity().getLayoutInflater().inflate(R.layout.divider, null);// TODO:
    // 23/5/2017 use or delete
    dividerItemDecoration =
        new DividerItemDecoration(getActivity(), false, getBoolean(PREFERENCE_SHOW_DIVIDERS));
    listView.addItemDecoration(dividerItemDecoration);
    mSwipeRefreshLayout.setColorSchemeColors(mainFragmentViewModel.getAccentColor());
    DefaultItemAnimator animator = new DefaultItemAnimator();
    listView.setItemAnimator(animator);
    mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(this);
    loadViews();
  }

  public void stopAnimation() {
    if ((!adapter.stoppedAnimation)) {
      for (int j = 0; j < listView.getChildCount(); j++) {
        View v = listView.getChildAt(j);
        if (v != null) v.clearAnimation();
      }
    }
    adapter.stoppedAnimation = true;
  }

  void setGridLayoutSpanSizeLookup(GridLayoutManager mLayoutManagerGrid) {

    mLayoutManagerGrid.setSpanSizeLookup(
        new CustomScrollGridLayoutManager.SpanSizeLookup() {

          @Override
          public int getSpanSize(int position) {
            switch (adapter.getItemViewType(position)) {
              case RecyclerAdapter.TYPE_HEADER_FILES:
              case RecyclerAdapter.TYPE_HEADER_FOLDERS:
                return (mainFragmentViewModel.getColumns() == 0
                        || mainFragmentViewModel.getColumns() == -1)
                    ? 3
                    : mainFragmentViewModel.getColumns();
              default:
                return 1;
            }
          }
        });
  }

  void switchToGrid() {
    mainFragmentViewModel.setList(false);

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

      // will always be grid, set alternate white background
      listView.setBackgroundColor(Utils.getColor(getContext(), R.color.grid_background_light));
    }

    if (mLayoutManagerGrid == null)
      if (mainFragmentViewModel.getColumns() == -1 || mainFragmentViewModel.getColumns() == 0)
        mLayoutManagerGrid = new CustomScrollGridLayoutManager(getActivity(), 3);
      else
        mLayoutManagerGrid =
            new CustomScrollGridLayoutManager(getActivity(), mainFragmentViewModel.getColumns());
    setGridLayoutSpanSizeLookup(mLayoutManagerGrid);
    listView.setLayoutManager(mLayoutManagerGrid);
    listView.clearOnScrollListeners();
    adapter = null;
  }

  void switchToList() {
    mainFragmentViewModel.setList(true);

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

      listView.setBackgroundDrawable(null);
    }

    if (mLayoutManager == null) mLayoutManager = new CustomScrollLinearLayoutManager(getActivity());
    listView.setLayoutManager(mLayoutManager);
    listView.clearOnScrollListeners();
    adapter = null;
  }

  public void switchView() {
    boolean isPathLayoutGrid =
        DataUtils.getInstance()
                .getListOrGridForPath(mainFragmentViewModel.getCurrentPath(), DataUtils.LIST)
            == DataUtils.GRID;
    reloadListElements(false, mainFragmentViewModel.getResults(), isPathLayoutGrid);
  }

  private void loadViews() {
    if (mainFragmentViewModel.getCurrentPath() != null) {
      if ((mainFragmentViewModel.getListElements() == null
              || mainFragmentViewModel.getListElements().size() == 0)
          && !mainFragmentViewModel.getResults()) {
        loadlist(mainFragmentViewModel.getCurrentPath(), true, mainFragmentViewModel.getOpenMode());
      } else {
        reloadListElements(
            true, mainFragmentViewModel.getResults(), !mainFragmentViewModel.isList());
      }
      if (mainFragmentViewModel.getSelection()) {
        for (Integer index : adapter.getCheckedItemsIndex()) {
          adapter.toggleChecked(index, null);
        }
      }
    } else {
      loadlist(mainFragmentViewModel.getHome(), true, mainFragmentViewModel.getOpenMode());
    }
  }

  public ActionMode.Callback mActionModeCallback =
      new ActionMode.Callback() {
        private void hideOption(int id, Menu menu) {
          MenuItem item = menu.findItem(id);
          item.setVisible(false);
        }

        private void showOption(int id, Menu menu) {
          MenuItem item = menu.findItem(id);
          item.setVisible(true);
        }

        void initMenu(Menu menu) {
          /*
          menu.findItem(R.id.cpy).setIcon(icons.getCopyDrawable());
          menu.findItem(R.id.cut).setIcon(icons.getCutDrawable());
          menu.findItem(R.id.delete).setIcon(icons.getDeleteDrawable());
          menu.findItem(R.id.all).setIcon(icons.getAllDrawable());
          */
        }

        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          // Inflate a menu resource providing context menu items
          MenuInflater inflater = mode.getMenuInflater();
          actionModeView = getActivity().getLayoutInflater().inflate(R.layout.actionmode, null);
          mode.setCustomView(actionModeView);

          getMainActivity().setPagingEnabled(false);
          getMainActivity().getFAB().hide();

          // translates the drawable content down
          // if (getMainActivity().isDrawerLocked) getMainActivity().translateDrawerList(true);

          // assumes that you have "contexual.xml" menu resources
          inflater.inflate(R.menu.contextual, menu);
          initMenu(menu);
          hideOption(R.id.addshortcut, menu);
          hideOption(R.id.share, menu);
          hideOption(R.id.openwith, menu);
          if (getMainActivity().mReturnIntent) showOption(R.id.openmulti, menu);
          // hideOption(R.id.setringtone,menu);
          mode.setTitle(getResources().getString(R.string.select));

          getMainActivity()
              .updateViews(new ColorDrawable(res.getColor(R.color.holo_dark_action_mode)));

          // do not allow drawer to open when item gets selected
          if (!getMainActivity().getDrawer().isLocked()) {
            getMainActivity().getDrawer().lockIfNotOnTablet(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
          }
          return true;
        }

        /**
         * the following method is called each time the action mode is shown. Always called after
         * onCreateActionMode, but may be called multiple times if the mode is invalidated.
         */
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
          ArrayList<LayoutElementParcelable> checkedItems = adapter.getCheckedItems();
          TextView textView1 = actionModeView.findViewById(R.id.item_count);
          textView1.setText(String.valueOf(checkedItems.size()));
          textView1.setOnClickListener(null);
          mode.setTitle(checkedItems.size() + "");
          hideOption(R.id.openmulti, menu);
          menu.findItem(R.id.all)
              .setTitle(
                  checkedItems.size()
                          == mainFragmentViewModel.getFolderCount()
                              + mainFragmentViewModel.getFileCount()
                      ? R.string.deselect_all
                      : R.string.select_all);

          if (mainFragmentViewModel.getOpenMode() != OpenMode.FILE) {
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.compress, menu);
            return true;
          }

          if (getMainActivity().mReturnIntent && SDK_INT >= JELLY_BEAN) {
            showOption(R.id.openmulti, menu);
          }
          // tv.setText(checkedItems.size());
          if (!mainFragmentViewModel.getResults()) {
            hideOption(R.id.openparent, menu);
            if (checkedItems.size() == 1) {
              showOption(R.id.addshortcut, menu);
              showOption(R.id.openwith, menu);
              showOption(R.id.share, menu);

              if (adapter.getCheckedItems().get(0).isDirectory) {
                hideOption(R.id.openwith, menu);
                hideOption(R.id.share, menu);
                hideOption(R.id.openmulti, menu);
              }

              if (getMainActivity().mReturnIntent)
                if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);

            } else {
              try {
                showOption(R.id.share, menu);
                if (getMainActivity().mReturnIntent)
                  if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);
                for (LayoutElementParcelable e : adapter.getCheckedItems()) {
                  if (e.isDirectory) {
                    hideOption(R.id.share, menu);
                    hideOption(R.id.openmulti, menu);
                  }
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
              hideOption(R.id.openwith, menu);
              hideOption(R.id.addshortcut, menu);
            }
          } else {
            if (checkedItems.size() == 1) {
              showOption(R.id.addshortcut, menu);
              showOption(R.id.openparent, menu);
              showOption(R.id.openwith, menu);
              showOption(R.id.share, menu);

              if (adapter.getCheckedItems().get(0).isDirectory) {
                hideOption(R.id.openwith, menu);
                hideOption(R.id.share, menu);
                hideOption(R.id.openmulti, menu);
              }
              if (getMainActivity().mReturnIntent && SDK_INT >= JELLY_BEAN) {
                showOption(R.id.openmulti, menu);
              }

            } else {
              hideOption(R.id.openparent, menu);
              hideOption(R.id.addshortcut, menu);

              if (getMainActivity().mReturnIntent)
                if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);
              try {
                for (LayoutElementParcelable e : adapter.getCheckedItems()) {
                  if (e.isDirectory) {
                    hideOption(R.id.share, menu);
                    hideOption(R.id.openmulti, menu);
                  }
                }
              } catch (Exception e) {
                e.printStackTrace();
              }

              hideOption(R.id.openwith, menu);
            }
          }

          if (mainFragmentViewModel.getOpenMode() != OpenMode.FILE) {
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.compress, menu);
            hideOption(R.id.hide, menu);
            hideOption(R.id.addshortcut, menu);
          }
          return true; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
          computeScroll();
          ArrayList<LayoutElementParcelable> checkedItems = adapter.getCheckedItems();
          switch (item.getItemId()) {
            case R.id.openmulti:
              try {

                Intent intent_result = new Intent(Intent.ACTION_SEND_MULTIPLE);
                ArrayList<Uri> resulturis = new ArrayList<>();

                for (LayoutElementParcelable element : checkedItems) {
                  HybridFileParcelable baseFile = element.generateBaseFile();
                  Uri resultUri = Utils.getUriForBaseFile(requireContext(), baseFile);

                  if (resultUri != null) {
                    resulturis.add(resultUri);
                  }
                }

                intent_result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                requireActivity().setResult(FragmentActivity.RESULT_OK, intent_result);
                intent_result.putParcelableArrayListExtra(Intent.EXTRA_STREAM, resulturis);
                requireActivity().finish();
                // mode.finish();
              } catch (Exception e) {
                e.printStackTrace();
              }
              return true;
            case R.id.about:
              LayoutElementParcelable x = checkedItems.get(0);
              GeneralDialogCreation.showPropertiesDialogWithPermissions(
                  x.generateBaseFile(),
                  x.permissions,
                  requireMainActivity(),
                  MainFragment.this,
                  requireMainActivity().isRootExplorer(),
                  utilsProvider.getAppTheme());
              mode.finish();
              return true;
            case R.id.delete:
              GeneralDialogCreation.deleteFilesDialog(
                  requireContext(),
                  requireMainActivity(),
                  checkedItems,
                  utilsProvider.getAppTheme());
              return true;
            case R.id.share:
              ArrayList<File> arrayList = new ArrayList<>();
              for (LayoutElementParcelable e : checkedItems) {
                arrayList.add(new File(e.desc));
              }
              if (arrayList.size() > 100)
                Toast.makeText(
                        getActivity(),
                        getResources().getString(R.string.share_limit),
                        Toast.LENGTH_SHORT)
                    .show();
              else {

                switch (mainFragmentViewModel.getListElements().get(0).getMode()) {
                  case DROPBOX:
                  case BOX:
                  case GDRIVE:
                  case ONEDRIVE:
                    FileUtils.shareCloudFile(
                        mainFragmentViewModel.getListElements().get(0).desc,
                        mainFragmentViewModel.getListElements().get(0).getMode(),
                        getContext());
                    break;
                  default:
                    FileUtils.shareFiles(
                        arrayList,
                        getActivity(),
                        utilsProvider.getAppTheme(),
                        mainFragmentViewModel.getAccentColor());
                    break;
                }
              }
              return true;
            case R.id.openparent:
              loadlist(new File(checkedItems.get(0).desc).getParent(), false, OpenMode.FILE);
              return true;
            case R.id.all:
              if (adapter.areAllChecked(mainFragmentViewModel.getCurrentPath())) {
                adapter.toggleChecked(false, mainFragmentViewModel.getCurrentPath());
                item.setTitle(R.string.select_all);
              } else {
                adapter.toggleChecked(true, mainFragmentViewModel.getCurrentPath());
                item.setTitle(R.string.deselect_all);
              }
              mode.invalidate();

              return true;
            case R.id.rename:
              final HybridFileParcelable f;
              f = checkedItems.get(0).generateBaseFile();
              rename(f);
              mode.finish();
              return true;
            case R.id.hide:
              for (int i1 = 0; i1 < checkedItems.size(); i1++) {
                hide(checkedItems.get(i1).desc);
              }
              updateList();
              mode.finish();
              return true;
            case R.id.ex:
              getMainActivity().mainActivityHelper.extractFile(new File(checkedItems.get(0).desc));
              mode.finish();
              return true;
            case R.id.cpy:
            case R.id.cut:
              {
                HybridFileParcelable[] copies = new HybridFileParcelable[checkedItems.size()];
                for (int i = 0; i < checkedItems.size(); i++) {
                  copies[i] = checkedItems.get(i).generateBaseFile();
                }
                int op =
                    item.getItemId() == R.id.cpy
                        ? PasteHelper.OPERATION_COPY
                        : PasteHelper.OPERATION_CUT;
                // Making sure we don't cause an IllegalArgumentException
                // when passing copies to PasteHelper
                if (copies.length > 0) {
                  PasteHelper pasteHelper = new PasteHelper(getMainActivity(), op, copies);
                  requireMainActivity().setPaste(pasteHelper);
                }
                mode.finish();
                return true;
              }
            case R.id.compress:
              ArrayList<HybridFileParcelable> copies1 = new ArrayList<>();
              for (int i4 = 0; i4 < checkedItems.size(); i4++) {
                copies1.add(checkedItems.get(i4).generateBaseFile());
              }
              GeneralDialogCreation.showCompressDialog(
                  requireMainActivity(), copies1, mainFragmentViewModel.getCurrentPath());
              mode.finish();
              return true;
            case R.id.openwith:
              FileUtils.openFile(
                  new File(checkedItems.get(0).desc), requireMainActivity(), sharedPref);
              return true;
            case R.id.addshortcut:
              addShortcut(checkedItems.get(0));
              mode.finish();
              return true;
            default:
              return false;
          }
        }

        // called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
          mActionMode = null;
          mainFragmentViewModel.setSelection(false);

          // translates the drawer content up
          // if (getMainActivity().isDrawerLocked) getMainActivity().translateDrawerList(false);

          getMainActivity().getFAB().show();
          if (!mainFragmentViewModel.getResults())
            adapter.toggleChecked(false, mainFragmentViewModel.getCurrentPath());
          else adapter.toggleChecked(false);
          getMainActivity().setPagingEnabled(true);

          getMainActivity()
              .updateViews(
                  new ColorDrawable(
                      MainActivity.currentTab == 1
                          ? mainFragmentViewModel.getPrimaryTwoColor()
                          : mainFragmentViewModel.getPrimaryColor()));

          if (getMainActivity().getDrawer().isLocked()) {
            getMainActivity().getDrawer().unlockIfNotOnTablet();
          }
        }
      };

  private BroadcastReceiver receiver2 =
      new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
          // load the list on a load broadcast
          // local file system don't need an explicit load, we've set an observer to
          // take actions on creation/moving/deletion/modification of file on current path

          updateList();
        }
      };

  private BroadcastReceiver decryptReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

          if (mainFragmentViewModel.isEncryptOpen()
              && mainFragmentViewModel.getEncryptBaseFile() != null) {
            FileUtils.openFile(
                mainFragmentViewModel.getEncryptBaseFile().getFile(),
                getMainActivity(),
                sharedPref);
            mainFragmentViewModel.setEncryptOpen(false);
          }
        }
      };

  public void home() {
    loadlist((mainFragmentViewModel.getHome()), false, OpenMode.FILE);
  }

  /**
   * method called when list item is clicked in the adapter
   *
   * @param isBackButton is it the back button aka '..'
   * @param position the position
   * @param layoutElementParcelable the list item
   * @param imageView the check icon that is to be animated
   */
  public void onListItemClicked(
      boolean isBackButton,
      int position,
      LayoutElementParcelable layoutElementParcelable,
      ImageView imageView) {
    if (mainFragmentViewModel.getResults()) {
      // check to initialize search results
      // if search task is been running, cancel it
      FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
      SearchWorkerFragment fragment =
          (SearchWorkerFragment) fragmentManager.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);
      if (fragment != null) {
        if (fragment.searchAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
          fragment.searchAsyncTask.cancel(true);
        }
        getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
      }

      mainFragmentViewModel.setRetainSearchTask(true);
      mainFragmentViewModel.setResults(false);
    } else {
      mainFragmentViewModel.setRetainSearchTask(false);
      MainActivityHelper.SEARCH_TEXT = null;
    }

    if (mainFragmentViewModel.getSelection()) {
      if (isBackButton) {
        mainFragmentViewModel.setSelection(false);
        if (mActionMode != null) mActionMode.finish();
        mActionMode = null;
      } else {
        // the first {goback} item if back navigation is enabled
        adapter.toggleChecked(position, imageView);
      }
    } else {
      if (isBackButton) {
        goBackItemClick();
      } else {
        // hiding search view if visible
        if (getMainActivity().getAppbar().getSearchView().isEnabled()) {
          getMainActivity().getAppbar().getSearchView().hideSearchView();
        }

        String path =
            !layoutElementParcelable.hasSymlink()
                ? layoutElementParcelable.desc
                : layoutElementParcelable.symlink;

        if (layoutElementParcelable.isDirectory) {
          computeScroll();
          loadlist(path, false, mainFragmentViewModel.getOpenMode());
        } else if (layoutElementParcelable.desc.endsWith(CryptUtil.CRYPT_EXTENSION)) {
          // decrypt the file
          mainFragmentViewModel.setEncryptOpen(true);
          mainFragmentViewModel.initEncryptBaseFile(
              getActivity().getExternalCacheDir().getPath()
                  + "/"
                  + layoutElementParcelable
                      .generateBaseFile()
                      .getName(getMainActivity())
                      .replace(CryptUtil.CRYPT_EXTENSION, ""));

          EncryptDecryptUtils.decryptFile(
              getContext(),
              getMainActivity(),
              this,
              mainFragmentViewModel.getOpenMode(),
              layoutElementParcelable.generateBaseFile(),
              getActivity().getExternalCacheDir().getPath(),
              utilsProvider,
              true);
        } else {
          if (getMainActivity().mReturnIntent) {
            // are we here to return an intent to another app
            returnIntentResults(layoutElementParcelable.generateBaseFile());
          } else {
            switch (layoutElementParcelable.getMode()) {
              case SMB:
                launchSMB(layoutElementParcelable.generateBaseFile(), getMainActivity());
                break;
              case SFTP:
                Toast.makeText(
                        getContext(),
                        getResources().getString(R.string.please_wait),
                        Toast.LENGTH_LONG)
                    .show();
                SshClientUtils.launchSftp(
                    layoutElementParcelable.generateBaseFile(), getMainActivity());
                break;
              case OTG:
                FileUtils.openFile(
                    OTGUtil.getDocumentFile(layoutElementParcelable.desc, getContext(), false),
                    (MainActivity) getActivity(),
                    sharedPref);
                break;
              case DOCUMENT_FILE:
                FileUtils.openFile(
                    OTGUtil.getDocumentFile(
                        layoutElementParcelable.desc,
                        SafRootHolder.getUriRoot(),
                        getContext(),
                        OpenMode.DOCUMENT_FILE,
                        false),
                    (MainActivity) getActivity(),
                    sharedPref);
                break;
              case DROPBOX:
              case BOX:
              case GDRIVE:
              case ONEDRIVE:
                Toast.makeText(
                        getContext(),
                        getResources().getString(R.string.please_wait),
                        Toast.LENGTH_LONG)
                    .show();
                CloudUtil.launchCloud(
                    layoutElementParcelable.generateBaseFile(),
                    mainFragmentViewModel.getOpenMode(),
                    getMainActivity());
                break;
              default:
                FileUtils.openFile(new File(path), (MainActivity) getActivity(), sharedPref);
                break;
            }
            DataUtils.getInstance().addHistoryFile(layoutElementParcelable.desc);
          }
        }
      }
    }
  }

  public void updateTabWithDb(Tab tab) {
    mainFragmentViewModel.setCurrentPath(tab.path);
    mainFragmentViewModel.setHome(tab.home);
    loadlist(mainFragmentViewModel.getCurrentPath(), false, OpenMode.UNKNOWN);
  }

  /**
   * Returns the intent with uri corresponding to specific {@link HybridFileParcelable} back to
   * external app
   */
  public void returnIntentResults(HybridFileParcelable baseFile) {

    getMainActivity().mReturnIntent = false;

    Uri mediaStoreUri = Utils.getUriForBaseFile(getActivity(), baseFile);
    Log.d(
        getClass().getSimpleName(),
        mediaStoreUri.toString()
            + "\t"
            + MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
    Intent intent = new Intent();
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setAction(Intent.ACTION_SEND);

    if (getMainActivity().mRingtonePickerIntent) {
      intent.setDataAndType(
          mediaStoreUri, MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, mediaStoreUri);
    } else {
      Log.d("pickup", "file");
      intent.setDataAndType(mediaStoreUri, MimeTypes.getExtension(baseFile.getPath()));
    }
    getActivity().setResult(FragmentActivity.RESULT_OK, intent);
    getActivity().finish();
  }

  LoadFilesListTask loadFilesListTask;

  /**
   * This loads a path into the MainFragment.
   *
   * @param providedPath the path to be loaded
   * @param back if we're coming back from any directory and want the scroll to be restored
   * @param providedOpenMode the mode in which the directory should be opened
   */
  public void loadlist(
      final String providedPath, final boolean back, final OpenMode providedOpenMode) {
    if (mainFragmentViewModel == null) {
      Log.w(getClass().getSimpleName(), "Viewmodel not available to load the data");
      return;
    }

    if (mActionMode != null) mActionMode.finish();

    mSwipeRefreshLayout.setRefreshing(true);

    if (loadFilesListTask != null && loadFilesListTask.getStatus() == AsyncTask.Status.RUNNING) {
      Log.w(getClass().getSimpleName(), "Existing load list task running, cancel current");
      loadFilesListTask.cancel(true);
    }

    OpenMode openMode = providedOpenMode;
    String actualPath = FileProperties.remapPathForApi30OrAbove(providedPath, false);

    if (!providedPath.equals(actualPath) && SDK_INT >= Q) {
      openMode = loadPathInQ(actualPath, providedPath, providedOpenMode);
    }
    // Monkeypatch :( to fix problems with unexpected non content URI path while openMode is still
    // OpenMode.DOCUMENT_FILE
    else if (actualPath.startsWith("/") && OpenMode.DOCUMENT_FILE.equals(openMode)) {
      openMode = OpenMode.FILE;
    }

    loadFilesListTask =
        new LoadFilesListTask(
            getActivity(),
            actualPath,
            this,
            openMode,
            getBoolean(PREFERENCE_SHOW_THUMB),
            getBoolean(PREFERENCE_SHOW_HIDDENFILES),
            (data) -> {
              mSwipeRefreshLayout.setRefreshing(false);
              if (data != null && data.second != null) {
                boolean isPathLayoutGrid =
                    DataUtils.getInstance().getListOrGridForPath(providedPath, DataUtils.LIST)
                        == DataUtils.GRID;
                setListElements(
                    data.second, back, providedPath, data.first, false, isPathLayoutGrid);
                setListElements(
                    data.second, back, providedPath, data.first, false, isPathLayoutGrid);
              } else {
                Log.w(getClass().getSimpleName(), "Load list operation cancelled");
              }
            });
    loadFilesListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @RequiresApi(api = Q)
  private OpenMode loadPathInQ(String actualPath, String providedPath, OpenMode providedMode) {

    boolean hasAccessToSpecialFolder = false;
    List<UriPermission> uriPermissions =
        getContext().getContentResolver().getPersistedUriPermissions();

    if (uriPermissions != null && uriPermissions.size() > 0) {
      for (UriPermission p : uriPermissions) {
        if (p.isReadPermission() && actualPath.startsWith(p.getUri().toString())) {
          hasAccessToSpecialFolder = true;
          SafRootHolder.setUriRoot(p.getUri());
          break;
        }
      }
    }

    if (!hasAccessToSpecialFolder) {
      Intent intent =
          new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
              .putExtra(
                  DocumentsContract.EXTRA_INITIAL_URI,
                  Uri.parse(FileProperties.remapPathForApi30OrAbove(providedPath, true)));
      MaterialDialog d =
          GeneralDialogCreation.showBasicDialog(
              getMainActivity(),
              R.string.android_data_prompt_saf_access,
              R.string.android_data_prompt_saf_access_title,
              android.R.string.ok,
              android.R.string.cancel);
      d.getActionButton(DialogAction.POSITIVE)
          .setOnClickListener(
              v -> {
                handleDocumentUriForRestrictedDirectories.launch(intent);
                d.dismiss();
              });
      d.show();
      return providedMode;
    } else {
      return OpenMode.DOCUMENT_FILE;
    }
  }

  void initNoFileLayout() {
    nofilesview = rootView.findViewById(R.id.nofilelayout);
    nofilesview.setColorSchemeColors(mainFragmentViewModel.getAccentColor());
    nofilesview.setOnRefreshListener(
        () -> {
          loadlist(
              (mainFragmentViewModel.getCurrentPath()), false, mainFragmentViewModel.getOpenMode());
          nofilesview.setRefreshing(false);
        });
    nofilesview
        .findViewById(R.id.no_files_relative)
        .setOnKeyListener(
            (v, keyCode, event) -> {
              if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                  getMainActivity().getFAB().requestFocus();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                  getMainActivity().onBackPressed();
                } else {
                  return false;
                }
              }
              return true;
            });
    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
      ((ImageView) nofilesview.findViewById(R.id.image))
          .setColorFilter(Color.parseColor("#666666"));
    } else if (utilsProvider.getAppTheme().equals(AppTheme.BLACK)) {
      nofilesview.setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
      ((TextView) nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
    } else {
      nofilesview.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
      ((TextView) nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
    }
  }

  /**
   * Loading adapter after getting a list of elements
   *
   * @param bitmap the list of objects for the adapter
   * @param back if we're coming back from any directory and want the scroll to be restored
   * @param path the path for the adapter
   * @param openMode the type of file being created
   * @param results is the list of elements a result from search
   * @param grid whether to set grid view or list view
   */
  public void setListElements(
      ArrayList<LayoutElementParcelable> bitmap,
      boolean back,
      String path,
      final OpenMode openMode,
      boolean results,
      boolean grid) {
    if (bitmap != null) {
      mainFragmentViewModel.setListElements(bitmap);
      mainFragmentViewModel.setCurrentPath(path);
      mainFragmentViewModel.setOpenMode(openMode);
      reloadListElements(back, results, grid);
    } else {
      // list loading cancelled
      // TODO: Add support for cancelling list loading
      loadlist(mainFragmentViewModel.getHome(), true, OpenMode.FILE);
    }
  }

  public void reloadListElements(boolean back, boolean results, boolean grid) {
    if (isAdded()) {
      mainFragmentViewModel.setResults(results);
      boolean isOtg = (OTGUtil.PREFIX_OTG + "/").equals(mainFragmentViewModel.getCurrentPath());

      if (getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON)
          && !"/".equals(mainFragmentViewModel.getCurrentPath())
          && (mainFragmentViewModel.getOpenMode() == OpenMode.FILE
              || mainFragmentViewModel.getOpenMode() == OpenMode.ROOT)
          && !isOtg
          && !mainFragmentViewModel.getIsOnCloud()
          && (mainFragmentViewModel.getListElements() != null
              && (mainFragmentViewModel.getListElements().size() == 0
                  || !mainFragmentViewModel
                      .getListElements()
                      .get(0)
                      .size
                      .equals(getString(R.string.goback))))
          && !results) {
        mainFragmentViewModel.getListElements().add(0, getBackElement());
      }

      if (mainFragmentViewModel.getListElements() != null
          && mainFragmentViewModel.getListElements().size() == 0
          && !results) {
        nofilesview.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        mSwipeRefreshLayout.setEnabled(false);
      } else {
        mSwipeRefreshLayout.setEnabled(true);
        nofilesview.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
      }

      if (grid && mainFragmentViewModel.isList()) {
        switchToGrid();
      } else if (!grid && !mainFragmentViewModel.isList()) {
        switchToList();
      }

      if (adapter == null) {
        final List<LayoutElementParcelable> listElements = mainFragmentViewModel.getListElements();

        adapter =
            new RecyclerAdapter(
                getMainActivity(),
                this,
                utilsProvider,
                sharedPref,
                listView,
                listElements == null ? Collections.emptyList() : listElements,
                requireContext(),
                grid);
      } else {
        adapter.setItems(listView, new ArrayList<>(mainFragmentViewModel.getListElements()));
      }

      mainFragmentViewModel.setStopAnims(true);

      if (mainFragmentViewModel.getOpenMode() != OpenMode.CUSTOM) {
        DataUtils.getInstance().addHistoryFile(mainFragmentViewModel.getCurrentPath());
      }

      listView.setAdapter(adapter);

      if (!mainFragmentViewModel.getAddHeader()) {
        listView.removeItemDecoration(dividerItemDecoration);
        mainFragmentViewModel.setAddHeader(true);
      }

      if (mainFragmentViewModel.getAddHeader() && mainFragmentViewModel.isList()) {
        dividerItemDecoration =
            new DividerItemDecoration(getActivity(), true, getBoolean(PREFERENCE_SHOW_DIVIDERS));
        listView.addItemDecoration(dividerItemDecoration);
        mainFragmentViewModel.setAddHeader(false);
      }

      if (back && scrolls.containsKey(mainFragmentViewModel.getCurrentPath())) {
        Bundle b = scrolls.get(mainFragmentViewModel.getCurrentPath());
        int index = b.getInt("index"), top = b.getInt("top");
        if (mainFragmentViewModel.isList()) {
          mLayoutManager.scrollToPositionWithOffset(index, top);
        } else {
          mLayoutManagerGrid.scrollToPositionWithOffset(index, top);
        }
      }

      getMainActivity().updatePaths(mainFragmentViewModel.getNo());
      getMainActivity().getFAB().show();
      getMainActivity().getAppbar().getAppbarLayout().setExpanded(true);
      listView.stopScroll();
      fastScroller.setRecyclerView(
          listView,
          mainFragmentViewModel.isList()
              ? 1
              : (mainFragmentViewModel.getColumns() == 0
                      || mainFragmentViewModel.getColumns() == -1)
                  ? 3
                  : mainFragmentViewModel.getColumns());
      mToolbarContainer.addOnOffsetChangedListener(
          (appBarLayout, verticalOffset) -> {
            fastScroller.updateHandlePosition(verticalOffset, 112);
          });
      fastScroller.registerOnTouchListener(
          () -> {
            if (mainFragmentViewModel.getStopAnims() && adapter != null) {
              stopAnimation();
              mainFragmentViewModel.setStopAnims(false);
            }
          });

      startFileObserver();
    } else {
      // fragment not added
      initNoFileLayout();
    }
  }

  private LayoutElementParcelable getBackElement() {
    if (mainFragmentViewModel.getBack() == null) {
      mainFragmentViewModel.setBack(
          new LayoutElementParcelable(
              requireContext(),
              true,
              getString(R.string.goback),
              getBoolean(PREFERENCE_SHOW_THUMB)));
    }
    return mainFragmentViewModel.getBack();
  }

  /**
   * Method will resume any decryption tasks like registering decryption receiver or deleting any
   * pending opened files in application cache
   */
  private void resumeDecryptOperations() {
    if (SDK_INT >= JELLY_BEAN_MR2) {
      (getActivity())
          .registerReceiver(
              decryptReceiver, new IntentFilter(EncryptDecryptUtils.DECRYPT_BROADCAST));
      if (!mainFragmentViewModel.isEncryptOpen()
          && !Utils.isNullOrEmpty(mainFragmentViewModel.getEncryptBaseFiles())) {
        // we've opened the file and are ready to delete it
        new DeleteTask(getActivity()).execute(mainFragmentViewModel.getEncryptBaseFiles());
        mainFragmentViewModel.setEncryptBaseFiles(new ArrayList<>());
      }
    }
  }

  private void startFileObserver() {
    switch (mainFragmentViewModel.getOpenMode()) {
      case ROOT:
      case FILE:
        if (customFileObserver != null
            && !customFileObserver.wasStopped()
            && customFileObserver.getPath().equals(getCurrentPath())) {
          return;
        }

        File file = null;
        if (mainFragmentViewModel.getCurrentPath() != null) {
          file = new File(mainFragmentViewModel.getCurrentPath());
        }
        if (file != null && file.isDirectory() && file.canRead()) {
          if (customFileObserver != null) {
            // already a watcher instantiated, first it should be stopped
            customFileObserver.stopWatching();
          }

          customFileObserver =
              new CustomFileObserver(
                  mainFragmentViewModel.getCurrentPath(),
                  new FileHandler(this, listView, getBoolean(PREFERENCE_SHOW_THUMB)));
          customFileObserver.startWatching();
        }
        break;
      default:
        break;
    }
  }

  /**
   * Show dialog to rename a file
   *
   * @param f the file to rename
   */
  public void rename(final HybridFileParcelable f) {
    MaterialDialog renameDialog =
        GeneralDialogCreation.showNameDialog(
            getMainActivity(),
            "",
            f.getName(getMainActivity()),
            getResources().getString(R.string.rename),
            getResources().getString(R.string.save),
            null,
            getResources().getString(R.string.cancel),
            (dialog, which) -> {
              EditText textfield = dialog.getCustomView().findViewById(R.id.singleedittext_input);
              String name1 = textfield.getText().toString().trim();

              getMainActivity()
                  .mainActivityHelper
                  .rename(
                      mainFragmentViewModel.getOpenMode(),
                      f.getPath(),
                      mainFragmentViewModel.getCurrentPath(),
                      name1,
                      f.isDirectory(),
                      getActivity(),
                      getMainActivity().isRootExplorer());
            },
            (text) -> {
              boolean isValidFilename = FileProperties.isValidFilename(text);

              if (!isValidFilename || text.startsWith(" ")) {
                return new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.invalid_name);
              } else if (text.length() < 1) {
                return new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
              }

              return new WarnableTextInputValidator.ReturnState();
            });

    // place cursor at the starting of edit text by posting a runnable to edit text
    // this is done because in case android has not populated the edit text layouts yet, it'll
    // reset calls to selection if not posted in message queue
    EditText textfield = renameDialog.getCustomView().findViewById(R.id.singleedittext_input);
    textfield.post(
        () -> {
          if (!f.isDirectory()) {
            textfield.setSelection(f.getNameString(getContext()).length());
          }
        });
  }

  public void computeScroll() {
    View vi = listView.getChildAt(0);
    int top = (vi == null) ? 0 : vi.getTop();
    int index;
    if (mainFragmentViewModel.isList()) index = mLayoutManager.findFirstVisibleItemPosition();
    else index = mLayoutManagerGrid.findFirstVisibleItemPosition();
    Bundle b = new Bundle();
    b.putInt("index", index);
    b.putInt("top", top);
    scrolls.put(mainFragmentViewModel.getCurrentPath(), b);
  }

  public void goBack() {
    if (mainFragmentViewModel.getOpenMode() == OpenMode.CUSTOM) {
      loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE);
      return;
    }

    HybridFile currentFile =
        new HybridFile(mainFragmentViewModel.getOpenMode(), mainFragmentViewModel.getCurrentPath());
    if (!mainFragmentViewModel.getResults()) {
      if (!mainFragmentViewModel.getRetainSearchTask()) {
        // normal case
        if (mainFragmentViewModel.getSelection()) {
          adapter.toggleChecked(false);
        } else {
          if (OpenMode.SMB.equals(mainFragmentViewModel.getOpenMode())) {
            if (mainFragmentViewModel.getSmbPath() != null
                && !mainFragmentViewModel
                    .getSmbPath()
                    .equals(mainFragmentViewModel.getCurrentPath())) {
              StringBuilder path = new StringBuilder(currentFile.getSmbFile().getParent());
              if (mainFragmentViewModel.getCurrentPath().indexOf('?') > 0)
                path.append(
                    mainFragmentViewModel
                        .getCurrentPath()
                        .substring(mainFragmentViewModel.getCurrentPath().indexOf('?')));
              loadlist(
                  path.toString().replace("%3D", "="), true, mainFragmentViewModel.getOpenMode());
            } else loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE);
          } else if (OpenMode.SFTP.equals(mainFragmentViewModel.getOpenMode())) {
            if (mainFragmentViewModel.getCurrentPath() != null
                && !mainFragmentViewModel
                    .getCurrentPath()
                    .substring(SSH_URI_PREFIX.length())
                    .contains("/")) {
              loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE);
            } else if (OpenMode.DOCUMENT_FILE.equals(mainFragmentViewModel.getOpenMode())) {
              loadlist(currentFile.getParent(getContext()), true, currentFile.getMode());
            } else {
              loadlist(
                  currentFile.getParent(getContext()), true, mainFragmentViewModel.getOpenMode());
            }
          } else if (("/").equals(mainFragmentViewModel.getCurrentPath())
              || (mainFragmentViewModel.getHome() != null
                  && mainFragmentViewModel.getHome().equals(mainFragmentViewModel.getCurrentPath()))
              || mainFragmentViewModel.getIsOnCloud()) {
            getMainActivity().exit();
          } else if (OpenMode.DOCUMENT_FILE.equals(mainFragmentViewModel.getOpenMode())) {
            if (!currentFile.getPath().startsWith("content://")) {
              mainFragmentViewModel.setOpenMode(OpenMode.FILE);
              currentFile.setMode(OpenMode.FILE);
              currentFile.setPath(Environment.getExternalStorageDirectory().getAbsolutePath());
              loadlist(currentFile.getPath(), false, mainFragmentViewModel.getOpenMode());
            } else {
              List<String> pathSegments = Uri.parse(currentFile.getPath()).getPathSegments();
              if (pathSegments.size() < 3) {
                mainFragmentViewModel.setOpenMode(OpenMode.FILE);
                String subPath = pathSegments.get(1);
                currentFile.setMode(OpenMode.FILE);
                currentFile.setPath(
                    new File(
                            Environment.getExternalStorageDirectory(),
                            subPath.substring(
                                subPath.lastIndexOf(':') + 1, subPath.lastIndexOf('/')))
                        .getAbsolutePath());
                loadlist(currentFile.getPath(), false, mainFragmentViewModel.getOpenMode());
              } else {
                loadlist(
                    currentFile.getParent(getContext()), true, mainFragmentViewModel.getOpenMode());
              }
            }

          } else if (FileUtils.canGoBack(getContext(), currentFile)) {
            loadlist(
                currentFile.getParent(getContext()), true, mainFragmentViewModel.getOpenMode());
          } else getMainActivity().exit();
        }
      } else {
        // case when we had pressed on an item from search results and wanna go back
        // leads to resuming the search task

        if (MainActivityHelper.SEARCH_TEXT != null) {

          // starting the search query again :O
          FragmentManager fm = getMainActivity().getSupportFragmentManager();

          // getting parent path to resume search from there
          String parentPath =
              new HybridFile(
                      mainFragmentViewModel.getOpenMode(), mainFragmentViewModel.getCurrentPath())
                  .getParent(getActivity());
          // don't fuckin' remove this line, we need to change
          // the path back to parent on back press
          mainFragmentViewModel.setCurrentPath(parentPath);

          MainActivityHelper.addSearchFragment(
              fm,
              new SearchWorkerFragment(),
              parentPath,
              MainActivityHelper.SEARCH_TEXT,
              mainFragmentViewModel.getOpenMode(),
              getMainActivity().isRootExplorer(),
              sharedPref.getBoolean(SearchWorkerFragment.KEY_REGEX, false),
              sharedPref.getBoolean(SearchWorkerFragment.KEY_REGEX_MATCHES, false));
        } else {
          loadlist(mainFragmentViewModel.getCurrentPath(), true, OpenMode.UNKNOWN);
        }
        mainFragmentViewModel.setRetainSearchTask(false);
      }
    } else {
      // to go back after search list have been popped
      FragmentManager fm = getActivity().getSupportFragmentManager();
      SearchWorkerFragment fragment =
          (SearchWorkerFragment) fm.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);
      if (fragment != null) {
        if (fragment.searchAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
          fragment.searchAsyncTask.cancel(true);
        }
      }
      if (mainFragmentViewModel.getCurrentPath() != null) {
        loadlist(
            new File(mainFragmentViewModel.getCurrentPath()).getPath(), true, OpenMode.UNKNOWN);
      }
      mainFragmentViewModel.setResults(false);
    }
  }

  public void reauthenticateSmb() {
    if (mainFragmentViewModel.getSmbPath() != null) {
      try {
        getMainActivity()
            .runOnUiThread(
                () -> {
                  int i;
                  AppConfig.toast(requireContext(), getString(R.string.unknown_error));
                  if ((i =
                          DataUtils.getInstance()
                              .containsServer(mainFragmentViewModel.getSmbPath()))
                      != -1) {
                    getMainActivity()
                        .showSMBDialog(
                            DataUtils.getInstance().getServers().get(i)[0],
                            mainFragmentViewModel.getSmbPath(),
                            true);
                  }
                });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void goBackItemClick() {
    if (mainFragmentViewModel.getOpenMode() == OpenMode.CUSTOM) {
      loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE);
      return;
    }
    HybridFile currentFile =
        new HybridFile(mainFragmentViewModel.getOpenMode(), mainFragmentViewModel.getCurrentPath());
    if (!mainFragmentViewModel.getResults()) {
      if (mainFragmentViewModel.getSelection()) {
        adapter.toggleChecked(false);
      } else {
        if (mainFragmentViewModel.getOpenMode() == OpenMode.SMB) {
          if (mainFragmentViewModel.getCurrentPath() != null
              && !mainFragmentViewModel
                  .getCurrentPath()
                  .equals(mainFragmentViewModel.getSmbPath())) {
            StringBuilder path = new StringBuilder(currentFile.getSmbFile().getParent());
            if (mainFragmentViewModel.getCurrentPath().indexOf('?') > 0)
              path.append(
                  mainFragmentViewModel
                      .getCurrentPath()
                      .substring(mainFragmentViewModel.getCurrentPath().indexOf('?')));
            loadlist(path.toString(), true, OpenMode.SMB);
          } else loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE);
        } else if (("/").equals(mainFragmentViewModel.getCurrentPath())
            || mainFragmentViewModel.getIsOnCloud()) {
          getMainActivity().exit();
        } else if (FileUtils.canGoBack(getContext(), currentFile)) {
          loadlist(currentFile.getParent(getContext()), true, mainFragmentViewModel.getOpenMode());
        } else getMainActivity().exit();
      }
    } else {
      loadlist(currentFile.getPath(), true, mainFragmentViewModel.getOpenMode());
    }
  }

  public void updateList() {
    computeScroll();
    loadlist(mainFragmentViewModel.getCurrentPath(), true, mainFragmentViewModel.getOpenMode());
  }

  @Override
  public void onResume() {
    super.onResume();
    (getActivity())
        .registerReceiver(receiver2, new IntentFilter(MainActivity.KEY_INTENT_LOAD_LIST));

    resumeDecryptOperations();
    startFileObserver();
  }

  @Override
  public void onPause() {
    super.onPause();
    (getActivity()).unregisterReceiver(receiver2);
    if (customFileObserver != null) {
      customFileObserver.stopWatching();
    }

    if (SDK_INT >= JELLY_BEAN_MR2) {
      (getActivity()).unregisterReceiver(decryptReceiver);
    }
  }

  public ArrayList<LayoutElementParcelable> addToSmb(
      @NonNull SmbFile[] mFile, @NonNull String path, boolean showHiddenFiles) throws SmbException {
    ArrayList<LayoutElementParcelable> smbFileList = new ArrayList<>();
    String extraParams = Uri.parse(path).getQuery();

    if (mainFragmentViewModel.getSearchHelper().size() > 500) {
      mainFragmentViewModel.getSearchHelper().clear();
    }
    for (SmbFile aMFile : mFile) {
      if ((DataUtils.getInstance().isFileHidden(aMFile.getPath()) || aMFile.isHidden())
          && !showHiddenFiles) {
        continue;
      }
      String name = aMFile.getName();
      name =
          (aMFile.isDirectory() && name.endsWith("/"))
              ? name.substring(0, name.length() - 1)
              : name;
      if (path.equals(mainFragmentViewModel.getSmbPath())) {
        if (name.endsWith("$")) continue;
      }
      if (aMFile.isDirectory()) {
        mainFragmentViewModel.setFolderCount(mainFragmentViewModel.getFolderCount() + 1);

        Uri.Builder aMFilePathBuilder = Uri.parse(aMFile.getPath()).buildUpon();
        if (!TextUtils.isEmpty(extraParams)) aMFilePathBuilder.query(extraParams);

        LayoutElementParcelable layoutElement =
            new LayoutElementParcelable(
                getContext(),
                name,
                aMFilePathBuilder.build().toString(),
                "",
                "",
                "",
                0,
                false,
                aMFile.lastModified() + "",
                true,
                getBoolean(PREFERENCE_SHOW_THUMB),
                OpenMode.SMB);

        mainFragmentViewModel.getSearchHelper().add(layoutElement.generateBaseFile());
        smbFileList.add(layoutElement);

      } else {
        mainFragmentViewModel.setFileCount(mainFragmentViewModel.getFileCount() + 1);
        LayoutElementParcelable layoutElement =
            new LayoutElementParcelable(
                getContext(),
                name,
                aMFile.getPath(),
                "",
                "",
                Formatter.formatFileSize(getContext(), aMFile.length()),
                aMFile.length(),
                false,
                aMFile.lastModified() + "",
                false,
                getBoolean(PREFERENCE_SHOW_THUMB),
                OpenMode.SMB);
        layoutElement.setMode(OpenMode.SMB);
        mainFragmentViewModel.getSearchHelper().add(layoutElement.generateBaseFile());
        smbFileList.add(layoutElement);
      }
    }
    return smbFileList;
  }

  // method to add search result entry to the LIST_ELEMENT arrayList
  private LayoutElementParcelable addTo(HybridFileParcelable mFile) {
    File f = new File(mFile.getPath());
    String size = "";
    if (!DataUtils.getInstance().isFileHidden(mFile.getPath())) {
      if (mFile.isDirectory()) {
        size = "";
        LayoutElementParcelable layoutElement =
            new LayoutElementParcelable(
                getContext(),
                f.getPath(),
                mFile.getPermission(),
                mFile.getLink(),
                size,
                0,
                true,
                mFile.getDate() + "",
                true,
                getBoolean(PREFERENCE_SHOW_THUMB),
                mFile.getMode());

        if (mainFragmentViewModel.getListElements() != null) {
          mainFragmentViewModel.getListElements().add(layoutElement);
        }
        mainFragmentViewModel.setFolderCount(mainFragmentViewModel.getFolderCount() + 1);
        return layoutElement;
      } else {
        long longSize = 0;
        try {
          if (mFile.getSize() != -1) {
            longSize = mFile.getSize();
            size = Formatter.formatFileSize(getContext(), longSize);
          } else {
            size = "";
            longSize = 0;
          }
        } catch (NumberFormatException e) {
          // e.printStackTrace();
        }
        try {
          LayoutElementParcelable layoutElement =
              new LayoutElementParcelable(
                  getContext(),
                  f.getPath(),
                  mFile.getPermission(),
                  mFile.getLink(),
                  size,
                  longSize,
                  false,
                  mFile.getDate() + "",
                  false,
                  getBoolean(PREFERENCE_SHOW_THUMB),
                  mFile.getMode());
          mainFragmentViewModel.getListElements().add(layoutElement);
          mainFragmentViewModel.setFileCount(mainFragmentViewModel.getFileCount() + 1);
          return layoutElement;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    return null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // not guaranteed to be called unless we call #finish();
    // please move code to onStop
  }

  public void hide(String path) {
    DataUtils.getInstance().addHiddenFile(path);
    File file = new File(path);
    if (file.isDirectory()) {
      File f1 = new File(path + "/" + ".nomedia");
      if (!f1.exists()) {
        try {
          getMainActivity()
              .mainActivityHelper
              .mkFile(
                  new HybridFile(OpenMode.FILE, path),
                  new HybridFile(OpenMode.FILE, f1.getPath()),
                  this);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      FileUtils.scanFile(getActivity(), new HybridFile[] {new HybridFile(OpenMode.FILE, path)});
    }
  }

  private void addShortcut(LayoutElementParcelable path) {
    // Adding shortcut for MainActivity
    // on Home screen
    final Context ctx = getContext();

    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) {
      Toast.makeText(
              getActivity(),
              getString(R.string.add_shortcut_not_supported_by_launcher),
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    Intent shortcutIntent = new Intent(ctx, MainActivity.class);
    shortcutIntent.putExtra("path", path.desc);
    shortcutIntent.setAction(Intent.ACTION_MAIN);
    shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

    // Using file path as shortcut id.
    ShortcutInfoCompat info =
        new ShortcutInfoCompat.Builder(ctx, path.desc)
            .setActivity(getMainActivity().getComponentName())
            .setIcon(IconCompat.createWithResource(ctx, R.mipmap.ic_launcher))
            .setIntent(shortcutIntent)
            .setLongLabel(path.desc)
            .setShortLabel(new File(path.desc).getName())
            .build();

    ShortcutManagerCompat.requestPinShortcut(ctx, info, null);
  }

  // This method is used to implement the modification for the pre Searching
  public void onSearchPreExecute(String query) {
    getMainActivity().getAppbar().getBottomBar().setPathText("");
    getMainActivity()
        .getAppbar()
        .getBottomBar()
        .setFullPathText(getString(R.string.searching, query));
  }

  // adds search results based on result boolean. If false, the adapter is initialised with initial
  // values, if true, new values are added to the adapter.
  public void addSearchResult(HybridFileParcelable a, String query) {
    if (listView != null) {

      // initially clearing the array for new result set
      if (!mainFragmentViewModel.getResults()) {
        if (mainFragmentViewModel.getListElements() != null) {
          mainFragmentViewModel.getListElements().clear();
        }
        mainFragmentViewModel.setFileCount(0);
        mainFragmentViewModel.setFolderCount(0);
      }

      // adding new value to LIST_ELEMENTS
      LayoutElementParcelable layoutElementAdded = addTo(a);
      if (!getMainActivity()
          .getAppbar()
          .getBottomBar()
          .getFullPathText()
          .contains(getString(R.string.searching))) {
        getMainActivity()
            .getAppbar()
            .getBottomBar()
            .setFullPathText(getString(R.string.searching, query));
      }
      if (!mainFragmentViewModel.getResults()) {
        reloadListElements(false, true, !mainFragmentViewModel.isList());
        getMainActivity().getAppbar().getBottomBar().setPathText("");
      } else {
        adapter.addItem(layoutElementAdded);
      }
      stopAnimation();
    }
  }

  public void onSearchCompleted(final String query) {
    if (!mainFragmentViewModel.getResults() && mainFragmentViewModel.getListElements() != null) {
      // no results were found
      mainFragmentViewModel.getListElements().clear();
    }
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Collections.sort(
            mainFragmentViewModel.getListElements(),
            new FileListSorter(
                mainFragmentViewModel.getDsort(),
                mainFragmentViewModel.getSortby(),
                mainFragmentViewModel.getAsc()));
        return null;
      }

      @Override
      public void onPostExecute(Void c) {
        reloadListElements(
            true,
            true,
            !mainFragmentViewModel
                .isList()); // TODO: 7/7/2017 this is really inneffient, use RecycleAdapter's
        // createHeaders()
        getMainActivity().getAppbar().getBottomBar().setPathText("");
        getMainActivity()
            .getAppbar()
            .getBottomBar()
            .setFullPathText(getString(R.string.search_results, query));
        mainFragmentViewModel.setResults(false);
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  public static void launchSMB(final HybridFileParcelable baseFile, final Activity activity) {
    final Streamer s = Streamer.getInstance();
    new Thread() {
      public void run() {
        try {
          /*
          List<SmbFile> subtitleFiles = new ArrayList<SmbFile>();

          // finding subtitles
          for (Layoutelements layoutelement : LIST_ELEMENTS) {
              SmbFile smbFile = new SmbFile(layoutelement.getDesc());
              if (smbFile.getName().contains(smbFile.getName())) subtitleFiles.add(smbFile);
          }
          */

          s.setStreamSrc(baseFile.getSmbFile(), baseFile.getSize());
          activity.runOnUiThread(
              () -> {
                try {
                  Uri uri =
                      Uri.parse(
                          Streamer.URL
                              + Uri.fromFile(new File(Uri.parse(baseFile.getPath()).getPath()))
                                  .getEncodedPath());
                  Intent i = new Intent(Intent.ACTION_VIEW);
                  i.setDataAndType(
                      uri, MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
                  PackageManager packageManager = activity.getPackageManager();
                  List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                  if (resInfos != null && resInfos.size() > 0) activity.startActivity(i);
                  else
                    Toast.makeText(
                            activity,
                            activity.getResources().getString(R.string.smb_launch_error),
                            Toast.LENGTH_SHORT)
                        .show();
                } catch (ActivityNotFoundException e) {
                  e.printStackTrace();
                }
              });

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }.start();
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  @Nullable
  public MainActivity getMainActivity() {
    return (MainActivity) getActivity();
  }

  @NonNull
  public MainActivity requireMainActivity() {
    return (MainActivity) requireActivity();
  }

  @Nullable
  public ArrayList<LayoutElementParcelable> getElementsList() {
    return mainFragmentViewModel.getListElements();
  }

  public void initTopAndEmptyAreaDragListeners(boolean destroy) {
    if (destroy) {
      mToolbarContainer.setOnDragListener(null);
      listView.stopScroll();
      listView.setOnDragListener(null);
      nofilesview.setOnDragListener(null);
    } else {
      mToolbarContainer.setOnDragListener(
          new TabFragmentBottomDragListener(
              () -> {
                smoothScrollListView(true);
                return null;
              },
              () -> {
                stopSmoothScrollListView();
                return null;
              }));
      listView.setOnDragListener(
          new RecyclerAdapterDragListener(
              adapter, null, mainFragmentViewModel.getDragAndDropPreference(), this));
      nofilesview.setOnDragListener(
          new RecyclerAdapterDragListener(
              adapter, null, mainFragmentViewModel.getDragAndDropPreference(), this));
    }
  }

  public void disableActionMode() {
    mainFragmentViewModel.setSelection(false);
    if (this.mActionMode != null) {
      this.mActionMode.finish();
    }
    this.mActionMode = null;
  }

  public void smoothScrollListView(boolean upDirection) {
    if (listView != null) {
      if (upDirection) {
        listView.smoothScrollToPosition(0);
      } else {
        listView.smoothScrollToPosition(adapter.getItemsDigested().size());
      }
    }
  }

  public void stopSmoothScrollListView() {
    if (listView != null) {
      listView.stopScroll();
    }
  }

  @Nullable
  public String getCurrentPath() {
    if (mainFragmentViewModel == null) {
      Log.w(getClass().getSimpleName(), "Viewmodel not available to get current path");
      return null;
    }
    return mainFragmentViewModel.getCurrentPath();
  }

  @Override
  public void changePath(@NonNull String path) {
    loadlist(path, false, mainFragmentViewModel.getOpenMode());
  }

  @Override
  public String getPath() {
    return getCurrentPath();
  }

  @Override
  public int getRootDrawable() {
    return R.drawable.ic_root_white_24px;
  }

  private boolean getBoolean(String key) {
    return getMainActivity().getBoolean(key);
  }

  @Override
  public void onGlobalLayout() {
    if (mainFragmentViewModel.getColumns() == 0 || mainFragmentViewModel.getColumns() == -1) {
      int screen_width = listView.getWidth();
      int dptopx = Utils.dpToPx(getContext(), 115);
      mainFragmentViewModel.setColumns(screen_width / dptopx);
      if (mainFragmentViewModel.getColumns() == 0 || mainFragmentViewModel.getColumns() == -1) {
        mainFragmentViewModel.setColumns(3);
      }
      if (!mainFragmentViewModel.isList()) {
        mLayoutManagerGrid.setSpanCount(mainFragmentViewModel.getColumns());
      }
    }
    if (!mainFragmentViewModel.isList()) {
      loadViews();
    }
    if (android.os.Build.VERSION.SDK_INT >= JELLY_BEAN) {
      mToolbarContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    } else {
      mToolbarContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }
  }

  public @Nullable MainFragmentViewModel getMainFragmentViewModel() {
    if (isAdded()) {
      return new ViewModelProvider(this).get(MainFragmentViewModel.class);
    } else {
      Log.e(getClass().getSimpleName(), "Failed to get viewmodel, fragment not yet added");
      return null;
    }
  }

  @Override
  public void adjustListViewForTv(
      @NonNull ItemViewHolder viewHolder, @NonNull MainActivity mainActivity) {
    try {
      int[] location = new int[2];
      viewHolder.rl.getLocationOnScreen(location);
      Log.i(getClass().getSimpleName(), "Current x and y " + location[0] + " " + location[1]);
      if (location[1] < getMainActivity().getAppbar().getAppbarLayout().getHeight()) {
        listView.scrollToPosition(Math.max(viewHolder.getAdapterPosition() - 5, 0));
      } else if (location[1] + viewHolder.rl.getHeight()
          > getContext().getResources().getDisplayMetrics().heightPixels) {
        listView.scrollToPosition(
            Math.min(viewHolder.getAdapterPosition() + 5, adapter.getItemCount() - 1));
      }
    } catch (IndexOutOfBoundsException e) {
      Log.w(getClass().getSimpleName(), "Failed to adjust scrollview for tv", e);
    }
  }
}
