package com.Mitac.FileManager;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.Mitac.FileManager.R;

public class BookMarkListAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private List<String> items;
	  
	  /* MyAdapter constructor  */  
	public BookMarkListAdapter(Context context,List<String> it){
		    mInflater = LayoutInflater.from(context);
		    items = it;
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
	  
	  
	  public View getView(int position,View convertView,ViewGroup par){
		    ViewHolder holder = null;
		    if(convertView == null){
			    	convertView = mInflater.inflate(R.layout.bm_list_item, null);
			    	  
			        /* init holder*/
			        holder = new ViewHolder();
			        holder.f_text = ((TextView)convertView.findViewById(R.id.text));
			        convertView.setTag(holder);
		     }else{
		    	  	holder = (ViewHolder) convertView.getTag();
		     }
		     holder.f_text.setText(items.get(position));  
		     return convertView;
	  }
	  
	  /**
	   * for improve efficiency, don't use set/get function
	   * class ViewHolder 
	   * */
	  private class ViewHolder{
		    TextView f_text;
	  }
}
