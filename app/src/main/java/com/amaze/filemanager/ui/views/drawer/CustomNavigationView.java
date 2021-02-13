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

package com.amaze.filemanager.ui.views.drawer;

import com.google.android.material.navigation.NavigationView;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** This class if for intercepting item selections so that they can be saved and restored. */
public class CustomNavigationView extends NavigationView
    implements NavigationView.OnNavigationItemSelectedListener {

  private OnNavigationItemSelectedListener subclassListener;
  private int checkedId = -1;

  public CustomNavigationView(Context context, AttributeSet attrs) {
    super(context, attrs);

    super.setNavigationItemSelectedListener(this);
  }

  @Override
  public void setNavigationItemSelectedListener(
      @Nullable OnNavigationItemSelectedListener listener) {
    subclassListener = listener;
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    if (subclassListener != null) {
      boolean shouldBeSelected = subclassListener.onNavigationItemSelected(item);

      if (shouldBeSelected) {
        onItemChecked(item);
      }

      return shouldBeSelected;
    } else {
      onItemChecked(item);
      return true;
    }
  }

  private void onItemChecked(MenuItem item) {
    checkedId = item.getItemId();
  }

  public void setCheckedItem(MenuItem item) {
    this.checkedId = item.getItemId();
    item.setChecked(true);
  }

  public void deselectItems() {
    checkedId = -1;
  }

  public @Nullable MenuItem getSelected() {
    if (checkedId == -1) return null;
    return getMenu().findItem(checkedId);
  }

  @Override
  public Parcelable onSaveInstanceState() {
    if (isNavigationViewSavedStateMissing()) {
      return super.onSaveInstanceState();
    }

    // begin boilerplate code that allows parent classes to save state
    Parcelable superState = super.onSaveInstanceState();

    SavedState ss = new SavedState(superState);
    // end

    ss.selectedId = this.checkedId;

    return ss;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    if (isNavigationViewSavedStateMissing()) {
      super.onRestoreInstanceState(state);
      return;
    }

    // begin boilerplate code so parent classes can restore state
    if (!(state instanceof SavedState)) {
      super.onRestoreInstanceState(state);
      return;
    }

    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    // end

    this.checkedId = ss.selectedId;
  }

  /**
   * This is a hack, when the SavedState class is unmarshalled a "ClassNotFoundException" will be
   * thrown (the actual class not found is
   * "android.support.design.widget.NavigationView$SavedState") and I seem to only be able to
   * replicate on Marshmallow (someone else replicated in N through O_MR1 see
   * https://github.com/TeamAmaze/AmazeFileManager/issues/1400#issuecomment-413086603). Trying to
   * find the class and returning false if Class.forName() throws "ClassNotFoundException" doesn't
   * work because the class seems to have been loaded with the current loader (not the one the
   * unmarshaller uses); of course I have no idea of what any of this means so I could be wrong. For
   * the crash see https://github.com/TeamAmaze/AmazeFileManager/issues/1101.
   */
  public boolean isNavigationViewSavedStateMissing() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
  }

  static class SavedState extends BaseSavedState {
    int selectedId;

    SavedState(Parcelable superState) {
      super(superState);
    }

    private SavedState(Parcel in) {
      super(in);
      this.selectedId = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeInt(this.selectedId);
    }

    // required field that makes Parcelables from a Parcel
    public static final Parcelable.Creator<SavedState> CREATOR =
        new Parcelable.Creator<SavedState>() {
          public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
          }

          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }
}
