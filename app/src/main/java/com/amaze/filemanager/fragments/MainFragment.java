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

package com.amaze.filemanager.fragments;

import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.*;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_DIVIDERS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.asynctasks.LoadFilesListTask;
import com.amaze.filemanager.asynchronous.handlers.FileHandler;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.database.models.Tab;
import com.amaze.filemanager.filesystem.CustomFileObserver;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.MediaStoreHack;
import com.amaze.filemanager.filesystem.PasteHelper;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.ui.views.FastScroller;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.ui.views.WarnableTextInputValidator;
import com.amaze.filemanager.utils.BottomBarButtonPath;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.SmbStreamer.Streamer;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.EncryptDecryptUtils;
import com.amaze.filemanager.utils.files.FileListSorter;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.google.android.material.appbar.AppBarLayout;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
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

import androidx.appcompat.view.ActionMode;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MainFragment extends Fragment implements BottomBarButtonPath {

  public ActionMode mActionMode;
  // TODO refactor
  public int sortby, dsort, asc;
  public String home;
  public boolean selection, results = false;
  public OpenMode openMode = OpenMode.FILE;

  /** boolean to identify if the view is a list or grid */
  public boolean IS_LIST = true;

  public SwipeRefreshLayout mSwipeRefreshLayout;
  public int file_count, folder_count, columns;
  public String smbPath;
  public ArrayList<HybridFileParcelable> searchHelper = new ArrayList<>();
  public int no;

  private String CURRENT_PATH = "";
  /** This is not an exact copy of the elements in the adapter */
  private ArrayList<LayoutElementParcelable> LIST_ELEMENTS;

  public RecyclerAdapter adapter;
  private SharedPreferences sharedPref;
  private Resources res;

  // ATTRIBUTES FOR APPEARANCE AND COLORS
  private int accentColor, primaryColor, primaryTwoColor;
  private LinearLayoutManager mLayoutManager;
  private GridLayoutManager mLayoutManagerGrid;
  private boolean addheader = false;
  private DividerItemDecoration dividerItemDecoration;
  private AppBarLayout mToolbarContainer;
  private boolean stopAnims = true;
  private SwipeRefreshLayout nofilesview;

  private RecyclerView listView;
  private UtilitiesProvider utilsProvider;
  private HashMap<String, Bundle> scrolls = new HashMap<>();
  private MainFragment ma = this;
  private View rootView;
  private View actionModeView;
  private FastScroller fastScroller;
  private CustomFileObserver customFileObserver;
  private DataUtils dataUtils;
  private boolean isEncryptOpen =
      false; // do we have to open a file when service is begin destroyed
  private HybridFileParcelable
      encryptBaseFile; // the cached base file which we're to open, delete it later
  private int ordinalValue;

  /** a list of encrypted base files which are supposed to be deleted */
  private ArrayList<HybridFileParcelable> encryptBaseFiles = new ArrayList<>();

  private MediaScannerConnection mediaScannerConnection;

  // defines the current visible tab, default either 0 or 1
  // private int mCurrentTab;

  /*
   * boolean identifying if the search task should be re-run on back press after pressing on
   * any of the search result
   */
  private boolean mRetainSearchTask = false;

  /** For caching the back button */
  private LayoutElementParcelable back = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);

    dataUtils = DataUtils.getInstance();
    utilsProvider = getMainActivity().getUtilsProvider();
    sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    res = getResources();

    no = getArguments().getInt("no", 1);
    home = getArguments().getString("home");
    CURRENT_PATH = getArguments().getString("lastpath");
    ordinalValue = getArguments().getInt("openmode", -1);

    if (ordinalValue != -1) openMode = OpenMode.getOpenMode(ordinalValue);

    IS_LIST = dataUtils.getListOrGridForPath(CURRENT_PATH, DataUtils.LIST) == DataUtils.LIST;

    accentColor = getMainActivity().getAccent();
    primaryColor = getMainActivity().getCurrentColorPreference().primaryFirstTab;
    primaryTwoColor = getMainActivity().getCurrentColorPreference().primarySecondTab;
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

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = inflater.inflate(R.layout.main_frag, container, false);
    setRetainInstance(true);
    listView = rootView.findViewById(R.id.listView);
    mToolbarContainer = getMainActivity().getAppbar().getAppbarLayout();
    fastScroller = rootView.findViewById(R.id.fastscroll);
    fastScroller.setPressedHandleColor(accentColor);
    listView.setOnTouchListener(
        (view, motionEvent) -> {
          if (adapter != null && stopAnims) {
            stopAnimation();
            stopAnims = false;
          }
          return false;
        });
    mToolbarContainer.setOnTouchListener(
        (view, motionEvent) -> {
          if (adapter != null && stopAnims) {
            stopAnimation();
            stopAnims = false;
          }
          return false;
        });

    mSwipeRefreshLayout = rootView.findViewById(R.id.activity_main_swipe_refresh_layout);

    mSwipeRefreshLayout.setOnRefreshListener(() -> loadlist((CURRENT_PATH), false, openMode));

    // String itemsstring = res.getString(R.string.items);// TODO: 23/5/2017 use or delete
    mToolbarContainer.setBackgroundColor(
        MainActivity.currentTab == 1 ? primaryTwoColor : primaryColor);

    //   listView.setPadding(listView.getPaddingLeft(), paddingTop, listView.getPaddingRight(),
    // listView.getPaddingBottom());
    return rootView;
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setHasOptionsMenu(false);
    // getMainActivity() = (MainActivity) getActivity();
    initNoFileLayout();
    getSortModes();
    this.setRetainInstance(false);
    HybridFile f = new HybridFile(OpenMode.UNKNOWN, CURRENT_PATH);
    f.generateMode(getActivity());
    getMainActivity().getAppbar().getBottomBar().setClickListener();

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT) && !IS_LIST) {
      listView.setBackgroundColor(Utils.getColor(getContext(), R.color.grid_background_light));
    } else {
      listView.setBackgroundDrawable(null);
    }

    listView.setHasFixedSize(true);
    columns = Integer.parseInt(sharedPref.getString(PREFERENCE_GRID_COLUMNS, "-1"));
    if (IS_LIST) {
      mLayoutManager = new LinearLayoutManager(getContext());
      listView.setLayoutManager(mLayoutManager);
    } else {
      if (columns == -1 || columns == 0)
        mLayoutManagerGrid = new GridLayoutManager(getActivity(), 3);
      else mLayoutManagerGrid = new GridLayoutManager(getActivity(), columns);
      setGridLayoutSpanSizeLookup(mLayoutManagerGrid);
      listView.setLayoutManager(mLayoutManagerGrid);
    }
    // use a linear layout manager
    // View footerView = getActivity().getLayoutInflater().inflate(R.layout.divider, null);// TODO:
    // 23/5/2017 use or delete
    dividerItemDecoration =
        new DividerItemDecoration(getActivity(), false, getBoolean(PREFERENCE_SHOW_DIVIDERS));
    listView.addItemDecoration(dividerItemDecoration);
    mSwipeRefreshLayout.setColorSchemeColors(accentColor);
    DefaultItemAnimator animator = new DefaultItemAnimator();
    listView.setItemAnimator(animator);
    mToolbarContainer
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                if ((columns == 0 || columns == -1)) {
                  int screen_width = listView.getWidth();
                  int dptopx = Utils.dpToPx(getContext(), 115);
                  columns = screen_width / dptopx;
                  if (columns == 0 || columns == -1) columns = 3;
                  if (!IS_LIST) mLayoutManagerGrid.setSpanCount(columns);
                }
                if (savedInstanceState != null && !IS_LIST)
                  onSavedInstanceState(savedInstanceState);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                  mToolbarContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                  mToolbarContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
              }
            });

    if (savedInstanceState == null) {
      loadlist(CURRENT_PATH, false, openMode);
    } else {
      if (IS_LIST) onSavedInstanceState(savedInstanceState);
    }
  }

  void setGridLayoutSpanSizeLookup(GridLayoutManager mLayoutManagerGrid) {

    mLayoutManagerGrid.setSpanSizeLookup(
        new GridLayoutManager.SpanSizeLookup() {

          @Override
          public int getSpanSize(int position) {
            switch (adapter.getItemViewType(position)) {
              case RecyclerAdapter.TYPE_HEADER_FILES:
              case RecyclerAdapter.TYPE_HEADER_FOLDERS:
                return columns;
              default:
                return 1;
            }
          }
        });
  }

  void switchToGrid() {
    IS_LIST = false;

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

      // will always be grid, set alternate white background
      listView.setBackgroundColor(Utils.getColor(getContext(), R.color.grid_background_light));
    }

    if (mLayoutManagerGrid == null)
      if (columns == -1 || columns == 0)
        mLayoutManagerGrid = new GridLayoutManager(getActivity(), 3);
      else mLayoutManagerGrid = new GridLayoutManager(getActivity(), columns);
    setGridLayoutSpanSizeLookup(mLayoutManagerGrid);
    listView.setLayoutManager(mLayoutManagerGrid);
    listView.clearOnScrollListeners();
    adapter = null;
  }

  void switchToList() {
    IS_LIST = true;

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

      listView.setBackgroundDrawable(null);
    }

    if (mLayoutManager == null) mLayoutManager = new LinearLayoutManager(getActivity());
    listView.setLayoutManager(mLayoutManager);
    listView.clearOnScrollListeners();
    adapter = null;
  }

  public void switchView() {
    boolean isPathLayoutGrid =
        dataUtils.getListOrGridForPath(CURRENT_PATH, DataUtils.LIST) == DataUtils.GRID;
    reloadListElements(false, results, isPathLayoutGrid);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    int index;
    View vi;
    if (listView != null) {
      if (IS_LIST) {
        index = (mLayoutManager).findFirstVisibleItemPosition();
        vi = listView.getChildAt(0);
      } else {
        index = (mLayoutManagerGrid).findFirstVisibleItemPosition();
        vi = listView.getChildAt(0);
      }

      int top = (vi == null) ? 0 : vi.getTop();

      outState.putInt("index", index);
      outState.putInt("top", top);
      outState.putParcelableArrayList("list", LIST_ELEMENTS);
      outState.putString("CURRENT_PATH", CURRENT_PATH);
      outState.putBoolean("selection", selection);
      outState.putInt("openMode", openMode.ordinal());
      outState.putInt("folder_count", folder_count);
      outState.putInt("file_count", file_count);

      if (selection) {
        outState.putIntegerArrayList("position", adapter.getCheckedItemsIndex());
      }

      outState.putBoolean("results", results);

      if (openMode == OpenMode.SMB) {
        outState.putString("SmbPath", smbPath);
      }
    }
  }

  void onSavedInstanceState(final Bundle savedInstanceState) {
    Bundle b = new Bundle();
    String cur = savedInstanceState.getString("CURRENT_PATH");

    if (cur != null) {
      b.putInt("index", savedInstanceState.getInt("index"));
      b.putInt("top", savedInstanceState.getInt("top"));
      scrolls.put(cur, b);

      openMode = OpenMode.getOpenMode(savedInstanceState.getInt("openMode", 0));
      if (openMode == OpenMode.SMB) smbPath = savedInstanceState.getString("SmbPath");
      LIST_ELEMENTS = savedInstanceState.getParcelableArrayList("list");
      CURRENT_PATH = cur;
      folder_count = savedInstanceState.getInt("folder_count", 0);
      file_count = savedInstanceState.getInt("file_count", 0);
      results = savedInstanceState.getBoolean("results");
      getMainActivity()
          .getAppbar()
          .getBottomBar()
          .updatePath(
              CURRENT_PATH,
              results,
              MainActivityHelper.SEARCH_TEXT,
              openMode,
              folder_count,
              file_count,
              this);
      if ((LIST_ELEMENTS == null || LIST_ELEMENTS.size() == 0) && !results) {
        loadlist(home, true, OpenMode.FILE);
      } else {
        reloadListElements(true, results, !IS_LIST);
      }
      if (savedInstanceState.getBoolean("selection")) {
        for (Integer index : savedInstanceState.getIntegerArrayList("position")) {
          adapter.toggleChecked(index, null);
        }
      }
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
          getMainActivity().floatingActionButton.hide();

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
          ArrayList<LayoutElementParcelable> positions = adapter.getCheckedItems();
          TextView textView1 = actionModeView.findViewById(R.id.item_count);
          textView1.setText(String.valueOf(positions.size()));
          textView1.setOnClickListener(null);
          mode.setTitle(positions.size() + "");
          hideOption(R.id.openmulti, menu);
          menu.findItem(R.id.all)
              .setTitle(
                  positions.size() == folder_count + file_count
                      ? R.string.deselect_all
                      : R.string.select_all);

          if (openMode != OpenMode.FILE) {
            hideOption(R.id.addshortcut, menu);
            hideOption(R.id.compress, menu);
            return true;
          }

          if (getMainActivity().mReturnIntent)
            if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);
          // tv.setText(positions.size());
          if (!results) {
            hideOption(R.id.openparent, menu);
            if (positions.size() == 1) {
              showOption(R.id.addshortcut, menu);
              showOption(R.id.openwith, menu);
              showOption(R.id.share, menu);

              File x = new File(adapter.getCheckedItems().get(0).desc);

              if (x.isDirectory()) {
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
                  File x = new File(e.desc);
                  if (x.isDirectory()) {
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
            if (positions.size() == 1) {
              showOption(R.id.addshortcut, menu);
              showOption(R.id.openparent, menu);
              showOption(R.id.openwith, menu);
              showOption(R.id.share, menu);

              File x = new File(adapter.getCheckedItems().get(0).desc);

              if (x.isDirectory()) {
                hideOption(R.id.openwith, menu);
                hideOption(R.id.share, menu);
                hideOption(R.id.openmulti, menu);
              }
              if (getMainActivity().mReturnIntent)
                if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);

            } else {
              hideOption(R.id.openparent, menu);
              hideOption(R.id.addshortcut, menu);

              if (getMainActivity().mReturnIntent)
                if (Build.VERSION.SDK_INT >= 16) showOption(R.id.openmulti, menu);
              try {
                for (LayoutElementParcelable e : adapter.getCheckedItems()) {
                  File x = new File(e.desc);
                  if (x.isDirectory()) {
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
                  Uri resultUri = Utils.getUriForBaseFile(getActivity(), baseFile);

                  if (resultUri != null) {
                    resulturis.add(resultUri);
                  }
                }

                intent_result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getActivity().setResult(FragmentActivity.RESULT_OK, intent_result);
                intent_result.putParcelableArrayListExtra(Intent.EXTRA_STREAM, resulturis);
                getActivity().finish();
                // mode.finish();
              } catch (Exception e) {
                e.printStackTrace();
              }
              return true;
            case R.id.about:
              LayoutElementParcelable x = checkedItems.get(0);
              GeneralDialogCreation.showPropertiesDialogWithPermissions(
                  (x).generateBaseFile(),
                  x.permissions,
                  (ThemedActivity) getActivity(),
                  getMainActivity().isRootExplorer(),
                  utilsProvider.getAppTheme());
              mode.finish();
              return true;
            case R.id.delete:
              GeneralDialogCreation.deleteFilesDialog(
                  getContext(),
                  LIST_ELEMENTS,
                  getMainActivity(),
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

                switch (LIST_ELEMENTS.get(0).getMode()) {
                  case DROPBOX:
                  case BOX:
                  case GDRIVE:
                  case ONEDRIVE:
                    FileUtils.shareCloudFile(
                        LIST_ELEMENTS.get(0).desc, LIST_ELEMENTS.get(0).getMode(), getContext());
                    break;
                  default:
                    FileUtils.shareFiles(
                        arrayList, getActivity(), utilsProvider.getAppTheme(), accentColor);
                    break;
                }
              }
              return true;
            case R.id.openparent:
              loadlist(new File(checkedItems.get(0).desc).getParent(), false, OpenMode.FILE);
              return true;
            case R.id.all:
              if (adapter.areAllChecked(CURRENT_PATH)) {
                adapter.toggleChecked(false, CURRENT_PATH);
                item.setTitle(R.string.select_all);
              } else {
                adapter.toggleChecked(true, CURRENT_PATH);
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

                PasteHelper pasteHelper = new PasteHelper(op, copies);
                getMainActivity().setPaste(pasteHelper);

                mode.finish();
                return true;
              }
            case R.id.compress:
              ArrayList<HybridFileParcelable> copies1 = new ArrayList<>();
              for (int i4 = 0; i4 < checkedItems.size(); i4++) {
                copies1.add(checkedItems.get(i4).generateBaseFile());
              }
              GeneralDialogCreation.showCompressDialog(
                  (MainActivity) getActivity(), copies1, CURRENT_PATH);
              mode.finish();
              return true;
            case R.id.openwith:
              FileUtils.openFile(new File(checkedItems.get(0).desc), getMainActivity(), sharedPref);
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
          selection = false;

          // translates the drawer content up
          // if (getMainActivity().isDrawerLocked) getMainActivity().translateDrawerList(false);

          getMainActivity().floatingActionButton.show();
          if (!results) adapter.toggleChecked(false, CURRENT_PATH);
          else adapter.toggleChecked(false);
          getMainActivity().setPagingEnabled(true);

          getMainActivity()
              .updateViews(
                  new ColorDrawable(MainActivity.currentTab == 1 ? primaryTwoColor : primaryColor));

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
          switch (openMode) {
            case ROOT:
            case FILE:
              // local file system don't need an explicit load, we've set an observer to
              // take actions on creation/moving/deletion/modification of file on current path

              // run media scanner
              String[] path = new String[1];
              String arg = intent.getStringExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE);

              // run media scanner for only one context
              if (arg != null && getMainActivity().getCurrentMainFragment() == MainFragment.this) {

                if (Build.VERSION.SDK_INT >= 19) {

                  path[0] = arg;

                  MediaScannerConnection.MediaScannerConnectionClient mediaScannerConnectionClient =
                      new MediaScannerConnection.MediaScannerConnectionClient() {
                        @Override
                        public void onMediaScannerConnected() {}

                        @Override
                        public void onScanCompleted(String path, Uri uri) {

                          Log.d("SCAN completed", path);
                        }
                      };

                  if (mediaScannerConnection != null) {
                    mediaScannerConnection.disconnect();
                  }
                  mediaScannerConnection =
                      new MediaScannerConnection(context, mediaScannerConnectionClient);
                  // FileUtils.scanFile(context, mediaScannerConnection, path);
                } else {
                  FileUtils.scanFile(new File(arg), context);
                }
              }
              // break;
            default:
              updateList();
              break;
          }
        }
      };

  private BroadcastReceiver decryptReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

          if (isEncryptOpen && encryptBaseFile != null) {
            FileUtils.openFile(new File(encryptBaseFile.getPath()), getMainActivity(), sharedPref);
            isEncryptOpen = false;
          }
        }
      };

  public void home() {
    ma.loadlist((ma.home), false, OpenMode.FILE);
  }

  /**
   * method called when list item is clicked in the adapter
   *
   * @param isBackButton is it the back button aka '..'
   * @param position the position
   * @param e the list item
   * @param imageView the check {@link RoundedImageView} that is to be animated
   */
  public void onListItemClicked(
      boolean isBackButton, int position, LayoutElementParcelable e, ImageView imageView) {
    if (results) {
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

      mRetainSearchTask = true;
      results = false;
    } else {
      mRetainSearchTask = false;
      MainActivityHelper.SEARCH_TEXT = null;
    }

    if (selection) {
      if (isBackButton) {
        selection = false;
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

        String path = !e.hasSymlink() ? e.desc : e.symlink;

        if (e.isDirectory) {
          computeScroll();
          loadlist(path, false, openMode);
        } else if (e.desc.endsWith(CryptUtil.CRYPT_EXTENSION)) {
          // decrypt the file
          isEncryptOpen = true;

          encryptBaseFile =
              new HybridFileParcelable(
                  getActivity().getExternalCacheDir().getPath()
                      + "/"
                      + e.generateBaseFile()
                          .getName(getMainActivity())
                          .replace(CryptUtil.CRYPT_EXTENSION, ""));
          encryptBaseFiles.add(encryptBaseFile);

          EncryptDecryptUtils.decryptFile(
              getContext(),
              getMainActivity(),
              ma,
              openMode,
              e.generateBaseFile(),
              getActivity().getExternalCacheDir().getPath(),
              utilsProvider,
              true);
        } else {
          if (getMainActivity().mReturnIntent) {
            // are we here to return an intent to another app
            returnIntentResults(e.generateBaseFile());
          } else {
            switch (e.getMode()) {
              case SMB:
                launchSMB(e.generateBaseFile(), getMainActivity());
                break;
              case SFTP:
                Toast.makeText(
                        getContext(),
                        getResources().getString(R.string.please_wait),
                        Toast.LENGTH_LONG)
                    .show();
                SshClientUtils.launchSftp(e.generateBaseFile(), getMainActivity());
                break;
              case OTG:
                FileUtils.openFile(
                    OTGUtil.getDocumentFile(e.desc, getContext(), false),
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
                CloudUtil.launchCloud(e.generateBaseFile(), openMode, getMainActivity());
                break;
              default:
                FileUtils.openFile(new File(e.desc), (MainActivity) getActivity(), sharedPref);
                break;
            }

            dataUtils.addHistoryFile(e.desc);
          }
        }
      }
    }
  }

  /**
   * Queries database to find entry for the specific path
   *
   * @param path the path to match with
   * @return the entry
   */
  private static EncryptedEntry findEncryptedEntry(Context context, String path) throws Exception {

    CryptHandler handler = new CryptHandler(context);

    EncryptedEntry matchedEntry = null;
    // find closest path which matches with database entry
    for (EncryptedEntry encryptedEntry : handler.getAllEntries()) {
      if (path.contains(encryptedEntry.getPath())) {

        if (matchedEntry == null
            || matchedEntry.getPath().length() < encryptedEntry.getPath().length()) {
          matchedEntry = encryptedEntry;
        }
      }
    }
    return matchedEntry;
  }

  public void updateTabWithDb(Tab tab) {
    CURRENT_PATH = tab.path;
    home = tab.home;
    loadlist(CURRENT_PATH, false, OpenMode.UNKNOWN);
  }

  /**
   * Returns the intent with uri corresponding to specific {@link HybridFileParcelable} back to
   * external app
   */
  public void returnIntentResults(HybridFileParcelable baseFile) {

    getMainActivity().mReturnIntent = false;

    Intent intent = new Intent();
    if (getMainActivity().mRingtonePickerIntent) {

      Uri mediaStoreUri = MediaStoreHack.getUriFromFile(baseFile.getPath(), getActivity());
      Log.d(
          getClass().getSimpleName(),
          mediaStoreUri.toString()
              + "\t"
              + MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
      intent.setDataAndType(
          mediaStoreUri, MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
      intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, mediaStoreUri);
      getActivity().setResult(FragmentActivity.RESULT_OK, intent);
      getActivity().finish();
    } else {

      Log.d("pickup", "file");

      Intent intentresult = new Intent();

      Uri resultUri = Utils.getUriForBaseFile(getActivity(), baseFile);
      intentresult.setAction(Intent.ACTION_SEND);
      intentresult.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      if (resultUri != null)
        intentresult.setDataAndType(resultUri, MimeTypes.getExtension(baseFile.getPath()));

      getActivity().setResult(FragmentActivity.RESULT_OK, intentresult);
      getActivity().finish();
      // mode.finish();
    }
  }

  LoadFilesListTask loadFilesListTask;

  /**
   * This loads a path into the MainFragment.
   *
   * @param path the path to be loaded
   * @param back if we're coming back from any directory and want the scroll to be restored
   * @param openMode the mode in which the directory should be opened
   */
  public void loadlist(final String path, final boolean back, final OpenMode openMode) {
    if (mActionMode != null) mActionMode.finish();

    mSwipeRefreshLayout.setRefreshing(true);

    if (loadFilesListTask != null && loadFilesListTask.getStatus() == AsyncTask.Status.RUNNING) {
      loadFilesListTask.cancel(true);
    }

    loadFilesListTask =
        new LoadFilesListTask(
            ma.getActivity(),
            path,
            ma,
            openMode,
            getBoolean(PREFERENCE_SHOW_THUMB),
            getBoolean(PREFERENCE_SHOW_HIDDENFILES),
            (data) -> {
              if (data != null && data.second != null) {
                boolean isPathLayoutGrid =
                    dataUtils.getListOrGridForPath(path, DataUtils.LIST) == DataUtils.GRID;
                setListElements(data.second, back, path, data.first, false, isPathLayoutGrid);
                mSwipeRefreshLayout.setRefreshing(false);
              }
            });
    loadFilesListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  void initNoFileLayout() {
    nofilesview = rootView.findViewById(R.id.nofilelayout);
    nofilesview.setColorSchemeColors(accentColor);
    nofilesview.setOnRefreshListener(
        () -> {
          loadlist((CURRENT_PATH), false, openMode);
          nofilesview.setRefreshing(false);
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
      LIST_ELEMENTS = bitmap;
      CURRENT_PATH = path;
      this.openMode = openMode;
      reloadListElements(back, results, grid);
    } else {
      // list loading cancelled
      // TODO: Add support for cancelling list loading
      loadlist(home, true, OpenMode.FILE);
    }
  }

  public void reloadListElements(boolean back, boolean results, boolean grid) {
    if (isAdded()) {
      this.results = results;
      boolean isOtg = CURRENT_PATH.equals(OTGUtil.PREFIX_OTG + "/"),
          isOnTheCloud =
              CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")
                  || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")
                  || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")
                  || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/");

      if (getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON)
          && !CURRENT_PATH.equals("/")
          && (openMode == OpenMode.FILE || openMode == OpenMode.ROOT)
          && !isOtg
          && !isOnTheCloud
          && (LIST_ELEMENTS.size() == 0
              || !LIST_ELEMENTS.get(0).size.equals(getString(R.string.goback)))
          && !results) {
        LIST_ELEMENTS.add(0, getBackElement());
      }

      if (LIST_ELEMENTS.size() == 0 && !results) {
        nofilesview.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        mSwipeRefreshLayout.setEnabled(false);
      } else {
        mSwipeRefreshLayout.setEnabled(true);
        nofilesview.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
      }

      if (grid && IS_LIST) switchToGrid();
      else if (!grid && !IS_LIST) switchToList();

      if (adapter == null) {
        adapter =
            new RecyclerAdapter(
                getMainActivity(),
                ma,
                utilsProvider,
                sharedPref,
                listView,
                LIST_ELEMENTS,
                ma.getActivity());
      } else {
        adapter.setItems(listView, new ArrayList<>(LIST_ELEMENTS));
      }

      stopAnims = true;

      if (openMode != OpenMode.CUSTOM) {
        dataUtils.addHistoryFile(CURRENT_PATH);
      }

      listView.setAdapter(adapter);

      if (!addheader) {
        listView.removeItemDecoration(dividerItemDecoration);
        addheader = true;
      }

      if (addheader && IS_LIST) {
        dividerItemDecoration =
            new DividerItemDecoration(getActivity(), true, getBoolean(PREFERENCE_SHOW_DIVIDERS));
        listView.addItemDecoration(dividerItemDecoration);
        addheader = false;
      }

      if (back && scrolls.containsKey(CURRENT_PATH)) {
        Bundle b = scrolls.get(CURRENT_PATH);
        int index = b.getInt("index"), top = b.getInt("top");
        if (IS_LIST) {
          mLayoutManager.scrollToPositionWithOffset(index, top);
        } else {
          mLayoutManagerGrid.scrollToPositionWithOffset(index, top);
        }
      }

      getMainActivity().updatePaths(no);
      listView.stopScroll();
      fastScroller.setRecyclerView(listView, IS_LIST ? 1 : columns);
      mToolbarContainer.addOnOffsetChangedListener(
          (appBarLayout, verticalOffset) -> {
            fastScroller.updateHandlePosition(verticalOffset, 112);
          });
      fastScroller.registerOnTouchListener(
          () -> {
            if (stopAnims && adapter != null) {
              stopAnimation();
              stopAnims = false;
            }
          });

      startFileObserver();
    } else {
      // fragment not added
      initNoFileLayout();
    }
  }

  private LayoutElementParcelable getBackElement() {
    if (back == null) {
      back =
          new LayoutElementParcelable(
              getContext(), true, getString(R.string.goback), getBoolean(PREFERENCE_SHOW_THUMB));
    }

    return back;
  }

  private void startFileObserver() {
    switch (openMode) {
      case ROOT:
      case FILE:
        if (customFileObserver != null
            && !customFileObserver.wasStopped()
            && customFileObserver.getPath().equals(getCurrentPath())) {
          return;
        }

        File file = new File(CURRENT_PATH);

        if (file.isDirectory() && file.canRead()) {
          if (customFileObserver != null) {
            // already a watcher instantiated, first it should be stopped
            customFileObserver.stopWatching();
          }

          customFileObserver =
              new CustomFileObserver(
                  CURRENT_PATH, new FileHandler(this, listView, getBoolean(PREFERENCE_SHOW_THUMB)));
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
              String name1 = textfield.getText().toString();

              getMainActivity()
                  .mainActivityHelper
                  .rename(
                      openMode,
                      f.getPath(),
                      CURRENT_PATH + "/" + name1,
                      getActivity(),
                      getMainActivity().isRootExplorer());
            },
            (text) -> {
              boolean isValidFilename = FileUtil.isValidFilename(text);

              if (!isValidFilename) {
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
    if (IS_LIST) index = mLayoutManager.findFirstVisibleItemPosition();
    else index = mLayoutManagerGrid.findFirstVisibleItemPosition();
    Bundle b = new Bundle();
    b.putInt("index", index);
    b.putInt("top", top);
    scrolls.put(CURRENT_PATH, b);
  }

  public void goBack() {
    if (openMode == OpenMode.CUSTOM) {
      loadlist(home, false, OpenMode.FILE);
      return;
    }

    HybridFile currentFile = new HybridFile(openMode, CURRENT_PATH);
    if (!results) {
      if (!mRetainSearchTask) {
        // normal case
        if (selection) {
          adapter.toggleChecked(false);
        } else {

          if (openMode == OpenMode.SMB) {
            try {
              if (!smbPath.equals(CURRENT_PATH)) {
                String path = (new SmbFile(CURRENT_PATH).getParent());
                loadlist((path), true, openMode);
              } else loadlist(home, false, OpenMode.FILE);
            } catch (MalformedURLException e) {
              e.printStackTrace();
            }

          } else if (openMode == OpenMode.SFTP) {
            if (!CURRENT_PATH.substring("ssh://".length()).contains("/"))
              loadlist(home, false, OpenMode.FILE);
            else loadlist(currentFile.getParent(getContext()), true, openMode);
          } else if (CURRENT_PATH.equals("/")
              || CURRENT_PATH.equals(home)
              || CURRENT_PATH.equals(OTGUtil.PREFIX_OTG + "/")
              || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")
              || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/")
              || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")
              || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/"))
            getMainActivity().exit();
          else if (FileUtils.canGoBack(getContext(), currentFile)) {
            loadlist(currentFile.getParent(getContext()), true, openMode);
          } else getMainActivity().exit();
        }
      } else {
        // case when we had pressed on an item from search results and wanna go back
        // leads to resuming the search task

        if (MainActivityHelper.SEARCH_TEXT != null) {

          // starting the search query again :O
          getMainActivity().mainFragment =
              (MainFragment) getMainActivity().getTabFragment().getCurrentTabFragment();
          FragmentManager fm = getMainActivity().getSupportFragmentManager();

          // getting parent path to resume search from there
          String parentPath = new HybridFile(openMode, CURRENT_PATH).getParent(getActivity());
          // don't fuckin' remove this line, we need to change
          // the path back to parent on back press
          CURRENT_PATH = parentPath;

          MainActivityHelper.addSearchFragment(
              fm,
              new SearchWorkerFragment(),
              parentPath,
              MainActivityHelper.SEARCH_TEXT,
              openMode,
              getMainActivity().isRootExplorer(),
              sharedPref.getBoolean(SearchWorkerFragment.KEY_REGEX, false),
              sharedPref.getBoolean(SearchWorkerFragment.KEY_REGEX_MATCHES, false));
        } else loadlist(CURRENT_PATH, true, OpenMode.UNKNOWN);

        mRetainSearchTask = false;
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
      loadlist(new File(CURRENT_PATH).getPath(), true, OpenMode.UNKNOWN);
      results = false;
    }
  }

  public void reauthenticateSmb() {
    if (smbPath != null) {
      try {
        getMainActivity()
            .runOnUiThread(
                () -> {
                  int i;
                  if ((i = dataUtils.containsServer(smbPath)) != -1) {
                    getMainActivity()
                        .showSMBDialog(dataUtils.getServers().get(i)[0], smbPath, true);
                  }
                });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void goBackItemClick() {
    if (openMode == OpenMode.CUSTOM) {
      loadlist(home, false, OpenMode.FILE);
      return;
    }
    HybridFile currentFile = new HybridFile(openMode, CURRENT_PATH);
    if (!results) {
      if (selection) {
        adapter.toggleChecked(false);
      } else {
        if (openMode == OpenMode.SMB) {
          try {
            if (!CURRENT_PATH.equals(smbPath)) {
              String path = (new SmbFile(CURRENT_PATH).getParent());
              loadlist((path), true, OpenMode.SMB);
            } else loadlist(home, false, OpenMode.FILE);
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
        } else if (CURRENT_PATH.equals("/")
            || CURRENT_PATH.equals(OTGUtil.PREFIX_OTG)
            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")
            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/")
            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")
            || CURRENT_PATH.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")) {
          getMainActivity().exit();
        } else if (FileUtils.canGoBack(getContext(), currentFile)) {
          loadlist(currentFile.getParent(getContext()), true, openMode);
        } else getMainActivity().exit();
      }
    } else {
      loadlist(currentFile.getPath(), true, openMode);
    }
  }

  public void updateList() {
    computeScroll();
    loadlist((CURRENT_PATH), true, openMode);
  }

  /**
   * Assigns sort modes A value from 0 to 3 defines sort mode as name/last modified/size/type in
   * ascending order Values from 4 to 7 defines sort mode as name/last modified/size/type in
   * descending order
   *
   * <p>Final value of {@link #sortby} varies from 0 to 3
   */
  public void getSortModes() {
    int t = SortHandler.getSortType(getContext(), getCurrentPath());
    if (t <= 3) {
      sortby = t;
      asc = 1;
    } else {
      asc = -1;
      sortby = t - 4;
    }

    dsort = Integer.parseInt(sharedPref.getString(PREFERENCE_DIRECTORY_SORT_MODE, "0"));
  }

  @Override
  public void onResume() {
    super.onResume();
    (getActivity())
        .registerReceiver(receiver2, new IntentFilter(MainActivity.KEY_INTENT_LOAD_LIST));

    getMainActivity().getDrawer().selectCorrectDrawerItemForPath(getPath());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

      (getActivity())
          .registerReceiver(
              decryptReceiver, new IntentFilter(EncryptDecryptUtils.DECRYPT_BROADCAST));
    }
    startFileObserver();
  }

  @Override
  public void onPause() {
    super.onPause();
    (getActivity()).unregisterReceiver(receiver2);
    if (customFileObserver != null) {
      customFileObserver.stopWatching();
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      (getActivity()).unregisterReceiver(decryptReceiver);
    }
  }

  @Override
  public void onStop() {
    super.onStop();

    if (mediaScannerConnection != null) mediaScannerConnection.disconnect();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

      if (!isEncryptOpen && encryptBaseFiles.size() != 0) {
        // we've opened the file and are ready to delete it
        new DeleteTask(getActivity()).execute(encryptBaseFiles);
      }
    }
  }

  public ArrayList<LayoutElementParcelable> addToSmb(
      SmbFile[] mFile, String path, boolean showHiddenFiles) throws SmbException {
    ArrayList<LayoutElementParcelable> smbFileList = new ArrayList<>();
    if (searchHelper.size() > 500) searchHelper.clear();
    for (SmbFile aMFile : mFile) {
      if ((dataUtils.isFileHidden(aMFile.getPath()) || aMFile.isHidden()) && !showHiddenFiles) {
        continue;
      }
      String name = aMFile.getName();
      name =
          (aMFile.isDirectory() && name.endsWith("/"))
              ? name.substring(0, name.length() - 1)
              : name;
      if (path.equals(smbPath)) {
        if (name.endsWith("$")) continue;
      }
      if (aMFile.isDirectory()) {
        folder_count++;

        LayoutElementParcelable layoutElement =
            new LayoutElementParcelable(
                getContext(),
                name,
                aMFile.getPath(),
                "",
                "",
                "",
                0,
                false,
                aMFile.lastModified() + "",
                true,
                getBoolean(PREFERENCE_SHOW_THUMB),
                OpenMode.SMB);

        searchHelper.add(layoutElement.generateBaseFile());
        smbFileList.add(layoutElement);

      } else {
        file_count++;
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
        searchHelper.add(layoutElement.generateBaseFile());
        smbFileList.add(layoutElement);
      }
    }
    return smbFileList;
  }

  // method to add search result entry to the LIST_ELEMENT arrayList
  private LayoutElementParcelable addTo(HybridFileParcelable mFile) {
    File f = new File(mFile.getPath());
    String size = "";
    if (!dataUtils.isFileHidden(mFile.getPath())) {
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

        LIST_ELEMENTS.add(layoutElement);
        folder_count++;
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
          LIST_ELEMENTS.add(layoutElement);
          file_count++;
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

    dataUtils.addHiddenFile(path);
    File file = new File(path);
    if (file.isDirectory()) {
      File f1 = new File(path + "/" + ".nomedia");
      if (!f1.exists()) {
        try {
          getMainActivity()
              .mainActivityHelper
              .mkFile(new HybridFile(OpenMode.FILE, f1.getPath()), this);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      FileUtils.scanFile(file, getActivity());
    }
  }

  public String getCurrentPath() {
    return CURRENT_PATH;
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
      if (!results) {
        LIST_ELEMENTS.clear();
        file_count = 0;
        folder_count = 0;
      }

      // adding new value to LIST_ELEMENTS
      LayoutElementParcelable layoutElementAdded = addTo(a);
      if (!results) {
        reloadListElements(false, true, !IS_LIST);
        getMainActivity().getAppbar().getBottomBar().setPathText("");
        getMainActivity()
            .getAppbar()
            .getBottomBar()
            .setFullPathText(getString(R.string.searching, query));
      } else {
        adapter.addItem(layoutElementAdded);
      }
      stopAnimation();
    }
  }

  public void onSearchCompleted(final String query) {
    if (!results) {
      // no results were found
      LIST_ELEMENTS.clear();
    }
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Collections.sort(LIST_ELEMENTS, new FileListSorter(dsort, sortby, asc));
        return null;
      }

      @Override
      public void onPostExecute(Void c) {
        reloadListElements(
            true, true, !IS_LIST); // TODO: 7/7/2017 this is really inneffient, use RecycleAdapter's
        // createHeaders()
        getMainActivity().getAppbar().getBottomBar().setPathText("");
        getMainActivity()
            .getAppbar()
            .getBottomBar()
            .setFullPathText(getString(R.string.search_results, query));
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

          s.setStreamSrc(new SmbFile(baseFile.getPath()), baseFile.getSize());
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

  public MainActivity getMainActivity() {
    return (MainActivity) getActivity();
  }

  public ArrayList<LayoutElementParcelable> getElementsList() {
    return LIST_ELEMENTS;
  }

  @Override
  public void changePath(String path) {
    loadlist(path, false, openMode);
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
}
