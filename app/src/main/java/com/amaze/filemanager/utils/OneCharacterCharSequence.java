package com.amaze.filemanager.utils;

import java.util.Arrays;

public class OneCharacterCharSequence implements CharSequence {
    private final char value;
    private final int length;

    public OneCharacterCharSequence(final char value, final int length) {
        this.value = value;
        this.length = length;
    }

    @Override
    public char charAt(int index) {
        if (index < length) return value;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new OneCharacterCharSequence(value, (end - start));
    }

    @Override
    public String toString() {
        char[] array = new char[length];
        Arrays.fill(array, value);
        return new String(array);
    }
}