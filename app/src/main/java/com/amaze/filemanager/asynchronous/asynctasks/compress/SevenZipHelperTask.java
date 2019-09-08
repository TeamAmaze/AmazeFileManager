/*
 * SevenZipHelperTask.java
 *
 * Copyright © 2018-2019 N00byKing <N00byKing@hotmail.de>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com> and Raymond Lai <airwave209gt at gmail.com>.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.asynchronous.asynctasks.compress;

import androidx.annotation.NonNull;
import android.widget.EditText;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.filesystem.compressed.ArchivePasswordCache;
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZArchiveEntry;
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZFile;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.application.AppConfig;

import org.apache.commons.compress.PasswordRequiredException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.tukaani.xz.CorruptedInputException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

public class SevenZipHelperTask extends CompressedHelperTask {

    private String filePath, relativePath;

    private boolean paused = false;

    public SevenZipHelperTask(String filePath, String relativePath, boolean goBack,
                         OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>> l) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    @Override
    void addElements(@NonNull ArrayList<CompressedObjectParcelable> elements) throws ArchiveException {
        while(true) {
            if (paused) continue;

            try {
                SevenZFile sevenzFile = (ArchivePasswordCache.getInstance().containsKey(filePath)) ?
                        new SevenZFile(new File(filePath), ArchivePasswordCache.getInstance().get(filePath).toCharArray()) :
                        new SevenZFile(new File(filePath));

                for (SevenZArchiveEntry entry : sevenzFile.getEntries()) {
                    String name = entry.getName();
                    boolean isInBaseDir = relativePath.equals("") && !name.contains(SEPARATOR);
                    boolean isInRelativeDir = name.contains(SEPARATOR)
                            && name.substring(0, name.lastIndexOf(SEPARATOR)).equals(relativePath);

                    if (isInBaseDir || isInRelativeDir) {
                        elements.add(new CompressedObjectParcelable(entry.getName(),
                                entry.getLastModifiedDate().getTime(), entry.getSize(), entry.isDirectory()));
                    }
                }
                paused = false;
                break;
            } catch (PasswordRequiredException e) {
                paused = true;
                publishProgress(e);
            } catch (IOException e) {
                throw new ArchiveException(String.format("7zip archive %s is corrupt", filePath));
            }
        }
    }

    @Override
    protected void onProgressUpdate(IOException... values) {
        super.onProgressUpdate(values);
        if (values.length < 1) return;

        IOException result = values[0];
        //We only handle PasswordRequiredException here.
        if(result instanceof PasswordRequiredException || result instanceof CorruptedInputException)
        {
            ArchivePasswordCache.getInstance().remove(filePath);
            GeneralDialogCreation.showPasswordDialog(AppConfig.getInstance().getMainActivityContext(),
                (MainActivity)AppConfig.getInstance().getMainActivityContext(),
                AppConfig.getInstance().getUtilsProvider().getAppTheme(),
                R.string.archive_password_prompt, R.string.authenticate_password,
                ((dialog, which) -> {
                    EditText editText = dialog.getView().findViewById(R.id.singleedittext_input);
                    String password = editText.getText().toString();
                    ArchivePasswordCache.getInstance().put(filePath, password);
                    paused = false;
                    dialog.dismiss();
                }), null);
        }
    }

}
