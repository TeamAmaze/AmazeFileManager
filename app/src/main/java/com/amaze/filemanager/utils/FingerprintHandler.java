package com.amaze.filemanager.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.utils.files.EncryptDecryptUtils;

/**
 * Created by vishal on 15/4/17.
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

    private Context context;
    private EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface;
    private Intent decryptIntent;
    private MaterialDialog materialDialog;

    // Constructor
    public FingerprintHandler(Context mContext, Intent intent, MaterialDialog materialDialog,
                              EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface) {
        context = mContext;
        this.decryptIntent = intent;
        this.materialDialog = materialDialog;
        this.decryptButtonCallbackInterface = decryptButtonCallbackInterface;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void authenticate(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject) {

        CancellationSignal cancellationSignal = new CancellationSignal();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
    }


    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {

    }


    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {

    }


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
