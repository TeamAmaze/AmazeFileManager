package com.amaze.filemanager.utils;

import java.util.Comparator;

/**
 * Created by Arpit on 20-11-2015.
 */
public class BookSorter implements Comparator<String[]> {

    // rename parameters : lhsBookNameAndPath, rhsBookNameAndPath, bookCompareResult

    @Override
    public int compare(String[] lhsBookNameAndPath, String[] rhsBookNameAndPath) {
        int bookCompareResult = lhsBookNameAndPath[0].compareToIgnoreCase(rhsBookNameAndPath[0]);

        if (bookCompareResult == 0) {
            // the title is same, compare their paths
            bookCompareResult = lhsBookNameAndPath[1].compareToIgnoreCase(rhsBookNameAndPath[1]);
        }
        return bookCompareResult;
    }
}
