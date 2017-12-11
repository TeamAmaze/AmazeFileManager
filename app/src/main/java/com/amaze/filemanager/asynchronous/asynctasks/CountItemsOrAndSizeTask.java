package com.amaze.filemanager.asynchronous.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.text.format.Formatter;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.OnProgressUpdate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Emmanuel
 *         on 12/5/2017, at 19:40.
 */

public class CountItemsOrAndSizeTask extends AsyncTask<Void, Pair<Integer, Long>, String> {

    private Context context;
    private TextView itemsText;
    private HybridFileParcelable file;
    private boolean isStorage;

    public CountItemsOrAndSizeTask(Context c, TextView itemsText, HybridFileParcelable f, boolean storage) {
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
            final AtomicInteger x = new AtomicInteger(0);
            file.forEachChildrenFile(context, false, new OnFileFound() {
                @Override
                public void onFileFound(HybridFileParcelable file) {
                    x.incrementAndGet();
                }
            });
            final int folderLength = x.intValue();
            long folderSize;

            if(isStorage) {
                folderSize = file.getUsableSpace();
            } else {
                folderSize = FileUtils.folderSize(file, new OnProgressUpdate<Long>() {
                    @Override
                    public void onUpdate(Long data) {
                        publishProgress(new Pair<>(folderLength, data));
                    }
                });
            }

            items = getText(folderLength, folderSize, false);
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
