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

package com.amaze.filemanager.ui.activities;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextEditorActivityEspressoTest {

  @Rule
  public ActivityTestRule<TextEditorActivity> activityRule =
      new ActivityTestRule<>(TextEditorActivity.class, true, false);

  private Context context;

  private Uri uri;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    File file = new File("/default.prop");
    uri = Uri.fromFile(file);
  }

  @Test
  public void testOpenFile() throws Exception {
    Intent intent =
        new Intent(context, TextEditorActivity.class)
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setType("text/plain")
            .setData(uri);
    activityRule.launchActivity(intent);
    CountDownLatch waiter = new CountDownLatch(1);
    while ("".equals(activityRule.getActivity().mInput.getText().toString())) {
      waiter.await();
    }
    waiter.countDown();
    assertNotEquals("", activityRule.getActivity().mInput.getText());
    assertNotEquals("foobar", activityRule.getActivity().mInput.getText());
    // Add extra time for you to see the Activity did load, and text is actually there
    // Thread.sleep(1000);
  }
}
