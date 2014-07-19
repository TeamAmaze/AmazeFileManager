package com.amaze.filemanager.adapters;


import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.fragments.*;
import com.amaze.filemanager.utils.*;
import java.util.*;
import java.io.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import de.keyboardsurfer.android.widget.crouton.*;

public class DialogAdapter extends ArrayAdapter<File>
{Shortcuts s=new Shortcuts();
    Activity context;
    public ArrayList<File> items;
    ///	public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();

    public DialogAdapter(Activity context, int resourceId,ArrayList<File> items) {
        super(context, resourceId,items);
        this.context = context;
        this.items=items;


    }


    private class ViewHolder {
        ImageButton image;
        TextView txtTitle;
        TextView txtDesc;

    }

    public View getView(int position, View convertView, ViewGroup parent) {
        File f=items.get(position);
        //final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.bookmarkrow, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.text1);
            vholder.image= (ImageButton) view.findViewById(R.id.delete_button);
            vholder.image.setVisibility(View.GONE);
            vholder.txtDesc=(TextView) view.findViewById(R.id.text2);

            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.txtTitle.setText(f.getName());
        holder.txtDesc.setText(f.getPath());
        return view;
    }
}
