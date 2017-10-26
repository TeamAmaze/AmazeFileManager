package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.OpenMode;

import java.io.File;
import java.util.ArrayList;


/**
 * Created by Arpit on 16-11-2014.
 */
public class HiddenAdapter extends RecyclerArrayAdapter<HybridFile, HiddenAdapter.ViewHolder> {

    private SharedPreferences sharedPrefs;
    private MainFragment context;
    private Context c;
    public ArrayList<HybridFile> items;
    private MaterialDialog materialDialog;
    private boolean hide;
    private DataUtils dataUtils = DataUtils.getInstance();
    ///	public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();

    public HiddenAdapter(Context context, MainFragment mainFrag,  SharedPreferences sharedPreferences,
                                @LayoutRes int layoutId, ArrayList<HybridFile> items,
                                MaterialDialog materialDialog, boolean hide) {
        addAll(items);
        this.c = context;
        this.context = mainFrag;
        sharedPrefs = sharedPreferences;
        this.items = items;
        this.hide = hide;
        this.materialDialog = materialDialog;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) c
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = mInflater.inflate(R.layout.bookmarkrow, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.render(position, getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageButton image;
        private TextView txtTitle;
        private TextView txtDesc;
        private LinearLayout row;

        ViewHolder(View view) {
            super(view);

            txtTitle = (TextView) view.findViewById(R.id.text1);
            image = (ImageButton) view.findViewById(R.id.delete_button);
            txtDesc = (TextView) view.findViewById(R.id.text2);
            row = (LinearLayout) view.findViewById(R.id.bookmarkrow);
        }

        void render(final int position, final HybridFile file) {
            txtTitle.setText(file.getName());
            String a = file.getReadablePath(file.getPath());
            txtDesc.setText(a);

            if (hide)
                image.setVisibility(View.GONE);

            // TODO: move the listeners to the constructor
            image.setOnClickListener(view -> {
                if (!file.isSmb() && file.isDirectory()) {
                    ArrayList<HybridFileParcelable> a1 = new ArrayList<>();
                    HybridFileParcelable baseFile = new HybridFileParcelable(items.get(position).getPath() + "/.nomedia");
                    baseFile.setMode(OpenMode.FILE);
                    a1.add(baseFile);
                    new DeleteTask(context.getActivity().getContentResolver(), c).execute((a1));
                }
                dataUtils.removeHiddenFile(items.get(position).getPath());
                items.remove(items.get(position));
                notifyDataSetChanged();
            });
            row.setOnClickListener(view -> {
                materialDialog.dismiss();
                new Thread(() -> {
                    if (file.isDirectory()) {
                        context.getActivity().runOnUiThread(() -> {
                            context.loadlist(file.getPath(), false, OpenMode.UNKNOWN);
                        });
                    } else {
                        if (!file.isSmb()) {
                            context.getActivity().runOnUiThread(() -> {
                                FileUtils.openFile(new File(file.getPath()), (MainActivity) context.getActivity(), sharedPrefs);
                            });
                        }
                    }
                }).start();
            });
        }
    }

    public void updateDialog(MaterialDialog dialog) {
        materialDialog = dialog;
    }
}
