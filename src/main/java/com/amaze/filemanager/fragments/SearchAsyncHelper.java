package com.amaze.filemanager.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by vishal on 26/2/16.
 */
public class SearchAsyncHelper extends Fragment {

    private HelperCallbacks mCallbacks;
    private String mPath, mInput;
    public SearchTask mSearchTask;
    private int mOpenMode;
    private boolean mRootMode;

    private static final String KEY_PATH = "path";
    private static final String KEY_INPUT = "input";
    private static final String KEY_OPEN_MODE = "open_mode";
    private static final String KEY_ROOT_MODE = "root_mode";

    private static final String WILDCARD_ANY = "*";
    private static final String WILDCARD_ANY_SINGLE = "?";
    private static final String WILDCARD_SQUARE_OPEN = "[";
    private static final String WILDCARD_SQUARE_CLOSE = "]";
    private static final String WILDCARD_NOT = "!";

    private static final String WILDCARD_PATTERN_ALPHANUMERIC = "[:alnum:]";
    private static final String WILDCARD_PATTERN_ALPHABETIC_ALL = "[:alpha:]";
    private static final String WILDCARD_PATTERN_NUMERALS = "[:digit:]";
    private static final String WILDCARD_PATTERN_ALPHABETIC_UPPER = "[:upper:]";
    private static final String WILDCARD_PATTERN_ALPHABETIC_LOWER = "[:lower:]";

    private static final String ESCAPE_JAVA_REGEX_PERIOD = ".";
    private static final String ESCAPE_JAVA_REGEX_PLUS = "+";

    // interface for activity to communicate with asynctask
    public interface HelperCallbacks {
        void onPreExecute();
        void onPostExecute();
        void onProgressUpdate(BaseFile val);
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
        mOpenMode = getArguments().getInt(KEY_OPEN_MODE);
        mRootMode = getArguments().getBoolean(KEY_ROOT_MODE);
        mSearchTask = new SearchTask();
        mSearchTask.execute(mPath);

    }

    @Override
    public void onDetach() {
        super.onDetach();

        // to avoid activity instance leak while changing activity configurations
        mCallbacks = null;
    }

    public class SearchTask extends AsyncTask<String, BaseFile, Void> {

        @Override
        protected void onPreExecute() {

            /*
            * Note that we need to check if the callbacks are null in each
            * method in case they are invoked after the Activity's and
            * Fragment's onDestroy() method have been called.
             */
            if (mCallbacks!=null) {

                mCallbacks.onPreExecute();
            }
        }

        // mCallbacks not checked for null because of possibility of
        // race conditions b/t worker thread main thread
        @Override
        protected Void doInBackground(String... params) {

            String path = params[0];
            HFile file=new HFile(mOpenMode, path);
            file.generateMode(getActivity());
            if(file.isSmb())return null;
            //search(file, mInput);

            // compile the regular expression in the input
            Pattern pattern = Pattern.compile(javaRegexToGrep(mInput));
            searchRegEx(file, pattern);
            return null;
        }

        @Override
        public void onPostExecute(Void c){
            if (mCallbacks!=null) {
                mCallbacks.onPostExecute();
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks!=null) mCallbacks.onCancelled();
        }

        @Override
        public void onProgressUpdate(BaseFile... val) {
            if (!isCancelled() && mCallbacks!=null) {
                mCallbacks.onProgressUpdate(val[0]);
            }
        }

        /**
         * Search for occurrences of a given text in file names
         * @param file the current path
         * @param text the searched text
         */
        private void search(HFile file, String text) {

            if (file.isDirectory()) {
                ArrayList<BaseFile> f = file.listFiles(mRootMode);
                // do you have permission to read this directory?
                if (!isCancelled())
                    for (BaseFile x : f) {
                        if (!isCancelled()) {
                            if (x.isDirectory()) {
                                if (x.getName().toLowerCase()
                                        .contains(text.toLowerCase())) {
                                    publishProgress(x);
                                }
                                if (!isCancelled()) search(x, text);

                            } else {
                                if (x.getName().toLowerCase()
                                        .contains(text.toLowerCase())) {
                                    publishProgress(x);
                                }
                            }
                        } else return;
                    }
                else return;
            } else {
                System.out
                        .println(file.getPath() + "Permission Denied");
            }
        }
        private void searchRegEx(HFile file, Pattern pattern) {

            if (file.isDirectory()) {
                ArrayList<BaseFile> f = file.listFiles(mRootMode);

                if (!isCancelled())
                    for (BaseFile x : f) {
                        if (!isCancelled()) {
                            if (x.isDirectory()) {
                                if (pattern.matcher(x.getName()).matches()) publishProgress(x);
                                if (!isCancelled()) searchRegEx(x, pattern);

                            } else {
                                if (pattern.matcher(x.getName()).matches()) {
                                    publishProgress(x);
                                }
                            }
                        } else return;
                    }
                else return;
            } else {
                System.out
                        .println(file.getPath() + "Permission Denied");
            }
        }

        private String javaRegexToGrep(String originalString) {
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

            System.out.println(stringBuilder.toString());
            return stringBuilder.toString();
        }
    }
}
