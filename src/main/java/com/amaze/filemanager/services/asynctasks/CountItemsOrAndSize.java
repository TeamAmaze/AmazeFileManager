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

public class CountItemsOrAndSize extends AsyncTask<Void, Void, String> {

    private Context context;
    private TextView itemsText;
    private BaseFile file;

    public CountItemsOrAndSize(Context c, TextView itemsText, BaseFile f) {
        this.context = c;
        this.itemsText = itemsText;
        file = f;
    }

    @Override
    protected String doInBackground(Void[] params) {
        String items = "";
        long fileLength = file.length(context);

        if (file.isDirectory(context)) {
            int x = file.listFiles(context, false).size();
            items = x + " " + context.getResources().getQuantityString(R.plurals.items, x) + "; "
                    + Formatter.formatFileSize(context, fileLength);
        } else {
            items = Formatter.formatFileSize(context, fileLength) + (" (" + fileLength + " "
                    + context.getResources().getQuantityString(R.plurals.bytes, (int) fileLength) //truncation is insignificant
                    + ")");
        }

        return items;
    }

    protected void onPostExecute(String items) {
        itemsText.setText(items);
    }
}
