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

/// Map between folders, files and streams.
class StreamMap {
    /// The first Archive.packStream index of each folder.
    int[] folderFirstPackStreamIndex;
    /// Offset to beginning of this pack stream's data, relative to the beginning of the first pack stream.
    long[] packStreamOffsets;
    /// Index of first file for each folder.
    int[] folderFirstFileIndex;
    /// Index of folder for each file.
    int[] fileFolderIndex;

    @Override
    public String toString() {
        return "StreamMap with indices of " + folderFirstPackStreamIndex.length
            + " folders, offsets of " + packStreamOffsets.length + " packed streams,"
            + " first files of " + folderFirstFileIndex.length + " folders and"
            + " folder indices for " + fileFolderIndex.length + " files";
    }
}
