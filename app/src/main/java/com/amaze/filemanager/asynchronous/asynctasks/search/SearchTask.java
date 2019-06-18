package com.amaze.filemanager.asynchronous.asynctasks.search;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.amaze.filemanager.asynchronous.asynctasks.SearchAsyncTask;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Consumer;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SearchTask extends RecursiveTask<List<HybridFileParcelable>> {

    private HybridFile hybridFile;
    private SearchAsyncTask.SearchFilter searchFilter;
    private WeakReference<Activity> context;
    private boolean mRootMode;
    private SearchAsyncTask searchAsyncTask;

    public SearchTask(WeakReference<Activity> context, boolean rootMode, HybridFile hybridFile,
                      SearchAsyncTask.SearchFilter searchFilter, SearchAsyncTask searchAsyncTask) {
        this.context = context;
        this.mRootMode = rootMode;
        this.hybridFile = hybridFile;
        this.searchAsyncTask = searchAsyncTask;
        this.searchFilter = searchFilter;
    }

    @Override
    protected List<HybridFileParcelable> compute() {
        List<HybridFileParcelable> hybridFileParcelables = new ArrayList<>();
        //List<SearchTask> tasks = new ArrayList<>();

        if (hybridFile.isDirectory(context.get())) {// do you have permission to read this directory?
            hybridFile.forEachChildrenFile(context.get(), mRootMode, file -> {
                if (searchAsyncTask!= null && !searchAsyncTask.isCancelled()) {
                    if (searchFilter.searchFilter(file.getName())) {
                        hybridFileParcelables.add(file);
                    }
                    if (file.isDirectory() && !isCancelled()) {
                        SearchTask searchTask = new SearchTask(context, mRootMode, file,
                                searchFilter, searchAsyncTask);
                        searchTask.fork();
                        //tasks.add(searchTask);
                    }
                }
            });
        } else {
            Log.d(getClass().getSimpleName(), "Cannot search " + hybridFile.getPath() + ": Permission Denied");
        }

        try {
            synchronized (this) {

                publishResults(hybridFileParcelables);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
            this.cancel(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
            this.cancel(true);
        }
        return hybridFileParcelables;
    }

    private void publishResults(List<HybridFileParcelable> hybridFileParcelables)
            throws ExecutionException, InterruptedException {

        if (hybridFileParcelables.size() > 0) {
            context.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (searchAsyncTask != null && !searchAsyncTask.isCancelled())
                        searchAsyncTask.onProgressUpdate(hybridFileParcelables.toArray(new
                                HybridFileParcelable[hybridFileParcelables.size()]));
                }
            });
        }
    }
}