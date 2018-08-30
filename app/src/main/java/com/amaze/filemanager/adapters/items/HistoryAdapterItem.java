package com.amaze.filemanager.adapters.items;

import android.view.View;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.holders.HiddenViewHolder;
import com.amaze.filemanager.adapters.holders.HistoryViewHolder;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.files.FileUtils;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapterItem extends AbstractFlexibleItem<HistoryViewHolder> {

    private final DataUtils dataUtils = DataUtils.getInstance();

    private final MainActivity mainActivity;
    private final MainFragment mainFragment;
    private final MaterialDialog materialDialog;

    private final int id;
    private final HybridFile hybridFile;

    public HistoryAdapterItem(MainActivity mainActivity, MainFragment mainFragment, MaterialDialog materialDialog,
                             int id, HybridFile hybridFile) {
        this.mainActivity = mainActivity;
        this.mainFragment = mainFragment;
        this.materialDialog = materialDialog;
        this.id = id;
        this.hybridFile = hybridFile;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof HistoryAdapterItem) {
            HistoryAdapterItem inItem = (HistoryAdapterItem) other;
            return this.id == inItem.id;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return id * 37 + 37;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.history_item;
    }

    @Override
    public HistoryViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new HistoryViewHolder(adapter, view);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, HistoryViewHolder holder, int position, List<Object> payloads) {
        HybridFile file = hybridFile;

        holder.txtTitle.setText(file.getName());
        String a = file.getReadablePath(file.getPath());
        holder.txtDesc.setText(a);

        holder.row.setOnClickListener(view -> {
            materialDialog.dismiss();
            new Thread(() -> {
                if (file.isDirectory()) {
                    mainActivity.runOnUiThread(() -> {
                        mainFragment.loadlist(file.getPath(), false, OpenMode.UNKNOWN);
                    });
                } else {
                    if (!file.isSmb()) {
                        mainActivity.runOnUiThread(() -> {
                            FileUtils.openFile(new File(file.getPath()), mainActivity, mainActivity.getPrefs());
                        });
                    }
                }
            }).start();
        });
    }
}
