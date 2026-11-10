/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters.glide;

import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.bumptech.glide.RequestBuilder;

import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = P)
public class RecyclerPreloadModelProviderTest {

  @Test
  public void getPreloadRequestBuilder_usesOptionalTransformation() {
    verifyTransformationIsOptional(false);
    verifyTransformationIsOptional(true);
  }

  private void verifyTransformationIsOptional(boolean isCircled) {
    ActivityController<TestFragmentActivity> controller =
        Robolectric.buildActivity(TestFragmentActivity.class).setup();
    try {
      Fragment fragment = new Fragment();
      controller
          .get()
          .getSupportFragmentManager()
          .beginTransaction()
          .add(fragment, null)
          .commitNow();

      RecyclerPreloadModelProvider provider =
          new RecyclerPreloadModelProvider(fragment, Collections.emptyList(), isCircled);
      IconDataParcelable iconData =
          new IconDataParcelable(IconDataParcelable.IMAGE_RES, R.drawable.ic_doc_image);

      RequestBuilder<Drawable> requestBuilder = provider.getPreloadRequestBuilder(iconData);

      assertNotNull(requestBuilder);
      assertTrue(requestBuilder.isTransformationSet());
      assertFalse(requestBuilder.isTransformationRequired());
    } finally {
      controller.pause().stop().destroy();
    }
  }

  public static class TestFragmentActivity extends FragmentActivity {}
}
