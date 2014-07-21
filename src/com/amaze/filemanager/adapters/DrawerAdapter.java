package com.amaze.filemanager.adapters;

import android.content.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.activities.*;
import com.amaze.filemanager.utils.*;

public class DrawerAdapter extends ArrayAdapter<String>
 {
  private final Context context;
  private final String[] values;
  MainActivity m;
  
  IconUtils icons;
  public DrawerAdapter(Context context, String[] values,MainActivity m,SharedPreferences Sp) {
    super(context, R.layout.rowlayout, values);
    this.context = context;
    this.values = values;
	
	icons=new IconUtils(Sp,m);
	this.m=m;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View rowView = inflater.inflate(R.layout.drawerrow, parent, false);
    TextView textView = (TextView) rowView.findViewById(R.id.firstline);
    ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
	LinearLayout l=(LinearLayout) rowView.findViewById(R.id.second);
	  l.setOnClickListener(new View.OnClickListener(){

			  public void onClick(View p1)
			  {m.selectItem(position);}
				  // TODO: Implement this method
			  
		  });
    textView.setText(values[position]);
	switch(position){
		case 0: imageView.setImageDrawable(icons.getSdDrawable());
		break;
		case 1:imageView.setImageDrawable(icons.getGridDrawable());
		break;
		case 2:imageView.setImageDrawable(icons.getBookDrawable());
		break;
		case 3: imageView.setImageDrawable(icons.getSettingDrawable());
		
	}
    // change the icon for Windows and iPhone
  

    return rowView;
  }
}
