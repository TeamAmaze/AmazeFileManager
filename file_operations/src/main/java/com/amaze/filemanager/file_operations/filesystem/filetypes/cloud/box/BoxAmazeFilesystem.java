package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.box;

import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.Account;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.CloudAmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.dropbox.DropboxAccount;

public final class BoxAmazeFilesystem extends CloudAmazeFilesystem {
  public static final String TAG = BoxAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = "box:/";

  public static final BoxAmazeFilesystem INSTANCE = new BoxAmazeFilesystem();

  private BoxAmazeFilesystem() {}

  @Override
  public String getPrefix() {
    return PREFIX;
  }

  @Override
  public Account getAccount() {
    return BoxAccount.INSTANCE;
  }
}
