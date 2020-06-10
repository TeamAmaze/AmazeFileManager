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

package com.amaze.filemanager.filesystem.ssh;

import java.io.IOException;

import androidx.annotation.NonNull;

import net.schmizz.sshj.connection.channel.direct.Session;

public abstract class SshClientSessionTemplate {
  public final String url;

  /**
   * Constructor.
   *
   * @param url SSH connection URL, in the form of <code>
   *     ssh://&lt;username&gt;:&lt;password&gt;@&lt;host&gt;:&lt;port&gt;</code> or <code>
   *     ssh://&lt;username&gt;@&lt;host&gt;:&lt;port&gt;</code>
   */
  public SshClientSessionTemplate(@NonNull String url) {
    this.url = url;
  }

  /**
   * Implement logic here.
   *
   * @param sshClientSession {@link Session} instance, with connection opened and authenticated
   * @param <T> Requested return type
   * @return Result of the execution of the type requested
   */
  public abstract <T> T execute(@NonNull Session sshClientSession) throws IOException;
}
