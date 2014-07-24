package com.amaze.filemanager.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

import com.amaze.filemanager.utils.Futils;

import java.io.File;
import java.util.ArrayList;

public class DeleteTask extends Service {

    @Override
    public void onCreate() {

    }

    ArrayList<File> files = new ArrayList<File>();
    // Binder given to clients
    Futils utils = new Futils();


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    Context cn = this;

    public class Doback extends AsyncTask<Bundle, Void, Integer> {
        ArrayList<File> files;

        public Doback() {
        }

        protected Integer doInBackground(Bundle... p1) {
            boolean b = true;
            files = utils.toFileArray(p1[0].getStringArrayList("array"));

            for (int i = 0; i < files.size(); i++) {
                boolean c = utils.deletefiles(files.get(i));
                if (!c) {
                    b = false;
                }

            }
            utils.scanFile(files.get(0).getParent(), cn);

            publishResults(b);
            return p1[0].getInt("id");
        }

        @Override
        public void onPostExecute(Integer b) {
            stopSelf(b);

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        ArrayList<String> a = intent.getStringArrayListExtra("files");
        b.putInt("id", startId);
        b.putStringArrayList("array", a);
        new Doback().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, b);
        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
    }

    private void publishResults(boolean b) {
        Intent intent = new Intent("loadlist");
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
