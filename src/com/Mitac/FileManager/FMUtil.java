package com.Mitac.FileManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.net.Uri;
import android.content.Intent;
import android.os.Environment;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.Mitac.FileManager.R;
//+ shin.chu 2014.8.1
import java.text.SimpleDateFormat;
import java.util.Date; 
//- shin.chu 2014.8.1

public class FMUtil {
	private static MimeTypes mMimeTypes;
	  
	  public static void getMimeTypes(Context cx) {
	    MimeTypeParser mtp = new MimeTypeParser();
	    XmlResourceParser in = cx.getResources().getXml(R.xml.mimetypes);
	    
	    try {
	    		mMimeTypes = mtp.fromXmlResource(in);
	    } catch (XmlPullParserException e) {
	    		throw new RuntimeException("PreselectedChannelsActivity: XmlPullParserException");
	    } catch (IOException e) {
	    		throw new RuntimeException("PreselectedChannelsActivity: IOException");
	    }
	  }
	  
	   /**
	   * get mimetype 
	   * @param f
	   * @param isOpen for open is true; for icon show is false
	   * @return
	   */
	  public static String getMIMEType(File f,boolean isOpen){
	    String type="";
	    String fName=f.getName();
	    String end ="";
	    if(fName.lastIndexOf(".")>0)
	    {
	    	/* get the extand name */
	    	end=fName.substring(fName.lastIndexOf(".")+1,fName.length()).toLowerCase(); 
	    	String t = mMimeTypes.getMimeType(fName);
	    	if(isOpen){
		        return t;
		    }else if(t.length()>6){
		          if(t.substring(0,5).equalsIgnoreCase("audio")){
		            type = "audio"; 
		          }else if(t.substring(0,5).equalsIgnoreCase("video")){
		            type = "video";
		          }else if(t.substring(0,5).equalsIgnoreCase("image")){
		            type = "image";
		          }else if(end.equals("apk")){
		            type = "apk";
		          }else if(end.equals("zip")){
		        	type = "zip";
		          }
		    }
	    }else{
	    	if(isOpen){
	    		return "*/*";
	    	}
	    }
		return type;
	  }
	  
	  /**
	   * fit image size
	   */
	  public static Bitmap fitSizePic(File f){ 
		    Bitmap resizeBmp = null;
		    BitmapFactory.Options opts = new BitmapFactory.Options(); 
	
		    if(f.length()<20480){         //0-20k
		      opts.inSampleSize = 1;
		    }else if(f.length()<51200){   //20-50k
		      opts.inSampleSize = 2;
		    }else if(f.length()<307200){  //50-300k
		      opts.inSampleSize = 4;
		    }else if(f.length()<819200){  //300-800k
		      opts.inSampleSize = 6;
		    }else if(f.length()<1048576){ //800-1024k
		      opts.inSampleSize = 8;
		    }else{
		      opts.inSampleSize = 10;
		    }
		    resizeBmp = BitmapFactory.decodeFile(f.getPath(),opts);
		    return resizeBmp; 
	  }
	  
	  public static String byteToString(long b){
		  String  show = "";
		  int sub_index = 0;
		  if(b>=1073741824){
	            sub_index = (String.valueOf((float)(b/1073741824))).indexOf(".");
	            show = ((float)b/1073741824+"000").substring(0,sub_index+3)+"GB";
	      }else if(b>=1048576){
	            sub_index = (String.valueOf((float)(b/1048576))).indexOf(".");
	            show =((float)b/1048576+"000").substring(0,sub_index+3)+"MB";
	      }else if(b>=1024){
	            sub_index = (String.valueOf((float)(b/1024))).indexOf(".");
	            show = ((float)b/1024+"000").substring(0,sub_index+3)+"KB";
	      }else if(b<1024){
	            show = String.valueOf(b)+"B";
	      }
		  return show;
	  }

	  /**
	   * file size description
	   * @param f
	   * @return
	   */
	  static long mSize =0;
	  public static String fileSizeMsg(File f){ 
		    long length =0;
		    if(f.isFile()){
		    	length= f.length();        
		    }else{
		    	mSize =0;
		    	getFolderSize(f);
		    	length = mSize;
		    }
            //+ shin.chu 2014.8.1
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd  hh:mm:ss");
		    //return byteToString(length); //original
		    return formatter.format(new Date(f.lastModified())).toString() + "\n" + byteToString(length);
            //- shin.chu 2014.8.1
	  }
	  
	  private static void getFolderSize(File f){
		  if (f.exists()) {
		        File[] files = f.listFiles();
		        if(files!=null){
			        for (int i = 0; i < files.length; i++) {
						  if(files[i].isDirectory()){
							  getFolderSize(files[i]);
						  }else{
							  mSize = mSize + files[i].length();
						  }
			        }
		        }
		  }
		  
	  }
	  
	  /**
	   * return total size of the files list
	   * @param files
	   * @return
	   */
	  public static long getSize(List<File> files){
		  long size = 0;
		  for(File file : files){
			  if (file.isFile())
				  size+=file.length();
			  else{
				  File[] subFiles = file.listFiles();
				  if (subFiles!=null && subFiles.length>0){
					  size+=getSize(new ArrayList<File>(Arrays.asList(subFiles)));
				  }
			  }
		  }
		  return size;
	  }

	  
	  /**
	   * check the file name character
	   * @param newName
	   * @return 
	   */
	  public static boolean checkPath(String newName){
	    boolean ret = false;
	    if(newName.indexOf(File.separator)==-1){
	      ret = true;
	    }
	    return ret;
	  }
	  
	  /**
	   * get contain information
	   */
	  public static String getContainInfor(File f){
		  int files =0;
		  int folders=0;
		  File [] fs = f.listFiles();
		  if(fs!=null)
		  {
			  for(int i=0;i<fs.length;i++){
				  if(fs[i].isDirectory()){
					  folders++;
				  }else{
					  files++;
				  }
			  }
		  }
		  return files + (files>1?" files,":" file,") + folders + (folders>1?" folders":" folder");
	  }
	  
	  /**
	   * time long -> yyyy-MM-dd
	   * @param time
	   * @return
	   */
	  public static String time2String( long time ){

		// method1 --by system
		//  Calendar   cal = Calendar.getInstance(); 
		//  cal.setTimeInMillis( time ); 
		//  return cal.getTime().toLocaleString();

		// method2 -- by self define
		  if(time == 0)
		  {
			  return "Unknown";
		  }
		  SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		  Date currentTime = new Date(time);
		  return format1.format(currentTime);
	}
		
	/**
	 * Send Storage Intent to refresh the media files
	 * This will not work on Android 4.4+
	 */
    public static void sendStorageIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(Environment.getExternalStorageDirectory()));
        context.sendBroadcast(intent);
    }

}
