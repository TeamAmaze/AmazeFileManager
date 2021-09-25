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

package com.amaze.filemanager.filesystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.asynctasks.PrepareCopyTask;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import androidx.annotation.Nullable;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Special immutable class for handling cut/copy operations.
 *
 * @author Emmanuel on 5/9/2017, at 09:59.
 */
public final class PasteHelper implements Parcelable {

  public static final int OPERATION_COPY = 0, OPERATION_CUT = 1;

  private final int operation;
  private final HybridFileParcelable[] paths;
  private Snackbar snackbar;
  @Nullable private MainActivity mainActivity;

  public PasteHelper(@Nullable MainActivity mainActivity, int op, HybridFileParcelable[] paths) {
    if (paths == null || paths.length == 0) throw new IllegalArgumentException();
    operation = op;
    this.paths = paths;
    this.mainActivity = mainActivity;
    showSnackbar();
  }

  private PasteHelper(Parcel in) {
    operation = in.readInt();
    paths = in.createTypedArray(HybridFileParcelable.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(operation);
    dest.writeTypedArray(paths, 0);
  }

  public static final Parcelable.Creator CREATOR =
      new Parcelable.Creator() {
        public PasteHelper createFromParcel(Parcel in) {
          return new PasteHelper(in);
        }

        public PasteHelper[] newArray(int size) {
          return new PasteHelper[size];
        }
      };

  public int getOperation() {
    return this.operation;
  }

  public HybridFileParcelable[] getPaths() {
    return paths;
  }

  /**
   * Invalidates the snackbar after fragment changes / reapply config changes. Keeping the contents
   * to copy/move intact
   *
   * @param mainActivity main activity
   * @param showSnackbar whether to show snackbar or hide
   */
  public void invalidateSnackbar(MainActivity mainActivity, boolean showSnackbar) {
    this.mainActivity = mainActivity;
    if (showSnackbar) {
      showSnackbar();
    } else {
      dismissSnackbar(false);
    }
  }

  public Snackbar getSnackbar() {
    return snackbar;
  }

  /**
   * Dismisses snackbar and fab
   *
   * @param shouldClearPasteData should the paste data be cleared
   */
  private void dismissSnackbar(boolean shouldClearPasteData) {
    if (snackbar != null) {
      snackbar.dismiss();
      snackbar = null;
    }
    if (shouldClearPasteData) {
      mainActivity.setPaste(null);
    }
  }

  private void showSnackbar() {
    Single.fromCallable(() -> getSnackbarContent())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new SingleObserver<Spanned>() {
              @Override
              public void onSubscribe(Disposable d) {}

              @Override
              public void onSuccess(Spanned spanned) {
                snackbar =
                    Utils.showCutCopySnackBar(
                        mainActivity,
                        spanned,
                        BaseTransientBottomBar.LENGTH_INDEFINITE,
                        R.string.paste,
                        () -> {
                          final MainFragment mainFragment =
                              Objects.requireNonNull(mainActivity.getCurrentMainFragment());
                          String path = mainFragment.getCurrentPath();
                          ArrayList<HybridFileParcelable> arrayList =
                              new ArrayList<>(Arrays.asList(paths));
                          boolean move = operation == PasteHelper.OPERATION_CUT;
                          new PrepareCopyTask(
                                  path,
                                  move,
                                  mainActivity,
                                  mainActivity.isRootExplorer(),
                                  mainFragment.getMainFragmentViewModel().getOpenMode())
                              .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, arrayList);
                          dismissSnackbar(true);
                        },
                        () -> dismissSnackbar(true));
              }

              @Override
              public void onError(Throwable e) {
                Log.e(
                    getClass().getSimpleName(),
                    "Failed to show paste snackbar due to " + e.getCause());
                e.printStackTrace();
              }
            });
  }

  private Spanned getSnackbarContent() {
    String operationText = "<b>%s</b>";
    operationText =
        String.format(
            operationText,
            operation == OPERATION_COPY
                ? mainActivity.getString(R.string.copy)
                : mainActivity.getString(R.string.move));
    operationText = operationText.concat(": ");
    int foldersCount = 0;
    int filesCount = 0;
    for (HybridFileParcelable fileParcelable : paths) {
      if (fileParcelable.isDirectory(mainActivity.getApplicationContext())) {
        foldersCount++;
      } else {
        filesCount++;
      }
    }
    operationText =
        operationText.concat(
            mainActivity.getString(R.string.folderfilecount, foldersCount, filesCount));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return Html.fromHtml(operationText, Html.FROM_HTML_MODE_COMPACT);
    } else {
      return Html.fromHtml(operationText);
    }
  }
}
