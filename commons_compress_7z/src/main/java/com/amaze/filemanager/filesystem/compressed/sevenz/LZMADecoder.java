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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.FlushShieldFilterOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.LZMAOutputStream;

class LZMADecoder extends CoderBase {
    LZMADecoder() {
        super(LZMA2Options.class, Number.class);
    }

    @Override
    InputStream decode(final String archiveName, final InputStream in, final long uncompressedLength,
            final Coder coder, final byte[] password) throws IOException {
        final byte propsByte = coder.properties[0];
        final int dictSize = getDictionarySize(coder);
        if (dictSize > LZMAInputStream.DICT_SIZE_MAX) {
            throw new IOException("Dictionary larger than 4GiB maximum size used in " + archiveName);
        }
        return new LZMAInputStream(in, uncompressedLength, propsByte, dictSize);
    }

    @SuppressWarnings("resource")
    @Override
    OutputStream encode(final OutputStream out, final Object opts)
        throws IOException {
        // NOOP as LZMAOutputStream throws an exception in flush
        return new FlushShieldFilterOutputStream(new LZMAOutputStream(out, getOptions(opts), false));
    }

    @Override
    byte[] getOptionsAsProperties(final Object opts) throws IOException {
        final LZMA2Options options = getOptions(opts);
        final byte props = (byte) ((options.getPb() * 5 + options.getLp()) * 9 + options.getLc());
        int dictSize = options.getDictSize();
        byte[] o = new byte[5];
        o[0] = props;
        ByteUtils.toLittleEndian(o, dictSize, 1, 4);
        return o;
    }

    @Override
    Object getOptionsFromCoder(final Coder coder, final InputStream in) throws IOException {
        final byte propsByte = coder.properties[0];
        int props = propsByte & 0xFF;
        int pb = props / (9 * 5);
        props -= pb * 9 * 5;
        int lp = props / 9;
        int lc = props - lp * 9;
        LZMA2Options opts = new LZMA2Options();
        opts.setPb(pb);
        opts.setLcLp(lc, lp);
        opts.setDictSize(getDictionarySize(coder));
        return opts;
    }

    private int getDictionarySize(final Coder coder) throws IllegalArgumentException {
        return (int) ByteUtils.fromLittleEndian(coder.properties, 1, 4);
    }

    private LZMA2Options getOptions(final Object opts) throws IOException {
        if (opts instanceof LZMA2Options) {
            return (LZMA2Options) opts;
        }
        final LZMA2Options options = new LZMA2Options();
        options.setDictSize(numberOptionOrDefault(opts));
        return options;
    }

    private int numberOptionOrDefault(final Object opts) {
        return numberOptionOrDefault(opts, LZMA2Options.DICT_SIZE_DEFAULT);
    }
}
