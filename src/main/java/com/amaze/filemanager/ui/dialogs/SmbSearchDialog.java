package com.amaze.filemanager.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.RecyclerArrayAdapter;
import com.amaze.filemanager.utils.Computer;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.SubnetScanner;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arpitkh996 on 16-01-2016.
 */
public class SmbSearchDialog extends DialogFragment {
    private UtilitiesProviderInterface utilsProvider;

    listViewAdapter listViewAdapter;
    ArrayList<Computer> computers = new ArrayList<>();
    SharedPreferences sharedPrefs;
    int fabskin;
    SubnetScanner subnetScanner;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        utilsProvider = (UtilitiesProviderInterface) getActivity();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        fabskin = Color.parseColor(PreferenceUtils.getAccentString(sharedPrefs));
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (subnetScanner != null)
            subnetScanner.interrupt();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.title(R.string.searchingdevices);
        builder.negativeColor(fabskin);
        builder.negativeText(R.string.cancel);
        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                if (subnetScanner != null)
                    subnetScanner.interrupt();
                dismiss();
            }
        });
        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                if (subnetScanner != null)
                    subnetScanner.interrupt();
                if (getActivity() != null && getActivity() instanceof MainActivity) {
                    dismiss();
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.showSMBDialog("", "", false);
                }
            }
        });
        builder.positiveText(R.string.use_custom_ip);
        builder.positiveColor(fabskin);
        computers.add(new Computer("-1", "-1"));
        listViewAdapter = new listViewAdapter(getActivity(), R.layout.smb_computers_row, computers);
        subnetScanner = new SubnetScanner(getActivity());
        subnetScanner.setObserver(new SubnetScanner.ScanObserver() {
            @Override
            public void computerFound(final Computer computer) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!computers.contains(computer))
                                computers.add(computers.size() - 1, computer);
                            listViewAdapter.notifyDataSetChanged();
                        }
                    });
            }

            @Override
            public void searchFinished() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (computers.size() == 1) {
                                dismiss();
                                Toast.makeText(getActivity(), R.string.nodevicefound, Toast.LENGTH_SHORT).show();
                                MainActivity mainActivity = (MainActivity) getActivity();
                                mainActivity.showSMBDialog("", "", false);
                                return;
                            }
                            computers.remove(computers.size() - 1);
                            listViewAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        subnetScanner.start();

        builder.adapter(listViewAdapter, null);
        return builder.build();
    }

    private class listViewAdapter extends RecyclerArrayAdapter<Computer, listViewAdapter.ViewHolder> {
        private static final int VIEW_PROGRESSBAR = 1;
        private static final int VIEW_ELEMENT = 2;

        LayoutInflater mInflater;
        Context context;

        public listViewAdapter(Context context, @LayoutRes int resource, List<Computer> objects) {
            this.context = context;
            addAll(objects);
            mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_PROGRESSBAR:
                    ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyle);
                    progressBar.setIndeterminate(true);
                    progressBar.setBackgroundDrawable(null);

                    return new ViewHolder(progressBar);
                default:
                case VIEW_ELEMENT:
                    View view = mInflater.inflate(R.layout.smb_computers_row, parent, false);

                    return new ElementViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.render(position, getItem(position));
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }

            public void render(final int p, Computer f) {

            }
        }

        class ElementViewHolder extends ViewHolder {
            private View rootView;

            private ImageView image;
            private TextView txtTitle;
            private TextView txtDesc;

            ElementViewHolder(View view) {
                super(view);

                rootView = view;

                txtTitle = (TextView) view.findViewById(R.id.firstline);
                image = (ImageView) view.findViewById(R.id.icon);
                txtDesc = (TextView) view.findViewById(R.id.secondLine);
            }

            @Override
            public void render(final int p, Computer f) {
                rootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (subnetScanner != null)
                            subnetScanner.interrupt();
                        if (getActivity() != null && getActivity() instanceof MainActivity) {
                            dismiss();
                            MainActivity mainActivity = (MainActivity) getActivity();
                            mainActivity.showSMBDialog(listViewAdapter.getItem(p).name, listViewAdapter.getItem(p).addr, false);
                        }
                    }
                });

                txtTitle.setText(f.name);
                image.setImageResource(R.drawable.ic_settings_remote_white_48dp);
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                    image.setColorFilter(Color.parseColor("#666666"));
                txtDesc.setText(f.addr);
            }
        }

        @Override
        public int getItemViewType(int position) {
            Computer f = getItem(position);
            if (f.addr.equals("-1")) {
                return VIEW_PROGRESSBAR;
            } else {
                return VIEW_ELEMENT;
            }
        }

    }

}
