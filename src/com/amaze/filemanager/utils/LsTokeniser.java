package com.amaze.filemanager.utils;

/**
 * Created by Arpit on 29-11-2014.
 */
public class LsTokeniser {

    private String mLine;
    private int mIndex = 0;
    private boolean foundTime;

    public LsTokeniser(String line) {
        mLine = line;
    }

    public String nextToken() {
        if (mIndex == -1) return null;
        else if (mIndex == 0) {
            // Getting the first token, get all text up to the first space
            int endIndex = mLine.indexOf(' ');
            mIndex = endIndex;
            return mLine.substring(0, endIndex);
        }
        mIndex++;
        if (!foundTime && Character.isSpaceChar(mLine.charAt(mIndex))) {
            // If not looking for the name, ignore extra spaces
            return nextToken();
        }
        int start = mIndex;
        if (foundTime) {
            if (mLine.indexOf("->", start) != -1) {
                // Represents a link, return everything before as name
                mIndex = mLine.indexOf("->", start);
                String token = mLine.substring(start, mIndex - 1);
                mIndex += 2;
                return token;
            } else {
                // Return the remaining content
                mIndex = -1;
                return mLine.substring(start, mLine.length());
            }
        }
        mIndex = mLine.indexOf(' ', start);
        String token = mLine.substring(start, mIndex);
        if (token.contains(":") && !foundTime) {
            // Found the time column, which means everything after this column is the name and link
            foundTime = true;
        }
        return token;
    }
}
