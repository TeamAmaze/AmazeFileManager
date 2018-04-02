package com.amaze.filemanager.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class BookSorterTest {

    //compare title's length and path's lengh. .. Ignore Case
    //two string array's first para : title, second para : path

    /**
     * Purpose: when LHS title's length bigger than RHS title's length, result is positive
     * Input: compare(lhs,rhs) lhs title's length > rhs title's length
     * Expected:
     * return positive integer
     */
    @Test
    public void testCompareLHSTitleBigAndPathBigger() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"abc1", "C:\\AmazeFileManager\\app\\abc1"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertTrue(bookSorter.compare(lhs, rhs) > 0);
    }

    /**
     * Purpose: when LHS title's length smaller than RHS title's length, result is negative
     * Input: compare(lhs,rhs) lhs title's length < rhs title's length
     * Expected:
     * return negative integer
     */
    @Test
    public void testCompareRHSTitleBigAndPathBigger() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"abc", "C:\\AmazeFileManager\\abc"};
        String[] rhs = {"abc2", "C:\\AmazeFileManager\\app\\abc2"};

        assertTrue(bookSorter.compare(lhs, rhs) < 0);
    }

    /**
     * Purpose: when LHS and RHS title's length are same but LHS path's length bigger than RHS path's length, , result is positive
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length and lhs path's length > path title's length
     * Expected:
     * return positive integer
     */
    @Test
    public void testCompareTitleSameAndRHSPathBigger() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"abc", "C:\\AmazeFileManager\\app\\abc"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertTrue(bookSorter.compare(lhs, rhs) > 0);
    }

    /**
     * Purpose: when LHS and RHS title 's length are same but LHS path's length smaller than RHS path's length, result is negative
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length and lhs path's length < path title's length
     * Expected:
     * return negative integer
     */
    @Test
    public void testCompareTitleSameAndLHSPathBigger() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"abc", "C:\\AmazeFileManager\\abc"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\app\\abc"};

        assertTrue(bookSorter.compare(lhs, rhs) < 0);
    }


    /**
     * Purpose: when LHS and RHS title 's length are same, LHS and RHS path's length are same, result is zero
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length and lhs path's length = path title's length
     * Expected:
     * return zero
     */
    // this case's expected real result is failure(same name can't exist)
    @Test
    public void testCompareTitleSameAndPathSame() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"abc", "C:\\AmazeFileManager\\abc"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertTrue(bookSorter.compare(lhs, rhs) == 0);
    }

    /**
     * Purpose: when LHS and RHS title 's length are same but Case difference,  LHS path's length bigger than RHS path's length, result is positive
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length(but Case difference) and lhs path's length > path title's length
     * Expected:
     * return positive integer
     */
    @Test
    public void testCompareTitleNotSameCaseAndLHSPathBigger() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"ABC", "C:\\AmazeFileManager\\app\\ABC"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertTrue(bookSorter.compare(lhs, rhs) > 0);
    }

    /**
     * Purpose: when LHS and RHS title 's length are same but Case difference,  LHS path's length smaller than RHS path's length, result is negative
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length(but Case difference) and lhs path's length < path title's length
     * Expected:
     * return negative integer
     */
    @Test
    public void testCompareTitleNotSameCaseAndRHSPathBigger() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"ABC", "C:\\AmazeFileManager\\ABC"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\app\\abc"};

        assertTrue(bookSorter.compare(lhs, rhs) < 0);
    }

    /**
     * Purpose: when LHS and RHS title 's length are same but Case difference, LHS and RHS path's length are same, result is zero
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length(but Case difference) and lhs path's length = path title's length
     * Expected:
     * return zero
     */
    // this case's expected real result is failure(same name can't exist)
    @Test
    public void testCompareTitleNotSameCaseAndPathSame() {
        BookSorter bookSorter = new BookSorter();
        String[] lhs = {"ABC", "C:\\AmazeFileManager\\ABC"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertTrue(bookSorter.compare(lhs, rhs) == 0);
    }
}