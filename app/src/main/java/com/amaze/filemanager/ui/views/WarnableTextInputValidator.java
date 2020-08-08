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
import com.amaze.filemanager.utils.SimpleTextWatcher;

import android.content.Context;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public final class WarnableTextInputValidator extends SimpleTextWatcher
    implements View.OnFocusChangeListener, View.OnTouchListener {
  private final Context context;
  private final EditText editText;
  private final View button;
  private final WarnableTextInputLayout textInputLayout;
  private final OnTextValidate validator;
  private @DrawableRes int warningDrawable, errorDrawable;

  public WarnableTextInputValidator(
      Context context,
      EditText editText,
      WarnableTextInputLayout textInputLayout,
      View positiveButton,
      OnTextValidate validator) {
    this.context = context;
    this.editText = editText;
    this.editText.setOnFocusChangeListener(this);
    this.editText.addTextChangedListener(this);
    this.textInputLayout = textInputLayout;
    button = positiveButton;
    button.setOnTouchListener(this);
    button.setEnabled(false);
    this.validator = validator;

    warningDrawable = R.drawable.ic_warning_24dp;
    errorDrawable = R.drawable.ic_error_24dp;
  }

  @Override
  public void onFocusChange(View v, boolean hasFocus) {
    if (!hasFocus) {
      int state = doValidate(false);
      button.setEnabled(state != ReturnState.STATE_ERROR);
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return performClick();
  }

  public boolean performClick() {
    boolean blockTouchEvent = doValidate(false) == ReturnState.STATE_ERROR;
    return blockTouchEvent;
  }

  @Override
  public void afterTextChanged(Editable s) {
    doValidate(false);
  }

  /** @return ReturnState.state */
  private int doValidate(boolean onlySetWarning) {
    ReturnState state = validator.isTextValid(editText.getText().toString());
    switch (state.state) {
      case ReturnState.STATE_NORMAL:
        textInputLayout.removeError();
        setEditTextIcon(null);
        button.setEnabled(true);
        break;
      case ReturnState.STATE_ERROR:
        if (!onlySetWarning) {
          textInputLayout.setError(context.getString(state.text));
          setEditTextIcon(errorDrawable);
        }
        button.setEnabled(false);
        break;
      case ReturnState.STATE_WARNING:
        textInputLayout.setWarning(state.text);
        setEditTextIcon(warningDrawable);
        button.setEnabled(true);
        break;
    }

    return state.state;
  }

  private void setEditTextIcon(@DrawableRes Integer drawable) {
    @DrawableRes int drawableInt = drawable != null ? drawable : 0;
    editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawableInt, 0);
  }

  public interface OnTextValidate {
    ReturnState isTextValid(String text);
  }

  public static class ReturnState {
    public static final int STATE_NORMAL = 0, STATE_ERROR = -1, STATE_WARNING = -2;

    public final int state;
    public final @StringRes int text;

    public ReturnState() {
      state = STATE_NORMAL;
      text = 0;
    }

    public ReturnState(int state, @StringRes int text) {
      this.state = state;
      this.text = text;
    }
  }
}
