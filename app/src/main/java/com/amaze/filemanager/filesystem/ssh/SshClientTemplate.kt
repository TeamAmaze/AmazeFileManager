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

import net.schmizz.sshj.SSHClient;

/**
 * Template class for executing actions with {@link SSHClient} while leave the complexities of
 * handling connection setup/teardown to {@link SshClientUtils}.
 */
public abstract class SshClientTemplate {
  public final String url;

  public final boolean closeClientOnFinish;

  /**
   * Constructor, with closeClientOnFinish set to true (that the connection must close after <code>
   * execute</code>.
   *
   * @param url SSH connection URL, in the form of <code>
   *     ssh://&lt;username&gt;:&lt;password&gt;@&lt;host&gt;:&lt;port&gt;</code> or <code>
   *     ssh://&lt;username&gt;@&lt;host&gt;:&lt;port&gt;</code>
   */
  public SshClientTemplate(@NonNull String url) {
    this(url, true);
  }

  /**
   * Constructor.
   *
   * @param url SSH connection URL, in the form of <code>
   *     ssh://&lt;username&gt;:&lt;password&gt;@&lt;host&gt;:&lt;port&gt;</code> or <code>
   *     ssh://&lt;username&gt;@&lt;host&gt;:&lt;port&gt;</code>
   * @param closeClientOnFinish if set to false, connection will be kept on after <code>execute
   *     </code> finishes. This is useful for other calls to reuse the connection.
   */
  public SshClientTemplate(@NonNull String url, boolean closeClientOnFinish) {
    this.url = url;
    this.closeClientOnFinish = closeClientOnFinish;
  }

  /**
   * Implement logic here.
   *
   * @param client {@link SSHClient} instance, with connection opened and authenticated
   * @param <T> Requested return type
   * @return Result of the execution of the type requested
   */
  public abstract <T> T execute(@NonNull SSHClient client) throws IOException;
}
