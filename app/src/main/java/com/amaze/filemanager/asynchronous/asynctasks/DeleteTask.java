/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.asynchronous.asynctasks;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.operations.singlefile.DeleteOperation;
import com.amaze.filemanager.filesystem.operations.Operator;
import com.amaze.filemanager.fragments.CompressedExplorerFragment;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.notifications.NotificationConstants;

import java.util.ArrayList;

public class DeleteTask extends AsyncTask<ArrayList<HybridFileParcelable>, String, Boolean> {

    private ArrayList<HybridFileParcelable> files;
    private Context context;
    private boolean rootMode;
    private CompressedExplorerFragment compressedExplorerFragment;

    public DeleteTask(Context context) {
        this.context = context;
        rootMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
    }

    public DeleteTask(Context context, CompressedExplorerFragment compressedExplorerFragment) {
        this.context = context;
        rootMode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
        this.compressedExplorerFragment = compressedExplorerFragment;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Toast.makeText(context, values[0], Toast.LENGTH_SHORT).show();
    }

    protected Boolean doInBackground(ArrayList<HybridFileParcelable>... files) {
        if(files == null || files[0] == null || files.length != 1) {
            throw new IllegalArgumentException("execute() parameter must be one list!");
        }

        boolean success = true;

        this.files = files[0];

        for (HybridFileParcelable file : this.files) {
            Operator deleteOperation = new Operator(new DeleteOperation(context, rootMode, file));
            deleteOperation.start();
            success = success && deleteOperation.hasFailed();
        }

        return success;
    }

    @Override
    public void onPostExecute(Boolean wasDeleted) {
        Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
        String path = files.get(0).getParent(context);
        intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, path);
        context.sendBroadcast(intent);

        if (!wasDeleted) {
            Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
        } else if (compressedExplorerFragment == null) {
            Toast.makeText(context, context.getResources().getString(R.string.done), Toast.LENGTH_SHORT).show();
        }

        if (compressedExplorerFragment!=null) {
            compressedExplorerFragment.files.clear();
        }

        // cancel any processing notification because of cut/paste operation
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NotificationConstants.COPY_ID);
    }
}



