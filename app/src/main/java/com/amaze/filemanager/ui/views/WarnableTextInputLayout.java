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

package com.amaze.filemanager.ui.views;

import com.amaze.filemanager.R;
import com.google.android.material.textfield.TextInputLayout;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 31/1/2018, at 14:50. */
public class WarnableTextInputLayout extends TextInputLayout {

  private boolean isStyleWarning = false;

  public WarnableTextInputLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /** Remove error or warning */
  public void removeError() {
    super.setError(null);
    setErrorEnabled(false);
  }

  @Override
  public void setError(@Nullable CharSequence error) {
    if (isStyleWarning) {
      setErrorEnabled(true);
      setErrorTextAppearance(R.style.error_inputTextLayout);
      isStyleWarning = false;
    }
    super.setError(error);
  }

  public void setWarning(@StringRes int text) {
    if (!isStyleWarning) {
      removeError();
      setErrorEnabled(true);
      setErrorTextAppearance(R.style.warning_inputTextLayout);
      isStyleWarning = true;
    }
    super.setError(getContext().getString(text));
  }
}
