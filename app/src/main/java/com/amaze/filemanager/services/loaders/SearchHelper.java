package com.amaze.filemanager.services.loaders;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.OpenMode;

import java.lang.ref.WeakReference;

/**
 * @author Emmanuel
 *         on 30/7/2017, at 12:47.
 */

public class SearchHelper {
    private static final int SEARCH_LOADER = 50600;

    public static void createSearch(final MainActivity mainActivity, final String query,
                                    final String directoryToSearchPath, final OpenMode openMode,
                                    final boolean root, final boolean regex, final boolean matches,
                                    boolean restart) {

        LoaderManager loaderManager = mainActivity.getSupportLoaderManager();

        SearchCallbacks callbacks = new SearchCallbacks(mainActivity, query, directoryToSearchPath,
                openMode, root, regex, matches);


        if(restart) {
            loaderManager.initLoader(SEARCH_LOADER, null, callbacks);
        } else if(loaderManager.getLoader(SEARCH_LOADER) == null) {
            loaderManager.initLoader(SEARCH_LOADER, null, callbacks).forceLoad();
        } else {
            loaderManager.restartLoader(SEARCH_LOADER, null, callbacks).forceLoad();
        }
    }

    public static void cancelSearch(MainActivity mainActivity) {
        mainActivity.getSupportLoaderManager().destroyLoader(SEARCH_LOADER);
    }

    private static class SearchCallbacks extends Handler implements LoaderManager.LoaderCallbacks<Void>,
            SearchLoader.OnCancelledListener {
        private WeakReference<MainActivity> mainActivity;
        private String query;
        private String directoryToSearchPath;
        private OpenMode openMode;
        private boolean root, regex, matches;

        private SearchCallbacks(MainActivity activity, String query, String directoryToSearchPath,
                                OpenMode openMode, boolean root, boolean regex, boolean matches) {
            super(Looper.getMainLooper());
            mainActivity = new WeakReference<>(activity);
            this.query = query;
            this.directoryToSearchPath = directoryToSearchPath;
            this.openMode = openMode;
            this.root = root;
            this.regex = regex;
            this.matches = matches;
        }

        @Override
        public Loader<Void> onCreateLoader(int id, Bundle args) {
            MainFragment mainFragment = mainActivity.get().mainFragment;

            mainFragment.mSwipeRefreshLayout.setRefreshing(true);
            mainFragment.onSearchPreExecute(query);

            SearchLoader searchLoader = new SearchLoader(mainActivity.get(), this, this);
            searchLoader.loadParameters(query, directoryToSearchPath, openMode, root, regex, matches);

            return searchLoader;
        }

        @Override
        public void handleMessage(Message inputMessage) {
            final Pair<String, BaseFile> queryToResult = (Pair<String, BaseFile>) inputMessage.obj;

            mainActivity.get().runOnUiThread(new Runnable() {
                public void run() {
                    mainActivity.get().mainFragment.addSearchResult(queryToResult.second, queryToResult.first);
                }
            });
        }

        @Override
        public void onCancelled() {
            mainActivity.get().runOnUiThread(new Runnable() {
                public void run() {
                    MainFragment mainFragment = mainActivity.get().mainFragment;
                    mainFragment.createViews(mainFragment.getLayoutElements(), false,
                            mainFragment.getCurrentPath(), mainFragment.openMode, false,
                            !mainFragment.IS_LIST);
                    mainFragment.mSwipeRefreshLayout.setRefreshing(false);
                }
            });

        }

        @Override
        public void onLoadFinished(Loader<Void> loader, Void data) {
            mainActivity.get().mainFragment.onSearchCompleted(query);
            mainActivity.get().mainFragment.mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onLoaderReset(Loader<Void> loader) {
            ((SearchLoader) loader).loadParameters(query, directoryToSearchPath, openMode,
                    root, regex, matches);
        }
    }

}
