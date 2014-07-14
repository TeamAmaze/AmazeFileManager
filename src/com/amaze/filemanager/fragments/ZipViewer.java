package com.amaze.filemanager.fragments;

import android.os.*;
import android.support.v4.app.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.utils.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.amaze.filemanager.adapters.*;
import android.widget.*;

public class ZipViewer extends ListFragment
//under construction dont read
{	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		File f=new File("/storage/emulated/0/Download/bootanimation.zip");
		new LoadListTask().execute(f);
		Toast.makeText(getActivity(),""+f.length(),Toast.LENGTH_SHORT).show();
	}	class LoadListTask extends AsyncTask<File, Void, ArrayList<ZipEntry>> {

		private File f;
        
		public LoadListTask() {
			
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		// Actual download method, run in the task thread
		protected ArrayList<ZipEntry> doInBackground(File... params) {
			// params comes from the execute() call: params[0] is the url.
		ArrayList<ZipEntry> elements=new ArrayList<ZipEntry>();
		try
			{
				ZipFile zipfile = new ZipFile(params[0]);
	
          //  int fileCount = zipfile.size();
            for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
           ZipEntry entry = (ZipEntry) e.nextElement();
		  elements.add(entry);
}
				}
				catch (IOException e)
				{}
return elements;
		}

		@Override
		// Once the image is downloaded, associates it to the imageView
		protected void onPostExecute(ArrayList<ZipEntry> bitmap) {
			ZipAdapter z=new ZipAdapter(getActivity(),R.layout.simplerow,bitmap);
			setListAdapter(z);
				
				}
				
				}
}
