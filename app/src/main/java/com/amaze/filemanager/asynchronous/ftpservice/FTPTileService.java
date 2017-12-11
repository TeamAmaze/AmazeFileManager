package com.amaze.filemanager.asynchronous.ftpservice;

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
public class FTPTileService extends TileService {
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
        f.addAction(FTPService.ACTION_STARTED);
        f.addAction(FTPService.ACTION_STOPPED);
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
        super.onClick();

        if (getQsTile().getState() == Tile.STATE_ACTIVE) {
            getApplicationContext().sendBroadcast(new Intent(FTPService.ACTION_STOP_FTPSERVER));
        } else {
            if (FTPService.isConnectedToWifi(getApplicationContext())
                    || FTPService.isConnectedToLocalNetwork(getApplicationContext())
                    || FTPService.isEnabledWifiHotspot(getApplicationContext())) {
                Intent i = new Intent(FTPService.ACTION_START_FTPSERVER);
                i.putExtra(FTPService.TAG_STARTED_BY_TILE, true);
                getApplicationContext().sendBroadcast(i);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.ftp_no_wifi), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (FTPService.isRunning()) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_ftp_dark));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_ftp_light));
        }
        tile.updateTile();
    }
}
