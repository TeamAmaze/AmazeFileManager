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

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.shadows.jcifs.smb.ShadowSmbFile;
import com.amaze.filemanager.test.ShadowCryptUtil;
import com.amaze.filemanager.utils.SmbUtil;

import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ActivityScenario.launch;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Config(
    shadows = {
      ShadowCryptUtil.class,
      ShadowSmbFile.class
    })
public class MainActivityTest extends AbstractMainActivityTest {

  @Test
  public void testUpdateSmbExceptionShouldNotThrowNPE() {
    scenario.onActivity(
        activity -> {
          String path = "smb://root:toor@192.168.1.1";
          String oldName = "SMB connection";
          String newName = "root@192.168.1.1";
          try {

            activity.addConnection(
                false,
                oldName,
                path,
                SmbUtil.getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path),
                null,
                null);
            activity.addConnection(
                true,
                newName,
                path,
                SmbUtil.getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path),
                oldName,
                path);

            ShadowLooper.idleMainLooper();

            await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> AppConfig.getInstance().getUtilsHandler().getSmbList().size() > 0);
            await()
                .atMost(5, TimeUnit.SECONDS)
                .until(
                    () ->
                        AppConfig.getInstance()
                            .getUtilsHandler()
                            .getSmbList()
                            .get(0)[0]
                            .equals(newName));
            List<String[]> verify = AppConfig.getInstance().getUtilsHandler().getSmbList();
            assertEquals(1, verify.size());
            String[] entry = verify.get(0);
            assertEquals(path, entry[1]);

          } catch (GeneralSecurityException | IOException e) {
            fail(e.getMessage());
          }
        });
  }
}
