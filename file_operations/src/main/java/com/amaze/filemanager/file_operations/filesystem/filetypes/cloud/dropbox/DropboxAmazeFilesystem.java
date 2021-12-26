package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.dropbox;

import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.Account;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.CloudAmazeFilesystem;

public final class DropboxAmazeFilesystem extends CloudAmazeFilesystem {
  public static final String TAG = DropboxAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = "dropbox:/";

  public static final DropboxAmazeFilesystem INSTANCE = new DropboxAmazeFilesystem();

  private DropboxAmazeFilesystem() {}

  @Override
  public String getPrefix() {
    return PREFIX;
  }

  @Override
  public Account getAccount() {
    return DropboxAccount.INSTANCE;
  }
}
