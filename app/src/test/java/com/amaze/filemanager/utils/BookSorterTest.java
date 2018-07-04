package com.amaze.filemanager.utils;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.*;

/**
 * compare title's length and path's lengh. .. Ignore Case
 * two string array's first para : title, second para : path
 */
public class BookSorterTest {
    private BookSorter bookSorter = new BookSorter();

    /**
     * Purpose: when LHS title's length bigger than RHS title's length, result is positive
     * Input: compare(lhs,rhs) lhs title's length > rhs title's length
     * Expected:
     * return positive integer
     */
    @Test
    public void testCompareLHSTitleBigAndPathBigger() {
        String[] lhs = {"abc1", "C:\\AmazeFileManager\\app\\abc1"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertThat(bookSorter.compare(lhs, rhs), greaterThan(0));
    }

    /**
     * Purpose: when LHS title's length smaller than RHS title's length, result is negative
     * Input: compare(lhs,rhs) lhs title's length < rhs title's length
     * Expected:
     * return negative integer
     */
    @Test
    public void testCompareRHSTitleBigAndPathBigger() {
        String[] lhs = {"abc", "C:\\AmazeFileManager\\abc"};
        String[] rhs = {"abc2", "C:\\AmazeFileManager\\app\\abc2"};

        assertThat(bookSorter.compare(lhs, rhs), lessThan(0));
    }

    /**
     * Purpose: when LHS and RHS title's length are same but LHS path's length bigger than RHS path's length, , result is positive
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length and lhs path's length > path title's length
     * Expected:
     * return positive integer
     */
    @Test
    public void testCompareTitleSameAndRHSPathBigger() {
        String[] lhs = {"abc", "C:\\AmazeFileManager\\app\\abc"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertThat(bookSorter.compare(lhs, rhs), greaterThan(0));
    }

    /**
     * Purpose: when LHS and RHS title 's length are same but LHS path's length smaller than RHS path's length, result is negative
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length and lhs path's length < path title's length
     * Expected:
     * return negative integer
     */
    @Test
    public void testCompareTitleSameAndLHSPathBigger() {
        String[] lhs = {"abc", "C:\\AmazeFileManager\\abc"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\app\\abc"};

        assertThat(bookSorter.compare(lhs, rhs), lessThan(0));
    }


    /**
     * this case's expected real result is failure(same name can't exist)
     *
     * Purpose: when LHS and RHS title 's length are same, LHS and RHS path's length are same, result is zero
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length and lhs path's length = path title's length
     * Expected:
     * return zero
     */
    @Test
    public void testCompareTitleSameAndPathSame() {
        String[] lhs = {"abc", "C:\\AmazeFileManager\\abc"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertEquals(bookSorter.compare(lhs, rhs), 0);
    }

    /**
     * Purpose: when LHS and RHS title 's length are same but Case difference,  LHS path's length bigger than RHS path's length, result is positive
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length(but Case difference) and lhs path's length > path title's length
     * Expected:
     * return positive integer
     */
    @Test
    public void testCompareTitleNotSameCaseAndLHSPathBigger() {
        String[] lhs = {"ABC", "C:\\AmazeFileManager\\app\\ABC"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertThat(bookSorter.compare(lhs, rhs), greaterThan(0));
    }

    /**
     * Purpose: when LHS and RHS title 's length are same but Case difference,  LHS path's length smaller than RHS path's length, result is negative
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length(but Case difference) and lhs path's length < path title's length
     * Expected:
     * return negative integer
     */
    @Test
    public void testCompareTitleNotSameCaseAndRHSPathBigger() {
        String[] lhs = {"ABC", "C:\\AmazeFileManager\\ABC"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\app\\abc"};

        assertThat(bookSorter.compare(lhs, rhs), lessThan(0));
    }

    /**
     * this case's expected real result is failure(same name can't exist)
     *
     * Purpose: when LHS and RHS title 's length are same but Case difference, LHS and RHS path's length are same, result is zero
     * Input: compare(lhs,rhs) lhs title's length = rhs title's length(but Case difference) and lhs path's length = path title's length
     * Expected:
     * return zero
     */
    @Test
    public void testCompareTitleNotSameCaseAndPathSame() {
        String[] lhs = {"ABC", "C:\\AmazeFileManager\\ABC"};
        String[] rhs = {"abc", "C:\\AmazeFileManager\\abc"};

        assertEquals(bookSorter.compare(lhs, rhs), 0);
    }
}