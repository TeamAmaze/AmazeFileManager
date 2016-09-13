package com.amaze.filemanager.utils.broadcast.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

/**
 * Created by vishal on 16/8/16.
 *
 * Broadcast receiver to handle otg connection
 */
public class OtgBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            
        }
    }
}
