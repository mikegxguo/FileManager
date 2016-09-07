package com.Mitac.FileManager;

import java.io.File;

public class FMFile implements Comparable<Object>{
	/**
	 * 
	 */
	public static final byte SortTypeName =1;
	public static final byte SortTypeType =2;
	public static final byte SortTypeSize =3;
	public static final byte SortTypeTime =4;
	private static byte CurSortType = SortTypeSize;
	private File mFile = null;

	public FMFile(File file) {
		mFile = file;
	}

	public static void setCurSortType(byte curSortType) {
		CurSortType = curSortType;
	}

	public byte getCurSortType() {
		return CurSortType;
	}

	public File getFile(){
		return mFile;
	}
	
	
	public int compareTo(Object another) {
		// TODO Auto-generated method stub
		FMFile fm = (FMFile)another;
		if(CurSortType == SortTypeName) {
			return mFile.getName().compareTo(fm.getFile().getName());
		}else if(CurSortType == SortTypeType) {
			if(mFile.isDirectory() && fm.getFile().isDirectory()){
				return 0;
			}else if(mFile.isDirectory()){
				return 1;
			}else if(fm.getFile().isDirectory()){
				return -1;
			}
			String name1 = mFile.getName();
			String name2 = fm.getFile().getName();
			String end1;
			String end2;
			
			if(name1.lastIndexOf(".")<=0){
				end1="";
			}else{
				end1 = name1.substring(name1.lastIndexOf(".")+1,name1.length()).toLowerCase();
			}
			
			if(name2.lastIndexOf(".")<=0){
				end2="";
			}else{
				end2 = name2.substring(name2.lastIndexOf(".")+1,name2.length()).toLowerCase();
			}
			return end1.compareTo(end2);
		}else if(CurSortType == SortTypeSize){
			long size1 = mFile.length();
			long size2 = fm.getFile().length();
			return size1>size2?1:(size1==size2?0:-1);
		}
		else if(CurSortType == SortTypeTime){
			long time1 = mFile.lastModified();
			long time2 = fm.getFile().lastModified();
			return time1>time2?1:(time1==time2?0:-1);
		}
		return mFile.compareTo(fm.getFile());
	}
}
