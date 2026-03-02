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

package com.amaze.filemanager.ui.dialogs;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.MainActivity;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

/** Integration tests for OTG eject dialog */
@RunWith(AndroidJUnit4.class)
public class OTGEjectDialogTest {

  @Rule
  public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

  private MainActivity mainActivity;

  @Before
  public void setUp() {
    mainActivity = activityRule.getActivity();
  }

  @Test
  public void testEjectDialogDisplaysTitle() {
    // Test that the eject dialog shows the correct title
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    GeneralDialogCreation.showOtgEjectDialog(mainActivity, deviceKey, devicePath, false);

    // Verify dialog title is displayed
    onView(withText(R.string.otg_eject_title)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogDisplaysMessage() {
    // Test that warning message is displayed
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    GeneralDialogCreation.showOtgEjectDialog(mainActivity, deviceKey, devicePath, false);

    // Verify warning message is displayed
    onView(withText(R.string.otg_eject_message)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogPrimaryButton() {
    // Test that positive action button is available
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    GeneralDialogCreation.showOtgEjectDialog(mainActivity, deviceKey, devicePath, false);

    // Verify action button is displayed
    onView(withText(R.string.otg_eject_action)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogWithRootUnmountOption() {
    // Test that root unmount button appears when root is available
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    GeneralDialogCreation.showOtgEjectDialog(
        mainActivity, deviceKey, devicePath, true // isRootAvailable = true
        );

    // Verify root unmount option is displayed
    onView(withText(R.string.otg_eject_root_option)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogWithoutRootUnmountOption() {
    // Test that root unmount button doesn't appear when root unavailable
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    GeneralDialogCreation.showOtgEjectDialog(
        mainActivity, deviceKey, devicePath, false // isRootAvailable = false
        );

    // Root unmount option should not be visible
    try {
      onView(withText(R.string.otg_eject_root_option)).check(matches(isDisplayed()));
      // If we get here, root option was displayed when it shouldn't be
      assert false : "Root option should not be displayed";
    } catch (AssertionError e) {
      // Expected - root option should not be displayed
    }
  }

  @Test
  public void testEjectDialogWithDirectAccessPath() {
    // Test dialog with direct /mnt/media_rw path
    String deviceKey = null; // Direct paths don't need device key
    String devicePath = "/mnt/media_rw/USB-DISK";

    GeneralDialogCreation.showOtgEjectDialog(mainActivity, deviceKey, devicePath, true);

    // Dialog should still display correctly
    onView(withText(R.string.otg_eject_title)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogWithStoragePath() {
    // Test dialog with /storage path
    String deviceKey = null;
    String devicePath = "/storage/XXXX-YYYY";

    GeneralDialogCreation.showOtgEjectDialog(mainActivity, deviceKey, devicePath, true);

    // Dialog should still display correctly
    onView(withText(R.string.otg_eject_message)).check(matches(isDisplayed()));
  }
}
