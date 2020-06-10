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

package com.amaze.filemanager.utils;

import java.util.Arrays;

public class OneCharacterCharSequence implements CharSequence {
  private final char value;
  private final int length;

  public OneCharacterCharSequence(final char value, final int length) {
    this.value = value;
    this.length = length;
  }

  @Override
  public char charAt(int index) {
    if (index < length) return value;
    throw new IndexOutOfBoundsException();
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return new OneCharacterCharSequence(value, (end - start));
  }

  @Override
  public String toString() {
    char[] array = new char[length];
    Arrays.fill(array, value);
    return new String(array);
  }
}
