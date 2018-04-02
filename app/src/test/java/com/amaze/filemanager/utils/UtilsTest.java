package com.amaze.filemanager.utils;

import org.junit.Test;

import static com.amaze.filemanager.utils.Utils.sanitizeInput;
import static org.junit.Assert.*;

public class UtilsTest {

    String test;
    @Test
    public void testsanitizeInput() {  //This function is sanitize the string. It removes ";","|","&&","..." from string.

        test="|a|";
        assertEquals("a",sanitizeInput(test));  //test the removing of pipe sign from string.
        test="...a...";
        assertEquals("a",sanitizeInput(test));  //test the removing of dots from string.
        test=";a;";
        assertEquals("a",sanitizeInput(test));  //test the removing of semicolon sign from string.
        test="&&a&&";
        assertEquals("a",sanitizeInput(test));  //test the removing of AMP sign from string.
        test="|a...";
        assertEquals("a",sanitizeInput(test));   //test the removing of pipe sign and semicolon sign from string.
        test="an &&apple";
        assertEquals("an apple",sanitizeInput(test));  //test the removing of AMP sign which are between two words.
        test="an ...apple";
        assertEquals("an apple",sanitizeInput(test));  //test the removing of dots which are between two words.
        test=";an |apple....";
        assertEquals("an apple.",sanitizeInput(test));  //test the removing of pipe sign and dots which are between two words. And test the fourth dot is not removed.

    }
}