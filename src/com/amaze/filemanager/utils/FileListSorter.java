package com.amaze.filemanager.utils;

import java.io.*;
import java.util.*;

public class FileListSorter implements Comparator<File>
 {

	

	private int dirsOnTop = 0;
	
	private int asc=1;
int sort=0;
	public FileListSorter(int dir,int sort,int asc){
this.dirsOnTop=dir;
		this.asc=asc;
		this.sort=sort;
		
	}

	@Override
	public int compare(File file1, File file2) {

		if(dirsOnTop==0)
		{
			if(file1.isDirectory() && file2.isFile())
			{
return -1;
				
				
			}
			else if(file2.isDirectory() && (file1).isFile())
			{
return 1;}}
		else if(dirsOnTop==1){	if(file1.isDirectory() && file2.isFile())
			{
				return 1;


			}
			else if(file2.isDirectory() && (file1).isFile())
			{
				return -1;}}
		else{}
		
		if (sort == 0)
		{
			return asc * file1.getName().compareToIgnoreCase(file2.getName());
		}
		else if (sort == 1)
		{return asc * Long.valueOf(file1.lastModified()).compareTo(Long.valueOf(file2.lastModified()));}
		else if (sort == 2)
		{	if (file1.isFile() && file2.isFile())
			{
				return asc * Long.valueOf(file1.length()).compareTo(Long.valueOf(file2.length()));
			}
			else
			{	return 1 * file1.getName().compareToIgnoreCase(file2.getName());}
		}

 
	
		
return 0;
		
	}



}
