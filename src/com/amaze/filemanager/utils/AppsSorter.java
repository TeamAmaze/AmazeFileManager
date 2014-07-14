package com.amaze.filemanager.utils;

import android.content.pm.*;
import java.util.*;

public class AppsSorter implements Comparator<ApplicationInfo>
{PackageManager p;
public AppsSorter(PackageManager p){this.p=p;}

	public int compare(ApplicationInfo p1, ApplicationInfo p2)
	{	return 1 *    p1.loadLabel(p).toString().compareToIgnoreCase(p2.loadLabel(p).toString());
		// TODO: Implement this method
		
	}
	
}
