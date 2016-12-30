package com.amaze.filemanager.services.ftpservice;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Created by vishal on 30/12/16.
 */

@TargetApi(Build.VERSION_CODES.N)
public class FTPTileService extends TileService {

    @Override
    public void onClick() {
        if (!FTPService.isRunning()) {
            if (FTPService.isConnectedToWifi(getApplicationContext())) {
                getApplicationContext().sendBroadcast(new Intent(FTPService.ACTION_START_FTPSERVER));
                getQsTile().setState(Tile.STATE_ACTIVE);
            }
            else {
                getQsTile().setState(Tile.STATE_UNAVAILABLE);
            }
        } else {
            getApplicationContext().sendBroadcast(new Intent(FTPService.ACTION_STOP_FTPSERVER));
            getQsTile().setState(Tile.STATE_INACTIVE);
        }
    }
}
