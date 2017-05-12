package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.BaseFile;

/**
 * @author Emmanuel
 *         on 12/5/2017, at 19:40.
 */

public class CountFolderItems extends AsyncTask<Void, Void, String> {

    private Context context;
    private TextView itemsText;
    private BaseFile file;

    public CountFolderItems(Context c, TextView itemsText, BaseFile f) {
        this.context = c;
        this.itemsText = itemsText;
        file = f;
    }

    @Override
    protected String doInBackground(Void[] params) {
        String items;

        if (file.isDirectory(context)) {
            int x = file.listFiles(context, false).size();
            items = x + " " + context.getResources().getString(x == 0 ? R.string.item : R.string.items);
        } else {
            items = Formatter.formatFileSize(context, file.length(context)) + (" (" + file.length(context) + " "
                    + context.getResources().getString(R.string.bytes).toLowerCase() + ")");
        }

        return items;
    }

    protected void onPostExecute(String items) {
        itemsText.setText(items);
    }
}
