/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

public class Constants {
    public static final String SEARCH_BROADCAST_ACTION =
            "SEARCH_BROADCAST";
    public static final String SEARCH_BROADCAST_ACTION_COMPLETED =
            "SEARCH_BROADCAST_COMPLETED";
    public static final String SEARCH_BROADCAST_ARRAY =
            "SEARCH_BROADCAST_RESULTS";
    public static final String SEARCH_BROADCAST_PRESENT_CONDITION =
            "SEARCH_BROADCAST_CURRENTLY_SEARCHING";
    /**
     * A bundle key that can be used by external apps when triggering file picking from Amaze.
     *
     * <p>This can be use as follows from an external app:</p>
     * <code>
     *  Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
     *  intent.setPackage("com.amaze.filemanager");
     *  intent.putExtra("com.amaze.filemanager.extra.TITLE", "Select the file...");
     *
     *  ... and then startActivityForResult
     * </code>
     *
     */
    public static final String FILE_PICKER_TITLE_BUNDLE_KEY = "com.amaze.filemanager.extra.TITLE";

}
