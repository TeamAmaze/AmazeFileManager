package com.amaze.filemanager.fragments;
import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.adapters.*;
import com.amaze.filemanager.utils.*;
import de.keyboardsurfer.android.widget.crouton.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;

import android.support.v4.app.ListFragment;

public class BookmarksManager extends ListFragment
{	Futils utils=new Futils();
Shortcuts s=new Shortcuts();
	BooksAdapter b;
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		((LinearLayout) getActivity().findViewById(R.id.buttons))
			.setVisibility(View.GONE);
		ListView vl=getListView();
//		float scale = getResources().getDisplayMetrics().density;
//		int dpAsPixels = (int) (10*scale + 0.5f);
//	    getListView().setPadding(dpAsPixels,0, dpAsPixels, 0);
//	    getListView().setDivider(null);
//		getListView().setDividerHeight(dpAsPixels);
//		vl.setCacheColorHint(android.R.color.transparent);
//		vl.setSelector(android.R.color.transparent);
//		vl.setHeaderDividersEnabled(true);
//		View divider=getActivity().getLayoutInflater().inflate(R.layout.divider,null);
//		vl.addFooterView(divider);
//		vl.addHeaderView(divider);
//		vl.setFooterDividersEnabled(true);
        vl.setFastScrollEnabled(true);
		refresh();
		
	}@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.activity_extra, menu);
		hideOption(R.id.item1,menu);
		hideOption(R.id.item2,menu);
		hideOption(R.id.item3,menu);
		hideOption(R.id.item4,menu);
		hideOption(R.id.item6,menu);
		hideOption(R.id.item9,menu);
    hideOption(R.id.item11,menu);
    hideOption(R.id.item10,menu);
    hideOption(R.id.item12,menu);

		
		
	}	private void hideOption(int id, Menu menu) {
		MenuItem item = menu.findItem(id);
		item.setVisible(false);
	}public boolean onOptionsItemSelected(MenuItem item) { // Handleitem
		// selection
switch (item.getItemId()) {
		case R.id.item7:refresh();
		break;
case R.id.item5:
	
	AlertDialog.Builder ba1=new AlertDialog.Builder(getActivity());
	ba1.setTitle(utils.getString(getActivity(),R.string.addbook));
	View v=getActivity().getLayoutInflater().inflate(R.layout.dialog,null);
	final EditText edir=(EditText)v.findViewById(R.id.newname);
	edir.setHint(utils.getString(getActivity(),R.string.enterpath));
	ba1.setView(v);
	ba1.setNegativeButton(utils.getString(getActivity(),R.string.cancel), new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface p1, int p2)
			{
				// TODO: Implement this method
			}
		});
	ba1.setPositiveButton(utils.getString(getActivity(),R.string.create), new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface p1, int p2)
			{try {
				File a=new File(edir.getText().toString());
				if(a.exists())
				{s.addS(a);
				b.items.add(a);
				b.notifyDataSetChanged();
				Crouton.makeText(getActivity(),utils.getString(getActivity(),R.string.success),Style.CONFIRM).show();
				}else{
					Crouton.makeText(getActivity(),utils.getString(getActivity(),R.string.filenotexists),Style.ALERT).show();
			} }catch (Exception e) {
				// TODO Auto-generated catch block
				Crouton.makeText(getActivity(),utils.getString(getActivity(),R.string.error),Style.ALERT).show();
			}
				// TODO: Implement this method
			}
		});
		ba1.show();
		
break;}
return super.onOptionsItemSelected(item);
}public void refresh(){	try
		{ArrayList<File> bx=s.readS();
		b	=new BooksAdapter(getActivity(), R.layout.bookmarkrow, bx);
			setListAdapter(b);
		}
		catch (IOException e)
		{}
		catch (SAXException e)
		{}
		catch (ParserConfigurationException e)
		{}}
}
