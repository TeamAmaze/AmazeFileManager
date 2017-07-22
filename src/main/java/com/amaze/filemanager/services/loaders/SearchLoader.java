package com.amaze.filemanager.services.loaders;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.util.Pair;
import android.util.Log;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.OpenMode;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Loader that checks for coincidences recursively
 *
 * @author Emmanuel
 *         on 19/7/2017, at 22:59.
 */

public class SearchLoader extends AsyncTaskLoader<Void> {

    private static final String TAG = "AmazeFileManager.Search";
    private String query, directory;
    private OpenMode openMode;
    private boolean isRoot, isRegexEnabled, isMatchesEnabled;
    private OnCancelledListener cancelledListener;

    /**
     * this Observable serves to pass items to the mother Activity
     */
    private Handler fileHandler;

    public SearchLoader(Context c, OnCancelledListener l, Handler handler) {
        super(c);

        cancelledListener = l;
        fileHandler = handler;
    }

    /**
     * My knowledge is not enough to make this better, this method serves to load all the variables
     * necessary to start the loadInBackground().
     */
    public void loadParameters(String query, String directoryToSearchPath, OpenMode openMode,
                               boolean root, boolean regex, boolean matches) {
        this.query = query;
        directory = directoryToSearchPath;
        this.openMode = openMode;
        isRoot = root;
        isRegexEnabled = regex;
        isMatchesEnabled = matches;
    }

    @Override
    public Void loadInBackground() {
        HFile file = new HFile(openMode, directory);
        file.generateMode(getContext());
        if (file.isSmb()) return null;

        if (!isRegexEnabled) {
            search(file, query);
        } else {
            Pattern pattern = Pattern.compile(bashRegexToJava(query));
            if (!isMatchesEnabled) searchRegExFind(file, pattern);
            else searchRegExMatch(file, pattern);
        }
        return null;
    }

    @Override
    protected boolean onCancelLoad() {
        cancelledListener.onCancelled();
        return true;
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
    }

    /**
     * Recursively search for occurrences of a given text in file names and publish the result
     *
     * @param directory the current path
     */
    private void search(HFile directory, SearchFilter filter) {
        if (directory.isDirectory(getContext())) {// do you have permission to read this directory?
            ArrayList<BaseFile> filesInDirectory = directory.listFiles(getContext(), isRoot);
            for (BaseFile file : filesInDirectory) {
                if (!isLoadInBackgroundCanceled()) {
                    if (filter.searchFilter(file.getName())) {
                        Pair<String, BaseFile> queryToResult = new Pair<>(query, file);

                        Message m = fileHandler.obtainMessage(0, queryToResult);
                        fileHandler.dispatchMessage(m);
                    }
                    if (file.isDirectory() && !isLoadInBackgroundCanceled()) {
                        search(file, filter);
                    }
                } else return;
            }
        } else {
            Log.d(TAG, "Cannot search " + directory.getPath() + ": Permission Denied");
        }
    }


    /**
     * Recursively search for occurrences of a given text in file names and publish the result
     *
     * @param file  the current path
     * @param query the searched text
     */
    private void search(HFile file, final String query) {
        search(file, new SearchFilter() {
            @Override
            public boolean searchFilter(String fileName) {
                return fileName.toLowerCase().contains(query.toLowerCase());
            }
        });
    }

    /**
     * Recursively find a java regex pattern {@link Pattern} in the file names and publish the result
     *
     * @param file    the current file
     * @param pattern the compiled java regex
     */
    private void searchRegExFind(HFile file, final Pattern pattern) {
        search(file, new SearchFilter() {
            @Override
            public boolean searchFilter(String fileName) {
                return pattern.matcher(fileName).find();
            }
        });
    }

    /**
     * Recursively match a java regex pattern {@link Pattern} with the file names and publish the result
     *
     * @param file    the current file
     * @param pattern the compiled java regex
     */
    private void searchRegExMatch(HFile file, final Pattern pattern) {
        search(file, new SearchFilter() {
            @Override
            public boolean searchFilter(String fileName) {
                return pattern.matcher(fileName).matches();
            }
        });
    }

    /**
     * method converts bash style regular expression to java. See {@link Pattern}
     *
     * @param originalString
     * @return converted string
     */
    private String bashRegexToJava(String originalString) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < originalString.length(); i++) {
            switch (originalString.charAt(i) + "") {
                case "*":
                    stringBuilder.append("\\w*");
                    break;
                case "?":
                    stringBuilder.append("\\w");
                    break;
                default:
                    stringBuilder.append(originalString.charAt(i));
                    break;
            }
        }

        Log.d(getClass().getSimpleName(), stringBuilder.toString());
        return stringBuilder.toString();
    }

    public interface SearchFilter {
        boolean searchFilter(String fileName);
    }

    public interface OnCancelledListener {
        void onCancelled();
    }

}