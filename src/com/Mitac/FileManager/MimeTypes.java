package com.Mitac.FileManager;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import android.webkit.MimeTypeMap;

public class MimeTypes {
	private Map<String, String> mMimeTypes;

	  public MimeTypes() {
		  mMimeTypes = new HashMap<String,String>();
	  }
	  
	public void put(String type, String extension) {
		    // Convert extensions to lower case letters for easier comparison
		    extension = extension.toLowerCase();
		    
		    mMimeTypes.put(type, extension);
	  }
	  
	  public String getMimeType(String filename) {
		   	String mimetype = "*/*";
		    String extension ="";
		    if(filename.lastIndexOf(".")>0){
		    	extension= filename.substring(filename.lastIndexOf("."),filename.length()).toLowerCase(); 
		    	extension = extension.toLowerCase();
		    	// Let's check the official map first. Webkit has a nice extension-to-MIME map.
				// Be sure to remove the first character from the extension, which is the "." character.
				if (extension.length() > 0) {
				      String webkitMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1));
                      // +frank527.lin # 02.21.2014 # fix bugid: N435_SC4641, N435_SC4642 mkv and flac issue
                      String localMimeType = mMimeTypes.get(extension);
                      if (extension.equals(".flac")) {
                          return localMimeType; 
				      } 
                      // -frank527.lin # 02.21.2014 # fix bugid: N435_SC4641, N435_SC4642 mkv and flac issue
                      if (webkitMimeType != null) {
				        // Found one. Let's take it!
				    	  Log.i("FileManager","default mimetype : " + webkitMimeType);
				    	  return webkitMimeType;
				      }
				}
				mimetype = mMimeTypes.get(extension);
				if(mimetype==null || mimetype.length()==0)
				{
					mimetype ="*/*";
				}
		    }
		    return mimetype;
	  }  
}
