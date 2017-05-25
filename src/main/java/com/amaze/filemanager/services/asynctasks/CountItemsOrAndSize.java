package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.text.format.Formatter;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.files.Futils;
import com.amaze.filemanager.utils.OnProgressUpdate;

/**
 * @author Emmanuel
 *         on 12/5/2017, at 19:40.
 */

public class CountItemsOrAndSize extends AsyncTask<Void, Pair<Integer, Long>, String> {

    private Context context;
    private TextView itemsText;
    private BaseFile file;
    private boolean isStorage;

    public CountItemsOrAndSize(Context c, TextView itemsText, BaseFile f, boolean storage) {
        this.context = c;
        this.itemsText = itemsText;
        file = f;
        isStorage = storage;
    }

    @Override
    protected String doInBackground(Void[] params) {
        String items = "";
        long fileLength = file.length(context);

        if (file.isDirectory(context)) {
            final int x = file.listFiles(context, false).size();
            long folderSize;

            if(isStorage) {
                folderSize = file.getUsableSpace();
            } else {
                folderSize = Futils.folderSize(file, new OnProgressUpdate<Long>() {
                    @Override
                    public void onUpdate(Long data) {
                        publishProgress(new Pair<>(x, data));
                    }
                });
            }

            items = getText(x, folderSize, false);
        } else {
            items = Formatter.formatFileSize(context, fileLength) + (" (" + fileLength + " "
                    + context.getResources().getQuantityString(R.plurals.bytes, (int) fileLength) //truncation is insignificant
                    + ")");
        }

        return items;
    }

    @Override
    protected void onProgressUpdate(Pair<Integer, Long>[] dataArr) {
        Pair<Integer, Long> data = dataArr[0];

        itemsText.setText(getText(data.first, data.second, true));
    }

    private String getText(int filesInFolder, long length, boolean loading) {
        String numOfItems = (filesInFolder != 0? filesInFolder + " ":"")
                + context.getResources().getQuantityString(R.plurals.items, filesInFolder) ;

        return numOfItems + "; " + (loading? ">":"") + Formatter.formatFileSize(context, length);
    }

    protected void onPostExecute(String items) {
        itemsText.setText(items);
    }
}
