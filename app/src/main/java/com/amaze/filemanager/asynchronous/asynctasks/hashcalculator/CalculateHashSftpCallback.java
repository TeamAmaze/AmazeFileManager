/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.hashcalculator;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils;
import com.amaze.filemanager.filesystem.ssh.SshClientSessionTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;

import androidx.annotation.WorkerThread;

import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;

public class CalculateHashSftpCallback implements Callable<Hash> {
  private final HybridFileParcelable file;

  public CalculateHashSftpCallback(HybridFileParcelable file) {
    if (!file.isSftp()) {
      throw new IllegalArgumentException("Use CalculateHashCallback");
    }

    this.file = file;
  }

  @WorkerThread
  @Override
  public Hash call() throws Exception {
    String md5Command = "md5sum -b \"%s\" | cut -c -32";
    String shaCommand = "sha256sum -b \"%s\" | cut -c -64";

    String md5 = SshClientUtils.execute(getHash(md5Command));
    String sha256 = SshClientUtils.execute(getHash(shaCommand));

    Objects.requireNonNull(md5);
    Objects.requireNonNull(sha256);

    return new Hash(md5, sha256);
  }

  private SshClientSessionTemplate<String> getHash(String command) {
    return new SshClientSessionTemplate<String>(file.getPath()) {
      @Override
      public String execute(Session session) throws IOException {
        String path = NetCopyClientUtils.INSTANCE.extractRemotePathFrom(file.getPath());
        String fullCommand = String.format(command, path);
        Session.Command cmd = session.exec(fullCommand);
        String result = new String(IOUtils.readFully(cmd.getInputStream()).toByteArray());
        cmd.close();
        if (cmd.getExitStatus() == 0) {
          return result;
        } else {
          return null;
        }
      }
    };
  }
}
