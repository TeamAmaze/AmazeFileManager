package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.HistoryManager;


import java.io.File;
import java.util.ArrayList;


/**
 * Created by Arpit on 16-11-2014.
 */
public class HiddenAdapter extends ArrayAdapter<HFile> {
    /*Shortcuts s;*/
    Main context;Context c;
    public ArrayList<HFile> items;
    MaterialDialog materialDialog;
    boolean hide;
    ///	public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();

    public HiddenAdapter(Context c,Main context, int resourceId, ArrayList<HFile> items,MaterialDialog materialDialog,boolean hide) {
        super(c, resourceId, items);
        this.c=c;
        this.context = context;
        this.items = items;
        this.hide=hide;
        this.materialDialog=materialDialog;
    /*    s = new Shortcuts(c,"shortcut.xml");
    */}


    private class ViewHolder {
        ImageButton image;
        TextView txtTitle;
        TextView txtDesc;
        LinearLayout row;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final HFile f = items.get(position);
        //final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) c
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.bookmarkrow, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.text1);
            vholder.image = (ImageButton) view.findViewById(R.id.delete_button);
            vholder.txtDesc = (TextView) view.findViewById(R.id.text2);
            vholder.row=(LinearLayout)view.findViewById(R.id.bookmarkrow);
            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.txtTitle.setText(f.getName());
        String a=f.getReadablePath(f.getPath());
        holder.txtDesc.setText(a);
        if(hide)
            holder.image.setVisibility(View.GONE);
            holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(!f.isSmb() && f.isDirectory())
            {
                ArrayList<BaseFile> a=new ArrayList<BaseFile>();
                BaseFile baseFile=new BaseFile(items.get(p).getPath()+"/.nomedia");
                baseFile.setMode(HFile.LOCAL_MODE);
                a.add(baseFile);
                new DeleteTask(context.getActivity().getContentResolver(),c).execute((a));
            }
                DataUtils.removeHiddenFile(items.get(p).getPath());
                items.remove(items.get(p));
                notifyDataSetChanged();
            }
        });
        holder.row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDialog.dismiss();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (f.isDirectory()) {
                            context.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    context.loadlist(f.getPath(), false, -1);
                                }
                            });
                        } else {
                            if(!f.isSmb()){
                                context.getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        context.utils.openFile(new File(f.getPath()), (MainActivity) context.getActivity());
                                    }
                                });
                            }
                        }
                    }
                }).start();
            }
        });
        return view;
    }
    public void updateDialog(MaterialDialog dialog){
        materialDialog=dialog;
    }


}
