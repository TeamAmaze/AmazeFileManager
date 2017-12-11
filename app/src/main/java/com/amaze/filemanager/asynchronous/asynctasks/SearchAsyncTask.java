package com.amaze.filemanager.asynchronous.asynctasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.SearchWorkerFragment;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OpenMode;

import java.lang.ref.WeakReference;
import java.util.regex.Pattern;

/**
 * @author Emmanuel
 *         on 15/8/2017, at 19:47.
 */

public class SearchAsyncTask extends AsyncTask<String, HybridFileParcelable, Void> {

    private static final String TAG = "SearchAsyncTask";

    private WeakReference<Activity> activity;
    private SearchWorkerFragment.HelperCallbacks mCallbacks;
    private String mInput;
    private OpenMode mOpenMode;
    private boolean mRootMode, isRegexEnabled, isMatchesEnabled;

    public SearchAsyncTask(Activity a, SearchWorkerFragment.HelperCallbacks l,
                           String input, OpenMode openMode, boolean root, boolean regex,
                           boolean matches) {
        activity = new WeakReference<>(a);
        mCallbacks = l;
        mInput = input;
        mOpenMode = openMode;
        mRootMode = root;
        isRegexEnabled = regex;
        isMatchesEnabled = matches;
    }

    @Override
    protected void onPreExecute() {
            /*
            * Note that we need to check if the callbacks are null in each
            * method in case they are invoked after the Activity's and
            * Fragment's onDestroy() method have been called.
             */
        if (mCallbacks != null) {
            mCallbacks.onPreExecute(mInput);
        }
    }

    // mCallbacks not checked for null because of possibility of
    // race conditions b/w worker thread main thread
    @Override
    protected Void doInBackground(String... params) {

        String path = params[0];
        HybridFile file = new HybridFile(mOpenMode, path);
        file.generateMode(activity.get());
        if (file.isSmb()) return null;

        // level 1
        // if regex or not
        if (!isRegexEnabled) {
            search(file, mInput);
        } else {
            // compile the regular expression in the input
            Pattern pattern = Pattern.compile(bashRegexToJava(mInput));
            // level 2
            if (!isMatchesEnabled) searchRegExFind(file, pattern);
            else searchRegExMatch(file, pattern);
        }
        return null;
    }

    @Override
    public void onPostExecute(Void c) {
        if (mCallbacks != null) {
            mCallbacks.onPostExecute(mInput);
        }
    }

    @Override
    protected void onCancelled() {
        if (mCallbacks != null) mCallbacks.onCancelled();
    }

    @Override
    public void onProgressUpdate(HybridFileParcelable... val) {
        if (!isCancelled() && mCallbacks != null) {
            mCallbacks.onProgressUpdate(val[0], mInput);
        }
    }

    /**
     * Recursively search for occurrences of a given text in file names and publish the result
     *
     * @param directory the current path
     */
    private void search(HybridFile directory, final SearchFilter filter) {
        if (directory.isDirectory(activity.get())) {// do you have permission to read this directory?
            directory.forEachChildrenFile(activity.get(), mRootMode, new OnFileFound() {
                @Override
                public void onFileFound(HybridFileParcelable file) {
                    if (!isCancelled()) {
                        if (filter.searchFilter(file.getName())) {
                            publishProgress(file);
                        }
                        if (file.isDirectory() && !isCancelled()) {
                            search(file, filter);
                        }
                    }
                }
            });
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
    private void search(HybridFile file, final String query) {
        search(file, fileName -> fileName.toLowerCase().contains(query.toLowerCase()));
    }

    /**
     * Recursively find a java regex pattern {@link Pattern} in the file names and publish the result
     *
     * @param file    the current file
     * @param pattern the compiled java regex
     */
    private void searchRegExFind(HybridFile file, final Pattern pattern) {
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
    private void searchRegExMatch(HybridFile file, final Pattern pattern) {
        search(file, fileName -> pattern.matcher(fileName).matches());
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

}
