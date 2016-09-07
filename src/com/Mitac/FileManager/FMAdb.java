package com.Mitac.FileManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FMAdb extends SQLiteOpenHelper {
		  private static final int version =2;
	 	  public FMAdb(Context context){
		    	super(context,"MYADB_FILE", null, version);
		  }
		  
		@Override
		  public void onCreate(SQLiteDatabase db){
			    String sql = "CREATE TABLE IF NOT EXISTS FILESET_TABLE (_ID INTEGER PRIMARY KEY AUTOINCREMENT,SORTTYPE INTEGER,SHOWMODE INTEGER)";
			    db.execSQL(sql);
		  }

		  @Override
		  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
			    String sql = "DROP TABLE IF EXISTS FILESET_TABLE";
			    db.execSQL(sql);
			    onCreate(db);
		  }

		  /**
		   * query all the data
		   * @return
		   */
		  public Cursor getFileSet(){
			    SQLiteDatabase db = this.getReadableDatabase();
			    Cursor cursor = db.query("FILESET_TABLE", null, null, null, null, null, null);
			    return cursor;
		  }

		  /**
		   * add item
		   * @param isFitSizePic show thumbnail or not
		   * @param isOpen       open directly or not
		   * @return
		   */
		  public long insertFileSet(int sortT,int showM){
			    SQLiteDatabase db = this.getWritableDatabase();
			    ContentValues cv = new ContentValues();
			    cv.put("SORTTYPE",sortT);
			    cv.put("SHOWMODE",showM);
			    long row = db.insert("FILESET_TABLE", null, cv);
			    return row;
		  }

		  /**
		   * update by ID
		   * @param id
		   * @param isFitSizePic  0 no  1 yes
		   * @param isOpen 0 no  1 yes
		   */
		  public long updateFileSet(int id,int sortT,int showM){
			    SQLiteDatabase db = this.getWritableDatabase();
			    String where = "_ID = ?";
			    String[] whereValue = { Integer.toString(id) };
			    ContentValues cv = new ContentValues();
			    cv.put("SORTTYPE",sortT);
			    cv.put("SHOWMODE",showM);
			    long row = db.update("FILESET_TABLE", cv, where, whereValue);
			    return row;
		  }
}

