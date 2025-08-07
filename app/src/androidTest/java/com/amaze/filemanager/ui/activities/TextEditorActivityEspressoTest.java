/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.activities;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.filters.Suppress;
import androidx.test.platform.app.InstrumentationRegistry;

@SmallTest
@RunWith(AndroidJUnit4.class)
@Suppress
// Have to rewrite to cope with Android 11 storage access model
public class TextEditorActivityEspressoTest {

  private Context context;

  private Uri uri;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    File file = new File("/default.prop");
    uri = Uri.fromFile(file);
  }

  @Test
  public void testOpenFile() {
    Intent intent =
        new Intent(context, TextEditorActivity.class)
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setType("text/plain")
            .setData(uri);
    try (ActivityScenario<TextEditorActivity> scenario = ActivityScenario.launch(intent)) {
      scenario.onActivity(
          activity -> {
            CountDownLatch waiter = new CountDownLatch(1);
            try {
              while ("".equals(activity.mainTextView.getText().toString())) {
                waiter.await();
              }
            } catch (InterruptedException ignored) {
            }
            waiter.countDown();
            assertNotEquals("", activity.mainTextView.getText().toString());
            assertNotEquals("foobar", activity.mainTextView.getText().toString());
            // Add extra time for you to see the Activity did load, and text is actually there
            // Thread.sleep(1000);
          });
    }
  }
}
