package com.amaze.filemanager.utils;

import java.util.Comparator;

/**
 * Created by Arpit on 20-11-2015.
 */
public class BookSorter implements Comparator<String[]> {

    @Override
    public int compare(String[] lhs, String[] rhs) {
        return lhs[0].compareToIgnoreCase(rhs[0]);
    }
}
