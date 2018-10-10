package com.amaze.filemanager.asynchronous.services.ftp;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import com.amaze.filemanager.R;

/**
 * Created by vishal on 1/1/17.
 */

@TargetApi(Build.VERSION_CODES.N)
public class FtpTileService extends TileService {
    private BroadcastReceiver ftpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTileState();
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();

        IntentFilter f = new IntentFilter();
        f.addAction(FtpService.ACTION_STARTED);
        f.addAction(FtpService.ACTION_STOPPED);
        registerReceiver(ftpReceiver, f);
        updateTileState();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

        unregisterReceiver(ftpReceiver);
    }

    @Override
    public void onClick() {
        unlockAndRun(()->{
            if (FtpService.isRunning()) {
                getApplicationContext().sendBroadcast(new Intent(FtpService.ACTION_STOP_FTPSERVER).setPackage(getPackageName()));
            } else {
                if (FtpService.isConnectedToWifi(getApplicationContext())
                        || FtpService.isConnectedToLocalNetwork(getApplicationContext())
                        || FtpService.isEnabledWifiHotspot(getApplicationContext())) {
                    Intent i = new Intent(FtpService.ACTION_START_FTPSERVER).setPackage(getPackageName());
                    i.putExtra(FtpService.TAG_STARTED_BY_TILE, true);
                    getApplicationContext().sendBroadcast(i);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.ftp_no_wifi), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (FtpService.isRunning()) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_ftp_dark));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_ftp_light));
        }
        tile.updateTile();
    }
}
