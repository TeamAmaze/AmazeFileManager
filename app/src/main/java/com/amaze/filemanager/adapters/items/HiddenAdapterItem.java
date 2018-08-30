package com.amaze.filemanager.adapters.items;

import android.view.View;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.holders.HiddenViewHolder;
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

public class HiddenAdapterItem extends AbstractFlexibleItem<HiddenViewHolder> {

    private final DataUtils dataUtils = DataUtils.getInstance();

    private final MainActivity mainActivity;
    private final MainFragment mainFragment;
    private final MaterialDialog materialDialog;

    private final int id;
    private final HybridFile hybridFile;

    public HiddenAdapterItem(MainActivity mainActivity, MainFragment mainFragment, MaterialDialog materialDialog,
                             int id, HybridFile hybridFile) {
        this.mainActivity = mainActivity;
        this.mainFragment = mainFragment;
        this.materialDialog = materialDialog;
        this.id = id;
        this.hybridFile = hybridFile;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof HiddenAdapterItem) {
            HiddenAdapterItem inItem = (HiddenAdapterItem) other;
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
        return R.layout.hidden_item;
    }

    @Override
    public HiddenViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new HiddenViewHolder(adapter, view);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, HiddenViewHolder holder, int position, List<Object> payloads) {
        HybridFile file = hybridFile;

        holder.txtTitle.setText(file.getName());
        String a = file.getReadablePath(file.getPath());
        holder.txtDesc.setText(a);

        holder.image.setOnClickListener(view -> {
            if (!file.isSmb() && file.isDirectory()) {
                ArrayList<HybridFileParcelable> a1 = new ArrayList<>();
                HybridFileParcelable baseFile = new HybridFileParcelable(hybridFile.getPath() + "/.nomedia");
                baseFile.setMode(OpenMode.FILE);
                a1.add(baseFile);
                new DeleteTask(mainActivity).execute((a1));
            }
            dataUtils.removeHiddenFile(hybridFile.getPath());
            adapter.removeItem(adapter.getCurrentItems().indexOf(this));// TODO: 30/08/18 fix
        });
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
