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

final class NID {
    public static final int kEnd = 0x00;
    public static final int kHeader = 0x01;
    public static final int kArchiveProperties = 0x02;
    public static final int kAdditionalStreamsInfo = 0x03;
    public static final int kMainStreamsInfo = 0x04;
    public static final int kFilesInfo = 0x05;
    public static final int kPackInfo = 0x06;
    public static final int kUnpackInfo = 0x07;
    public static final int kSubStreamsInfo = 0x08;
    public static final int kSize = 0x09;
    public static final int kCRC = 0x0A;
    public static final int kFolder = 0x0B;
    public static final int kCodersUnpackSize = 0x0C;
    public static final int kNumUnpackStream = 0x0D;
    public static final int kEmptyStream = 0x0E;
    public static final int kEmptyFile = 0x0F;
    public static final int kAnti = 0x10;
    public static final int kName = 0x11;
    public static final int kCTime = 0x12;
    public static final int kATime = 0x13;
    public static final int kMTime = 0x14;
    public static final int kWinAttributes = 0x15;
    public static final int kComment = 0x16;
    public static final int kEncodedHeader = 0x17;
    public static final int kStartPos = 0x18;
    public static final int kDummy = 0x19;
}
