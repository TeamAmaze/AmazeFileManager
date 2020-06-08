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

/**
 * Created by Vishal on 21/12/15 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com> A helper
 * class which provides data structure of key/value pair
 *
 * <p>typedef ImmutableEntry<ImmutableEntry<Integer, Integer>, Integer> MapEntry
 */
public class MapEntry extends ImmutableEntry<ImmutableEntry<Integer, Integer>, Integer> {

  /**
   * Constructor to provide values to the pair
   *
   * @param key object of {@link ImmutableEntry} which is another key/value pair
   * @param value integer object in the pair
   */
  public MapEntry(ImmutableEntry<Integer, Integer> key, Integer value) {
    super(key, value);
  }
}
