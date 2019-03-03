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

import java.util.Arrays;

/**
 * The (partially) supported compression/encryption methods used in 7z archives.
 *
 * <p>All methods with a _FILTER suffix are used as preprocessors with
 * the goal of creating a better compression ratio with the compressor
 * that comes next in the chain of methods.  7z will in general only
 * allow them to be used together with a "real" compression method but
 * Commons Compress doesn't enforce this.</p>
 *
 * <p>The BCJ_ filters work on executable files for the given platform
 * and convert relative addresses to absolute addresses in CALL
 * instructions.  This means they are only useful when applied to
 * executables of the chosen platform.</p>
 */
public enum SevenZMethod {
    /** no compression at all */
    COPY(new byte[] { (byte)0x00 }),
    /** LZMA - only supported when reading */
    LZMA(new byte[] { (byte)0x03, (byte)0x01, (byte)0x01 }),
    /** LZMA2 */
    LZMA2(new byte[] { (byte)0x21 }),
    /** Deflate */
    DEFLATE(new byte[] { (byte)0x04, (byte)0x01, (byte)0x08 }),
    /**
     * Deflate64
     * @since 1.16
     */
    DEFLATE64(new byte[] { (byte)0x04, (byte)0x01, (byte)0x09 }),
    /** BZIP2 */
    BZIP2(new byte[] { (byte)0x04, (byte)0x02, (byte)0x02 }),
    /**
     * AES encryption with a key length of 256 bit using SHA256 for
     * hashes - only supported when reading
     */
    AES256SHA256(new byte[] { (byte)0x06, (byte)0xf1, (byte)0x07, (byte)0x01 }),
    /**
     * BCJ x86 platform version 1.
     * @since 1.8
     */
    BCJ_X86_FILTER(new byte[] { 0x03, 0x03, 0x01, 0x03 }),
    /**
     * BCJ PowerPC platform.
     * @since 1.8
     */
    BCJ_PPC_FILTER(new byte[] { 0x03, 0x03, 0x02, 0x05 }),
    /**
     * BCJ I64 platform.
     * @since 1.8
     */
    BCJ_IA64_FILTER(new byte[] { 0x03, 0x03, 0x04, 0x01 }),
    /**
     * BCJ ARM platform.
     * @since 1.8
     */
    BCJ_ARM_FILTER(new byte[] { 0x03, 0x03, 0x05, 0x01 }),
    /**
     * BCJ ARM Thumb platform.
     * @since 1.8
     */
    BCJ_ARM_THUMB_FILTER(new byte[] { 0x03, 0x03, 0x07, 0x01 }),
    /**
     * BCJ Sparc platform.
     * @since 1.8
     */
    BCJ_SPARC_FILTER(new byte[] { 0x03, 0x03, 0x08, 0x05 }),
    /**
     * Delta filter.
     * @since 1.8
     */
    DELTA_FILTER(new byte[] { 0x03 });

    private final byte[] id;

    SevenZMethod(final byte[] id) {
        this.id = id;
    }

    byte[] getId() {
        final byte[] copy = new byte[id.length];
        System.arraycopy(id, 0, copy, 0, id.length);
        return copy;
    }

    static SevenZMethod byId(final byte[] id) {
        for (final SevenZMethod m : SevenZMethod.class.getEnumConstants()) {
            if (Arrays.equals(m.id, id)) {
                return m;
            }
        }
        return null;
    }
}
