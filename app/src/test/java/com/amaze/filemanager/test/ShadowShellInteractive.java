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

package com.amaze.filemanager.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import eu.chainfire.libsuperuser.Shell;
import kotlin.jvm.functions.Function1;

/**
 * Shadow of {@link Shell.Interactive}, for {@link com.amaze.filemanager.filesystem.RootHelperTest}.
 *
 * <p>Only tested for {@link
 * com.amaze.filemanager.filesystem.root.ListFilesCommand#listFiles(String, boolean, boolean,
 * Function1, Function1)}, so only guarantees work for that.
 *
 * <p><strong>DO NOT RUN THIS ON NON-UNIX OS. YOU SHOULD KNOW THIS ALREADY.</strong>
 */
@Implements(Shell.Interactive.class)
public class ShadowShellInteractive {

  @Implementation
  public boolean isRunning() {
    return true;
  }

  @Implementation
  public boolean waitForIdle() {
    return true;
  }

  @Implementation
  public void addCommand(
      String command, int code, Shell.OnCommandResultListener onCommandResultListener)
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
    pb.environment().put("LC_ALL", "en_US.utf8");
    pb.environment().put("LANG", "en_US.utf8");
    pb.environment().put("TIME_STYLE", "long-iso");
    Process process = pb.start();
    int exitValue = process.waitFor();
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
    List<String> result = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
      result.add(line);
    }
    onCommandResultListener.onCommandResult(exitValue, code, result);
  }
}
