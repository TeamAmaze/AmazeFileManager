/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.utils;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

/** Created by vishal on 15/4/17. */
@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

  private Context context;
  private EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface;
  private Intent decryptIntent;
  private MaterialDialog materialDialog;

  // Constructor
  public FingerprintHandler(
      Context mContext,
      Intent intent,
      MaterialDialog materialDialog,
      EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface) {
    context = mContext;
    this.decryptIntent = intent;
    this.materialDialog = materialDialog;
    this.decryptButtonCallbackInterface = decryptButtonCallbackInterface;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  public void authenticate(
      FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject) {

    CancellationSignal cancellationSignal = new CancellationSignal();
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT)
        != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
  }

  @Override
  public void onAuthenticationError(int errMsgId, CharSequence errString) {}

  @Override
  public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {}

  @Override
  public void onAuthenticationFailed() {
    materialDialog.cancel();
    decryptButtonCallbackInterface.failed();
  }

  @Override
  public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {

    materialDialog.cancel();
    decryptButtonCallbackInterface.confirm(decryptIntent);
  }
}
