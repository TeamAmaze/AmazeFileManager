package com.amaze.filemanager.ui.activities.texteditor;

import androidx.annotation.IntDef;

import java.io.File;

public class ReturnedValueOnReadFile {
  public static final int NORMAL = 0;
  public static final int EXCEPTION_STREAM_NOT_FOUND = -1;
  public static final int EXCEPTION_IO = -2;
  public static final int EXCEPTION_OOM = -3;

  @IntDef({NORMAL, EXCEPTION_STREAM_NOT_FOUND, EXCEPTION_IO, EXCEPTION_OOM})
  @interface ErrorCode {}

  public final String fileContents;
  public final @ErrorCode int error;
  public final File cachedFile;

  public ReturnedValueOnReadFile(String fileContents, File cachedFile) {
    this.fileContents = fileContents;
    this.cachedFile = cachedFile;

    this.error = NORMAL;
  }

  public ReturnedValueOnReadFile(@ErrorCode int error) {
    this.error = error;

    this.fileContents = null;
    this.cachedFile = null;
  }
}