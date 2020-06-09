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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;

class BoundedFileChannelInputStream extends InputStream {
    private static final int MAX_BUF_LEN = 8192;
    private final ByteBuffer buffer;
    private final FileChannel channel;
    private long bytesRemaining;

    public BoundedFileChannelInputStream(final FileChannel channel,
                                         final long size) {
        this.channel = channel;
        this.bytesRemaining = size;
        if (size < MAX_BUF_LEN && size > 0) {
            buffer = ByteBuffer.allocate((int) size);
        } else {
            buffer = ByteBuffer.allocate(MAX_BUF_LEN);
        }
    }

    @Override
    public int read() throws IOException {
        if (bytesRemaining > 0) {
            --bytesRemaining;
            int read = read(1);
            if (read < 0) {
                return read;
            }
            return buffer.get() & 0xff;
        }
        return -1;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (bytesRemaining == 0) {
            return -1;
        }
        int bytesToRead = len;
        if (bytesToRead > bytesRemaining) {
            bytesToRead = (int) bytesRemaining;
        }
        int bytesRead;
        ByteBuffer buf;
        if (bytesToRead <= buffer.capacity()) {
            buf = buffer;
            bytesRead = read(bytesToRead);
        } else {
            buf = ByteBuffer.allocate(bytesToRead);
            bytesRead = channel.read(buf);
            buf.flip();
        }
        if (bytesRead >= 0) {
            buf.get(b, off, bytesRead);
            bytesRemaining -= bytesRead;
        }
        return bytesRead;
    }

    private int read(int len) throws IOException {
        buffer.rewind().limit(len);
        int read = channel.read(buffer);
        buffer.flip();
        return read;
    }

    @Override
    public void close() {
        // the nested channel is controlled externally
    }
}
