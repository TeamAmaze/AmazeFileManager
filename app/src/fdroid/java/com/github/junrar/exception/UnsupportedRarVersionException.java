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

package com.github.junrar.exception;

/**
 * Thrown when the archive signature names a RAR format this library cannot read: a future format
 * version byte ({@code 0x02}..{@code 0x04}) that the library predates (unrar {@code RARFMT_FUTURE},
 * {@code d861246:archive.cpp:122,178-181}: "so we can return a sensible warning in case we'll want
 * to change the archive format sometimes in the future"). The ancient RAR 1.4 format (marker {@code
 * 52 45 7e 5e}, unrar {@code RARFMT14}) no longer throws this -- {@code Archive} reads its headers
 * through a dedicated loop (P1, issue #293). Distinct from {@link BadRarArchiveException} (no valid
 * signature at all).
 */
public class UnsupportedRarVersionException extends Exception {
  public UnsupportedRarVersionException(Throwable cause) {
    super(cause);
  }

  public UnsupportedRarVersionException() {}
}
