package com.Mitac.FileManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.zip.ZipException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import com.Mitac.FileManager.R;

/**
 * 2012.10.12. Updated by David Kim
 * Fixed for Issue SC3377, SC3380 : Between PC and Tablet, creating/deleting/copying/moving doesnt' refresh.
 */

public class FileManagerActivity extends Activity  implements OnItemLongClickListener {
      private final static String DEV_TAG = "frank527.lin";
      private List<String> items = null;   //names
      private List<String> paths = null;   //paths
      private List<String> sizes = null;   //sizes
      private FMFile[] fmFiles = null;
      
      private TextView mCurPath;
      
      private ListView bmListView;
      private ListView fileListView;
      private GridView fileGridView;
      private FileListAdapter mCurAdarpter;
      
      private CheckBox mSelectAllCheckBox;
      private LinearLayout belowButtons;
      private Button belowLeftBt;
      private Button belowRightBt;
      private AlertDialog addDialog = null;
      
      protected final static int MENU_ADD =    Menu.FIRST;          //new file or folder
      protected final static int MENU_SORTING =  Menu.FIRST + 1;    //Sorting type
      protected final static int MENU_MODE =  Menu.FIRST + 2;       //list/grid mode change
      protected final static int MENU_EDIT =  Menu.FIRST + 3;       //edit
      protected final static int MENU_SEARCH =  Menu.FIRST + 5;       //more: search update
      protected final static int MENU_PROPERTY =  Menu.FIRST + 4;       //more: search update
    
      /*
      private static String[] mbookMarksPath =   {Environment.getInternalStorageDirectory().getPath(),
                                                    Environment.getExternalStorageDirectory().getPath(),
                                                    Environment.getUSBExternalStorageDirectory().getPath()};
       */
      private static String[] mbookMarksPath = new String[5]; // +frank527.lin #bugid:263 # Aug 1, 2012 # modify value from 4 to 5
      
/*    
      private final static String[] mbookMarks ={"SDCard","USB Storage","pig"};
      private final static String[] mbookMarksPath ={"/mnt/sdcard","/mnt/usbstorage","/mnt/fsl"};
    */  
      private String rootPath="";         
      
      private FMAdb db;
      private int infor_id;
      private RadioGroup sorting_type;    
     
      private byte sortingType = FMFile.SortTypeType;
      
      private final static int EDIT_COPY =1;
      private final static int EDIT_MOVE =2;
      private final static int EDIT_DELETE =3;
      private final static int EDIT_ZIP =4;
      // private final static int EDIT_SENDTO =5;
      private int mEditSelect=0;
      
      private List<File> mSelectedFiles = null;
      private boolean needDeleteOld = false;
      
      
      private ProgressDialog mNotifyDlg;
      private boolean mRun = false;
      private boolean isSearch = false;
      private boolean showSearch = false;
      private boolean mServiceCancel = false;
      
      private boolean mShowModeList = true;
      private boolean mMultiSelect = false;
      private boolean mIsShowSearchResult = false;
      
      private static final String LOGTAG = "FileManager";
      
      private int DirPrefixLenth;
      
      BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                //Log.i(LOGTAG,intent.getAction());
                //Log.i(LOGTAG,intent.getData().toString());
                if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    FileManagerActivity.this.RefreshBMList(null);           
                } else if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)
                          || intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)
                          || intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED) || intent.getAction().equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) { // +fix bugid: SC4364 # frank527.lin # Nov 22, 2013
                    FileManagerActivity.this.RefreshBMList(intent.getData().toString());
                    // +fix bugid: SC4364 remove sdcard directly while coping files will cause app crash issue # frank527.lin # Nov 22, 2013
                    if (mBoundService != null) {
                        mServiceCancel=true;
                        FileManagerActivity.this.doUnbindService();
                        Toast.makeText(FileManagerActivity.this, "external storage is removed: current service is terminated...", Toast.LENGTH_SHORT).show();
                    }
                    // -fix bugid: SC4364 # frank527.lin # Nov 22, 2013
                }
            }   
        };
      @Override
      public boolean onCreateOptionsMenu(Menu menu){
            super.onCreateOptionsMenu(menu);
            menu.add(Menu.NONE, MENU_ADD, 0, R.string.MenuAdd).setIcon(R.drawable.add);
            menu.add(Menu.NONE, MENU_SORTING, 0, R.string.MenuSorting).setIcon(R.drawable.sort);
            menu.add(Menu.NONE, MENU_MODE, 0, R.string.MenuModeChange).setIcon(R.drawable.mode);
            menu.add(Menu.NONE, MENU_EDIT, 0, R.string.MenuEdit).setIcon(R.drawable.edit);
            menu.add(Menu.NONE, MENU_SEARCH, 0, R.string.MenuSearch).setIcon(R.drawable.search);
            menu.add(Menu.NONE, MENU_PROPERTY, 0, R.string.MenuProperty).setIcon(R.drawable.prop);
            return true;
      }
      
      private final int EDIT_FINISHED =1;
      
      private Handler mHandle = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
                //String m = msg.toString();
                switch(msg.what){
                    case EDIT_FINISHED:
                        mNotifyDlg.hide();
                        //if(mServiceCancel==false){
                            if(isSearch){
                                isSearch = false;
                                List<String> res = mBoundService.getSearchResult();
                                if(res.isEmpty()){
                                    Toast.makeText(FileManagerActivity.this, getString(R.string.searchNoResult), Toast.LENGTH_SHORT).show();
                                }else{
                                    showSearchResult(res,false,false,false);
                                }
                            }else{
                                if(mBoundService.getResult()==true){
                                    if(mServiceCancel==false){
                                        Toast.makeText(FileManagerActivity.this, getString(R.string.success), Toast.LENGTH_SHORT).show();
                                    }
                                    String str = mCurPath.getText().toString().substring(DirPrefixLenth);
                                    getFileDir(str,false,false);
                                }else{
                                    if(mServiceCancel==false){
                                        Toast.makeText(FileManagerActivity.this, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            mMultiSelect = false;
                            belowButtons.setVisibility(View.GONE);
                        //}
                        doUnbindService();
                        break;
                    default:
                        break;
                }
            }
      };
      
      private class updateProgressThread implements Runnable{
            public void run() {
                // TODO Auto-generated method stub
                SystemClock.sleep(1000);
                if(mNotifyDlg != null && mNotifyDlg.isShowing())
                {
                    while(mRun){
                        if(mBoundService!=null){
                            //Log.i(LOGTAG,"service status :" + mBoundService.getStatus());
                            if(mBoundService.getStatus()== editService.status_isRunning){
                                
                            }else{
                                mHandle.sendEmptyMessageDelayed(EDIT_FINISHED, 0);
                                break;
                            }
                        }
                        SystemClock.sleep(1000);
                    }
                }
            }     
      }
      
      private editService mBoundService =null;
      private boolean mIsBound = false;
      private boolean mIsFilesServiceBound = false;
      List<File> mFilesList = null;

      private ServiceConnection mConnection = new ServiceConnection() {
          public void onServiceConnected(ComponentName className, IBinder service) {
              // This is called when the connection with the service has been
              // established, giving us the service object we can use to
              // interact with the service.  Because we have bound to a explicit
              // service that we know is running in our own process, we can
              // cast its IBinder to a concrete class and directly access it.
              mBoundService = ((editService.LocalBinder)service).getService();
              if(mIsFilesServiceBound){
                  mBoundService.setDealFilesCollection(mFilesList);
              }
              mIsFilesServiceBound = false;
          }

          public void onServiceDisconnected(ComponentName className) {
              // This is called when the connection with the service has been
              // unexpectedly disconnected -- that is, its process crashed.
              // Because it is running in our same process, we should never
              // see this happen.
              mBoundService = null;
          }
      };

      void doBindService() {
          // Establish a connection with the service.  We use an explicit
          // class name because we want a specific service implementation that
          // we know will be running in our own process (and thus won't be
          // supporting component replacement by other applications).
          //Log.i(LOGTAG,"doBindService");
          bindService(new Intent(FileManagerActivity.this,editService.class), mConnection, Context.BIND_AUTO_CREATE);
          mIsBound = true;
      }

      void doUnbindService() {
          //Log.i(LOGTAG,"doUnbindService");
          if (mIsBound) {
              // Detach our existing connection.
              unbindService(mConnection);
              mIsBound = false;
          }
      }
      
      void doStartService(Intent intend){
          startService(intend);
      }
  
      //for dialogue dismiss
      private void noDisappear(AlertDialog dlg){
          try
          {
              Field field = dlg.getClass().getSuperclass().getDeclaredField("mShowing");
              field.setAccessible(true);
              //set mShowing to false,indicate the dialogue has been closed
              field.set(dlg, false);
              dlg.dismiss();
          }
          catch (Exception e)
          {
          }
      }
      
      private void beDisappear(AlertDialog dlg){
          try
          {
              Field field = dlg.getClass().getSuperclass().getDeclaredField("mShowing");
              field.setAccessible(true);
              //reset mShowing to true,for close success
              field.set(dlg, true);
              dlg.dismiss();
          }
          catch (Exception e)
          {
              
          }
      }
      
      
      @Override
      public boolean onOptionsItemSelected(MenuItem item){
            super.onOptionsItemSelected(item);
            if(rootPath.length()==0){
                return true;
            }
            switch (item.getItemId()){
              case MENU_ADD:
                newDirOrFile();
                break;
              case MENU_SORTING:
                sorting();
                break;
              case MENU_MODE:
                modeChange(item);
                break;
              case MENU_EDIT:
                editDialog();
                break;
              case MENU_SEARCH:
                searchDialog();
                break;
              case MENU_PROPERTY:
                property();
                break;
            }
            return true;
      }
      
      private void modeChange(MenuItem item){
          if(mShowModeList==false){
              fileListView.setVisibility(View.VISIBLE);
              fileGridView.setVisibility(View.GONE);
              mShowModeList = true;
          }else{
              fileListView.setVisibility(View.GONE);
              fileGridView.setVisibility(View.VISIBLE);
              mShowModeList = false;
          }
          
          if(mIsShowSearchResult){
              showSearchResult(null,false,true,false);
          }else{
              String str = mCurPath.getText().toString().substring(DirPrefixLenth);
              getFileDir(str,mMultiSelect,true);
          }
      }
      
      
      /**
       * override onKeyDown function, for to parent directory
       */
      @Override  
      public boolean onKeyDown(int keyCode,KeyEvent event) {   
          if (keyCode == KeyEvent.KEYCODE_BACK) {
              if(mMultiSelect){
                  mSelectedFiles = null;
                  belowButtons.setVisibility(View.GONE);
                  if(mMultiSelect){
                        mMultiSelect = false;
                        if(mIsShowSearchResult){
                            showSearchResult(null,false,true,false);
                        }else{
                            getFileDir(null,false,true);
                        }
                  }
                  return true;
              }else{
                  if(mIsShowSearchResult){
                      getFileDir(mCurPath.getText().toString().substring(DirPrefixLenth),false,false);
                      return true;
                  }else{
                      if(rootPath.length()==0 || rootPath.equals(mCurPath.getText().toString().substring(DirPrefixLenth))){
                        return super.onKeyDown(keyCode,event); 
                      }else{
                        File file = new File(mCurPath.getText().toString().substring(DirPrefixLenth));
                        getFileDir(file.getParent(),false,false);
                        return true; 
                      }
                  }    
              }
          } else{  
              return super.onKeyDown(keyCode,event); 
          }
      }  

      /*
       * get device list information
       */
      private int getDeviceState(List<String> storageDevices){
          mbookMarksPath[0] ="";
          mbookMarksPath[1] ="";
          mbookMarksPath[2] ="";
          mbookMarksPath[3] ="";
          mbookMarksPath[4] ="";
          
            int i=0;
            if(Environment.getExternalStorageState().equals("mounted")){
                mbookMarksPath[i] = Environment.getExternalStorageDirectory().getPath();
                Log.i(LOGTAG, "**********************************");
                Log.i(LOGTAG, mbookMarksPath[0]);
                Log.i(LOGTAG, "**********************************");
                storageDevices.add(getString(R.string.internalStorage));
                i++;
            }
            
            
            return i;
      }
          
     @Override
      protected void onCreate(Bundle icicle){
            super.onCreate(icicle);
            setContentView(R.layout.main);
            
            DirPrefixLenth = this.getString(R.string.dirInfor).length()-2;
            bmListView = (ListView)findViewById(R.id.bookMarkList);
            List<String> storageDevices = new ArrayList<String>();
                
            bmListView.setOnItemClickListener(onItemClickBM);
            
            fileListView = (ListView)findViewById(R.id.list);
            fileGridView = (GridView)findViewById(R.id.grid);
            fileListView.setOnItemLongClickListener(this);
            fileListView.setOnItemClickListener(onItemClickL);
            fileGridView.setOnItemLongClickListener(this);
            fileGridView.setOnItemClickListener(onItemClickL);
                        
            belowButtons = (LinearLayout)findViewById(R.id.belowButton);
            belowLeftBt = (Button)findViewById(R.id.belowLeftbt);
            belowRightBt = (Button)findViewById(R.id.belowRightbt);
            
            belowButtons.setVisibility(View.GONE);
            
            db = new FMAdb(this);
            Cursor myCursor = db.getFileSet();
            byte SortType = FMFile.SortTypeName;
            int ShowMode = 0;
            
            if(myCursor.moveToFirst()){
                infor_id = myCursor.getInt(myCursor.getColumnIndex("_ID"));
                SortType = (byte) myCursor.getInt(myCursor.getColumnIndex("SORTTYPE"));
                ShowMode = myCursor.getInt(myCursor.getColumnIndex("SHOWMODE"));
                myCursor.close();
            }else{
                  db.insertFileSet(SortType, ShowMode);
                  myCursor = db.getFileSet();
                  myCursor.moveToFirst();
                  infor_id = myCursor.getInt(myCursor.getColumnIndex("_ID"));
                  myCursor.close();
            }
            
            mShowModeList = (ShowMode==0?true:false);
            sortingType = SortType;
            mCurPath = (TextView)findViewById(R.id.path_text);
            mSelectAllCheckBox = (CheckBox)findViewById(R.id.selectAll);
            mSelectAllCheckBox.setOnCheckedChangeListener(listener_selectAll);
            mNotifyDlg = new ProgressDialog(FileManagerActivity.this);
            mNotifyDlg.setOnCancelListener(new OnCancelListener(){
                public void onCancel(DialogInterface dialog) {
                    // +frank527.lin # bugid: SC3142 # 20120907
                    AlertDialog.Builder warningDialog = new AlertDialog.Builder(FileManagerActivity.this);
                    warningDialog.setTitle("Confirm Cancel");
                    warningDialog.setMessage("Do you really want to cancel it?");
                    warningDialog.setIcon(android.R.drawable.ic_dialog_alert);
                    warningDialog.setCancelable(false);
                    
                    warningDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mNotifyDlg.show();
                        }         
                    });
                    
                    warningDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(mBoundService!=null){
                                mBoundService.stopServiceThread();
                                mServiceCancel = true;
                                Toast.makeText(FileManagerActivity.this, "Service canceled!!!", Toast.LENGTH_SHORT).show();
                            }
                        }         
                    });
                    
                    warningDialog.show();
                    // -frank527.lin # bugid: SC3142 # 20120907
                    
                    // TODO Auto-generated method stub
                    /* // +frank527.lin # bugid: SC3142 # 20120907 # original
                    if(mBoundService!=null){
                        mBoundService.stopServiceThread();
                        mServiceCancel = true;
                    }
                    */ // -frank527.lin # bugid: SC3142 # 20120907 # original
                        
                    
                }});
            
            fileListView.setVisibility(mShowModeList?View.VISIBLE:View.GONE);
            fileGridView.setVisibility(mShowModeList?View.GONE:View.VISIBLE);
            
            Intent intent = getIntent();
            String path="";
            if(intent!=null && intent.getExtras()!=null){
                path = intent.getExtras().getString("path");
            }
            if(getDeviceState(storageDevices)!=0){
                rootPath=mbookMarksPath[0];
                BookMarkListAdapter ad = new BookMarkListAdapter(this,storageDevices);
                bmListView.setAdapter(ad);
                if(path==null || path.length()==0){
                    getFileDir(rootPath,false,false);
                }else{
                    File f = new File(path);
                    if(f.isDirectory()==true){
                        rootPath=path.substring(0,path.indexOf("/", 5));
                        getFileDir(path,false,false);
                    }else{
                        getFileDir(rootPath,false,false);
                    }
                }
            }
            
            FMUtil.getMimeTypes(this);
            
            IntentFilter inf = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
            inf.addDataScheme("file");
            inf.addAction(Intent.ACTION_MEDIA_EJECT);
            inf.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            // + fix bugid: SC4364 # frank527.lin # Nov 22, 2013
            inf.addAction(Intent.ACTION_MEDIA_REMOVED);
            inf.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
            // - fix bugid: SC4364 # frank527.lin # Nov 22, 2013
            
            
            //register media mount/unmount broadcast receiver
            this.registerReceiver(mReceiver, inf);   
      }
     
      private void RefreshBMList(String removePath){
          List<String> storageDevices = new ArrayList<String>();
          getDeviceState(storageDevices);           
          BookMarkListAdapter ad = new BookMarkListAdapter(this,storageDevices);
          bmListView.setAdapter(ad);
          if(removePath!=null){
              if(removePath.substring("file://".length()).equals(rootPath)){
                  rootPath=mbookMarksPath[0];
                  getFileDir(rootPath,false,false);
                  mSelectedFiles = null;
                  belowButtons.setVisibility(View.GONE);
                  if(mMultiSelect){
                        mMultiSelect = false;
                  }
              }
          }
      }
      
      @Override
      protected void onDestroy() {
         db.updateFileSet(infor_id, sortingType,mShowModeList==true?0:1);
         db.close();
         doUnbindService();
         this.unregisterReceiver(mReceiver);
         mNotifyDlg.dismiss();
         super.onDestroy();
      }
      
      
      /**
       * select all check listener
       */
      CompoundButton.OnCheckedChangeListener listener_selectAll = new CompoundButton.OnCheckedChangeListener(){

            public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                // TODO Auto-generated method stub
                if(arg1){
                    mSelectedFiles = new ArrayList<File>();
                    for(int i=0;i<fmFiles.length;i++){
                        mSelectedFiles.add(fmFiles[i].getFile());
                    }
                    //Log.i(LOGTAG,"select all");
                    mCurAdarpter.setAllCheckState(true);
                }else{
                    if(mSelectedFiles!=null){
                        mSelectedFiles.clear();
                        //Log.i(LOGTAG,"unselect all");
                    }
                    mCurAdarpter.setAllCheckState(false);
                }
            }
      };
      
     /**
       * paste button click function
       */
      Button.OnClickListener listener_paste = new Button.OnClickListener(){
            private boolean hasDuplicateFile = false; // +fix bugid: SC4365 # frank527.lin # Nov 19, 2013

            public void onClick(View arg0) {        
                String new_path = mCurPath.getText().toString().substring(DirPrefixLenth)+ File.separator;
                
                /**
                 * check if the dest path is writable
                 */
                File f = new File(new_path);
                
                /**
                 * Check there's enough space to copy
                 * + 2014.05.09 David.Kim
                 */
                long availableSize = f.getUsableSpace();
                long sourceSize = FMUtil.getSize(mSelectedFiles);
                
                // If there's not enough space then show the warning and stop.
                if (availableSize <= sourceSize){
                    new AlertDialog.Builder(FileManagerActivity.this)
                    .setTitle(R.string.paste)
                    .setIcon(R.drawable.alert)
                    .setMessage(R.string.no_space)
                    .setPositiveButton(getString(R.string.OK),
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog,int which) {
                            }
                    	}
                    ).show();
                }else{
	                if (f.canWrite() == false)
	                {
	                    Toast.makeText(FileManagerActivity.this, getString(R.string.pasteInfor4), Toast.LENGTH_SHORT).show();
	                    return;
	                }
	                
	                File f_new = null;
	                for (int i = 0; i < mSelectedFiles.size(); i++) {
	                    String src_path = mSelectedFiles.get(i).getPath();
	                           
	                    /**
	                     * check if copy/move to the same directory or a subdirectory.
	                     */
	                    //Log.i(LOGTAG,"copy/move ["+ src_path +"] to " + new_path);
	                    if (new_path.equals(mSelectedFiles.get(i).getParent() + File.separator)){
	                        Toast.makeText(FileManagerActivity.this, getString(R.string.pasteInfor1), Toast.LENGTH_SHORT).show();
	                        return;
	                    } else if(new_path.indexOf(src_path + File.separator )==0){
	                        Toast.makeText(FileManagerActivity.this, getString(R.string.pasteInfor2), Toast.LENGTH_SHORT).show();
	                        return;
	                    }
	                        
	                    // check if a same name file/folder is exist
	                    String newFullPath = new_path + mSelectedFiles.get(i).getName();
	                    
	                    f_new = new File(newFullPath);
	                    final File f_new2 = f_new; // +fix bugid: SC4365 # frank527.lin # Nov 19, 2013
	                    if (f_new.exists()) {
	                        String msg = String.format(getString(R.string.replaceInfor1), f_new.getName());
	                        // +fix bugid: SC4365 existing file or directory cannot be replaced issue # frank527.lin # Nov 19, 2013
	                        hasDuplicateFile = true;
	                        // Toast.makeText(FileManagerActivity.this,msg,Toast.LENGTH_SHORT).show(); // original
	                        
	                        new AlertDialog.Builder(FileManagerActivity.this)
	                                .setTitle(getString(R.string.confirmReplace))
	                                .setIcon(R.drawable.alert)
	                                .setMessage(msg)
	                                .setPositiveButton(getString(R.string.OK),
	                                    new DialogInterface.OnClickListener(){
	                                        public void onClick(DialogInterface dialog,int which) {
	                                            if(needDeleteOld) {
	                                                Intent itent = new Intent(FileManagerActivity.this, editService.class);
	                                                itent.putExtra("action", "move");
	                                                itent.putExtra("srcPath", "");
	                                                itent.putExtra("destPath", f_new2.getParent());
	                                                doStartService(itent);
	                                                mIsFilesServiceBound = true;
	                                                mFilesList = mSelectedFiles;
	                                                doBindService();
	                                                // show notify dialogue
	                                                mNotifyDlg.setMessage(getString(R.string.moveWaiting));
	                                                mNotifyDlg.setCancelable(true);
	                                                mNotifyDlg.show();
	                                                // create thread to check if move finished
	                                                Thread t = new Thread(new updateProgressThread());
	                                                mRun = true;
	                                                mServiceCancel = false;
	                                                t.start();
	                                            } else {
	                                                Intent itent = new Intent(FileManagerActivity.this, editService.class);
	                                                itent.putExtra("action", "copy");
	                                                itent.putExtra("srcPath", "");
	                                                itent.putExtra("destPath", f_new2.getParent());
	                                                doStartService(itent);
	                                                mIsFilesServiceBound = true;
	                                                mFilesList = mSelectedFiles;
	                                                doBindService();
	                                                // show notify dialogue
	                                                mNotifyDlg.setMessage(getString(R.string.copyWaiting));
	                                                mNotifyDlg.setCancelable(true);
	                                                mNotifyDlg.show();
	                                                // create thread to check if move finished
	                                                Thread t = new Thread(new updateProgressThread());
	                                                mRun = true;
	                                                mServiceCancel = false;
	                                                t.start();
	                                            }
	                                            belowButtons.setVisibility(View.GONE);
	                                            hasDuplicateFile = false;
	                                        }
	                                })
	                                .setNegativeButton(getString(R.string.Cancel),
	                                    new DialogInterface.OnClickListener(){
	                                        public void onClick(DialogInterface dialog, int which){
	                                            hasDuplicateFile = false;
	                                            return;
	                                        }
	                        }).show();
	                    }
	                }
	
	                if (!hasDuplicateFile) {
	                    if (needDeleteOld) {
	                        // move file in service
	                        Intent itent = new Intent(FileManagerActivity.this, editService.class);
	                        itent.putExtra("action", "move");
	                        itent.putExtra("srcPath", "");
	                        itent.putExtra("destPath", f_new.getParent());
	                        doStartService(itent);
	                        mIsFilesServiceBound = true;
	                        mFilesList = mSelectedFiles;
	                        doBindService();
	                        // show notify dialogue
	                        mNotifyDlg.setMessage(getString(R.string.moveWaiting));
	                        mNotifyDlg.setCancelable(true);
	                        mNotifyDlg.show();
	                        // create thread to check if move finished
	                        Thread t = new Thread(new updateProgressThread());
	                        mRun = true;
	                        mServiceCancel=false;
	                        t.start();
	                    } else {
	                        // copy file in service
	                        Intent itent = new Intent(FileManagerActivity.this, editService.class);
	                        itent.putExtra("action", "copy");
	                        itent.putExtra("srcPath", "");
	                        itent.putExtra("destPath", f_new.getParent());
	                        doStartService(itent);
	                        mIsFilesServiceBound = true;
	                        mFilesList = mSelectedFiles;
	                        doBindService();
	                        // show notify dialogue
	                        mNotifyDlg.setMessage(getString(R.string.copyWaiting));
	                        mNotifyDlg.setCancelable(true);
	                        mNotifyDlg.show();
	                        // create thread to check if move finished
	                        Thread t = new Thread(new updateProgressThread());
	                        mRun = true;
	                        mServiceCancel=false;
	                        t.start();                      
	                    }
	                    belowButtons.setVisibility(View.GONE);
	                }
	            }      
            }
     };
     
     /**
       * paste cancel click function
       */
      Button.OnClickListener listener_belowCancel = new Button.OnClickListener(){
            public void onClick(View arg0) {
                mSelectedFiles = null;
                belowButtons.setVisibility(View.GONE);
                if(mMultiSelect){
                    mMultiSelect = false;
                    if(mIsShowSearchResult){
                        showSearchResult(null,false,true,false);
                    }else{
                        getFileDir(null,false,true);
                    }
                }
            }      
     };
     
     
     /**
      * copyTo/moveTo listener
      */
     Button.OnClickListener listener_CopyMoveTo = new Button.OnClickListener(){
            public void onClick(View arg0) {
                if(mSelectedFiles.isEmpty()){
                    Toast.makeText(FileManagerActivity.this,getString(R.string.noSelectPrompt).toString(),Toast.LENGTH_LONG).show();
                    return;
                }
                needDeleteOld = belowLeftBt.getText().toString().equals(getString(R.string.MoveTo));
                belowLeftBt.setText(getString(R.string.paste));
                belowLeftBt.setOnClickListener(listener_paste);
                mMultiSelect = false;
                
                if(mIsShowSearchResult){
                    getFileDir(mCurPath.getText().toString().substring(DirPrefixLenth),false,false);
                }else{
                    getFileDir(null,false,true);
                }
            }      
     };
     
     /**
      * delete listener
      */
     Button.OnClickListener listener_Delete = new Button.OnClickListener(){
            public void onClick(View arg0) {
                if(mSelectedFiles.isEmpty()){
                    Toast.makeText(FileManagerActivity.this,getString(R.string.noSelectPrompt).toString(),Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(FileManagerActivity.this)
                    .setTitle(getString(R.string.confirmDel))
                    .setIcon(R.drawable.alert)
                    .setMessage(getString(R.string.deleteFilesConfirm))
                    .setPositiveButton(getString(R.string.OK),
                            new DialogInterface.OnClickListener(){
                                public void onClick(DialogInterface dialog,int which){          
                                    /* delete file or folder */
                                    // delete file in service
                                    Intent itent = new Intent(FileManagerActivity.this, editService.class);
                                    itent.putExtra("action", "delete");
                                    itent.putExtra("srcPath", "");
                                    doStartService(itent);
                                    mIsFilesServiceBound = true;
                                    mFilesList = mSelectedFiles;
                                    doBindService();
                                    // show notify dialogue
                                    mNotifyDlg.setMessage(getString(R.string.deleteWaiting));
                                    mNotifyDlg.setCancelable(true);
                                    mNotifyDlg.show();
                                    // create thread to check if move finished
                                    Thread t = new Thread(new updateProgressThread());
                                    mRun = true;
                                    mServiceCancel=false;
                                    t.start();      
                                    belowButtons.setVisibility(View.GONE);
                          }
                    })
               .setNegativeButton(getString(R.string.Cancel),
                     new DialogInterface.OnClickListener(){
                          public void onClick(DialogInterface dialog, int which){
                          }
             }).show();
                
            }      
     };
     
     /**
      * listener_Zip
      */
     Button.OnClickListener listener_Zip = new Button.OnClickListener(){
            public void onClick(View arg0) {
                if(mSelectedFiles.isEmpty()){
                    Toast.makeText(FileManagerActivity.this,getString(R.string.noSelectPrompt).toString(),Toast.LENGTH_LONG).show();
                    return;
                }
                
                LayoutInflater factory=LayoutInflater.from(FileManagerActivity.this);
                View myView=factory.inflate(R.layout.zip,null);
                final EditText myEditText=(EditText)myView.findViewById(R.id.zip_edit);
                myEditText.setText("");
                
                final AlertDialog zipDialog = new AlertDialog.Builder(FileManagerActivity.this).create();
                OnClickListener listenerOK = new DialogInterface.OnClickListener(){
                      public void onClick(DialogInterface dialog, int which){
                        /* get the modified path*/
                        final String zipName = myEditText.getText().toString()+".zip";             //get file name
                        final String pFile = mCurPath.getText().toString().substring(DirPrefixLenth) + File.separator;           //get file path
                        final String newPath = pFile+zipName;                               //get full path
                        if(!FMUtil.checkPath(zipName)){
                              Toast.makeText(FileManagerActivity.this,getString(R.string.renameInfor4),Toast.LENGTH_SHORT).show();
                              noDisappear(zipDialog);
                              return;
                        }
                        
                        final File f_new = new File(newPath);
                        if(f_new.exists()){
                                String msg = String.format(getString(R.string.renameInfor1), zipName);
                                Toast.makeText(FileManagerActivity.this, msg, Toast.LENGTH_SHORT).show();
                                noDisappear(zipDialog);
                                return;
                        }else{
                                // zip file in service
                                Intent itent = new Intent(FileManagerActivity.this, editService.class);
                                itent.putExtra("action", "zip");
                                itent.putExtra("srcPath", "");
                                itent.putExtra("destPath", f_new.getPath());
                                doStartService(itent);
                                mIsFilesServiceBound = true;
                                mFilesList = mSelectedFiles;
                                doBindService();
                                
                                // show notify dialogue
                                mNotifyDlg.setMessage(getString(R.string.zipWaiting));
                                mNotifyDlg.setCancelable(true);
                                mNotifyDlg.show();
                                // create thread to check if move finished
                                Thread t = new Thread(new updateProgressThread());
                                mRun = true;
                                mServiceCancel=false;
                                t.start();
                                
                                beDisappear(zipDialog);
                                belowButtons.setVisibility(View.GONE);
                                if(showSearch){
                                    showSearchResult(null,false,true,false);
                                }else{
                                    getFileDir(null,false,true);
                                }
                                return;
                      }};
                };
                
                /* set rename dialogue and show it */
                zipDialog.setView(myView);
                zipDialog.setTitle(getString(R.string.zipInfo2));
                zipDialog.setButton(getString(R.string.OK),listenerOK);
                zipDialog.setButton2(getString(R.string.Cancel),
                    new DialogInterface.OnClickListener(){
                      public void onClick(DialogInterface dialog, int which){
                          beDisappear(zipDialog);
                      }
                    }
                );
                zipDialog.show();
            }
     };
     
     
     /**
       * list/grip item click listener
       * no select mark
       */
     AdapterView.OnItemClickListener onItemClickBM = new AdapterView.OnItemClickListener(){
         public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                File file = new File(mbookMarksPath[position]);
                fileOrDirHandle(file,"short");
                rootPath = mbookMarksPath[position];
         };
     };
     
      /**
       * list/grip item click listener
       * no select mark
       */
     AdapterView.OnItemClickListener onItemClickL = new AdapterView.OnItemClickListener(){
         public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                File file = new File(paths.get(position));
                fileOrDirHandle(file,"short");
         };
     };
     
     /**
       * list/grip item click listener
       * select marked
       */
     AdapterView.OnItemClickListener onItemClickLWithMark = new AdapterView.OnItemClickListener(){
         public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                CheckBox ch = (CheckBox)(view.findViewById(R.id.checkbox));
                if(ch.isChecked()){
                    ch.setChecked(false);
                    mSelectedFiles.remove(new File(paths.get(position)));
                    mCurAdarpter.setCheckState(position, false);
                }else{
                    ch.setChecked(true);
                    mSelectedFiles.add(new File(paths.get(position)));
                    mCurAdarpter.setCheckState(position, true);
                }
         };
     };

      /**
       * list/grid item long click listener
       */
     
      public boolean onItemLongClick(AdapterView<?> arg0, View arg1,int arg2, long arg3){
            File file = new File(paths.get(arg2));
            fileOrDirHandle(file,"long");
            return true;
      }

      /**
       * file item clicked(long/short) function
       * @param file
       * @param flag
       */
      private void fileOrDirHandle(final File file,String flag){
        /* long click menu OnClickListener */
        OnClickListener listener_list=new DialogInterface.OnClickListener(){
          public void onClick(DialogInterface dialog,int which){
                    if(which==0){
                        copyFileOrDir(file);
                    }else if(which==1){
                        moveFileOrDir(file);
                    }else if(which==2){
                        modifyFileOrDir(file);
                    }else if(which==3){
                        delFileOrDir(file);
                    }else if(which==4){
                        /*
                         * show zip dialogue
                         */
                        zipFileOrDir(file);             
                    }else if(which==5){
                        if(file.isDirectory()){
                            /* property */
                            showProperty(file);
                        }else{
                            /* openwith function */
                            openwithFile(file);
                        }
                    }else if(which==6){
                        if(file.isFile()){
                            /* property */
                            showProperty(file);
                        }
                    }
        }};
        
        if(flag.equals("long")){
            Resources res = getResources();
            String[] menuList;
            if(file.isDirectory()){
                menuList = res.getStringArray(R.array.folderLongClickMenu);
            }else{
                menuList = res.getStringArray(R.array.fileLongClickMenu);
            }
            /* show long click menu */
            new AlertDialog.Builder(FileManagerActivity.this)
                .setTitle(file.getName())
                .setIcon(file.isDirectory()?R.drawable.folder:R.drawable.file)
                .setItems(menuList,listener_list)
                .setPositiveButton(getString(R.string.Cancel),
                 new DialogInterface.OnClickListener(){
                  public void onClick(DialogInterface dialog, int which){
                  }
                }).show();
        }else{
            /**
             * short click function
             */
            if(file.exists()){
                if(file.isDirectory()){
                  getFileDir(file.getPath(),false,false);
                }else{
                  openFile(file);
                }
            }
      }
    }  
      
      /**
       * constructor file list adapter
       * @param filePath
       */
      private void getFileDir(String filePath,boolean ifMark,boolean onlyUpdate){
          showSearch = false;
          String dirpath = filePath;
          if(filePath!=null && filePath.endsWith("/")){
              dirpath = filePath.substring(0, filePath.length()-1);
          }
            /* set current path textview */
          if(onlyUpdate==false){
                String path = String.format(getString(R.string.dirInfor),dirpath);
                mCurPath.setText(path);
                items = new ArrayList<String>();
                paths = new ArrayList<String>();
                sizes = new ArrayList<String>();
                File f = new File(filePath);  
                File[] files = f.listFiles();
                /**
                 * sort file
                 */
                fmFiles = null;
                if(files!=null){
                    fmFiles = new FMFile[files.length];
                    for(int i=0;i<files.length;i++){
                        fmFiles[i] = new FMFile(files[i]);
                    }
                    FMFile.setCurSortType(sortingType);
                    Arrays.sort(fmFiles,0,files.length);
                }
               
                if(fmFiles!=null){
                /* add all the file to ArrayList */
                    for(int i=0;i<fmFiles.length;i++){
                      if(fmFiles[i].getFile().isDirectory()){
                        items.add(fmFiles[i].getFile().getName());
                        paths.add(fmFiles[i].getFile().getPath());
                        sizes.add("");
                      }
                    }
                    for(int i=0;i<fmFiles.length;i++){
                      if(fmFiles[i].getFile().isFile()){
                        items.add(fmFiles[i].getFile().getName());
                        paths.add(fmFiles[i].getFile().getPath());
                        sizes.add(FMUtil.fileSizeMsg(fmFiles[i].getFile()));
                      }
                    }
                } 
          }
            
            /* *
             * construct adapter and and to file list view
             */
            if(mShowModeList){
                mCurAdarpter = new FileListAdapter(this,items,paths,sizes,0,true,ifMark,false);
                fileListView.setAdapter(mCurAdarpter);
                if(ifMark){
                    mSelectAllCheckBox.setChecked(false);
                    mSelectAllCheckBox.setVisibility(View.VISIBLE);
                    fileListView.setOnItemClickListener(onItemClickLWithMark);
                }else{
                    mSelectAllCheckBox.setVisibility(View.GONE);
                    fileListView.setOnItemClickListener(onItemClickL);
                }
            }else {
                mCurAdarpter = new FileListAdapter(this,items,paths,sizes,0,false,ifMark,false);
                fileGridView.setAdapter(mCurAdarpter);
                if(ifMark){
                    mSelectAllCheckBox.setChecked(false);
                    mSelectAllCheckBox.setVisibility(View.VISIBLE);
                    fileGridView.setOnItemClickListener(onItemClickLWithMark);
                }else{
                    mSelectAllCheckBox.setVisibility(View.GONE);
                    fileGridView.setOnItemClickListener(onItemClickL);
                }
            }
            mIsShowSearchResult = false;
      }
      
      /**
       * show search result list
       */
      private void showSearchResult(List<String> pathList,boolean ifMark,boolean onlyChangeMode,boolean updateSortingMode){
          showSearch = true;
          if(onlyChangeMode==false){
                  /* set current path textview */
                items = new ArrayList<String>();
                paths = new ArrayList<String>();
                sizes = new ArrayList<String>();
                 if(updateSortingMode==false){    
                        /**
                         * sort file
                         */
                        fmFiles = null;
                        if(!pathList.isEmpty()){
                            fmFiles = new FMFile[pathList.size()];
                            for(int i=0;i<pathList.size();i++){
                                fmFiles[i] = new FMFile(new File(pathList.get(i)));
                            }
                        }
                  }
              
                if(fmFiles!=null){
                    FMFile.setCurSortType(sortingType);
                    Arrays.sort(fmFiles,0,fmFiles.length);
                /* add all the file to ArrayList */
                    for(int i=0;i<fmFiles.length;i++){
                      if(fmFiles[i].getFile().isDirectory()){
                        items.add(fmFiles[i].getFile().getName());
                        paths.add(fmFiles[i].getFile().getPath());
                        sizes.add("");
                      }
                    }
                    for(int i=0;i<fmFiles.length;i++){
                      if(fmFiles[i].getFile().isFile()){
                        items.add(fmFiles[i].getFile().getName());
                        paths.add(fmFiles[i].getFile().getPath());
                        sizes.add(FMUtil.fileSizeMsg(fmFiles[i].getFile()));
                      }
                    }
                }
          }
            /* *
             * construct adapter and and to file list view
             */
            if(mShowModeList){
                mCurAdarpter = new FileListAdapter(this,items,paths,sizes,0,true,ifMark,true);
                fileListView.setAdapter(mCurAdarpter);
                if(ifMark){
                    mSelectAllCheckBox.setChecked(false);
                    mSelectAllCheckBox.setVisibility(View.VISIBLE);
                    fileListView.setOnItemClickListener(onItemClickLWithMark);
                }else{
                    mSelectAllCheckBox.setVisibility(View.GONE);
                    fileListView.setOnItemClickListener(onItemClickL);
                }
            }else {
                mCurAdarpter = new FileListAdapter(this,items,paths,sizes,0,false,ifMark,true);
                fileGridView.setAdapter(mCurAdarpter);
                if(ifMark){
                    mSelectAllCheckBox.setChecked(false);
                    mSelectAllCheckBox.setVisibility(View.VISIBLE);
                    fileGridView.setOnItemClickListener(onItemClickLWithMark);
                }else{
                    mSelectAllCheckBox.setVisibility(View.GONE);
                    fileGridView.setOnItemClickListener(onItemClickL);
                }
            }
            mIsShowSearchResult = true;
      }
      
      /**
       *  new file or folder
       */
      private void newDirOrFile(){
          if(addDialog==null){
                addDialog = new AlertDialog.Builder(FileManagerActivity.this).create();
                LayoutInflater factory = LayoutInflater.from(FileManagerActivity.this);
                /* initialize myView */
                View myView = factory.inflate(R.layout.new_alert,null);
                //final RadioButton rb_dir=(RadioButton)myView.findViewById(R.id.newdir_radio);
                //final RadioButton rb_file =(RadioButton)myView.findViewById(R.id.newfile_radio);
                //final RadioGroup radioGroup = (RadioGroup)myView.findViewById(R.id.new_radio);
                final EditText newEditText=(EditText)myView.findViewById(R.id.new_edit);
               
                /* set view to dialogue */
                addDialog.setView(myView);
                addDialog.setTitle(getString(R.string.MenuAdd));
                
                //radiogroup change listener
                /**
                radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
                
                  public void onCheckedChanged(RadioGroup group, int checkedId){
                      if(checkedId == rb_file.getId()){
                        //new_textView.setText("New file:");
                      }else if(checkedId == rb_dir.getId()){
                        //new_textView.setText("New folder:");
                      }
                    }
                });
                */
                /* new file/folder confirm dialogue */
                addDialog.setButton(getString(R.string.OK), 
                  new DialogInterface.OnClickListener(){
                  public void onClick(DialogInterface dialog, int which){
                    final String newName = newEditText.getText().toString();  //get the file name
                    // check if the filename is null or contain '/'
                    if(newName.length()==0)
                    {
                        Toast.makeText(addDialog.getContext(),getString(R.string.newInfor1).toString(),Toast.LENGTH_LONG).show();
                        noDisappear(addDialog);
                        return;
                    }
                    else if(newName.indexOf('/')>=0)
                    {
                        Toast.makeText(addDialog.getContext(),getString(R.string.newInfor2).toString(),Toast.LENGTH_LONG).show();
                        noDisappear(addDialog);
                        return;
                    }
                    
                    //final int checkedId = radioGroup.getCheckedRadioButtonId();       
                    final String newPath = mCurPath.getText().toString().substring(DirPrefixLenth) + File.separator + newName;  //new file full path
                    final File f_new =new File(newPath);
                    
                    // check if the file has been exist
                    if(f_new.exists()){
                        String msg = String.format(getString(R.string.newInfor3), newName);
                        Toast.makeText(addDialog.getContext(),msg,Toast.LENGTH_LONG).show();
                        noDisappear(addDialog);
                        return;
                    }else{
                        // confirm if create?
                        String msg = String.format(getString(R.string.newConfirmMsg),newName);
                        new AlertDialog.Builder(FileManagerActivity.this)
                              .setTitle(getString(R.string.Notice))
                              .setIcon(R.drawable.alert)
                              .setMessage(msg)
                              .setPositiveButton(getString(R.string.OK),
                               new DialogInterface.OnClickListener(){
                                  public void onClick(DialogInterface dialog,int which){
                                
                                   // if(checkedId == rb_dir.getId()){                                      
                                          if(f_new.mkdirs()){
                                            Toast.makeText(FileManagerActivity.this,getString(R.string.success),Toast.LENGTH_SHORT).show();
                                            getFileDir(f_new.getParent(),false,false);
                                            //Send Storgae mounted broadcast to rescan the files or folders
                                            FMUtil.sendStorageIntent(FileManagerActivity.this);
                                          }else{
                                            Toast.makeText(FileManagerActivity.this,getString(R.string.failed),Toast.LENGTH_SHORT).show();
                                          }                                     
                                    /**}else{                                    
                                          if(newFile(f_new)){
                                            Toast.makeText(FileManagerActivity.this,getString(R.string.success),Toast.LENGTH_SHORT).show();
                                            getFileDir(f_new.getParent(),false);
                                            mMultiSelect =false;
                                          }else{
                                            Toast.makeText(FileManagerActivity.this,getString(R.string.failed),Toast.LENGTH_SHORT).show();
                                          }             
                                    }
                                    */
                                  }
                              })
                              .setNegativeButton(getString(R.string.Cancel),
                               new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog,int which){
                               }
                              }).show();
                        beDisappear(addDialog);
                      }
                  }});
                addDialog.setButton2(getString(R.string.Cancel),
                      new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which){
                            beDisappear(addDialog);
                        }
                     });
          }
          addDialog.show();
       }
      
      /**
       * rename function
       * @param f
       */
      private void modifyFileOrDir(File f) {
            final File f_old = f;
            LayoutInflater factory=LayoutInflater.from(FileManagerActivity.this);
            View myView=factory.inflate(R.layout.rename_alert,null);
            final EditText myEditText=(EditText)myView.findViewById(R.id.rename_edit);
            myEditText.setText(f_old.getName());
            final AlertDialog renameDialog = new AlertDialog.Builder(FileManagerActivity.this).create();
            
            OnClickListener listenerFileEdit = new DialogInterface.OnClickListener(){
              public void onClick(DialogInterface dialog, int which){
                /* get the modified path*/
                final String modName = myEditText.getText().toString();             //get file name
                final String pFile = f_old.getParentFile().getPath() + File.separator;           //get file path
                final String newPath = pFile+modName;                               //get full path
                if(!FMUtil.checkPath(modName)){
                      Toast.makeText(FileManagerActivity.this,getString(R.string.renameInfor4),Toast.LENGTH_SHORT).show();
                      noDisappear(renameDialog);
                      return;
                }
                final File f_new = new File(newPath);
                if(f_new.exists()){
                    if(!modName.equals(f_old.getName())){
                        String msg = String.format(getString(R.string.renameInfor1), modName);
                        Toast.makeText(FileManagerActivity.this, msg, Toast.LENGTH_SHORT).show();
                        noDisappear(renameDialog);
                        return;
                    }else{
                        Toast.makeText(FileManagerActivity.this,getString(R.string.renameInfor2),Toast.LENGTH_SHORT).show();
                        noDisappear(renameDialog);
                        return;
                    }
                }else{
                          String msg = String.format(getString(R.string.renameInfor3), f_old.getName(),modName);
                          new AlertDialog.Builder(FileManagerActivity.this)
                            .setTitle(getString(R.string.ConfirmRename))
                            .setIcon(R.drawable.alert)
                            .setMessage(msg)
                            .setPositiveButton(getString(R.string.OK),
                                  new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog,int which){
                                              String oldPath = f_old.getPath();
                                              if(f_old.renameTo(f_new)){
                                                Intent itent = new Intent(FileManagerActivity.this, editService.class);
                                                itent.putExtra("action", "scanMedia");
                                                itent.putExtra("srcPath", oldPath);
                                                itent.putExtra("destPath", f_new.getPath());
                                                doStartService(itent);
                                                Toast.makeText(FileManagerActivity.this,getString(R.string.success),Toast.LENGTH_SHORT).show();
                                                /* reload file list */
                                                getFileDir(pFile,mMultiSelect,false);
                                                //Send Storgae mounted broadcast to rescan the files or folders
                                                FMUtil.sendStorageIntent(FileManagerActivity.this);
                                              }else{
                                                  Toast.makeText(FileManagerActivity.this,getString(R.string.failed),Toast.LENGTH_SHORT).show();
                                              }
                                    }
                                  })
                            .setNegativeButton(getString(R.string.Cancel),
                                   new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog,int which){
                                        
                                    }
                                  }).show();
                      beDisappear(renameDialog);
                      return;
              }};
            };
            
            /* set rename dialogue and show it */
            renameDialog.setView(myView);
            renameDialog.setTitle(getString(R.string.Rename));
            renameDialog.setButton(getString(R.string.OK),listenerFileEdit);
            renameDialog.setButton2(getString(R.string.Cancel),
                new DialogInterface.OnClickListener(){
                  public void onClick(DialogInterface dialog, int which){
                      beDisappear(renameDialog);
                  }
                }
            );
            renameDialog.show();
      }
       
      /**
       * Copy function
       * @param file
       */
      private void copyFileOrDir(File f) {
            mSelectedFiles = new ArrayList<File>();
            mSelectedFiles.add(f);
            needDeleteOld = false;
            belowButtons.setVisibility(View.VISIBLE);
            belowLeftBt.setText(R.string.paste);
            belowLeftBt.setOnClickListener(listener_paste);
            belowRightBt.setOnClickListener(listener_belowCancel);
      }
      
      /**
       * move function
       * @param file
       */
      private void moveFileOrDir(File f) {
          if(f.canWrite()==false)
          {
              Toast.makeText(FileManagerActivity.this, getString(R.string.canotMove), Toast.LENGTH_SHORT).show();
              return;
          }
          mSelectedFiles = new ArrayList<File>();
          mSelectedFiles.add(f);
          needDeleteOld = true;
          belowButtons.setVisibility(View.VISIBLE);
          belowLeftBt.setText(R.string.paste);
          belowLeftBt.setOnClickListener(listener_paste);
          belowRightBt.setOnClickListener(listener_belowCancel);
      }
      
      
      /**
       * delete function
       * @param f
       */
      private void delFileOrDir(File f){
         final File f_del = f;
         String msg = String.format(getString(R.string.deleteInfor1), f_del.getName());
         new AlertDialog.Builder(FileManagerActivity.this)
            .setTitle(getString(R.string.confirmDel))
            .setIcon(R.drawable.alert)
            .setMessage(msg)
            .setPositiveButton(getString(R.string.OK),
                 new DialogInterface.OnClickListener(){
                      public void onClick(DialogInterface dialog,int which){          
                        /* delete file or folder */
                            // delete file in service
                            Intent itent = new Intent(FileManagerActivity.this, editService.class);
                            itent.putExtra("action", "delete");
                            itent.putExtra("srcPath", f_del.getPath());
                            doStartService(itent);
                            doBindService();
                            // show notify dialogue
                            mNotifyDlg.setMessage(getString(R.string.deleteWaiting));
                            mNotifyDlg.setCancelable(true);
                            mNotifyDlg.show();
                            // create thread to check if move finished
                            Thread t = new Thread(new updateProgressThread());
                            mRun = true;
                            mServiceCancel=false;
                            t.start();      
                      }
                })
           .setNegativeButton(getString(R.string.Cancel),
                 new DialogInterface.OnClickListener(){
                      public void onClick(DialogInterface dialog, int which){
                      }
         }).show();
     }
      
      /**
       * send file
       */
      private void sendFile(File f){
          String type = "*/*";
          Intent intent = new Intent();
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.setAction(android.content.Intent.ACTION_SEND);
          //set file and MimeType of intent
          intent.setDataAndType(Uri.fromFile(f),type);
          startActivity(intent);
      }
      
      /**
       * open with file
       */
      private void openwithFile(File f){
          String type = "*/*";
          Intent intent = new Intent();
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.setAction(android.content.Intent.ACTION_VIEW);
          //set file and MimeType of intent
          intent.setDataAndType(Uri.fromFile(f),type);
          startActivity(intent);
      }
      
      
      /**
       * zip directory
       */
      private void zipFileOrDir(File f){
            final File srcFile = f;
            LayoutInflater factory=LayoutInflater.from(FileManagerActivity.this);
            View myView=factory.inflate(R.layout.zip,null);
            final EditText myEditText=(EditText)myView.findViewById(R.id.zip_edit);
            myEditText.setText(f.getName());
            final AlertDialog zipDialog = new AlertDialog.Builder(FileManagerActivity.this).create();
            
            OnClickListener listenerOK = new DialogInterface.OnClickListener(){
                  public void onClick(DialogInterface dialog, int which){
                    /* get the modified path*/
                    final String zipName = myEditText.getText().toString()+".zip";             //get file name
                    final String pFile = srcFile.getParentFile().getPath() + File.separator;           //get file path
                    final String newPath = pFile+zipName;                               //get full path
                    if(!FMUtil.checkPath(zipName)){
                          Toast.makeText(FileManagerActivity.this,getString(R.string.renameInfor4),Toast.LENGTH_SHORT).show();
                          noDisappear(zipDialog);
                          return;
                    }
                    
                    final File f_new = new File(newPath);
                    if(f_new.exists()){
                            String msg = String.format(getString(R.string.renameInfor1), zipName);
                            Toast.makeText(FileManagerActivity.this, msg, Toast.LENGTH_SHORT).show();
                            noDisappear(zipDialog);
                            return;
                    }else{
                            // zip file in service
                            Intent itent = new Intent(FileManagerActivity.this, editService.class);
                            itent.putExtra("action", "zip");
                            itent.putExtra("srcPath", "");
                            itent.putExtra("destPath", f_new.getPath());
                            doStartService(itent);
                            mIsFilesServiceBound = true;
                            mFilesList = new ArrayList<File>();
                            mFilesList.add(srcFile);
                            doBindService();
                            
                            // show notify dialogue
                            mNotifyDlg.setMessage(getString(R.string.zipWaiting));
                            mNotifyDlg.setCancelable(true);
                            mNotifyDlg.show();
                            // create thread to check if move finished
                            Thread t = new Thread(new updateProgressThread());
                            mRun = true;
                            mServiceCancel=false;
                            t.start();
                            
                            beDisappear(zipDialog);
                            return;
                  }};
            };
            
            /* set rename dialogue and show it */
            zipDialog.setView(myView);
            zipDialog.setTitle(getString(R.string.zipInfo2));
            zipDialog.setButton(getString(R.string.OK),listenerOK);
            zipDialog.setButton2(getString(R.string.Cancel),
                new DialogInterface.OnClickListener(){
                  public void onClick(DialogInterface dialog, int which){
                      beDisappear(zipDialog);
                  }
                }
            );
            zipDialog.show();
      }
      
      
      /**
       * Property
       */
      private void showProperty(File f){
          final File fi = f;
          String infor;
          if(fi.isDirectory()){
              infor = String.format(getString(R.string.propertyStringFolder), fi.getName(),(fi.canRead()?"r":"-")  + (fi.canWrite()?"w":"-"),FMUtil.fileSizeMsg(fi),FMUtil.getContainInfor(fi));
          }else{
              infor = String.format(getString(R.string.propertyStringFile), fi.getName(),(fi.canRead()?"r":"-")  + (fi.canWrite()?"w":"-"),FMUtil.fileSizeMsg(fi),FMUtil.time2String(fi.lastModified()));
          }
          new AlertDialog.Builder(FileManagerActivity.this)
                .setTitle(getString(R.string.MenuProperty))
                .setMessage(infor)
                .setPositiveButton(getString(R.string.OK),
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog,int which){          
                            }
                        }).show();
      }
      
      
      /**
       * menu property function
       */
      private void property(){
          File f = new File(rootPath);
          String info;
          
          String deviceType = "";
//          if(rootPath.equals(Environment.getSDInternalStorageDirectory().getPath())){
//                deviceType = getString(R.string.internalStorage);
//          }else if(rootPath.equals(Environment.getExternal1StorageDirectory().getPath())){
//                deviceType = getString(R.string.externalCard);
//          }else if(rootPath.equals(Environment.getUSB1StorageDirectory().getPath())){
//                deviceType = getString(R.string.usbStorage);
//          }
        if(rootPath.equals("/mnt/sdcard")){
            deviceType = getString(R.string.internalStorage);
        }else if(rootPath.equals("/mnt/ext_sdcard")){
            deviceType = getString(R.string.externalCard);
        }else if(rootPath.equals("/mnt/usb1_storage")){
            deviceType = getString(R.string.usbStorage);
        }
          
          StatFs sf = new StatFs(rootPath);
          long blockSize = (long)sf.getBlockCount()*(long)sf.getBlockSize(); 
          long vblockSize = (long)sf.getFreeBlocks()*(long)sf.getBlockSize();
          info = String.format(getString(R.string.propertyStringBM1), f.getName(),deviceType,FMUtil.byteToString(blockSize),FMUtil.byteToString(vblockSize));
        
          new AlertDialog.Builder(FileManagerActivity.this)
            .setTitle(getString(R.string.MenuProperty))
            .setMessage(info)
            .setPositiveButton(getString(R.string.OK),
                    new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog,int which){   
                            
                        }
          }).show();
      }
       
      /**
       * sorting
       */
      private void sorting() {
          LayoutInflater factory=LayoutInflater.from(FileManagerActivity.this);
          View myView = factory.inflate(R.layout.sorting_type,null);
          sorting_type = (RadioGroup)myView.findViewById(R.id.sortingTypeGroup);
          switch(sortingType){
                case FMFile.SortTypeName:
                    sorting_type.check(R.id.sortingTypeName);
                    break;
                case FMFile.SortTypeType:
                    sorting_type.check(R.id.sortingTypeType);
                    break;
                case FMFile.SortTypeSize:
                    sorting_type.check(R.id.sortingTypeSize);
                    break;
                case FMFile.SortTypeTime:
                    sorting_type.check(R.id.sortingTypeModifyTime);
                    break;
          }
          
          final Dialog sortingDialog = new Dialog(FileManagerActivity.this);
          
          RadioGroup.OnCheckedChangeListener listenerSorting = new RadioGroup.OnCheckedChangeListener(){
            
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                // TODO Auto-generated method stub
                switch(arg1){
                    case R.id.sortingTypeName:
                        sortingType = FMFile.SortTypeName;
                        break;
                    case R.id.sortingTypeType:
                        sortingType = FMFile.SortTypeType;
                        break;
                    case R.id.sortingTypeSize:
                        sortingType = FMFile.SortTypeSize;
                        break;
                    case R.id.sortingTypeModifyTime:
                        sortingType = FMFile.SortTypeTime;
                        break;
                }
                 sortingDialog.hide();
                 sortingDialog.dismiss();
                 if(mIsShowSearchResult){
                     showSearchResult(null,false,false,true);
                 }else{
                     getFileDir(mCurPath.getText().toString().substring(DirPrefixLenth),mMultiSelect,false);
                 }
            }
          };

          sorting_type.setOnCheckedChangeListener(listenerSorting);
          ViewGroup.LayoutParams  lp = new ViewGroup.LayoutParams(300,300);
          sortingDialog.setContentView(myView,lp);
          sortingDialog.setTitle(getString(R.string.MenuSorting));
          sortingDialog.show();
    }
      
      private void showMultiSelect(){
          mMultiSelect = true;
          if(mIsShowSearchResult){
              showSearchResult(null,true,true,false);
          }else{
              String str = mCurPath.getText().toString().substring(DirPrefixLenth);
              getFileDir(str,mMultiSelect,false);
          }
          switch(mEditSelect){
              case EDIT_COPY:
                  belowLeftBt.setText(getString(R.string.CopyTo));
                  belowLeftBt.setOnClickListener(listener_CopyMoveTo);
                  break;
              case EDIT_MOVE:
                  belowLeftBt.setText(getString(R.string.MoveTo));
                  belowLeftBt.setOnClickListener(listener_CopyMoveTo);
                  break;
              case EDIT_DELETE:
                  belowLeftBt.setText(getString(R.string.Delete));
                  belowLeftBt.setOnClickListener(listener_Delete);
                  break;
              case EDIT_ZIP:
                  belowLeftBt.setText(getString(R.string.Zip));
                  belowLeftBt.setOnClickListener(listener_Zip);
                  break;
              default:
                  break;
          }
          mSelectedFiles = new ArrayList<File>();
          belowRightBt.setOnClickListener(listener_belowCancel);
          belowButtons.setVisibility(View.VISIBLE);
      }
      
      /**
       * show edit items for user select
       */
      private void editDialog(){
          OnClickListener listener_list=new DialogInterface.OnClickListener(){
              public void onClick(DialogInterface dialog,int which){
                  mEditSelect = which +1;
                  showMultiSelect();
          }};
          /* show edit items */
          String[] editList = getResources().getStringArray(R.array.editList);
            new AlertDialog.Builder(FileManagerActivity.this)
                .setTitle(getString(R.string.MenuEdit))
                .setItems(editList,listener_list)
                .setPositiveButton(getString(R.string.Cancel),
                     new DialogInterface.OnClickListener(){
                      public void onClick(DialogInterface dialog, int which){
                      }
                }).show();
      }
      
      /**
       * show search dialogue
       */
      private void searchDialog(){
            LayoutInflater factory=LayoutInflater.from(FileManagerActivity.this);
            View myView=factory.inflate(R.layout.search,null);
            
            final TextView searchPath = (TextView)myView.findViewById(R.id.curPath);
            final EditText searchName=(EditText)myView.findViewById(R.id.searchName);
            searchPath.setText(getString(R.string.MenuSearch) + " " + mCurPath.getText().toString());
            
            final AlertDialog searchDialog = new AlertDialog.Builder(FileManagerActivity.this).create();
            
            OnClickListener listenerSearchOk = new DialogInterface.OnClickListener(){
                  public void onClick(DialogInterface dialog, int which){
                    /* get the modified path*/
                    final String searchStr = searchName.getText().toString();             
                    final String searchPath =  mCurPath.getText().toString().substring(DirPrefixLenth); 
                    if(searchStr.length()==0 || !FMUtil.checkPath(searchStr)){
                          Toast.makeText(FileManagerActivity.this,getString(R.string.searchError1),Toast.LENGTH_SHORT).show();
                          noDisappear(searchDialog);
                          return;
                    }
                                 
                    Intent itent = new Intent(FileManagerActivity.this, editService.class);
                    itent.putExtra("action", "search");
                    itent.putExtra("srcPath", searchPath);
                    itent.putExtra("destPath",searchStr);
                    doStartService(itent);
                    doBindService();
                    // show notify dialogue
                    mNotifyDlg.setMessage(getString(R.string.searchWaiting));
                    mNotifyDlg.setCancelable(true);
                    mNotifyDlg.setButton(getString(R.string.Cancel),
                            new DialogInterface.OnClickListener(){
                                public void onClick(DialogInterface dialog, int which){
                                    mBoundService.stopServiceThread();
                                    mServiceCancel = true;
                                }
                    });
                    mNotifyDlg.show();
                    // create thread to check if move finished
                    Thread t = new Thread(new updateProgressThread());
                    mRun = true;
                    mServiceCancel=false;
                    t.start();
                    beDisappear(searchDialog);
                    isSearch = true;
                    return;
                  }
            };
            
            /* set rename dialogue and show it */
            searchDialog.setView(myView);
            searchDialog.setTitle(getString(R.string.MenuSearch));
            searchDialog.setButton(getString(R.string.OK),listenerSearchOk);
            searchDialog.setButton2(getString(R.string.Cancel),
                new DialogInterface.OnClickListener(){
                  public void onClick(DialogInterface dialog, int which){
                      beDisappear(searchDialog);
                  }
                }
            );
            searchDialog.show();
      }
      
      
      /**
       * new file
       * @param file
       * @return
       */
      public boolean newFile(File f) {
        try {
          f.createNewFile();
        }catch (Exception e){
          return false;
        }
        return true;
      }
      
      private boolean checkFolder(String path, String folder){
          File f = new File(path);
          File[] subs = f.listFiles();
          for(File s:subs){
              if(s.isDirectory() && s.getName().equals(folder)){
                  return true;
              }
          }
          return false;
      }
      
      private boolean checkFile(String path, String file){
          File f = new File(path);
          File[] subs = f.listFiles();
          for(File s:subs){
              if(s.isFile() && s.getName().equals(file)){
                  return true;
              }
          }
          return false;
      }
      
      /**
       * open file
       * @param f
       */
      private void openFile(File f){
       
        //pop up a dialogue box for select
        String type = FMUtil.getMIMEType(f,true); 
          
        final File fp = f;
        if(type.equals("application/zip"))
        {
              LayoutInflater factory=LayoutInflater.from(FileManagerActivity.this);
              View myView = factory.inflate(R.layout.unzip,null);
              Button unzipHere = (Button)myView.findViewById(R.id.btUnzipHere);
              Button unzipToFolder =(Button)myView.findViewById(R.id.btUnzipToFolder);
              unzipToFolder.setText(String.format(getString(R.string.unzipToFolder), f.getName()+".zip"));
              final Dialog unzipDialog = new Dialog(FileManagerActivity.this);
              unzipHere.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v) {
                    List<String> zipEntrys;
                    try {
                        zipEntrys = zipFileUtil.getEntriesNames(fp);
                    } catch (ZipException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Toast.makeText(FileManagerActivity.this, getString(R.string.zipError), Toast.LENGTH_SHORT).show();
                        return;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Toast.makeText(FileManagerActivity.this, getString(R.string.zipError), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    OnClickListener listenerOk = new DialogInterface.OnClickListener(){

                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            // unzip file in service
                            Intent itent = new Intent(FileManagerActivity.this, editService.class);
                            itent.putExtra("action", "unzip");
                            itent.putExtra("srcPath", fp.getPath());
                            itent.putExtra("destPath", mCurPath.getText().toString().substring(DirPrefixLenth));
                            doStartService(itent);
                            doBindService();
                            
                            // show notify dialogue
                            mNotifyDlg.setMessage(getString(R.string.zipWaiting));
                            mNotifyDlg.setCancelable(true);
                            mNotifyDlg.show();
                            // create thread to check if move finished
                            Thread t = new Thread(new updateProgressThread());
                            mRun = true;
                            mServiceCancel=false;
                            t.start();
                        }
                    };
                    boolean foundSame = false;
                    for(int i=0;i<zipEntrys.size();i++){
                        int idx = zipEntrys.get(i).indexOf("/");
                        if(idx>0){
                            if(checkFolder(mCurPath.getText().toString().substring(DirPrefixLenth),zipEntrys.get(i).substring(0, idx))){
                                final AlertDialog warnDialog = new AlertDialog.Builder(FileManagerActivity.this).create();
                                warnDialog.setMessage(getString(R.string.unzipWarning1));
                                warnDialog.setTitle(getString(R.string.unzip));
                                warnDialog.setButton(getString(R.string.OK), listenerOk);
                                warnDialog.setButton2(getString(R.string.Cancel),  new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog,int which) {
                                        // TODO Auto-generated method stub
                                    }});
                                warnDialog.show();
                                foundSame = true;
                                break;
                            }
                        }else{
                            if(checkFile(mCurPath.getText().toString().substring(DirPrefixLenth),zipEntrys.get(i))){
                                final AlertDialog warnDialog = new AlertDialog.Builder(FileManagerActivity.this).create();
                                warnDialog.setMessage(getString(R.string.unzipWarning1));
                                warnDialog.setTitle(getString(R.string.unzip));
                                warnDialog.setButton(getString(R.string.OK), listenerOk);
                                warnDialog.setButton2(getString(R.string.Cancel),  new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog,int which) {
                                        // TODO Auto-generated method stub
                                    }});
                                warnDialog.show();
                                foundSame = true;
                                break;
                            }
                        }
                    }
                    if(foundSame==false){
                        // unzip file in service
                        Intent itent = new Intent(FileManagerActivity.this, editService.class);
                        itent.putExtra("action", "unzip");
                        itent.putExtra("srcPath", fp.getPath());
                        itent.putExtra("destPath", mCurPath.getText().toString().substring(DirPrefixLenth));
                        doStartService(itent);
                        doBindService();
                        
                        // show notify dialogue
                        mNotifyDlg.setMessage(getString(R.string.zipWaiting));
                        mNotifyDlg.setCancelable(true);
                        mNotifyDlg.show();
                        // create thread to check if move finished
                        Thread t = new Thread(new updateProgressThread());
                        mRun = true;
                        mServiceCancel=false;
                        t.start();
                    }
                    unzipDialog.dismiss();
                }     
              });
              unzipToFolder.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        // unzip file in service
                        Intent itent = new Intent(FileManagerActivity.this, editService.class);
                        itent.putExtra("action", "unzip");
                        itent.putExtra("srcPath", fp.getPath());
                        itent.putExtra("destPath", mCurPath.getText().toString().substring(DirPrefixLenth)+File.separator+fp.getName()+".zip");
                        doStartService(itent);
                        doBindService();
                        
                        // show notify dialogue
                        mNotifyDlg.setMessage(getString(R.string.zipWaiting));
                        mNotifyDlg.setCancelable(true);
                        mNotifyDlg.show();
                        // create thread to check if move finished
                        Thread t = new Thread(new updateProgressThread());
                        mRun = true;
                        mServiceCancel=false;
                        t.start();
                        unzipDialog.dismiss();
                    }     
              });
              
              ViewGroup.LayoutParams  lp = new ViewGroup.LayoutParams(300,150);
              unzipDialog.setContentView(myView,lp);
              unzipDialog.setTitle(getString(R.string.unzip));
              unzipDialog.show();
        }
        else{

            Uri fullUri=null;
            
            /**
             * // for music full show
            String path = f.getPath();
            Cursor cu = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String [] {MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.DATA + "=?", new String [] {path}, null);
            if(cu!=null){
                cu.moveToFirst();
                int idx = cu.getColumnIndex(MediaStore.Audio.Media._ID);
                fullUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,cu.getInt(idx)+"");
                Log.i("janyDebug",fullUri.toString());
            }
            */
            
            fullUri = Uri.fromFile(f);
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            //set file and MimeType of intent
            intent.setDataAndType(fullUri,type);
            try{
                startActivity(intent);
            }catch(Exception e){
                intent.setDataAndType(fullUri,"*/*");
                startActivity(intent);
            }
        }
      }
}
