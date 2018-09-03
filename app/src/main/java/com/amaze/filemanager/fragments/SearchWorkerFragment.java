package com.amaze.filemanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.SearchAsyncTask;
import com.amaze.filemanager.utils.OpenMode;

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

    private HelperCallbacks mCallbacks;

    // interface for activity to communicate with asynctask
    public interface HelperCallbacks {
        void onPreExecute(String query);
        void onPostExecute(String query);
        void onProgressUpdate(HybridFileParcelable val, String query);
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
        String mPath = getArguments().getString(KEY_PATH);
        String mInput = getArguments().getString(KEY_INPUT);
        OpenMode mOpenMode = OpenMode.getOpenMode(getArguments().getInt(KEY_OPEN_MODE));
        boolean mRootMode = getArguments().getBoolean(KEY_ROOT_MODE);
        boolean isRegexEnabled = getArguments().getBoolean(KEY_REGEX);
        boolean isMatchesEnabled = getArguments().getBoolean(KEY_REGEX_MATCHES);

        mSearchAsyncTask = new SearchAsyncTask(getActivity(), mCallbacks, mInput, mOpenMode,
                mRootMode, isRegexEnabled, isMatchesEnabled);
        mSearchAsyncTask.execute(mPath);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // to avoid activity instance leak while changing activity configurations
        mCallbacks = null;
    }

}
