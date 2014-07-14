package com.amaze.filemanager.adapters;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.utils.*;
import java.util.*;
import java.util.zip.*;
import android.graphics.drawable.*;

public class ZipAdapter extends ArrayAdapter<ZipEntry>
{Context c;
Drawable folder,unknown;ArrayList<ZipEntry> enter;
public ZipAdapter(Context c,int id,ArrayList<ZipEntry> enter){
	super(c,id,enter);this.enter=enter;this.c=c;folder=c.getResources().getDrawable(R.drawable.ic_grid_folder1);unknown=c.getResources().getDrawable(R.drawable.ic_doc_generic_am);
	}	private class ViewHolder {
		ImageView imageView;
		TextView txtTitle;
		TextView txtDesc;
		TextView date;
		RelativeLayout rl;

	}	public View getView(int position, View convertView, ViewGroup parent) {

		final ZipEntry rowItem = enter.get(position);

		View view;
		final int p = position;
		if (convertView == null) {
			int i=R.layout.simplerow;
		
			LayoutInflater mInflater = (LayoutInflater) c
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
			view = mInflater.inflate(i, null);
			final ViewHolder vholder = new ViewHolder();

			vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
			vholder.imageView = (ImageView) view.findViewById(R.id.icon);
			vholder.rl = (RelativeLayout) view.findViewById(R.id.second);
		
			view.setTag(vholder);

		} else {
			view = convertView;

		}	final ViewHolder holder = (ViewHolder) view.getTag();
	holder.txtTitle.setText(	rowItem.getName());
	if(rowItem.isDirectory()){holder.imageView.setImageDrawable(folder);}
	else{holder.imageView.setImageDrawable(unknown);}
		holder.rl.setOnClickListener(new View.OnClickListener(){

				public void onClick(View p1)
				{
					// TODO: Implement this method
				}
			});
		return view;}
}
