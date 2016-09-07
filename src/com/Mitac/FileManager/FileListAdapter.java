package com.Mitac.FileManager;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.Mitac.FileManager.R;

public class FileListAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private Bitmap mIcon_folder;
	private Bitmap mIcon_file;
	private Bitmap mIcon_image;
	private Bitmap mIcon_audio;
	private Bitmap mIcon_video;
	private Bitmap mIcon_apk;
	private Bitmap mIcon_zip;
	private List<String> items;
	private List<String> paths;
	private List<String> sizes;
	private boolean ifChecks[];
	private int isZoom = 0;
	private boolean isList = true;
	private boolean isMarkShow = false;
	private boolean isFullPathShow = false;
	  
	  /* MyAdapter constructor  */  
	public FileListAdapter(Context context,List<String> it,List<String> pa,List<String> si,int zm,boolean isl,boolean markShow,boolean isFullPath){
		 mInflater = LayoutInflater.from(context);
		 items = it;
		 paths = pa;
		 sizes = si;
		 isZoom = zm;
		 isList = isl;
		 isMarkShow = markShow;
		 isFullPathShow = isFullPath;
		 ifChecks = new boolean[it.size()];
		 mIcon_folder = BitmapFactory.decodeResource(context.getResources(),R.drawable.folder);      //folder
		 mIcon_file = BitmapFactory.decodeResource(context.getResources(),R.drawable.file);          //file
		 mIcon_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.image);        //image
		 mIcon_audio = BitmapFactory.decodeResource(context.getResources(),R.drawable.audio);        //music
		 mIcon_video = BitmapFactory.decodeResource(context.getResources(),R.drawable.video);        //video
		 mIcon_apk = BitmapFactory.decodeResource(context.getResources(),R.drawable.apk);            //apk
		 mIcon_zip = BitmapFactory.decodeResource(context.getResources(),R.drawable.zip);            //zip   	   
	}   
	  
	  /* override functions */
	 
	  public int getCount(){
	    return items.size();
	  }

	 
	  public Object getItem(int position){
	    return items.get(position);
	  }
	  
	  
	  public long getItemId(int position){
	    return position;
	  }
	  
	  public void setCheckState(int position,boolean isChecked){
		  ifChecks[position] = isChecked;
	  }
	  
	  public void setAllCheckState(boolean isChecked){
		  for(int i=0;i<ifChecks.length;i++){
			  ifChecks[i] = isChecked;
		  }
		  this.notifyDataSetChanged();
	  }
	  
	  public View getView(int position,View convertView,ViewGroup par){
		  Bitmap bitMap = null;
		  ViewHolder holder = null;
		  if(convertView == null){
			  if(isList){
				  convertView = mInflater.inflate(R.layout.list_items, null);
			  }else{
				  convertView = mInflater.inflate(R.layout.grid_items, null);
			  }
			  /* init holder*/
			  holder = new ViewHolder();
			  holder.f_title = ((TextView) convertView.findViewById(R.id.f_title));
			  holder.f_text = ((TextView) convertView.findViewById(R.id.f_text));
			  holder.f_icon = ((ImageView) convertView.findViewById(R.id.f_icon));
			  holder.f_check = ((CheckBox)convertView.findViewById(R.id.checkbox));
			  if(isList==false){
				  holder.f_text.setVisibility(View.GONE);
			  }
			  if(isMarkShow){
				  holder.f_check.setVisibility(View.VISIBLE);
			  }
			  convertView.setTag(holder);
		  }else{
			  holder = (ViewHolder) convertView.getTag();
		  }
		  
		  File f = new File(paths.get(position).toString());
		  /*Set the text and icon of files/folders */
		  if(isFullPathShow){
			  holder.f_title.setText(f.getPath());
		  }else{
			  holder.f_title.setText(f.getName());
		  }
		  
		  holder.f_check.setChecked(ifChecks[position]);
		  
		  String f_type = FMUtil.getMIMEType(f,false);
		  if(f.isDirectory()){
			  holder.f_icon.setImageBitmap(mIcon_folder);
			  holder.f_text.setText("");
		  }else{
			  holder.f_text.setText(sizes.get(position));
			  if("image".equals(f_type)){
	              if(isZoom == 1){
		                bitMap = FMUtil.fitSizePic(f);
		                if(bitMap!=null){
		                 holder.f_icon.setImageBitmap(bitMap);
		                }else{
		                  holder.f_icon.setImageBitmap(mIcon_image);
		                }
	              }else{
	            	  	holder.f_icon.setImageBitmap(mIcon_image);
	              }
	              bitMap = null;
			  }else if("audio".equals(f_type)){
	              holder.f_icon.setImageBitmap(mIcon_audio);
			  }else if("video".equals(f_type)){
	              holder.f_icon.setImageBitmap(mIcon_video);
			  }else if("apk".equals(f_type)){
	              holder.f_icon.setImageBitmap(mIcon_apk);
			  }else if("zip".equals(f_type)){
	              holder.f_icon.setImageBitmap(mIcon_zip);
			  }else{
	              holder.f_icon.setImageBitmap(mIcon_file);
			  }          
		  }
		  return convertView;
	  }
	  
	  /**
	   * for improve efficiency, don't use set/get function
	   * class ViewHolder 
	   * */
	  private class ViewHolder{
		    TextView f_title;
		    TextView f_text;
		    ImageView f_icon;
		    CheckBox f_check;
	  }
}
