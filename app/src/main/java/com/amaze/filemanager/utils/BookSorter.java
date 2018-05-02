package com.amaze.filemanager.utils;

import java.util.Comparator;

/**
 * Created by Arpit on 20-11-2015.
 */
public class BookSorter implements Comparator<String[]> {

    /* rename parameters - compare method : "lhs->lhsBookNameAndPath", "rhs->rhsBookNameAndPath", "result->bookCompareResult"
    It does not know what role it plays with existing parameter names. (Improved understandability)
    */

    /* extract method - compare method : make isBookNameSame(bookNameCompare) and change "result==0->isBookNameSame(bookNameCompare)"
    I don't know that the name of the book is the same when you see "result == 0". (Improved understandability)
     */

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
