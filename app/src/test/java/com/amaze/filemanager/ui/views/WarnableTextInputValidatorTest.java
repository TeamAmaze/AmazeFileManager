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

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.R;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {JELLY_BEAN, KITKAT, P})
public class WarnableTextInputValidatorTest {

  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    context.setTheme(R.style.appCompatBlack);
  }

  @Test
  public void testValidate() {
    EditText textfield = new AppCompatEditText(context);
    WarnableTextInputLayout layout =
        new WarnableTextInputLayout(context, Robolectric.buildAttributeSet().build());
    Button button = new AppCompatButton(context);
    WarnableTextInputValidator.OnTextValidate validator =
        text ->
            ("Pass".equals(text))
                ? new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_NORMAL, R.string.ok)
                : new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.error);

    WarnableTextInputValidator target =
        new WarnableTextInputValidator(
            ApplicationProvider.getApplicationContext(), textfield, layout, button, validator);
    textfield.setText("");
    target.performClick();
    assertFalse(button.isEnabled());
    assertEquals(context.getString(R.string.error), layout.getError());
    textfield.setText("pass");
    target.performClick();
    assertFalse(button.isEnabled());
    assertEquals(context.getString(R.string.error), layout.getError());
    textfield.setText("Pass");
    target.performClick();
    assertTrue(button.isEnabled());
    assertNull(layout.getError());
  }
}
