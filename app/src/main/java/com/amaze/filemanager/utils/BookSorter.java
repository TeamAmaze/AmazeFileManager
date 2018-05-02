package com.amaze.filemanager.utils;

import java.util.Comparator;

/**
 * Created by Arpit on 20-11-2015.
 */
public class BookSorter implements Comparator<String[]> {

    // rename parameters : lhsBookNameAndPath, rhsBookNameAndPath, bookCompareResult
    // extract method : isBookNameSame(bookNameCompare)
    
    @Override
    public int compare(String[] lhsBookNameAndPath, String[] rhsBookNameAndPath) {
        int bookCompareResult = lhsBookNameAndPath[0].compareToIgnoreCase(rhsBookNameAndPath[0]);

        if (isBookNameSame(bookCompareResult)) {
            // the title is same, compare their paths
            bookCompareResult = lhsBookNameAndPath[1].compareToIgnoreCase(rhsBookNameAndPath[1]);
        }
        return bookCompareResult;
    }

    private boolean isBookNameSame(int bookNameCompare) {
        return bookNameCompare == 0 ? true : false;
    }

}
