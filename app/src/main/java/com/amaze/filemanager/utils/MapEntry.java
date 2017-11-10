package com.amaze.filemanager.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Vishal on 21/12/15.
 * A helper class which provides data structure of key/value pair
 */
public class MapEntry implements Map.Entry {

    private KeyMapEntry key;
    private Integer value;

    /**
     * Constructor to provide values to the pair
     * @param key object of {@link KeyMapEntry} which is another key/value pair
     * @param value integer object in the pair
     */
    public MapEntry(KeyMapEntry key, Integer value) {
        this.key = key;
        this.value = value;
    }
    @Override
    public Object getKey() {
        return this.key;
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    @Override
    public Object setValue(Object object) {
        // use constructor
        return null;
    }

    public static class KeyMapEntry implements Map.Entry {

        private Integer key, value;

        public KeyMapEntry(Integer key, Integer value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Object getKey() {
            return this.key;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        @Override
        public Object setValue(Object object) {
            // use constructor
            return null;
        }
    }
}
