package com.amaze.filemanager.services.ftpservice;

import android.annotation.TargetApi;
import android.content.Intent;
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

    // callbacks are not guaranteed to be called serially, so initialize this before use
    private Tile mTile;

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        mTile = getQsTile();
        mTile.setState(Tile.STATE_INACTIVE);
        mTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        mTile = getQsTile();

        if (!FTPService.isRunning()) {
            if (FTPService.isConnectedToWifi(getApplicationContext())) {
                startServer();
                mTile.setState(Tile.STATE_ACTIVE);
                mTile.updateTile();
            }
            else {
                mTile.setState(Tile.STATE_INACTIVE);
                mTile.updateTile();
                Toast.makeText(getApplicationContext(), getString(R.string.ftp_no_wifi), Toast.LENGTH_LONG).show();
            }
        } else {
            stopServer();
            mTile.setState(Tile.STATE_INACTIVE);
            mTile.updateTile();
        }
    }

    /**
     * Sends a broadcast to start ftp server
     */
    private void startServer() {
        getApplicationContext().sendBroadcast(new Intent(FTPService.ACTION_START_FTPSERVER));
    }

    /**
     * Sends a broadcast to stop ftp server
     */
    private void stopServer() {
        getApplicationContext().sendBroadcast(new Intent(FTPService.ACTION_STOP_FTPSERVER));
    }
}
