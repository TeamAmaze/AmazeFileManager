package com.amaze.filemanager.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;

import com.amaze.filemanager.database.models.explorer.Sort;

import java.util.HashSet;
import java.util.Set;

import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS;

/**
 * Created by Ning on 5/28/2018.
 */

public class SortHandler {

    private final ExplorerDatabase database;

    public static int getSortType(Context context, String path) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> onlyThisFloders = sharedPref.getStringSet(PREFERENCE_SORTBY_ONLY_THIS, new HashSet<>());
        final boolean onlyThis = onlyThisFloders.contains(path);
        final int globalSortby = Integer.parseInt(sharedPref.getString("sortby", "0"));
        if (!onlyThis) {
            return globalSortby;
        }
        SortHandler sortHandler = new SortHandler();
        Sort sort = sortHandler.findEntry(path);
        if (sort == null) {
            return globalSortby;
        }
        return sort.type;
    }

    public SortHandler() {
        database = ExplorerDatabase.getInstance();
    }

    public void addEntry(Sort sort) {
        database.sortDao().insert(sort);
    }

    public void clear(String path) {
        database.sortDao().clear(database.sortDao().find(path));
    }

    public void updateEntry(Sort oldSort, Sort newSort) {
        database.sortDao().update(newSort);
    }

    @Nullable
    public Sort findEntry(String path) {
        return database.sortDao().find(path);
    }
}
