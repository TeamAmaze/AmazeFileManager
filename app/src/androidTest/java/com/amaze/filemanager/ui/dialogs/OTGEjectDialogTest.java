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

package com.amaze.filemanager.ui.dialogs;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import java.io.IOException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.MainActivity;

import android.os.Build;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

/** Integration tests for OTG eject dialog */
@RunWith(AndroidJUnit4.class)
public class OTGEjectDialogTest {

  @Rule
  public ActivityScenarioRule<MainActivity> activityRule =
      new ActivityScenarioRule<>(MainActivity.class);

  /**
   * On Android 11+, MainActivity shows a "grant all files access" dialog on top of any other dialog
   * shown afterwards (it re-checks and re-shows it on every onResume), which hides the OTG eject
   * dialog from the view hierarchy and makes Espresso assertions fail with NoMatchingViewException.
   *
   * <p>Rather than clicking through the dialog + Settings toggle with UiAutomator (flaky: races
   * with the dialog's asynchronous appearance, and can even toggle permission back OFF if clicked
   * when already granted), grant MANAGE_EXTERNAL_STORAGE directly via an `appops` shell command.
   * This must run in @BeforeClass - before activityRule's @Rule launches MainActivity for the first
   * time - so the dialog never has a chance to appear at all.
   */
  @BeforeClass
  public static void grantManageStoragePermission() throws IOException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      return;
    }

    String packageName =
        InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
    UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    device.executeShellCommand("appops set " + packageName + " MANAGE_EXTERNAL_STORAGE allow");
  }

  /**
   * Dismiss the OTG eject dialog shown by the test via the back button, instead of destroying (and
   * thus relaunching) the whole MainActivity instance between every test. Since MainActivity is
   * declared launchMode="singleInstance", destroying/relaunching it on every test causes
   * window-focus churn on the emulator and intermittent
   * RootViewPicker$RootViewWithoutFocusException failures. Keeping a single stable instance alive
   * for the whole test class avoids that.
   */
  @After
  public void dismissDialog() {
    try {
      pressBack();
    } catch (Exception ignored) {
      // No dialog/view to dismiss - nothing to do.
    }
  }

  /**
   * Shows the OTG eject dialog and waits for the window manager/animations to settle before handing
   * control back to the caller for Espresso assertions. Without this, Espresso can intermittently
   * fail with RootViewPicker$RootViewWithoutFocusException on slower devices/emulators while the
   * dialog window is still transitioning into focus.
   */
  private void showOtgEjectDialogAndWaitForIdle(
      ActivityScenario<MainActivity> scenario,
      String deviceKey,
      String devicePath,
      boolean isRootAvailable) {
    scenario.onActivity(
        mainActivity ->
            GeneralDialogCreation.showOtgEjectDialog(
                mainActivity, deviceKey, devicePath, isRootAvailable));
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle();
  }

  @Test
  public void testEjectDialogDisplaysTitle() {
    // Test that the eject dialog shows the correct title
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    ActivityScenario<MainActivity> scenario = activityRule.getScenario();
    showOtgEjectDialogAndWaitForIdle(scenario, deviceKey, devicePath, false);

    // Verify dialog title is displayed. Espresso view assertions must run on the
    // instrumentation thread, not inside onActivity() which runs on the main thread.
    onView(withText(R.string.otg_eject_title)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogDisplaysMessage() {
    // Test that warning message is displayed
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    ActivityScenario<MainActivity> scenario = activityRule.getScenario();
    showOtgEjectDialogAndWaitForIdle(scenario, deviceKey, devicePath, false);

    // Verify warning message is displayed
    onView(withText(R.string.otg_eject_message)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogPrimaryButton() {
    // Test that positive action button is available
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    ActivityScenario<MainActivity> scenario = activityRule.getScenario();
    showOtgEjectDialogAndWaitForIdle(scenario, deviceKey, devicePath, false);

    // Verify action button is displayed
    onView(withText(R.string.otg_eject_action)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogWithRootUnmountOption() {
    // Test that root unmount button appears when root is available
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    ActivityScenario<MainActivity> scenario = activityRule.getScenario();
    showOtgEjectDialogAndWaitForIdle(
        scenario, deviceKey, devicePath, true // isRootAvailable = true
        );

    // Verify root unmount option is displayed
    onView(withText(R.string.otg_eject_root_option)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogWithoutRootUnmountOption() {
    // Test that root unmount button doesn't appear when root unavailable
    String deviceKey = "1234:5678";
    String devicePath = "otg:/1234:5678/";

    ActivityScenario<MainActivity> scenario = activityRule.getScenario();
    showOtgEjectDialogAndWaitForIdle(
        scenario, deviceKey, devicePath, false // isRootAvailable = false
        );

    // Root unmount option should not be visible
    onView(withText(R.string.otg_eject_root_option)).check(doesNotExist());
  }

  @Test
  public void testEjectDialogWithDirectAccessPath() {
    // Test dialog with direct /mnt/media_rw path
    String deviceKey = null; // Direct paths don't need device key
    String devicePath = "/mnt/media_rw/USB-DISK";

    ActivityScenario<MainActivity> scenario = activityRule.getScenario();
    showOtgEjectDialogAndWaitForIdle(scenario, deviceKey, devicePath, true);

    // Dialog should still display correctly
    onView(withText(R.string.otg_eject_title)).check(matches(isDisplayed()));
  }

  @Test
  public void testEjectDialogWithStoragePath() {
    // Test dialog with /storage path
    String deviceKey = null;
    String devicePath = "/storage/XXXX-YYYY";

    ActivityScenario<MainActivity> scenario = activityRule.getScenario();
    showOtgEjectDialogAndWaitForIdle(scenario, deviceKey, devicePath, true);

    // Dialog should still display correctly
    onView(withText(R.string.otg_eject_message)).check(matches(isDisplayed()));
  }
}
