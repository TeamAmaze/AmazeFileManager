package com.amaze.filemanager.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.OpenMode;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Worker fragment designed to not be destroyed when the activity holding it is recreated
 * (aka the state changes like screen rotation) thus maintaining alive an AsyncTask (SearchTask in this case)
 *
 * Created by vishal on 26/2/16 edited by EmmanuelMess.
 */
public class SearchWorkerFragment extends Fragment {

    public static final String KEY_PATH = "path";
    public static final String KEY_INPUT = "input";
    public static final String KEY_OPEN_MODE = "open_mode";
    public static final String KEY_ROOT_MODE = "root_mode";
    public static final String KEY_REGEX = "regex";
    public static final String KEY_REGEX_MATCHES = "matches";

    public SearchAsyncTask mSearchAsyncTask;

    private static final String TAG = "SearchWorkerFragment";

    private HelperCallbacks mCallbacks;
    private String mPath, mInput;
    private OpenMode mOpenMode;
    private boolean mRootMode, isRegexEnabled, isMatchesEnabled;

    // interface for activity to communicate with asynctask
    public interface HelperCallbacks {
        void onPreExecute(String query);
        void onPostExecute(String query);
        void onProgressUpdate(BaseFile val,String query);
        void onCancelled();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // hold instance of activity as there is a change in device configuration
        mCallbacks = (HelperCallbacks) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        mPath = getArguments().getString(KEY_PATH);
        mInput = getArguments().getString(KEY_INPUT);
        mOpenMode = OpenMode.getOpenMode(getArguments().getInt(KEY_OPEN_MODE));
        mRootMode = getArguments().getBoolean(KEY_ROOT_MODE);
        isRegexEnabled = getArguments().getBoolean(KEY_REGEX);
        isMatchesEnabled = getArguments().getBoolean(KEY_REGEX_MATCHES);
        mSearchAsyncTask = new SearchAsyncTask();
        mSearchAsyncTask.execute(mPath);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // to avoid activity instance leak while changing activity configurations
        mCallbacks = null;
    }

    public class SearchAsyncTask extends AsyncTask<String, BaseFile, Void> {
        @Override
        protected void onPreExecute() {
            /*
            * Note that we need to check if the callbacks are null in each
            * method in case they are invoked after the Activity's and
            * Fragment's onDestroy() method have been called.
             */
            if (mCallbacks!=null) {
                mCallbacks.onPreExecute(mInput);
            }
        }

        // mCallbacks not checked for null because of possibility of
        // race conditions b/w worker thread main thread
        @Override
        protected Void doInBackground(String... params) {

            String path = params[0];
            HFile file=new HFile(mOpenMode, path);
            file.generateMode(getActivity());
            if(file.isSmb())return null;

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
        public void onPostExecute(Void c){
            if (mCallbacks!=null) {
                mCallbacks.onPostExecute(mInput);
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks!=null) mCallbacks.onCancelled();
        }

        @Override
        public void onProgressUpdate(BaseFile... val) {
            if (!isCancelled() && mCallbacks!=null) {
                mCallbacks.onProgressUpdate(val[0],mInput);
            }
        }

        /**
         * Recursively search for occurrences of a given text in file names and publish the result
         * @param directory the current path
         */
        private void search(HFile directory, SearchFilter filter) {
            if (directory.isDirectory(getContext())) {// do you have permission to read this directory?
                ArrayList<BaseFile> filesInDirectory = directory.listFiles(getContext(), mRootMode);
                for (BaseFile file : filesInDirectory) {
                    if (!isCancelled()) {
                        if (filter.searchFilter(file.getName())) {
                            publishProgress(file);
                        }
                        if (file.isDirectory() && !isCancelled()) {
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
         * @param file the current path
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
         * @param file the current file
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
         * @param file the current file
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
         * @param originalString
         * @return converted string
         */
        private String bashRegexToJava(String originalString) {
            StringBuilder stringBuilder = new StringBuilder();

            for(int i=0; i<originalString.length(); i++) {
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
    }

    public interface SearchFilter {
        boolean searchFilter(String fileName);
    }
}
