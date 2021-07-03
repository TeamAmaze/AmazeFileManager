package com.amaze.filemanager.ui.activities.texteditor;

import java.io.File;

public class ReturnedValueOnReadFile {
  public final String fileContents;
  public final File cachedFile;

  public ReturnedValueOnReadFile(String fileContents, File cachedFile) {
    this.fileContents = fileContents;
    this.cachedFile = cachedFile;
  }

}