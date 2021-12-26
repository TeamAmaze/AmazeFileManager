package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.gdrive;

import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.Account;
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.CloudAmazeFilesystem;

public final class GoogledriveAmazeFilesystem extends CloudAmazeFilesystem {
  public static final String TAG = GoogledriveAmazeFilesystem.class.getSimpleName();

  public static final String PREFIX = "gdrive:/";

  public static final GoogledriveAmazeFilesystem INSTANCE = new GoogledriveAmazeFilesystem();

  private GoogledriveAmazeFilesystem() {}

  @Override
  public String getPrefix() {
    return PREFIX;
  }

  @Override
  public Account getAccount() {
    return GoogledriveAccount.INSTANCE;
  }
}
