package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.onedrive;

import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.Account;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.CloudAmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.dropbox.DropboxAccount;

public final class OnedriveAmazeFilesystem extends CloudAmazeFilesystem {
  public static final String TAG = OnedriveAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = "onedrive:/";

  public static final OnedriveAmazeFilesystem INSTANCE = new OnedriveAmazeFilesystem();

  private OnedriveAmazeFilesystem() {}

  @Override
  public String getPrefix() {
    return PREFIX;
  }

  @Override
  public Account getAccount() {
    return OnedriveAccount.INSTANCE;
  }
}
