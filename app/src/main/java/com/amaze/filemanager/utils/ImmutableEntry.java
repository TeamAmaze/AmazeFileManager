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

import java.util.Map;

import androidx.annotation.Nullable;

/**
 * From:
 * https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/ImmutableEntry.java
 * Author: Guava
 */
public class ImmutableEntry<K, V> implements Map.Entry<K, V> {
  private final K key;
  private final V value;

  public ImmutableEntry(@Nullable K key, @Nullable V value) {
    this.key = key;
    this.value = value;
  }

  @Nullable
  @Override
  public final K getKey() {
    return key;
  }

  @Nullable
  @Override
  public final V getValue() {
    return value;
  }

  @Override
  public final V setValue(V value) {
    throw new UnsupportedOperationException();
  }
}
