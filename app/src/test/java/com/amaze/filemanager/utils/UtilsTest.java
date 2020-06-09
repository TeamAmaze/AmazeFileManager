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

import static com.amaze.filemanager.utils.Utils.formatTimer;
import static com.amaze.filemanager.utils.Utils.sanitizeInput;
import static org.junit.Assert.*;

import org.junit.Test;

public class UtilsTest {
  @Test
  public void
      testSanitizeInput() { // This function is sanitize the string. It removes ";","|","&&","..."
    // from string.
    assertEquals("a", sanitizeInput("|a|")); // test the removing of pipe sign from string.
    assertEquals("a", sanitizeInput("...a...")); // test the removing of dots from string.
    assertEquals("a", sanitizeInput(";a;")); // test the removing of semicolon sign from string.
    assertEquals("a", sanitizeInput("&&a&&")); // test the removing of AMP sign from string.
    assertEquals(
        "a",
        sanitizeInput("|a...")); // test the removing of pipe sign and semicolon sign from string.
    assertEquals(
        "an apple",
        sanitizeInput("an &&apple")); // test the removing of AMP sign which are between two words.
    assertEquals(
        "an apple",
        sanitizeInput("an ...apple")); // test the removing of dots which are between two words.
    assertEquals(
        "an apple.",
        sanitizeInput(
            ";an |apple....")); // test the removing of pipe sign and dots which are between two
    // words. And test the fourth dot is not removed.
  }

  @Test
  public void testFormatTimer() {
    assertEquals("10:00", formatTimer(600));
    assertEquals("00:00", formatTimer(0));
    assertEquals("00:45", formatTimer(45));
    assertEquals("02:45", formatTimer(165));
    assertEquals("30:33", formatTimer(1833));
  }
}
