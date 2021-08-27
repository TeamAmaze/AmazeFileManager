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

import static android.provider.MediaStore.MediaColumns.DISPLAY_NAME;
import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION;
import static kotlin.io.ConstantsKt.DEFAULT_BUFFER_SIZE;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.CompressedExplorerAdapter;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity;
import com.amaze.filemanager.ui.colors.ColorPreferenceHelper;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.DividerItemDecoration;
import com.amaze.filemanager.ui.views.FastScroller;
import com.amaze.filemanager.utils.BottomBarButtonPath;
import com.amaze.filemanager.utils.Utils;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.google.android.material.appbar.AppBarLayout;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import kotlin.io.ByteStreamsKt;

public class CompressedExplorerFragment extends Fragment implements BottomBarButtonPath {
  public static final String KEY_PATH = "path";

  private static final String KEY_CACHE_FILES = "cache_files";
  private static final String KEY_URI = "uri";
  private static final String KEY_FILE = "file";
  private static final String KEY_WHOLE_LIST = "whole_list";
  private static final String KEY_ELEMENTS = "elements";
  private static final String KEY_OPEN = "is_open";
  private static final String TAG = CompressedExplorerFragment.class.getSimpleName();

  public File compressedFile;

  /**
   * files to be deleted from cache with a Map maintaining key - the root of directory created (for
   * deletion purposes after we exit out of here and value - the path of file to open
   */
  public ArrayList<HybridFileParcelable> files;

  public boolean selection = false;
  public String relativeDirectory =
      ""; // Normally this would be "/" but for pathing issues it isn't
  public @ColorInt int accentColor, iconskin;
  public String year;
  public CompressedExplorerAdapter compressedExplorerAdapter;
  public ActionMode mActionMode;
  public boolean coloriseIcons, showSize, showLastModified, gobackitem;
  public ArrayList<CompressedObjectParcelable> elements = new ArrayList<>();
  public MainActivity mainActivity;
  public RecyclerView listView;
  public SwipeRefreshLayout swipeRefreshLayout;
  public boolean isOpen = false; // flag states whether to open file after service extracts it

  private FastScroller fastScroller = null;
  private UtilitiesProvider utilsProvider;
  private Decompressor decompressor;
  private View rootView;
  private boolean addheader = true;
  private LinearLayoutManager mLayoutManager;
  private DividerItemDecoration dividerItemDecoration;
  private boolean showDividers;
  private View mToolbarContainer;
  private boolean stopAnims = true;
  private int file = 0, folder = 0;
  private boolean isCachedCompressedFile = false;
  private final AppBarLayout.OnOffsetChangedListener offsetListenerForToolbar =
      (appBarLayout, verticalOffset) -> {
        if (fastScroller == null) {
          return;
        }
        fastScroller.updateHandlePosition(verticalOffset, 112);
      };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    utilsProvider = ((BasicActivity) getActivity()).getUtilsProvider();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = inflater.inflate(R.layout.main_frag, container, false);
    mainActivity = (MainActivity) getActivity();
    listView = rootView.findViewById(R.id.listView);
    listView.setOnTouchListener(
        (view, motionEvent) -> {
          if (compressedExplorerAdapter == null) {
            return false;
          }

          if (stopAnims && !compressedExplorerAdapter.stoppedAnimation) {
            stopAnim();
          }
          compressedExplorerAdapter.stoppedAnimation = true;

          stopAnims = false;
          return false;
        });
    swipeRefreshLayout = rootView.findViewById(R.id.activity_main_swipe_refresh_layout);
    swipeRefreshLayout.setOnRefreshListener(this::refresh);
    swipeRefreshLayout.setRefreshing(true);

    return rootView;
  }

  public void stopAnim() {
    for (int j = 0; j < listView.getChildCount(); j++) {
      View v = listView.getChildAt(j);
      if (v != null) v.clearAnimation();
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireActivity());

    String fileName = prepareCompressedFile();

    mToolbarContainer = mainActivity.getAppbar().getAppbarLayout();
    mToolbarContainer.setOnTouchListener(
        (v, motionEvent) -> {
          if (stopAnims) {
            if ((!compressedExplorerAdapter.stoppedAnimation)) {
              stopAnim();
            }
            compressedExplorerAdapter.stoppedAnimation = true;
          }
          stopAnims = false;
          return false;
        });

    listView.setVisibility(View.VISIBLE);
    mLayoutManager = new LinearLayoutManager(getActivity());
    listView.setLayoutManager(mLayoutManager);

    if (utilsProvider.getAppTheme().equals(AppTheme.DARK)) {
      rootView.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
    } else if (utilsProvider.getAppTheme().equals(AppTheme.BLACK)) {
      listView.setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
    } else {
      listView.setBackgroundColor(Utils.getColor(getContext(), android.R.color.background_light));
    }

    gobackitem = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON, false);
    coloriseIcons = sp.getBoolean(PreferencesConstants.PREFERENCE_COLORIZE_ICONS, true);
    showSize = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE, false);
    showLastModified = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED, true);
    showDividers = sp.getBoolean(PreferencesConstants.PREFERENCE_SHOW_DIVIDERS, true);
    year = ("" + Calendar.getInstance().get(Calendar.YEAR)).substring(2, 4);

    accentColor = mainActivity.getAccent();
    iconskin = mainActivity.getCurrentColorPreference().getIconSkin();

    // mainActivity.findViewById(R.id.buttonbarframe).setBackgroundColor(Color.parseColor(skin));

    if (savedInstanceState == null && compressedFile != null) {
      files = new ArrayList<>();
      // adding a cache file to delete where any user interaction elements will be cached
      String path =
          (isCachedCompressedFile)
            ? compressedFile.getAbsolutePath()
            : getActivity().getExternalCacheDir().getPath() + SEPARATOR + fileName;
      files.add(new HybridFileParcelable(path));
      decompressor = CompressedHelper.getCompressorInstance(getContext(), compressedFile);

      changePath("");
    } else {
      onRestoreInstanceState(savedInstanceState);
    }
    mainActivity.supportInvalidateOptionsMenu();
  }

  @NonNull
  private String prepareCompressedFile() {
    String pathArg = getArguments().getString(KEY_PATH);
    String fileName = null;
    Uri pathUri = Uri.parse(pathArg);
    if (ContentResolver.SCHEME_CONTENT.equals(pathUri.getScheme())) {
      Cursor cursor =
          requireContext()
              .getContentResolver()
              .query(pathUri, new String[] {DISPLAY_NAME}, null, null, null);
      if (cursor != null) {
        try {
          if (cursor.moveToFirst()) {
            fileName = cursor.getString(0);
            compressedFile = new File(requireContext().getCacheDir(), fileName);
          } else {
            // At this point, we know nothing the file the URI represents, we are doing everything
            // wild guess.
            compressedFile =
                File.createTempFile("compressed", null, requireContext().getCacheDir());
            fileName = compressedFile.getName();
          }
          compressedFile.deleteOnExit();
          ByteStreamsKt.copyTo(
              requireContext().getContentResolver().openInputStream(pathUri),
              new FileOutputStream(compressedFile),
              DEFAULT_BUFFER_SIZE);
          isCachedCompressedFile = true;
        } catch (Exception e) {
          Log.e(TAG, "Error opening URI " + pathUri + " for reading", e);
          AppConfig.toast(
              requireContext(),
              requireContext()
                  .getString(
                      R.string.compressed_explorer_fragment_error_open_uri, pathUri.toString()));
          requireActivity().onBackPressed();
        } finally {
          cursor.close();
        }
      }
    } else {
      compressedFile = new File(pathUri.getPath());
      fileName = compressedFile.getName().substring(0, compressedFile.getName().lastIndexOf("."));
    }
    return fileName;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelableArrayList(KEY_ELEMENTS, elements);
    outState.putString(KEY_PATH, relativeDirectory);
    outState.putString(KEY_URI, compressedFile.getPath());
    outState.putString(KEY_FILE, compressedFile.getPath());
    outState.putParcelableArrayList(KEY_CACHE_FILES, files);
    outState.putBoolean(KEY_OPEN, isOpen);
  }

  private void onRestoreInstanceState(Bundle savedInstanceState) {
    compressedFile = new File(Uri.parse(savedInstanceState.getString(KEY_URI)).getPath());
    files = savedInstanceState.getParcelableArrayList(KEY_CACHE_FILES);
    isOpen = savedInstanceState.getBoolean(KEY_OPEN);
    elements = savedInstanceState.getParcelableArrayList(KEY_ELEMENTS);
    relativeDirectory = savedInstanceState.getString(KEY_PATH, "");

    decompressor = CompressedHelper.getCompressorInstance(getContext(), compressedFile);
    createViews(elements, relativeDirectory);
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

        View v;

        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          // Inflate a menu resource providing context menu items
          MenuInflater inflater = mode.getMenuInflater();
          v = getActivity().getLayoutInflater().inflate(R.layout.actionmode, null);
          mode.setCustomView(v);
          // assumes that you have "contexual.xml" menu resources
          inflater.inflate(R.menu.contextual, menu);
          hideOption(R.id.cpy, menu);
          hideOption(R.id.cut, menu);
          hideOption(R.id.delete, menu);
          hideOption(R.id.addshortcut, menu);
          hideOption(R.id.share, menu);
          hideOption(R.id.openwith, menu);
          showOption(R.id.all, menu);
          hideOption(R.id.compress, menu);
          hideOption(R.id.hide, menu);
          showOption(R.id.ex, menu);
          mode.setTitle(getString(R.string.select));
          mainActivity.updateViews(
              new ColorDrawable(Utils.getColor(getContext(), R.color.holo_dark_action_mode)));
          if (Build.VERSION.SDK_INT >= 21) {

            Window window = getActivity().getWindow();
            if (mainActivity.getBoolean(PREFERENCE_COLORED_NAVIGATION))
              window.setNavigationBarColor(Utils.getColor(getContext(), android.R.color.black));
          }
          if (Build.VERSION.SDK_INT < 19) {
            mainActivity.getAppbar().getToolbar().setVisibility(View.GONE);
          }
          return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
          ArrayList<Integer> positions = compressedExplorerAdapter.getCheckedItemPositions();
          ((TextView) v.findViewById(R.id.item_count)).setText(positions.size() + "");
          menu.findItem(R.id.all)
              .setTitle(
                  positions.size() == folder + file ? R.string.deselect_all : R.string.select_all);

          return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
          switch (item.getItemId()) {
            case R.id.all:
              ArrayList<Integer> positions = compressedExplorerAdapter.getCheckedItemPositions();
              boolean shouldDeselectAll = positions.size() != folder + file;
              compressedExplorerAdapter.toggleChecked(shouldDeselectAll);
              mode.invalidate();
              item.setTitle(shouldDeselectAll ? R.string.deselect_all : R.string.select_all);
              if (!shouldDeselectAll) {
                selection = false;
                mActionMode.finish();
                mActionMode = null;
              }
              return true;
            case R.id.ex:
              Toast.makeText(getActivity(), getString(R.string.extracting), Toast.LENGTH_SHORT)
                  .show();

              String[] dirs =
                  new String[compressedExplorerAdapter.getCheckedItemPositions().size()];
              for (int i = 0; i < dirs.length; i++) {
                dirs[i] =
                    elements.get(compressedExplorerAdapter.getCheckedItemPositions().get(i)).path;
              }

              decompressor.decompress(compressedFile.getPath(), dirs);

              mode.finish();
              return true;
          }
          return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
          if (compressedExplorerAdapter != null) compressedExplorerAdapter.toggleChecked(false);
          @ColorInt
          int primaryColor =
              ColorPreferenceHelper.getPrimary(
                  mainActivity.getCurrentColorPreference(), MainActivity.currentTab);
          selection = false;
          mainActivity.updateViews(new ColorDrawable(primaryColor));
          if (Build.VERSION.SDK_INT >= 21) {

            Window window = getActivity().getWindow();
            if (mainActivity.getBoolean(PREFERENCE_COLORED_NAVIGATION))
              window.setNavigationBarColor(mainActivity.skinStatusBar);
          }
          mActionMode = null;
        }
      };

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    // Clearing the touch listeners allows the fragment to
    // be cleaned after it is destroyed, preventing leaks
    mToolbarContainer.setOnTouchListener(null);
    ((AppBarLayout) mToolbarContainer).removeOnOffsetChangedListener(offsetListenerForToolbar);

    mainActivity.supportInvalidateOptionsMenu();

    // needed to remove any extracted file from cache, when onResume was not called
    // in case of opening any unknown file inside the zip

    if (files.get(0).exists()) {
      new DeleteTask(getActivity(), this).execute(files);
    }
    if (isCachedCompressedFile) {
      compressedFile.delete();
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    mainActivity.getFAB().hide();
    Intent intent = new Intent(getActivity(), ExtractService.class);
    getActivity().bindService(intent, mServiceConnection, 0);
  }

  @Override
  public void onPause() {
    super.onPause();

    getActivity().unbindService(mServiceConnection);
  }

  private ServiceConnection mServiceConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {}

        @Override
        public void onServiceDisconnected(ComponentName name) {
          // open file if pending
          if (isOpen) {
            // open most recent entry added to files to be deleted from cache
            File cacheFile = new File(files.get(files.size() - 1).getPath());
            if (cacheFile.exists()) {
              FileUtils.openFile(cacheFile, mainActivity, mainActivity.getPrefs());
            }
            // reset the flag and cache file, as it's root is already in the list for deletion
            isOpen = false;
            files.remove(files.size() - 1);
          }
        }
      };

  @Override
  public void changePath(String folder) {
    if (folder == null) folder = "";
    if (folder.startsWith("/")) folder = folder.substring(1);

    boolean addGoBackItem = gobackitem && !isRoot(folder);
    String finalfolder = folder;
    if (decompressor != null) {
      decompressor
          .changePath(
              folder,
              addGoBackItem,
              result -> {
                if (result.exception == null) {
                  elements = result.result;
                  createViews(elements, finalfolder);
                  swipeRefreshLayout.setRefreshing(false);
                  updateBottomBar();
                } else {
                  archiveCorruptOrUnsupportedToast(result.exception);
                }
              })
          .execute();
      swipeRefreshLayout.setRefreshing(true);
      updateBottomBar();
    } else {
      archiveCorruptOrUnsupportedToast(null);
    }
  }

  @Override
  public String getPath() {
    if (!isRootRelativePath()) return SEPARATOR + relativeDirectory;
    else return "";
  }

  @Override
  public int getRootDrawable() {
    return R.drawable.ic_compressed_white_24dp;
  }

  private void refresh() {
    changePath(relativeDirectory);
  }

  private void updateBottomBar() {
    String path =
        !isRootRelativePath()
            ? compressedFile.getName() + SEPARATOR + relativeDirectory
            : compressedFile.getName();
    mainActivity
        .getAppbar()
        .getBottomBar()
        .updatePath(path, false, null, OpenMode.FILE, folder, file, this);
  }

  private void createViews(List<CompressedObjectParcelable> items, String dir) {
    if (compressedExplorerAdapter == null) {
      compressedExplorerAdapter =
          new CompressedExplorerAdapter(
              getActivity(),
              utilsProvider,
              items,
              this,
              decompressor,
              PreferenceManager.getDefaultSharedPreferences(getActivity()));
      listView.setAdapter(compressedExplorerAdapter);
    } else {
      compressedExplorerAdapter.generateZip(items);
    }

    folder = 0;
    file = 0;
    for (CompressedObjectParcelable item : items) {
      if (item.type == CompressedObjectParcelable.TYPE_GOBACK) continue;

      if (item.directory) folder++;
      else file++;
    }

    stopAnims = true;
    if (!addheader) {
      listView.removeItemDecoration(dividerItemDecoration);
      // listView.removeItemDecoration(headersDecor);
      addheader = true;
    } else {
      dividerItemDecoration = new DividerItemDecoration(getActivity(), true, showDividers);
      listView.addItemDecoration(dividerItemDecoration);
      // headersDecor = new StickyRecyclerHeadersDecoration(compressedExplorerAdapter);
      // listView.addItemDecoration(headersDecor);
      addheader = false;
    }
    fastScroller = rootView.findViewById(R.id.fastscroll);
    fastScroller.setRecyclerView(listView, 1);
    fastScroller.setPressedHandleColor(mainActivity.getAccent());
    ((AppBarLayout) mToolbarContainer).addOnOffsetChangedListener(offsetListenerForToolbar);
    listView.stopScroll();
    relativeDirectory = dir;
    updateBottomBar();
    swipeRefreshLayout.setRefreshing(false);
  }

  public boolean canGoBack() {
    return !isRootRelativePath();
  }

  public void goBack() {
    changePath(new File(relativeDirectory).getParent());
  }

  private boolean isRootRelativePath() {
    return isRoot(relativeDirectory);
  }

  private boolean isRoot(String folder) {
    return folder == null || folder.isEmpty();
  }

  private void archiveCorruptOrUnsupportedToast(@Nullable Throwable e) {
    @StringRes
    int msg =
        (e != null
                && e.getCause() != null
                && UnsupportedRarV5Exception.class.isAssignableFrom(e.getCause().getClass()))
            ? R.string.error_unsupported_v5_rar
            : R.string.archive_unsupported_or_corrupt;
    Toast.makeText(
            getActivity(),
            getActivity().getString(msg, compressedFile.getAbsolutePath()),
            Toast.LENGTH_LONG)
        .show();
    getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
  }
}
