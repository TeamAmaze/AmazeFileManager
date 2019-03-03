/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.amaze.filemanager.filesystem.compressed.sevenz;

/**
 * Combines a SevenZMethod with configuration options for the method.
 *
 * <p>The exact type and interpretation of options depends on the
 * method being configured.  Currently supported are:</p>
 *
 * <table summary="Options">
 * <tr><th>Method</th><th>Option Type</th><th>Description</th></tr>
 * <tr><td>BZIP2</td><td>Number</td><td>Block Size - an number between 1 and 9</td></tr>
 * <tr><td>DEFLATE</td><td>Number</td><td>Compression Level - an number between 1 and 9</td></tr>
 * <tr><td>LZMA2</td><td>Number</td><td>Dictionary Size - a number between 4096 and 768 MiB (768 &lt;&lt; 20)</td></tr>
 * <tr><td>LZMA2</td><td>org.tukaani.xz.LZMA2Options</td><td>Whole set of LZMA2 options.</td></tr>
 * <tr><td>DELTA_FILTER</td><td>Number</td><td>Delta Distance - a number between 1 and 256</td></tr>
 * </table>
 *
 * @Immutable
 * @since 1.8
 */
public class SevenZMethodConfiguration {
    private final SevenZMethod method;
    private final Object options;

    /**
     * Doesn't configure any additional options.
     * @param method the method to use
     */
    public SevenZMethodConfiguration(final SevenZMethod method) {
        this(method, null);
    }

    /**
     * Specifies and method plus configuration options.
     * @param method the method to use
     * @param options the options to use
     * @throws IllegalArgumentException if the method doesn't understand the options specified.
     */
    public SevenZMethodConfiguration(final SevenZMethod method, final Object options) {
        this.method = method;
        this.options = options;
        if (options != null && !Coders.findByMethod(method).canAcceptOptions(options)) {
            throw new IllegalArgumentException("The " + method + " method doesn't support options of type "
                                               + options.getClass());
        }
    }

    /**
     * The specified method.
     * @return the method
     */
    public SevenZMethod getMethod() {
        return method;
    }

    /**
     * The specified options.
     * @return the options
     */
    public Object getOptions() {
        return options;
    }

}
