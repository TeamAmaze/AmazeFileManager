package com.amaze.filemanager.fragments;

import android.content.*;
import android.graphics.drawable.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.utils.*;
import de.keyboardsurfer.android.widget.crouton.*;
import java.util.*;

public class ProcessViewer extends Fragment{
	
	LinearLayout rootView ;
	Futils utils=new Futils();
	ArrayList<Integer> CopyIds=new ArrayList<Integer>();
	ArrayList<Integer> CancelledCopyIds=new ArrayList<Integer>();
	ArrayList<Integer> ExtractIds=new ArrayList<Integer>();
	ArrayList<Integer> CancelledExtractIds=new ArrayList<Integer>();
	ArrayList<Integer> ZipIds=new ArrayList<Integer>();
	ArrayList<Integer> CancelledZipIds=new ArrayList<Integer>();
	SharedPreferences Sp;IconUtils icons;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
	View	root= (ViewGroup) inflater.inflate(R.layout.processparent,
				container, false);
	rootView=(LinearLayout) root.findViewById(R.id.secondbut);
	getActivity().getActionBar().setSubtitle("Processes");
			((LinearLayout) getActivity().findViewById(R.id.buttons))
				.setVisibility(View.GONE);
			Sp=PreferenceManager.getDefaultSharedPreferences(getActivity());
			icons=new IconUtils(Sp,getActivity());
	return root;	
	}
	@Override
	public void onResume(){
		super.onResume();
		(getActivity()).registerReceiver(Copy_Receiver, new IntentFilter("copy"));
	(getActivity()).registerReceiver(Extract_Receiver, new IntentFilter("EXTRACT_CONDITION"));
		(getActivity()).registerReceiver(Zip_Receiver, new IntentFilter("ZIPPING"));
	}
	@Override
	public void onPause(){
		super.onPause();
		(getActivity()).unregisterReceiver(Copy_Receiver);
		(getActivity()).unregisterReceiver(Extract_Receiver);
    	rootView.removeAllViewsInLayout();
		CopyIds.clear();CancelledCopyIds.clear();
		ExtractIds.clear();CancelledExtractIds.clear();
		ZipIds.clear();CancelledZipIds.clear();
	}

	  private BroadcastReceiver Copy_Receiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			Bundle b=arg1.getExtras();
			if(b!=null){
				int id=b.getInt("id");
			final	Integer id1=new Integer(id);
			if(!CancelledCopyIds.contains(id1)){
				if(CopyIds.contains(id1)){
				
					boolean completed=b.getBoolean("COPY_COMPLETED",false);
					View process=rootView.findViewWithTag("copy"+id);
					  if(completed){ 	rootView.removeViewInLayout(process);CopyIds.remove(	CopyIds.indexOf(id1));}
					else{
							String name=b.getString("name");
					int p1=b.getInt("p1");
					int p2=b.getInt("p2");
					long total=b.getLong("total");
					long done=b.getLong("done");
						boolean move=b.getBoolean("move",false);
						String text=utils.getString(getActivity(),R.string.copying)+ "\n"+name+"\n"+utils.readableFileSize(done)+"/"+utils.readableFileSize(total)+"\n"+p1+"%";
						if(move){text=utils.getString(getActivity(),R.string.copying)+ "\n"+name+"\n"+utils.readableFileSize(done)+"/"+utils.readableFileSize(total)+"\n"+p1+"%";}
					  ((TextView)process.findViewById(R.id.progressText)).setText(text);
					ProgressBar p=(ProgressBar)process.findViewById(R.id.progressBar1);
					p.setProgress(p1);
					p.setSecondaryProgress(p2);}
				}else{
					View 	root=getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
					  root.setTag("copy"+id);
				    ImageButton cancel=(ImageButton) root.findViewById(R.id.delete_button);
					Drawable icon=icons.getCopyDrawable();
					boolean move=b.getBoolean("move",false);
					if(move){icon=icons.getCutDrawable();}
					((ImageView)root.findViewById(R.id.progressImage)).setImageDrawable(icon);
					  cancel.setOnClickListener(new View.OnClickListener(){

							  public void onClick(View p1)
							  {Crouton.makeText(getActivity(),utils.getString(getActivity(),R.string.stopping),Style.CONFIRM).show();
							  Intent i=new Intent("copycancel");
							  i.putExtra("id",id1);
							  getActivity().sendBroadcast(i);
								  rootView.removeView(  rootView.findViewWithTag("copy"+id1));
								  
								  CopyIds.remove(	CopyIds.indexOf(id1));
								  CancelledCopyIds.add(id1);
								  // TODO: Implement this method
							  }
						  });
					
					String name=b.getString("name");
					int p1=b.getInt("p1");
					int p2=b.getInt("p2");
				   
					String text=utils.getString(getActivity(),R.string.copying)+ "\n"+name;
					if(move){text=utils.getString(getActivity(),R.string.moving)+ "\n"+name;}
					((TextView)root.findViewById(R.id.progressText)).setText(text);
					ProgressBar p=(ProgressBar)root.findViewById(R.id.progressBar1);
					p.setProgress(p1);
					p.setSecondaryProgress(p2);
					CopyIds.add(id1);
					rootView.addView(root);
				}}
			}
		}};
	private BroadcastReceiver Extract_Receiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			Bundle b=arg1.getExtras();
			if(b!=null){
				final int id=b.getInt("id");
			
				if(!CancelledExtractIds.contains(id)){
					if(ExtractIds.contains(id)){

						boolean completed=b.getBoolean("extract_completed",false);
						View process=rootView.findViewWithTag("extract"+id);
						if(completed){ 	rootView.removeViewInLayout(process);ExtractIds.remove(ExtractIds.indexOf(id));}
						else{
							String name=b.getString("name");
							int p1=b.getInt("p1");
							
							ProgressBar p=(ProgressBar)process.findViewById(R.id.progressBar1);
							if(p1<=100){
								((TextView)process.findViewById(R.id.progressText)).setText(utils.getString(getActivity(),R.string.extracting)+"\n"+name+"\n"+p1+"%");
						
							p.setProgress(p1);}
							}
					}else{
						View 	root=getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
						root.setTag("extract"+id);
						((ImageView)root.findViewById(R.id.progressImage)).setImageDrawable(getResources().getDrawable(R.drawable.archive_blue));
						ImageButton cancel=(ImageButton) root.findViewById(R.id.delete_button);
						cancel.setOnClickListener(new View.OnClickListener(){

								public void onClick(View p1)
								{   Crouton.makeText(getActivity(),utils.getString(getActivity(),R.string.stopping),Style.CONFIRM).show();
								    Intent i=new Intent("excancel");
									i.putExtra("id",id);
									getActivity().sendBroadcast(i);
									rootView.removeView(  rootView.findViewWithTag("extract"+id));
								
								    ExtractIds.remove(ExtractIds.indexOf(id));
									CancelledExtractIds.add(id);
									// TODO: Implement this method
								}
							});
		
						String name=b.getString("name");
						int p1=b.getInt("p1");
						

						((TextView)root.findViewById(R.id.progressText)).setText(utils.getString(getActivity(),R.string.extracting)+"\n"+name);
						ProgressBar p=(ProgressBar)root.findViewById(R.id.progressBar1);
						p.setProgress(p1);
					
						ExtractIds.add(id);
						rootView.addView(root);
					}}
			}
		}};
	private BroadcastReceiver Zip_Receiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			Bundle b=arg1.getExtras();
		
			if(b!=null){
				final int id=b.getInt("id");

				if(!CancelledZipIds.contains(id)){
					if(ZipIds.contains(id)){

						boolean completed=b.getBoolean("ZIP_COMPLETED",false);
						View process=rootView.findViewWithTag("zip"+id);
						if(completed){ 	rootView.removeViewInLayout(process);ZipIds.remove(ZipIds.indexOf(id));}
						else{
							String name=b.getString("name");
							int p1=b.getInt("ZIP_PROGRESS");

							ProgressBar p=(ProgressBar)process.findViewById(R.id.progressBar1);
							if(p1<=100){
								((TextView)process.findViewById(R.id.progressText)).setText(utils.getString(getActivity(),R.string.zipping)+"\n"+name+"\n"+p1+"%");

								p.setProgress(p1);}
						}
					}else{
						View 	root=getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
						root.setTag("zip"+id);
						((ImageView)root.findViewById(R.id.progressImage)).setImageDrawable(getResources().getDrawable(R.drawable.archive_blue));
						ImageButton cancel=(ImageButton) root.findViewById(R.id.delete_button);
						cancel.setOnClickListener(new View.OnClickListener(){

								public void onClick(View p1)
								{   Crouton.makeText(getActivity(),utils.getString(getActivity(),R.string.stopping),Style.CONFIRM).show();
								    Intent i=new Intent("zipcancel");
									i.putExtra("id",id);
									getActivity().sendBroadcast(i);
									rootView.removeView(  rootView.findViewWithTag("zip"+id));
									
								    ZipIds.remove(ZipIds.indexOf(id));
									CancelledZipIds.add(id);
									// TODO: Implement this method
								}
							});

						String name=b.getString("name");
						int p1=b.getInt("ZIP_PROGRESS");


						((TextView)root.findViewById(R.id.progressText)).setText(utils.getString(getActivity(),R.string.zipping)+"\n"+name);
						ProgressBar p=(ProgressBar)root.findViewById(R.id.progressBar1);
						p.setProgress(p1);

						ZipIds.add(id);
						rootView.addView(root);
					}}
			}
		}};
}
