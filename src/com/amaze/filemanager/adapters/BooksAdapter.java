package com.amaze.filemanager.adapters;
import android.app.*;
import android.preference.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.utils.*;
import de.keyboardsurfer.android.widget.crouton.*;
import java.io.*;
import java.util.*;
import com.amaze.filemanager.fragments.*;

public class BooksAdapter extends ArrayAdapter<File>
 {Shortcuts s=new Shortcuts();
		Activity context;
		public ArrayList<File> items;
		BookmarksManager b;
	///	public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();
        
		public BooksAdapter(Activity context, int resourceId,ArrayList<File> items,BookmarksManager b) {
			super(context, resourceId,items);
			this.context = context;
		this.items=items;
		this.b=b;
		}

	
		private class ViewHolder {
			ImageButton image;
			TextView txtTitle;
			TextView txtDesc;
			RelativeLayout rl;

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
			//	vholder.txtDesc = (TextView) view.findViewById(R.id.secondLine);
				vholder.txtTitle = (TextView) view.findViewById(R.id.text1);
				vholder.image= (ImageButton) view.findViewById(R.id.delete_button);
				vholder.txtDesc=(TextView) view.findViewById(R.id.text2);
			//	vholder.date = (TextView) view.findViewById(R.id.date);

				view.setTag(vholder);

			} else {
				view = convertView;

			}
			final ViewHolder holder = (ViewHolder) view.getTag();
		     holder.txtTitle.setText(f.getName());
			 holder.txtDesc.setText(f.getPath());
			 holder.image.setImageDrawable(b.icons.getCancelDrawable());
	     	holder.image.setOnClickListener(new View.OnClickListener(){

					public void onClick(View p1)
					{try{s.removeS(items.get(p),context);
					items.remove(p);notifyDataSetChanged();
					}catch(Exception e){Crouton.makeText(context,e+"",Style.INFO).show();}
						// TODO: Implement this method
					}
				});
			return view;
		}
	}
