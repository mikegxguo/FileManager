package com.Mitac.FileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;
import 	android.media.MediaScannerConnection;
import android.os.Environment;
//import android.os.storage.IMountService;
//import android.os.ServiceManager;
import android.os.IBinder;
import android.os.RemoteException;
import java.io.IOException;

/**
 * 2012.10.12. Updated by David Kim
 * Fixed for Issue SC3377, SC3380 : Between PC and Tablet, creating/deleting/copying/moving doesnt' refresh.
 */
public class editService extends Service {
    private static String LOGTAG = "FileManagerService";
    // thread state
    public static final int status_isRunning = 1;
    public static final int status_isIdle = 2;
    private int mStatus = status_isIdle;

    private boolean mResult = true;
    private String srcPath;
    private String destPath;
    boolean mRun = false;
    private Thread thd = null;
    private List<String> paths = null;
    private Collection<File> mDealFiles = null;

    private MediaScannerConnection mConnect;
    private WakeLock mSuspendLock = null;

    /**
     * @return if edit function is finished
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * @return fail or success
     */
    public boolean getResult() {
        return mResult;
    }

    /**
     * @return search result
     */
    public List<String> getSearchResult() {
        return paths;
    }

    /**
     * set zip file list
     */
    public void setDealFilesCollection(Collection<File> f) {
        Log.i(LOGTAG, "setDealFilesCollection:" + f.size());
        mDealFiles = f;
    }

    /**
     * stopService
     */
    public void stopServiceThread() {
        if(mRun) {
            mRun = false;
            try {
                zipFileUtil.setStopFlag(true);
                thd.join();
            } catch(Exception e) {

            }
            thd = null;
            unlockSuspend();
        }
        mStatus = status_isIdle;
        mResult = true;
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public editService getService() {
            return editService.this;
        }
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.i(LOGTAG, "onCreate");
        // TODO Auto-generated method stub
        super.onCreate();
        mConnect = new MediaScannerConnection(this, null);
        mConnect.connect();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.i(LOGTAG, "onDestroy");
        super.onDestroy();
        if(mRun) {
            mRun = false;
            try {
                thd.join();
            } catch(Exception e) {

            }
            thd = null;
        }
        unlockSuspend();
        mConnect.disconnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.i(LOGTAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        try { // fix bugid:SC4881_N435 filemanager runtime error when using browser #frank527.lin # 05.07.2014
            String ac = intent.getStringExtra("action");
            srcPath = intent.getStringExtra("srcPath");
            destPath = intent.getStringExtra("destPath");
            while(mStatus != status_isIdle) {
                Log.i(LOGTAG, "!!!!before edit thread hasn't finished!!!!!");
                SystemClock.sleep(1000);
            }
            if(ac.equals("copy")) {
                thd = new Thread(new copyThread());
                thd.start();
                mRun = true;
            } else if(ac.equals("move")) {
                thd = new Thread(new moveThread());
                thd.start();
                mRun = true;
            } else if(ac.equals("delete")) {
                thd = new Thread(new deleteThread());
                thd.start();
                mRun = true;
            } else if(ac.equals("search")) {
                thd = new Thread(new searchThread());
                thd.start();
                mRun = true;
            } else if(ac.equals("zip")) {
                thd = new Thread(new zipThread());
                thd.start();
                mRun = true;
            } else if(ac.equals("unzip")) {
                thd = new Thread(new unzipThread());
                thd.start();
                mRun = true;
            } else if(ac.equals("scanMedia")) {
                thd = new Thread(new reScanMediaThread());
                thd.start();
                mRun = true;
            }
        } catch(NullPointerException e) {
            Log.e(LOGTAG, "intent is null");
        }
        return START_STICKY;
    }

    private void lockSuspend() {
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mSuspendLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FileManager");
        mSuspendLock.acquire();
    }

    private void unlockSuspend() {
        if(mSuspendLock != null) {
            if(mSuspendLock.isHeld()) {
                mSuspendLock.release();
            }
            mSuspendLock = null;
        }
    }

    private class copyThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            mStatus = status_isRunning;
            lockSuspend();
            if(srcPath.length() != 0) {
                File f = new File(srcPath);
                if(f.isDirectory()) {
                    mResult = copyDir(srcPath, destPath);
                } else {
                    mResult = copyFile(srcPath, destPath);
                }
            } else {
                while(mDealFiles == null) {
                    Log.i(LOGTAG, "sleep for files got!!");
                    SystemClock.sleep(1000);
                }
                for(File f: mDealFiles) {
                    if(f.isDirectory()) {
                        mResult = copyDir(f.getPath(), destPath);
                    } else {
                        mResult = copyFile(f.getPath(), destPath);
                    }
                    if(mRun == false) {
                        break;
                    }
                }
                mDealFiles = null;
            }
            FMUtil.sendStorageIntent(editService.this);
            unlockSuspend();
            mStatus = status_isIdle;
            Log.i(LOGTAG, "copy end: " + mResult);
        }
    }

    private class moveThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            mStatus = status_isRunning;
            lockSuspend();
            if(srcPath.length() != 0) {
                File f = new File(srcPath);
                if(f.isDirectory()) {
                    mResult = moveDir(srcPath, destPath);
                } else {
                    mResult = moveFile(srcPath, destPath);
                }
            } else {
                while(mDealFiles == null) {
                    Log.i(LOGTAG, "sleep for files got!!");
                    SystemClock.sleep(1000);
                }
                for(File f: mDealFiles) {
                    String oldPath = f.getPath();
                    String newPath = destPath + File.separator + f.getName();
                    boolean isDir = f.isDirectory();
                    if(f.renameTo(new File(newPath)) == false) {
                        if(f.isDirectory()) {
                            mResult = moveDir(f.getPath(), destPath);
                        } else {
                            mResult = moveFile(f.getPath(), destPath);
                        }
                    } else {
                        Log.d(LOGTAG, "rename success:" + oldPath + "-->" + newPath);
                        deleteFromMediaProvider(getContentResolver(), oldPath, isDir);
                        if(isDir == false) {
                            mConnect.scanFile(newPath, null);
                        } else {
                            mediaProviderScanFolder(newPath);
                        }
                    }
                    if(mRun == false) {
                        break;
                    }
                }
                mDealFiles = null;
            }
            //Send Storgae mounted broadcast to rescan the files or folders
            FMUtil.sendStorageIntent(editService.this);
            unlockSuspend();
            mStatus = status_isIdle;
            Log.i(LOGTAG, "move end:" + mResult);
        }
    }

    private class deleteThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            mStatus = status_isRunning;
            lockSuspend();
            if(srcPath.length() != 0) {
                File f = new File(srcPath);
                if(f.isDirectory()) {
                    mResult = delDir(f);
                } else {
                    mResult = delFile(f);
                }
            } else {
                while(mDealFiles == null) {
                    SystemClock.sleep(1000);
                    Log.i(LOGTAG, "sleep for files got!!");
                }
                for(File f: mDealFiles) {
                    Log.i(LOGTAG, f.getName());
                    if(f.isDirectory()) {
                        mResult = delDir(f);
                    } else {
                        mResult = delFile(f);
                    }
                    if(mRun == false) {
                        break;
                    }
                }
                mDealFiles = null;
            }
            //Send Storgae mounted broadcast to rescan the files or folders
            FMUtil.sendStorageIntent(editService.this);
            unlockSuspend();
            mStatus = status_isIdle;
            Log.i(LOGTAG, "delete end:" + mResult);
        }
    }

    private class searchThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            mStatus = status_isRunning;
            lockSuspend();
            paths = new ArrayList<String>();
            File f = new File(srcPath);
            if(f.isDirectory() == false) {
                mResult = false;
            } else {
                mResult = searchDir(f, destPath);
            }
            unlockSuspend();
            mStatus = status_isIdle;
            Log.i(LOGTAG, "search end:" + mResult);
        }
    }

    private class zipThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            mStatus = status_isRunning;
            lockSuspend();
            while(mDealFiles == null) {
                Log.i(LOGTAG, "sleep for files got!!");
                SystemClock.sleep(1000);
            }
            try {
                Log.i(LOGTAG, mDealFiles.toString());
                zipFileUtil.setStopFlag(false);
                zipFileUtil.zipFiles(mDealFiles, new File(destPath));
                mResult = true;
            } catch(Exception e) {
                mResult = false;
                delFile(new File(destPath));
            }
            mDealFiles = null;
            mStatus = status_isIdle;
            unlockSuspend();
            Log.i(LOGTAG, "zip end:" + mResult);
        }
    }

    private class unzipThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            mStatus = status_isRunning;
            lockSuspend();
            try {
                Log.i(LOGTAG, "unzip file :" + srcPath);
                Log.i(LOGTAG, "unzip to folder :" + destPath);
                zipFileUtil.setStopFlag(false);
                zipFileUtil.unZipFile(new File(srcPath), destPath, mConnect);
                mResult = true;
            } catch(Exception e) {
                mResult = false;
            }
            unlockSuspend();
            mStatus = status_isIdle;
            Log.i(LOGTAG, "unzip end:" + mResult);
        }
    }

    private class reScanMediaThread implements Runnable {
        public void run() {
            // TODO Auto-generated method stub
            mStatus = status_isRunning;
            lockSuspend();
            try {
                Log.i(LOGTAG, "delete file/folder :" + srcPath);
                File f = new File(destPath);
                deleteFromMediaProvider(getContentResolver(), srcPath, f.isDirectory());
                if(f.isDirectory() == true) {
                    Log.i(LOGTAG, "scan folder :" + destPath);
                    mediaProviderScanFolder(destPath);
                } else {
                    Log.i(LOGTAG, "scan file :" + destPath);
                    mConnect.scanFile(destPath, null);
                }
                mResult = true;
            } catch(Exception e) {
                mResult = false;
            }
            unlockSuspend();
            mStatus = status_isIdle;
            Log.i(LOGTAG, "scan media end:" + mResult);
        }
    }
    /**
       * copy single file
       * @param oldPath String: old file full path:/xx
       * @param newPath String: new path:/xx/ss
       * @return boolean
       */
    private boolean copyFile(String oldPath, String newPath) {
        
        // +frank527.lin # 08.27.2014
        InputStream inStream = null;
        FileOutputStream fs = null;
        // -frank527.lin # 08.27.2014
        try {
            int bytesum = 0;
            int byteread = 0;
            String f_new = "";
            File f_old = new File(oldPath);
            // check if enough space for copy
            StatFs sf = new StatFs(newPath);
            long vblockSize = (long)sf.getFreeBlocks() * (long)sf.getBlockSize();
            if(vblockSize < f_old.length()) {
                return false;
            }
            if(newPath.endsWith(File.separator)) {
                f_new = newPath + f_old.getName();
            } else {
                f_new = newPath + File.separator + f_old.getName();
            }
            new File(newPath).mkdirs();              //if folder isn't exist, then create
            new File(f_new).createNewFile();         //if file isn't exist, then create
                                                     //if file is exist
            if(f_old.exists() && mRun) {
                inStream = new FileInputStream(oldPath); //read the old file
                fs = new FileOutputStream(f_new);
                byte[] buffer = new byte[1444];
                while((byteread = inStream.read(buffer)) != -1) {
                    if(mRun == false) {
                        new File(f_new).delete();
                        f_new = null;
                        break;
                    }
                    bytesum += byteread; //byte count
                    fs.write(buffer, 0, byteread);
                }
                
                if(f_new != null) {
                    Log.d(LOGTAG, "add file: " + oldPath);
                    mConnect.scanFile(f_new, null);
                }

                // +frank527.lin # 08.27.2014
                if(inStream != null) {
                    inStream.close();
                    inStream = null;
                }

                if(fs != null) {
                    fs.close();
                    fs = null;
                }
                // -frank527.lin # 08.27.2014
            }
        } catch(Exception e) {
            // +frank527.lin # 08.27.2014
            try { // release the 
                if(inStream != null) {
                    inStream.close();
                    inStream = null;
                }

                if(fs != null) {
                    fs.close();
                    fs = null;
                }
            } catch(IOException ioe) {}
            // -frank527.lin # 08.27.2014
            return false;
        }
        return true;
    }


    /**
     * copy folder
     * @param oldPath String: /aa/bb   11,22
     * @param newPath String: /ss/cc
     * @return boolean
     */
    private boolean copyDir(String oldPath, String newPath) {
        try {
            File f_old = new File(oldPath);                           //copy folder:  /aa/bb---[1.txt,rr]
            String d_old = "";
            String d_new = newPath + File.separator + f_old.getName();    //new path:  /cc/dd  ==> /cc/dd/bb
            new File(d_new).mkdirs();                                 //if folder isn't exist, then create /cc/dd/bb
            File[] files = (File[])f_old.listFiles();
            for(int i = 0; i < files.length; i++) {
                if(mRun == false) {
                    break;
                }
                d_old = oldPath + File.separator + files[i].getName();      //the files included in the filder: /aa/bb/1.txt,folder: /aa/bb/rr
                if(files[i].isFile()) {
                    if(copyFile(d_old, d_new) == false) {
                        return false;
                    }
                } else {
                    if(copyDir(d_old, d_new) == false) {
                        return false;
                    }
                }
            }
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    /**
     * move file
     * @param oldPath String: /fqf.txt
     * @param newPath String: xx/fqf.txt
     */
    private boolean moveFile(String oldPath, String newPath) {
        boolean ret = false;
        try {
            if(copyFile(oldPath, newPath)) {
                new File(oldPath).delete();
                Log.d(LOGTAG, "delete file: " + oldPath);
                deleteFromMediaProvider(getContentResolver(), oldPath, false);
                ret = true;
            } else {
                return false;
            }
        } catch(Exception e) {
            return false;
        }
        return ret;
    }

    /**
     * move folder
     * @param oldPath String : /xx
     * @param newPath String : /cc/xx
     */
    private boolean moveDir(String oldPath, String newPath) {
        boolean ret = false;
        try {
            if(copyDir(oldPath, newPath)) {
                if(delDir(new File(oldPath))) {
                    ret = true;
                }
            } else {
                return false;
            }
        } catch(Exception e) {
            return false;
        }
        return ret;
    }

    /**
     * delete single file
     * @param file
     * @return
     */
    private boolean delFile(File f) {
        boolean ret  = false;
        String path = f.getPath();
        try {
            if(f.exists()) {
                f.delete();
                Log.d(LOGTAG, "delete file: " + path);
                deleteFromMediaProvider(getContentResolver(), path, false);
                ret = true;
            }
        } catch(Exception e) {
            return false;
        }
        return ret;
    }

    /**
     * delete folder
     * @param FMFile
     * @return
     */
    private boolean delDir(File f) {
        boolean ret  = false;
        try {
            if(f.exists()) {
                File[] files = (File[])f.listFiles();
                for(int i = 0; i < files.length; i++) {
                    if(mRun == false) {
                        break;
                    }
                    if(files[i].isDirectory()) {
                        if(!delDir(files[i])) {
                            return false;
                        }
                    } else {
                        delFile(files[i]);
/*
                    String path = files[i].getPath();
                    files[i].delete();
                    Log.d(LOGTAG,"delete file: " + path);
                    deleteFromMediaProvider(getContentResolver(),path,false);
*/
                    }
                }
                String path = f.getPath();
                Log.d(LOGTAG, "delete file: " + path);
                f.delete();    //delete empty folder
                deleteFromMediaProvider(getContentResolver(), path, true);
                ret = true;
            }
        } catch(Exception e) {
            return false;
        }
        return ret;
    }

    private boolean searchDir(File dir, String matchStr) {
        boolean res = true;
        try {
            if(dir.exists()) {
                File[] files = dir.listFiles();
                for(int i = 0; i < files.length; i++) {
                    if(mRun == false) {
                        break;
                    }
                    Log.i(LOGTAG, files[i].getPath());
                    if(files[i].getName().indexOf(matchStr) > -1) {
                        paths.add(files[i].getPath());
                    }
                    if(files[i].isDirectory()) {
                        res = searchDir(files[i], matchStr);
                    }
                }
            }
        } catch(Exception e) {
            res = false;
        }
        return res;
    }

    private void deleteFromMediaProvider(ContentResolver cr, String urlstring, boolean isFolder) {
        final Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI;
        final Uri uriVideos = Video.Media.EXTERNAL_CONTENT_URI;
        final Uri uriAudios = Audio.Media.EXTERNAL_CONTENT_URI;

        if(isFolder == false) {
            String whereStr = Images.ImageColumns.DATA + "=\"" + urlstring + "\"";
            if(cr.delete(uriImages, whereStr, null) < 1) {
                whereStr = Video.VideoColumns.DATA + "=\"" + urlstring + "\"";
                if(cr.delete(uriVideos, whereStr, null) < 1) {
                    whereStr = Audio.AudioColumns.DATA + "=\"" + urlstring + "\"";
                    cr.delete(uriAudios, whereStr, null);
                }
            }
        } else {
            Log.i(LOGTAG, "Delete folder : " + urlstring);
            String whereStr = Images.ImageColumns.DATA + " like \'" + urlstring + "/%\'";
            cr.delete(uriImages, whereStr, null);
            whereStr = Video.VideoColumns.DATA + " like \'" + urlstring + "/%\'";
            cr.delete(uriVideos, whereStr, null);
            whereStr = Audio.AudioColumns.DATA + " like \'" + urlstring + "/%\'";
            cr.delete(uriAudios, whereStr, null);
        }
    }

    private void mediaProviderScanFolder(String folderName) {
        File f = new File(folderName);
        File[] files = (File[])f.listFiles();
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                if(files[i].isFile()) {
                    mConnect.scanFile(files[i].getPath(), null);
                } else {
                    mediaProviderScanFolder(files[i].getPath());
                }
            }
        }
    }
}
