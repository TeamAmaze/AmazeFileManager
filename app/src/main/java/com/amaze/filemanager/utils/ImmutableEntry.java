/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amaze.filemanager.utils;

import androidx.annotation.Nullable;

import java.util.Map;

/**
 * From: https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/ImmutableEntry.java
 * Author: Guava
 */
public class ImmutableEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private final V value;

    public ImmutableEntry(@Nullable K key, @Nullable V value) {
        this.key = key;
        this.value = value;
    }

    @Nullable
    @Override
    public final K getKey() {
        return key;
    }

    @Nullable
    @Override
    public final V getValue() {
        return value;
    }

    @Override
    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }

}