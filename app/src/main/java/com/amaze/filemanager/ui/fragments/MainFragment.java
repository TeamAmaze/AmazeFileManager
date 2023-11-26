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
import static com.amaze.filemanager.filesystem.FileProperties.ANDROID_DATA_DIRS;
import static com.amaze.filemanager.filesystem.FileProperties.ANDROID_DEVICE_DATA_DIRS;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_DIVIDERS;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.RecyclerAdapter;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.adapters.holders.ItemViewHolder;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.asynctasks.LoadFilesListTask;
import com.amaze.filemanager.asynchronous.asynctasks.TaskKt;
import com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem.SortSearchResultTask;
import com.amaze.filemanager.asynchronous.handlers.FileHandler;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.models.explorer.Tab;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.CustomFileObserver;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils;
import com.amaze.filemanager.filesystem.files.FileListSorter;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.files.MediaConnectionUtils;
import com.amaze.filemanager.ui.ExtensionsKt;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.MainActivityViewModel;
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
import com.amaze.filemanager.utils.GenericExtKt;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
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
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;

public class MainFragment extends Fragment
    implements BottomBarButtonPath,
        ViewTreeObserver.OnGlobalLayoutListener,
        AdjustListViewForTv<ItemViewHolder> {

  private static final Logger LOG = LoggerFactory.getLogger(MainFragment.class);
  private static final String KEY_FRAGMENT_MAIN = "main";

  public SwipeRefreshLayout mSwipeRefreshLayout;

  public RecyclerAdapter adapter;
  private SharedPreferences sharedPref;

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
  private FastScroller fastScroller;
  private CustomFileObserver customFileObserver;

  // defines the current visible tab, default either 0 or 1
  // private int mCurrentTab;

  private MainFragmentViewModel mainFragmentViewModel;
  private MainActivityViewModel mainActivityViewModel;

  private final ActivityResultLauncher<Intent> handleDocumentUriForRestrictedDirectories =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            if (SDK_INT >= Q) {
              if (result.getData() != null && getContext() != null) {
                getContext()
                    .getContentResolver()
                    .takePersistableUriPermission(
                        result.getData().getData(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                SafRootHolder.setUriRoot(result.getData().getData());
                loadlist(result.getData().getDataString(), false, OpenMode.DOCUMENT_FILE, true);
              } else if (getContext() != null) {
                AppConfig.toast(requireContext(), getString(R.string.operation_unsuccesful));
              }
            }
          });

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mainFragmentViewModel = new ViewModelProvider(this).get(MainFragmentViewModel.class);
    mainActivityViewModel =
        new ViewModelProvider(requireMainActivity()).get(MainActivityViewModel.class);

    utilsProvider = requireMainActivity().getUtilsProvider();
    sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
    mainFragmentViewModel.initBundleArguments(getArguments());
    mainFragmentViewModel.initIsList();
    mainFragmentViewModel.initColumns(sharedPref);
    mainFragmentViewModel.initSortModes(
        SortHandler.getSortType(getContext(), getCurrentPath()), sharedPref);
    mainFragmentViewModel.setAccentColor(requireMainActivity().getAccent());
    mainFragmentViewModel.setPrimaryColor(
        requireMainActivity().getCurrentColorPreference().getPrimaryFirstTab());
    mainFragmentViewModel.setPrimaryTwoColor(
        requireMainActivity().getCurrentColorPreference().getPrimarySecondTab());
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
    mToolbarContainer = requireMainActivity().getAppbar().getAppbarLayout();
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

    mSwipeRefreshLayout.setOnRefreshListener(() -> updateList(true));

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
      if (mainFragmentViewModel.getColumns() == null)
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
        new DividerItemDecoration(requireActivity(), false, getBoolean(PREFERENCE_SHOW_DIVIDERS));
    listView.addItemDecoration(dividerItemDecoration);
    mSwipeRefreshLayout.setColorSchemeColors(mainFragmentViewModel.getAccentColor());
    DefaultItemAnimator animator = new DefaultItemAnimator();
    listView.setItemAnimator(animator);
    mToolbarContainer.getViewTreeObserver().addOnGlobalLayoutListener(this);
    loadViews();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
    fragmentManager.executePendingTransactions();
    fragmentManager.putFragment(outState, KEY_FRAGMENT_MAIN, this);
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
    mainFragmentViewModel.setAdapterListItems(null);
    mainFragmentViewModel.setIconList(null);
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
    mainFragmentViewModel.setAdapterListItems(null);
    mainFragmentViewModel.setIconList(null);
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
      if (mainFragmentViewModel.getListElements().size() == 0
          && !mainFragmentViewModel.getResults()) {
        loadlist(
            mainFragmentViewModel.getCurrentPath(),
            true,
            mainFragmentViewModel.getOpenMode(),
            false);
      } else {
        reloadListElements(
            true, mainFragmentViewModel.getResults(), !mainFragmentViewModel.isList());
      }
    } else {
      loadlist(mainFragmentViewModel.getHome(), true, mainFragmentViewModel.getOpenMode(), false);
    }
  }

  private BroadcastReceiver receiver2 =
      new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
          // load the list on a load broadcast
          // local file system don't need an explicit load, we've set an observer to
          // take actions on creation/moving/deletion/modification of file on current path
          if (getCurrentPath() != null) {
            mainActivityViewModel.evictPathFromListCache(getCurrentPath());
          }
          updateList(false);
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
                requireMainActivity(),
                sharedPref);
            mainFragmentViewModel.setEncryptOpen(false);
          }
        }
      };

  public void home() {
    loadlist((mainFragmentViewModel.getHome()), false, OpenMode.FILE, false);
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
      AppCompatImageView imageView) {
    if (mainFragmentViewModel.getResults()) {
      // check to initialize search results
      // if search task is been running, cancel it
      FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
      SearchWorkerFragment fragment =
          (SearchWorkerFragment) fragmentManager.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);
      if (fragment != null) {
        if (fragment.searchAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
          fragment.searchAsyncTask.cancel(true);
        }
        requireActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
      }

      mainFragmentViewModel.setRetainSearchTask(true);
      mainFragmentViewModel.setResults(false);
    } else {
      mainFragmentViewModel.setRetainSearchTask(false);
      MainActivityHelper.SEARCH_TEXT = null;
    }

    if (requireMainActivity().getListItemSelected()) {
      if (isBackButton) {
        requireMainActivity().setListItemSelected(false);
        if (requireMainActivity().getActionModeHelper().getActionMode() != null) {
          requireMainActivity().getActionModeHelper().getActionMode().finish();
        }
        requireMainActivity().getActionModeHelper().setActionMode(null);
      } else {
        // the first {goback} item if back navigation is enabled
        registerListItemChecked(position, imageView);
      }
    } else {
      if (isBackButton) {
        goBackItemClick();
      } else {
        // hiding search view if visible
        if (requireMainActivity().getAppbar().getSearchView().isEnabled()) {
          requireMainActivity().getAppbar().getSearchView().hideSearchView();
        }

        String path =
            !layoutElementParcelable.hasSymlink()
                ? layoutElementParcelable.desc
                : layoutElementParcelable.symlink;

        if (layoutElementParcelable.isDirectory) {
          computeScroll();
          loadlist(path, false, mainFragmentViewModel.getOpenMode(), false);
        } else if (layoutElementParcelable.desc.endsWith(CryptUtil.CRYPT_EXTENSION)
            || layoutElementParcelable.desc.endsWith(CryptUtil.AESCRYPT_EXTENSION)) {
          // decrypt the file
          mainFragmentViewModel.setEncryptOpen(true);
          mainFragmentViewModel.initEncryptBaseFile(
              getActivity().getExternalCacheDir().getPath()
                  + "/"
                  + layoutElementParcelable
                      .generateBaseFile()
                      .getName(getMainActivity())
                      .replace(CryptUtil.CRYPT_EXTENSION, "")
                      .replace(CryptUtil.AESCRYPT_EXTENSION, ""));

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
            returnIntentResults(
                new HybridFileParcelable[] {layoutElementParcelable.generateBaseFile()});
          } else {
            layoutElementParcelable.generateBaseFile().openFile(getMainActivity(), false);
            DataUtils.getInstance().addHistoryFile(layoutElementParcelable.desc);
          }
        }
      }
    }
  }

  public void registerListItemChecked(int position, AppCompatImageView imageView) {
    MainActivity mainActivity = requireMainActivity();
    if (mainActivity.mReturnIntent
        && !mainActivity.getIntent().getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)) {
      // Only one item should be checked
      ArrayList<Integer> checkedItemsIndex = adapter.getCheckedItemsIndex();
      if (checkedItemsIndex.contains(position)) {
        // The clicked item was the only item checked so it can be unchecked
        adapter.toggleChecked(position, imageView);
      } else {
        // The clicked item was not checked so we have to uncheck all currently checked items
        for (Integer index : checkedItemsIndex) {
          adapter.toggleChecked(index, imageView);
        }
        // Now we check the clicked item
        adapter.toggleChecked(position, imageView);
      }
    } else adapter.toggleChecked(position, imageView);
  }

  public void updateTabWithDb(Tab tab) {
    mainFragmentViewModel.setCurrentPath(tab.path);
    mainFragmentViewModel.setHome(tab.home);
    loadlist(mainFragmentViewModel.getCurrentPath(), false, OpenMode.UNKNOWN, false);
  }

  /**
   * Returns the intent with uri corresponding to specific {@link HybridFileParcelable} back to
   * external app
   */
  public void returnIntentResults(HybridFileParcelable[] baseFiles) {
    requireMainActivity().mReturnIntent = false;
    HashMap<HybridFileParcelable, Uri> resultUris = new HashMap<>();
    ArrayList<String> failedPaths = new ArrayList<>();

    for (HybridFileParcelable baseFile : baseFiles) {
      @Nullable Uri resultUri = Utils.getUriForBaseFile(requireActivity(), baseFile);
      if (resultUri != null) {
        resultUris.put(baseFile, resultUri);
        LOG.debug(
            resultUri + "\t" + MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
      } else {
        failedPaths.add(baseFile.getPath());
      }
    }

    if (!resultUris.isEmpty()) {
      Intent intent = new Intent();
      intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      if (resultUris.size() == 1) {
        intent.setAction(Intent.ACTION_SEND);
        Map.Entry<HybridFileParcelable, Uri> result = resultUris.entrySet().iterator().next();
        Uri resultUri = result.getValue();
        HybridFileParcelable resultBaseFile = result.getKey();

        if (requireMainActivity().mRingtonePickerIntent) {
          intent.setDataAndType(
              resultUri,
              MimeTypes.getMimeType(resultBaseFile.getPath(), resultBaseFile.isDirectory()));
          intent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, resultUri);
        } else {
          LOG.debug("pickup file");
          intent.setDataAndType(resultUri, MimeTypes.getExtension(resultBaseFile.getPath()));
        }

      } else {
        LOG.debug("pickup multiple files");
        // Build ClipData
        ArrayList<ClipData.Item> uriDataClipItems = new ArrayList<>();
        HashSet<String> mimeTypes = new HashSet<>();
        for (Map.Entry<HybridFileParcelable, Uri> result : resultUris.entrySet()) {
          HybridFileParcelable baseFile = result.getKey();
          Uri uri = result.getValue();
          mimeTypes.add(MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
          uriDataClipItems.add(new ClipData.Item(uri));
        }
        ClipData clipData =
            new ClipData(
                ClipDescription.MIMETYPE_TEXT_URILIST,
                mimeTypes.toArray(new String[0]),
                uriDataClipItems.remove(0));
        for (ClipData.Item item : uriDataClipItems) {
          clipData.addItem(item);
        }

        intent.setClipData(clipData);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putParcelableArrayListExtra(
            Intent.EXTRA_STREAM, new ArrayList<>(resultUris.values()));
      }

      requireActivity().setResult(FragmentActivity.RESULT_OK, intent);
    }
    if (!failedPaths.isEmpty()) {
      LOG.warn("Unable to get URIs from baseFiles {}", failedPaths);
    }
    requireActivity().finish();
  }

  LoadFilesListTask loadFilesListTask;

  /**
   * This loads a path into the MainFragment.
   *
   * @param providedPath the path to be loaded
   * @param back if we're coming back from any directory and want the scroll to be restored
   * @param providedOpenMode the mode in which the directory should be opened
   * @param forceReload whether use cached list or force reload the list items
   */
  public void loadlist(
      final String providedPath,
      final boolean back,
      final OpenMode providedOpenMode,
      boolean forceReload) {
    if (mainFragmentViewModel == null) {
      LOG.warn("Viewmodel not available to load the data");
      return;
    }

    if (getMainActivity() != null
        && getMainActivity().getActionModeHelper() != null
        && getMainActivity().getActionModeHelper().getActionMode() != null) {
      getMainActivity().getActionModeHelper().getActionMode().finish();
    }

    mSwipeRefreshLayout.setRefreshing(true);

    if (loadFilesListTask != null && loadFilesListTask.getStatus() == AsyncTask.Status.RUNNING) {
      LOG.warn("Existing load list task running, cancel current");
      loadFilesListTask.cancel(true);
    }

    OpenMode openMode = providedOpenMode;
    String actualPath = FileProperties.remapPathForApi30OrAbove(providedPath, false);

    if (SDK_INT >= Q && ArraysKt.any(ANDROID_DATA_DIRS, providedPath::contains)) {
      openMode = loadPathInQ(actualPath, providedPath, providedOpenMode);
    }
    // Monkeypatch :( to fix problems with unexpected non content URI path while openMode is still
    // OpenMode.DOCUMENT_FILE
    else if (actualPath.startsWith("/")
        && (OpenMode.DOCUMENT_FILE.equals(openMode) || OpenMode.ANDROID_DATA.equals(openMode))) {
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
            forceReload,
            (data) -> {
              mSwipeRefreshLayout.setRefreshing(false);
              if (data != null && data.second != null) {
                boolean isPathLayoutGrid =
                    DataUtils.getInstance().getListOrGridForPath(providedPath, DataUtils.LIST)
                        == DataUtils.GRID;
                setListElements(
                    data.second, back, providedPath, data.first, false, isPathLayoutGrid);
              } else {
                LOG.warn("Load list operation cancelled");
              }
            });
    loadFilesListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @RequiresApi(api = Q)
  private OpenMode loadPathInQ(String actualPath, String providedPath, OpenMode providedMode) {

    if (GenericExtKt.containsPath(ANDROID_DEVICE_DATA_DIRS, providedPath)
        && !OpenMode.ANDROID_DATA.equals(providedMode)) {
      return OpenMode.ANDROID_DATA;
    } else if (actualPath.startsWith("/")) {
      return OpenMode.FILE;
    } else if (actualPath.equals(providedPath)) {
      return providedMode;
    } else {
      boolean hasAccessToSpecialFolder = false;
      List<UriPermission> uriPermissions =
          requireContext().getContentResolver().getPersistedUriPermissions();

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
                requireMainActivity(),
                R.string.android_data_prompt_saf_access,
                R.string.android_data_prompt_saf_access_title,
                android.R.string.ok,
                android.R.string.cancel);
        d.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener(
                v -> {
                  ExtensionsKt.runIfDocumentsUIExists(
                      intent,
                      requireMainActivity(),
                      () -> handleDocumentUriForRestrictedDirectories.launch(intent));

                  d.dismiss();
                });
        d.show();
        // At this point LoadFilesListTask will be triggered.
        // No harm even give OpenMode.FILE here, it loads blank when it doesn't; and after the
        // UriPermission is granted loadlist will be called again
        return OpenMode.FILE;
      } else {
        return OpenMode.DOCUMENT_FILE;
      }
    }
  }

  void initNoFileLayout() {
    nofilesview = rootView.findViewById(R.id.nofilelayout);
    nofilesview.setColorSchemeColors(mainFragmentViewModel.getAccentColor());
    nofilesview.setOnRefreshListener(
        () -> {
          loadlist(
              (mainFragmentViewModel.getCurrentPath()),
              false,
              mainFragmentViewModel.getOpenMode(),
              false);
          nofilesview.setRefreshing(false);
        });
    nofilesview
        .findViewById(R.id.no_files_relative)
        .setOnKeyListener(
            (v, keyCode, event) -> {
              if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                  requireMainActivity().getFAB().requestFocus();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                  requireMainActivity().onBackPressed();
                } else {
                  return false;
                }
              }
              return true;
            });
    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
      ((AppCompatImageView) nofilesview.findViewById(R.id.image))
          .setColorFilter(Color.parseColor("#666666"));
    } else if (utilsProvider.getAppTheme().equals(AppTheme.BLACK)) {
      nofilesview.setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
      ((AppCompatTextView) nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
    } else {
      nofilesview.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
      ((AppCompatTextView) nofilesview.findViewById(R.id.nofiletext)).setTextColor(Color.WHITE);
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
      List<LayoutElementParcelable> bitmap,
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
      loadlist(mainFragmentViewModel.getHome(), true, OpenMode.FILE, false);
    }
  }

  public void reloadListElements(boolean back, boolean results, boolean grid) {
    if (isAdded()) {
      mainFragmentViewModel.setResults(results);
      boolean isOtg = (OTGUtil.PREFIX_OTG + "/").equals(mainFragmentViewModel.getCurrentPath());

      if (getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON)
          && !"/".equals(mainFragmentViewModel.getCurrentPath())
          && (mainFragmentViewModel.getOpenMode() == OpenMode.FILE
              || mainFragmentViewModel.getOpenMode() == OpenMode.ROOT
              || (mainFragmentViewModel.getIsCloudOpenMode()
                  && !mainFragmentViewModel.getIsOnCloudRoot()))
          && !isOtg
          && (mainFragmentViewModel.getListElements().size() == 0
              || !mainFragmentViewModel
                  .getListElements()
                  .get(0)
                  .size
                  .equals(getString(R.string.goback)))
          && !results) {
        mainFragmentViewModel.getListElements().add(0, getBackElement());
      }

      if (mainFragmentViewModel.getListElements().size() == 0 && !results) {
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
                requireMainActivity(),
                this,
                utilsProvider,
                sharedPref,
                listView,
                listElements,
                requireContext(),
                grid);
      } else {
        adapter.setItems(listView, mainFragmentViewModel.getListElements());
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
            new DividerItemDecoration(
                requireMainActivity(), true, getBoolean(PREFERENCE_SHOW_DIVIDERS));
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

      requireMainActivity().updatePaths(mainFragmentViewModel.getNo());
      requireMainActivity().showFab();
      requireMainActivity().getAppbar().getAppbarLayout().setExpanded(true);
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

      listView.post(
          () -> {
            String fileName = requireMainActivity().getScrollToFileName();

            if (fileName != null)
              mainFragmentViewModel
                  .getScrollPosition(fileName)
                  .observe(
                      getViewLifecycleOwner(),
                      scrollPosition -> {
                        if (scrollPosition != -1)
                          listView.scrollToPosition(
                              Math.min(scrollPosition + 4, adapter.getItemCount() - 1));
                        adapter.notifyItemChanged(scrollPosition);
                      });
          });

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
      (requireMainActivity())
          .registerReceiver(
              decryptReceiver, new IntentFilter(EncryptDecryptUtils.DECRYPT_BROADCAST));
      if (!mainFragmentViewModel.isEncryptOpen()
          && !Utils.isNullOrEmpty(mainFragmentViewModel.getEncryptBaseFiles())) {
        // we've opened the file and are ready to delete it
        new DeleteTask(requireMainActivity()).execute(mainFragmentViewModel.getEncryptBaseFiles());
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
              AppCompatEditText textfield =
                  dialog.getCustomView().findViewById(R.id.singleedittext_input);
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
    AppCompatEditText textfield =
        renameDialog.getCustomView().findViewById(R.id.singleedittext_input);
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
      loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE, false);
      return;
    }

    HybridFile currentFile =
        new HybridFile(mainFragmentViewModel.getOpenMode(), mainFragmentViewModel.getCurrentPath());
    if (!mainFragmentViewModel.getResults()) {
      if (!mainFragmentViewModel.getRetainSearchTask()) {
        // normal case
        if (requireMainActivity().getListItemSelected()) {
          adapter.toggleChecked(false);
        } else {
          if (OpenMode.SMB.equals(mainFragmentViewModel.getOpenMode())) {
            if (mainFragmentViewModel.getSmbPath() != null
                && !mainFragmentViewModel
                    .getSmbPath()
                    .equals(mainFragmentViewModel.getCurrentPath())) {
              StringBuilder path = new StringBuilder(currentFile.getSmbFile().getParent());
              if (mainFragmentViewModel.getCurrentPath() != null
                  && mainFragmentViewModel.getCurrentPath().indexOf('?') > 0)
                path.append(
                    mainFragmentViewModel
                        .getCurrentPath()
                        .substring(mainFragmentViewModel.getCurrentPath().indexOf('?')));
              loadlist(
                  path.toString().replace("%3D", "="),
                  true,
                  mainFragmentViewModel.getOpenMode(),
                  false);
            } else loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE, false);
          } else if (OpenMode.SFTP.equals(mainFragmentViewModel.getOpenMode())) {
            if (currentFile.getParent(requireContext()) == null) {
              loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE, false);
            } else if (OpenMode.DOCUMENT_FILE.equals(mainFragmentViewModel.getOpenMode())) {
              loadlist(currentFile.getParent(getContext()), true, currentFile.getMode(), false);
            } else {

              String parent = currentFile.getParent(getContext());

              if (parent == null)
                parent =
                    mainFragmentViewModel.getHome(); // fall back by traversing back to home folder

              loadlist(parent, true, mainFragmentViewModel.getOpenMode(), false);
            }
          } else if (OpenMode.FTP.equals(mainFragmentViewModel.getOpenMode())) {
            if (mainFragmentViewModel.getCurrentPath() != null) {
              String parent = currentFile.getParent(getContext());
              // Hack.
              if (parent != null && parent.contains("://")) {
                loadlist(parent, true, mainFragmentViewModel.getOpenMode(), false);
              } else {
                loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE, false);
              }
            } else {
              loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE, false);
            }
          } else if (("/").equals(mainFragmentViewModel.getCurrentPath())
              || (mainFragmentViewModel.getHome() != null
                  && mainFragmentViewModel.getHome().equals(mainFragmentViewModel.getCurrentPath()))
              || mainFragmentViewModel.getIsOnCloudRoot()) {
            getMainActivity().exit();
          } else if (OpenMode.DOCUMENT_FILE.equals(mainFragmentViewModel.getOpenMode())
              && !currentFile.getPath().startsWith("content://")) {
            if (CollectionsKt.contains(
                ANDROID_DEVICE_DATA_DIRS, currentFile.getParent(getContext()))) {
              loadlist(currentFile.getParent(getContext()), false, OpenMode.ANDROID_DATA, false);
            } else {
              loadlist(
                  currentFile.getParent(getContext()),
                  true,
                  mainFragmentViewModel.getOpenMode(),
                  false);
            }
          } else if (FileUtils.canGoBack(getContext(), currentFile)) {
            loadlist(
                currentFile.getParent(getContext()),
                true,
                mainFragmentViewModel.getOpenMode(),
                false);
          } else {
            requireMainActivity().exit();
          }
        }
      } else {
        // case when we had pressed on an item from search results and wanna go back
        // leads to resuming the search task

        if (MainActivityHelper.SEARCH_TEXT != null) {

          // starting the search query again :O
          FragmentManager fm = requireMainActivity().getSupportFragmentManager();

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
              requireMainActivity().isRootExplorer(),
              sharedPref.getBoolean(SearchWorkerFragment.KEY_REGEX, false),
              sharedPref.getBoolean(SearchWorkerFragment.KEY_REGEX_MATCHES, false));
        } else {
          loadlist(mainFragmentViewModel.getCurrentPath(), true, OpenMode.UNKNOWN, false);
        }
        mainFragmentViewModel.setRetainSearchTask(false);
      }
    } else {
      // to go back after search list have been popped
      FragmentManager fm = requireMainActivity().getSupportFragmentManager();
      SearchWorkerFragment fragment =
          (SearchWorkerFragment) fm.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);
      if (fragment != null) {
        if (fragment.searchAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
          fragment.searchAsyncTask.cancel(true);
        }
      }
      if (mainFragmentViewModel.getCurrentPath() != null) {
        loadlist(
            new File(mainFragmentViewModel.getCurrentPath()).getPath(),
            true,
            OpenMode.UNKNOWN,
            false);
      }
      mainFragmentViewModel.setResults(false);
    }
  }

  public void reauthenticateSmb() {
    if (mainFragmentViewModel.getSmbPath() != null) {
      try {
        requireMainActivity()
            .runOnUiThread(
                () -> {
                  int i;
                  AppConfig.toast(requireContext(), getString(R.string.unknown_error));
                  if ((i =
                          DataUtils.getInstance()
                              .containsServer(mainFragmentViewModel.getSmbPath()))
                      != -1) {
                    requireMainActivity()
                        .showSMBDialog(
                            DataUtils.getInstance().getServers().get(i)[0],
                            mainFragmentViewModel.getSmbPath(),
                            true);
                  }
                });
      } catch (Exception e) {
        LOG.warn("failure when reauthenticating smb connection", e);
      }
    }
  }

  public void goBackItemClick() {
    if (mainFragmentViewModel.getOpenMode() == OpenMode.CUSTOM) {
      loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE, false);
      return;
    }
    HybridFile currentFile =
        new HybridFile(mainFragmentViewModel.getOpenMode(), mainFragmentViewModel.getCurrentPath());
    if (!mainFragmentViewModel.getResults()) {
      if (requireMainActivity().getListItemSelected()) {
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
            loadlist(path.toString(), true, OpenMode.SMB, false);
          } else loadlist(mainFragmentViewModel.getHome(), false, OpenMode.FILE, false);
        } else if (("/").equals(mainFragmentViewModel.getCurrentPath())
            || mainFragmentViewModel.getIsOnCloudRoot()) {
          requireMainActivity().exit();
        } else if (FileUtils.canGoBack(getContext(), currentFile)) {
          loadlist(
              currentFile.getParent(getContext()),
              true,
              mainFragmentViewModel.getOpenMode(),
              false);
        } else requireMainActivity().exit();
      }
    } else {
      loadlist(currentFile.getPath(), true, mainFragmentViewModel.getOpenMode(), false);
    }
  }

  public void updateList(boolean forceReload) {
    computeScroll();
    loadlist(
        mainFragmentViewModel.getCurrentPath(),
        true,
        mainFragmentViewModel.getOpenMode(),
        forceReload);
  }

  @Override
  public void onResume() {
    super.onResume();
    (requireActivity())
        .registerReceiver(receiver2, new IntentFilter(MainActivity.KEY_INTENT_LOAD_LIST));

    resumeDecryptOperations();
    startFileObserver();
  }

  @Override
  public void onPause() {
    super.onPause();
    (requireActivity()).unregisterReceiver(receiver2);
    if (customFileObserver != null) {
      customFileObserver.stopWatching();
    }

    if (SDK_INT >= JELLY_BEAN_MR2) {
      (requireActivity()).unregisterReceiver(decryptReceiver);
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
                requireContext(),
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
                requireContext(),
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
  @Nullable
  private LayoutElementParcelable addTo(@NonNull HybridFileParcelable hybridFileParcelable) {
    if (DataUtils.getInstance().isFileHidden(hybridFileParcelable.getPath())) {
      return null;
    }

    if (hybridFileParcelable.isDirectory()) {
      LayoutElementParcelable layoutElement =
          new LayoutElementParcelable(
              requireContext(),
              hybridFileParcelable.getPath(),
              hybridFileParcelable.getPermission(),
              hybridFileParcelable.getLink(),
              "",
              0,
              true,
              hybridFileParcelable.getDate() + "",
              true,
              getBoolean(PREFERENCE_SHOW_THUMB),
              hybridFileParcelable.getMode());

      mainFragmentViewModel.getListElements().add(layoutElement);
      mainFragmentViewModel.setFolderCount(mainFragmentViewModel.getFolderCount() + 1);
      return layoutElement;
    } else {
      long longSize = 0;
      String size = "";
      if (hybridFileParcelable.getSize() != -1) {
        longSize = hybridFileParcelable.getSize();
        size = Formatter.formatFileSize(getContext(), longSize);
      }

      LayoutElementParcelable layoutElement =
          new LayoutElementParcelable(
              requireContext(),
              hybridFileParcelable.getPath(),
              hybridFileParcelable.getPermission(),
              hybridFileParcelable.getLink(),
              size,
              longSize,
              false,
              hybridFileParcelable.getDate() + "",
              false,
              getBoolean(PREFERENCE_SHOW_THUMB),
              hybridFileParcelable.getMode());
      mainFragmentViewModel.getListElements().add(layoutElement);
      mainFragmentViewModel.setFileCount(mainFragmentViewModel.getFileCount() + 1);
      return layoutElement;
    }
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
          requireMainActivity()
              .mainActivityHelper
              .mkFile(
                  new HybridFile(OpenMode.FILE, path),
                  new HybridFile(OpenMode.FILE, f1.getPath()),
                  this);
        } catch (Exception e) {
          LOG.warn("failure when hiding file", e);
        }
      }
      MediaConnectionUtils.scanFile(
          requireMainActivity(), new HybridFile[] {new HybridFile(OpenMode.FILE, path)});
    }
  }

  public void addShortcut(LayoutElementParcelable path) {
    // Adding shortcut for MainActivity
    // on Home screen
    final Context ctx = requireContext();

    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
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
            .setActivity(requireMainActivity().getComponentName())
            .setIcon(IconCompat.createWithResource(ctx, R.mipmap.ic_launcher))
            .setIntent(shortcutIntent)
            .setLongLabel(path.desc)
            .setShortLabel(new File(path.desc).getName())
            .build();

    ShortcutManagerCompat.requestPinShortcut(ctx, info, null);
  }

  // This method is used to implement the modification for the pre Searching
  public void onSearchPreExecute(String query) {
    requireMainActivity().getAppbar().getBottomBar().setPathText("");
    requireMainActivity()
        .getAppbar()
        .getBottomBar()
        .setFullPathText(getString(R.string.searching, query));
  }

  // adds search results based on result boolean. If false, the adapter is initialised with initial
  // values, if true, new values are added to the adapter.
  public void addSearchResult(@NonNull HybridFileParcelable hybridFileParcelable, String query) {
    if (listView == null) {
      return;
    }

    // initially clearing the array for new result set
    if (!mainFragmentViewModel.getResults()) {
      mainFragmentViewModel.getListElements().clear();
      mainFragmentViewModel.setFileCount(0);
      mainFragmentViewModel.setFolderCount(0);
    }

    // adding new value to LIST_ELEMENTS
    @Nullable LayoutElementParcelable layoutElementAdded = addTo(hybridFileParcelable);
    if (!requireMainActivity()
        .getAppbar()
        .getBottomBar()
        .getFullPathText()
        .contains(getString(R.string.searching))) {
      requireMainActivity()
          .getAppbar()
          .getBottomBar()
          .setFullPathText(getString(R.string.searching, query));
    }
    if (!mainFragmentViewModel.getResults()) {
      reloadListElements(false, true, !mainFragmentViewModel.isList());
      requireMainActivity().getAppbar().getBottomBar().setPathText("");
    } else if (layoutElementAdded != null) {
      adapter.addItem(layoutElementAdded);
    }
    stopAnimation();
  }

  public void onSearchCompleted(final String query) {
    final List<LayoutElementParcelable> elements = mainFragmentViewModel.getListElements();
    if (!mainFragmentViewModel.getResults()) {
      // no results were found
      mainFragmentViewModel.getListElements().clear();
    }
    TaskKt.fromTask(
        new SortSearchResultTask(
            elements,
            new FileListSorter(
                mainFragmentViewModel.getDsort(), mainFragmentViewModel.getSortType()),
            this,
            query));
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
  public List<LayoutElementParcelable> getElementsList() {
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

  public void smoothScrollListView(boolean upDirection) {
    if (listView != null) {
      if (upDirection) {
        listView.smoothScrollToPosition(0);
      } else {
        listView.smoothScrollToPosition(mainFragmentViewModel.getAdapterListItems().size());
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
      LOG.warn("Viewmodel not available to get current path");
      return null;
    }
    return mainFragmentViewModel.getCurrentPath();
  }

  @Override
  public void changePath(@NonNull String path) {
    loadlist(path, false, mainFragmentViewModel.getOpenMode(), false);
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
    return requireMainActivity().getBoolean(key);
  }

  @Override
  public void onGlobalLayout() {
    if (mainFragmentViewModel.getColumns() == null) {
      int screenWidth = listView.getWidth();
      int dpToPx = Utils.dpToPx(requireContext(), 115);
      if (dpToPx == 0) {
        // HACK to fix a crash see #3249
        dpToPx = 1;
      }
      mainFragmentViewModel.setColumns(screenWidth / dpToPx);
      if (!mainFragmentViewModel.isList()) {
        mLayoutManagerGrid.setSpanCount(mainFragmentViewModel.getColumns());
      }
    }
    // TODO: This trigger causes to lose selected items in case of grid view,
    //  but is necessary to adjust columns for grid view when screen is rotated
    /*if (!mainFragmentViewModel.isList()) {
      loadViews();
    }*/
    if (android.os.Build.VERSION.SDK_INT >= JELLY_BEAN) {
      mToolbarContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    } else {
      mToolbarContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }
  }

  public @Nullable MainFragmentViewModel getMainFragmentViewModel() {
    if (isAdded()) {
      if (mainFragmentViewModel == null) {
        mainFragmentViewModel = new ViewModelProvider(this).get(MainFragmentViewModel.class);
      }
      return mainFragmentViewModel;
    } else {
      LOG.error("Failed to get viewmodel, fragment not yet added");
      return null;
    }
  }

  public @Nullable MainActivityViewModel getMainActivityViewModel() {
    if (isAdded()) {
      if (mainActivityViewModel == null) {
        mainActivityViewModel =
            new ViewModelProvider(requireMainActivity()).get(MainActivityViewModel.class);
      }
      return mainActivityViewModel;
    } else {
      LOG.error("Failed to get viewmodel, fragment not yet added");
      return null;
    }
  }

  @Override
  public void adjustListViewForTv(
      @NonNull ItemViewHolder viewHolder, @NonNull MainActivity mainActivity) {
    try {
      int[] location = new int[2];
      viewHolder.baseItemView.getLocationOnScreen(location);
      LOG.info("Current x and y " + location[0] + " " + location[1]);
      if (location[1] < requireMainActivity().getAppbar().getAppbarLayout().getHeight()) {
        listView.scrollToPosition(Math.max(viewHolder.getAdapterPosition() - 5, 0));
      } else if (location[1] + viewHolder.baseItemView.getHeight()
          > requireContext().getResources().getDisplayMetrics().heightPixels) {
        listView.scrollToPosition(
            Math.min(viewHolder.getAdapterPosition() + 5, adapter.getItemCount() - 1));
      }
    } catch (IndexOutOfBoundsException e) {
      LOG.warn("Failed to adjust scrollview for tv", e);
    }
  }
}
