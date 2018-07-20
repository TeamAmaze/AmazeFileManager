package com.amaze.filemanager.utils;

import org.junit.Test;

import static com.amaze.filemanager.utils.Utils.sanitizeInput;
import static org.junit.Assert.*;

public class UtilsTest {
    @Test
    public void testSanitizeInput() {  //This function is sanitize the string. It removes ";","|","&&","..." from string.
        assertEquals("a",sanitizeInput("|a|"));  //test the removing of pipe sign from string.
        assertEquals("a",sanitizeInput("...a..."));  //test the removing of dots from string.
        assertEquals("a",sanitizeInput(";a;"));  //test the removing of semicolon sign from string.
        assertEquals("a",sanitizeInput("&&a&&"));  //test the removing of AMP sign from string.
        assertEquals("a",sanitizeInput("|a..."));   //test the removing of pipe sign and semicolon sign from string.
        assertEquals("an apple",sanitizeInput("an &&apple"));  //test the removing of AMP sign which are between two words.
        assertEquals("an apple",sanitizeInput("an ...apple"));  //test the removing of dots which are between two words.
        assertEquals("an apple.",sanitizeInput(";an |apple...."));  //test the removing of pipe sign and dots which are between two words. And test the fourth dot is not removed.
    }
}