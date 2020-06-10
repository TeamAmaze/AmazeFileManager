package com.amaze.filemanager.activities.superclasses;

import android.os.AsyncTask;

public interface BaseAsyncTask {

  /**
   * Safely fetch an object for null safety, otherwise interrupt the task. Task should implement {@link AsyncTask#onCancelled()}
   * @param t given object to return
   * @param asyncTask task implementing this interface
   * @param <T> data type, preferably activities / fragments / contexts which can be null at any given point of time while task is running
   * @return T t
   */
  default <T> T nullCheckOrInterrupt(T t, AsyncTask asyncTask) {
    if (t != null) {
      return t;
    } else {
      asyncTask.cancel(true);
      return null;
    }
  }
}
