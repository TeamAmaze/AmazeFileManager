package com.amaze.filemanager.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;

import java.util.ArrayList;

/**
 * Created by vishal on 26/2/16.
 */
public class AsyncHelper extends Fragment {

    private HelperCallbacks mCallbacks;
    private String mPath, mInput;
    public SearchTask mSearchTask;
    private int mOpenMode;
    private boolean mRootMode;

    private static final String KEY_PATH = "path";
    private static final String KEY_INPUT = "input";
    private static final String KEY_OPEN_MODE = "open_mode";
    private static final String KEY_ROOT_MODE = "root_mode";

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

    class SearchTask extends AsyncTask<String, BaseFile, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            /*
            * Note that we need to check if the callbacks are null in each
            * method in case they are invoked after the Activity's and
            * Fragment's onDestroy() method have been called.
             */
            if (mCallbacks!=null) {

                mCallbacks.onPreExecute();
            }
        }

        // mcallbacks not checked for null because of possibility of
        // race conditions b/t worker thread main thread
        @Override
        protected Void doInBackground(String... params) {

            String path = params[0];
            HFile file=new HFile(mOpenMode, path);
            file.generateMode(getActivity());
            if(file.isSmb())return null;
            search(file, mInput);
            return null;
        }

        @Override
        public void onPostExecute(Void c){
            if (mCallbacks!=null) {
                mCallbacks.onPostExecute();
            }
        }

        @Override
        public void onProgressUpdate(BaseFile... val) {
            if (!isCancelled() && mCallbacks!=null) {
                mCallbacks.onProgressUpdate(val[0]);
            }
        }

        public void search(HFile file, String text) {

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
                        }
                    }
            } else {
                System.out
                        .println(file.getPath() + "Permission Denied");
            }
        }
    }
}
