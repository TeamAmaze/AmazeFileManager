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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComputerParcelableTest {

  /**
   * Purpose: check computerParcelable and object are the equal. Input:
   * computerParcelable.equals(object) ComputerParcelable == Object Expected: result is true
   */
  @Test
  public void testObjectEquals() {
    ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
    Object object = new ComputerParcelable("com1", "1");

    assertTrue(computerParcelable.equals(object));
  }

  /**
   * Purpose: when computerParcelable's name and object's name are not the same, confirm that the
   * two are different. Input: computerParcelable.equals(object) only ComputerParcelable.addr ==
   * Object.addr Expected: result is false
   */
  @Test
  public void testObjectNotEqualsName() {
    ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
    Object object = new ComputerParcelable("com2", "1");

    assertFalse(computerParcelable.equals(object));
  }

  /**
   * Purpose: when computerParcelable's address and object's address are not the same, confirm that
   * the two are different. Input: computerParcelable.equals(object) only ComputerParcelable.name ==
   * Object.name Expected: result is false
   */
  @Test
  public void testObjectNotEqualsAddr() {
    ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
    Object object = new ComputerParcelable("com1", "2");

    assertFalse(computerParcelable.equals(object));
  }

  /**
   * Purpose: when computerParcelable's name/address and object's name/address are not the same,
   * confirm that the two are different. Input: computerParcelable.equals(object) ComputerParcelable
   * and Object not same(name, address) Expected: result is false
   */
  @Test
  public void testObjectNotEqualsTwo() {
    ComputerParcelable computerParcelable = new ComputerParcelable("com1", "1");
    Object object = new ComputerParcelable("com2", "2");

    assertFalse(computerParcelable.equals(object));
  }
}
