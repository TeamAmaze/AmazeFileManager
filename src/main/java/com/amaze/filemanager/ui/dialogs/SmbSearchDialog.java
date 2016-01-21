package com.amaze.filemanager.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.utils.Computer;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.SubnetScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arpitkh996 on 16-01-2016.
 */
public class SmbSearchDialog extends DialogFragment {
    Listviewadapter listviewadapter;
    ArrayList<Computer> computers = new ArrayList<>();
    SharedPreferences Sp;
    int theme, fabskin;
    SubnetScanner subnetScanner;
    Context context;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        context=getActivity();
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        theme = PreferenceUtils.getTheme(Sp);
        fabskin = Color.parseColor(PreferenceUtils.getAccentString(Sp));
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if(subnetScanner!=null)
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
                if(getActivity()!=null && getActivity() instanceof MainActivity) {
                    dismiss();
                    MainActivity mainActivity=(MainActivity)getActivity();
                    mainActivity.showSMBDialog("","",false);
                }
                }
        });
        builder.positiveText("Use custom IP");
        builder.positiveColor(fabskin);
        computers.add(new Computer("-1", "-1"));
        listviewadapter = new Listviewadapter(getActivity(), R.layout.smb_computers_row, computers);
        subnetScanner = new SubnetScanner(getActivity());
        subnetScanner.setObserver(new SubnetScanner.ScanObserver() {
            @Override
            public void computerFound(final Computer computer) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!computers.contains(computer))
                            computers.add(computers.size() - 1, computer);
                            listviewadapter.notifyDataSetChanged();
                        }
                    });
            }

            @Override
            public void searchFinished() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(computers.size()==1){
                                dismiss();
                                Toast.makeText(getActivity(),R.string.nodevicefound,Toast.LENGTH_SHORT).show();
                                MainActivity mainActivity=(MainActivity)getActivity();
                                mainActivity.showSMBDialog("","",false);
                                return;
                            }
                            computers.remove(computers.size()-1);
                            listviewadapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        subnetScanner.start();

        builder.adapter(listviewadapter, new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {

            }
        });
        return builder.build();
    }

    private class Listviewadapter extends ArrayAdapter<Computer> {
        LayoutInflater mInflater;

        public Listviewadapter(Context context, int resource, List<Computer> objects) {
            super(context, resource, objects);
            mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        }


        private class ViewHolder {
            ImageView image;
            TextView txtTitle;
            TextView txtDesc;
            // RelativeLayout row;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            Computer f = getItem(position);
            if (f.addr.equals("-1")) {
                ProgressBar progressBar = new ProgressBar(getContext(),null,android.R.attr.progressBarStyle );
                progressBar.setIndeterminate(true);
                progressBar.setBackgroundDrawable(null);
                return progressBar;
            }
            View view;
            final int p = position;
            view = mInflater.inflate(R.layout.smb_computers_row, null);
            final ViewHolder holder = new ViewHolder();
            holder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            holder.image = (ImageView) view.findViewById(R.id.icon);
            holder.txtDesc = (TextView) view.findViewById(R.id.secondLine);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (subnetScanner != null)
                        subnetScanner.interrupt();
                    if(getActivity()!=null && getActivity() instanceof MainActivity){
                        dismiss();
                        MainActivity mainActivity=(MainActivity)getActivity();
                        mainActivity.showSMBDialog(listviewadapter.getItem(p).name,listviewadapter.getItem(p).addr,false);
                    }
                }
            });
            if (holder != null && holder.txtTitle != null) {
                holder.txtTitle.setText(f.name);
                holder.image.setImageResource(R.drawable.ic_settings_remote_white_48dp);
                if (theme == 0)
                    holder.image.setColorFilter(Color.parseColor("#666666"));
                holder.txtDesc.setText(f.addr);
            }

            return view;
        }
    }
}
