package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
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
import java.io.File;
import java.util.ArrayList;


/**
 * This Adapter contains all logic related to showing the list of hidden files.
 *
 * Created by Arpit on 16-11-2014 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>.
 *
 * @author Bowie Chen on 2019-10-26.
 * @see com.amaze.filemanager.adapters.holders.HiddenViewHolder
 */
public class HiddenAdapter extends RecyclerView.Adapter<HiddenViewHolder> {
    private static final String TAG = "HiddenAdapter";

    private SharedPreferences sharedPrefs;
    private MainFragment mainFragment;
    private Context context;
    private ArrayList<HybridFile> hiddenFiles;
    private MaterialDialog materialDialog;
    private boolean hide;
    private DataUtils dataUtils = DataUtils.getInstance();

    public HiddenAdapter(Context context, MainFragment mainFrag, SharedPreferences sharedPreferences,
                         ArrayList<HybridFile> hiddenFiles, MaterialDialog materialDialog, boolean hide) {
        this.context = context;
        this.mainFragment = mainFrag;
        sharedPrefs = sharedPreferences;
        this.hiddenFiles = new ArrayList<>(hiddenFiles);
        this.hide = hide;
        this.materialDialog = materialDialog;
    }

    @Override
    @NonNull
    public HiddenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = mInflater.inflate(R.layout.bookmarkrow, parent, false);

        return new HiddenViewHolder(view);
    }

    @Override
    @SuppressWarnings("unchecked") // suppress varargs warnings
    public void onBindViewHolder(HiddenViewHolder holder, int position) {
        HybridFile file = hiddenFiles.get(position);

        holder.textTitle.setText(file.getName(context));
        holder.textDescription.setText(file.getReadablePath(file.getPath()));

        if (hide) {
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(view -> {
            // if the user taps on the delete button, un-hide the file.
            // TODO: the "hide files" feature just hide files from view in Amaze and not create .nomedia

            if (!file.isSmb() && file.isDirectory(context)) {
                HybridFileParcelable nomediaFile = new HybridFileParcelable(
                        hiddenFiles.get(position).getPath() + "/" + FileUtils.NOMEDIA_FILE);
                nomediaFile.setMode(OpenMode.FILE);

                ArrayList<HybridFileParcelable> filesToDelete = new ArrayList<>();
                filesToDelete.add(nomediaFile);

                DeleteTask task = new DeleteTask(context);
                task.execute(filesToDelete);
            }

            dataUtils.removeHiddenFile(hiddenFiles.get(position).getPath());
            hiddenFiles.remove(hiddenFiles.get(position));
            notifyDataSetChanged();
        });
        holder.row.setOnClickListener(view -> {
            // if the user taps on the hidden file, take the user there.
            materialDialog.dismiss();
            new Thread(() -> {
                FragmentActivity fragmentActivity = mainFragment.getActivity();
                if (fragmentActivity == null) {
                    // nullity check
                    return;
                }

                if (file.isDirectory(context)) {
                    fragmentActivity.runOnUiThread(
                            () -> mainFragment.loadlist(file.getPath(), false, OpenMode.UNKNOWN));
                } else if (!file.isSmb()) {
                    fragmentActivity.runOnUiThread(() -> FileUtils
                            .openFile(new File(file.getPath()), (MainActivity) fragmentActivity,
                                    sharedPrefs));
                } else {
                    Log.w(TAG, "User tapped on a directory but conditions not met; nothing is done.");
                }
            }).start();
        });
    }

    public void updateDialog(MaterialDialog dialog) {
        materialDialog = dialog;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return hiddenFiles.size();
    }

}
