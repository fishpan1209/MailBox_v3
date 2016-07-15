package com.fishpan1209.mailbox_v3;

import java.io.*;
import java.util.*;
import javax.naming.*;
import java.text.SimpleDateFormat;

import java.lang.Integer.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.sql.*;

import java.util.regex.*;

public final class getFileListNew

{    
    public getFileListNew(){
    	
    }

	/** 
	 * The primary method for the Java service
	 *
	 * @param pipeline
	 *            The IData pipeline
	 * @throws IOException 
	 * @throws ServiceException
	 */
	public String[] getFileList(String dirname) throws IOException {
		// get parameters from pipeline
				
		boolean debug 		= false; // debug mode
		boolean doFileFiltering = true; // default = false
		boolean doFileSorting 	= false;
		
		// default conditions
		String updateInterval = "5000"; // default 5 s
		String batchCount = "-1"; // default to return all files							// files
		String checkSize = "N";
		String sorting = "N";
		
		/*
		IDataCursor pipelineCursor 	= pipeline.getCursor();
		String		dirname 	= IDataUtil.getString( pipelineCursor, "dirname" );
		String		updateInterval 	= IDataUtil.getString( pipelineCursor, "updateInterval" );
		String		batchCount 	= IDataUtil.getString( pipelineCursor, "batchCount" );
		String		checkSize 	= IDataUtil.getString( pipelineCursor, "checkSize" );
		String		sorting 	= IDataUtil.getString( pipelineCursor, "sorting" );
		*/
		if ( (sorting != null) && sorting.equals("Y") ) doFileSorting = true;
		
		// pipelineCursor.destroy();
		
		java.util.Date 	tDate;
		String 		dirPathname = dirname ;
		File 		dir 	    = new File(dirPathname);
		
		// filter for removing .done, .lck and  .list files from the list
		class MailboxFileFilter implements FilenameFilter {
		    public boolean accept(File dir, String name) {
				// do not filter out the lock file so expired lock files can be removed.
				return ((! name.endsWith(".done")) && (! name.endsWith(".list")));
		    }
		}
		
		if ( dir.exists() )
		{   
			File[]		files = null;
			if ( doFileFiltering == true ) {
				FilenameFilter	filter	= new MailboxFileFilter();
				files			= dir.listFiles(filter);		  // All file list in the specified directory
			}
			else{
				files			= dir.listFiles();				// All file list in the specified directory
			}
			
			String[] 	filelist 	= null;					// file list to be returned
			Vector		fileListVector	= new Vector();
			int 		fileCount 		= 0;					// 
			long 		maxListFileSize		= 1000000;				// n bytes
			File 		inFile;								// A file from the directory
			long 		lastUpdatedInterval;						// last updated interval for a file
			long 		updInterval		=0; 					// 
			int 		batchCnt 		= 0;					// max. number of files to be returned
			int 		returnFileCount = 0; 						// number of file to be returned
			boolean 	isDoneFile 		= false;				// does the file has has a .done suffix?
			boolean 	isListFile 		= false;				// does the file has has a .list suffix?
			boolean 	isLockFile 		= false;				// does the file has has a .lck suffix?
			boolean 	isFileComplete 	= true;					// update: do not check completeness does the file has has completely copied or transferred
			boolean		chkSize = false;
			if ( checkSize != null && checkSize.equals("true") ) {
				chkSize = true;
			}
		
			try
			{
				updInterval = Long.parseLong(updateInterval.trim());
				batchCnt 	= Integer.parseInt(batchCount.trim());
			}
			catch (NumberFormatException ex)
			{
				updInterval = 10000;		// default to 5 seconds
				batchCnt 	= -1;		// default to return all files.
			}
		
			if ( debug ) {
				logmessage("===================");
				logmessage("Total size: " + files.length);
			}
		
			if ( doFileSorting == true )  {
				// sort the file list base on the last modified time in ascending order.
				files = sortFileListByLastModified(files);
			}
		
			tDate = new java.util.Date();   // get the current system time
		
			long totalListFileSize = 0;  //counter variable
			// loop the file list
			
			for(int i=0; i<files.length; i++)
			{
				inFile = files[i];
		
				if ( inFile.getName().endsWith(".list") == true )       // check if file is .list file
					isListFile = true;
				else if ( inFile.getName().endsWith(".done") == true )  // check if file is .done file
					isDoneFile = true;
				else if ( inFile.getName().endsWith(".lck") == true ) { // check if file is .lck  file
					//System.out.println("lck files: "+inFile.getName());
					isLockFile = true;
					if ( debug ) logmessage("FxMailBoxManager:getFileList() found lock file: " + inFile.getName() );
					if ( fileLockExpired(dirPathname, inFile.getName()) == true ) {
						if ( debug )logmessage("FxMailBoxManager:getFileList() found expired lock file: " + inFile.getName() );
						if ( releaseFileLock(dirPathname, inFile.getName()) == false )
							logmessage("Error: FxMailBoxManager:getFileList() exception: unable to unlock file: " + inFile.getName() );
					}
					else if ( debug )
						logmessage("FxMailBoxManager:getFileList() found un-expired lock file: " + inFile.getName() );
				}
		
				/*
				 update: no check on file completion
				// check if file is complete by checking the last update time and current system time.
				// if input updInterval is negative, no comparison will be done.
				lastUpdatedInterval = tDate.getTime() - inFile.lastModified();
				if ( debug ) logmessage("last updated interval: " + lastUpdatedInterval );
				if ( (updInterval < 0) || (updInterval >= 0) && (lastUpdatedInterval > updInterval) )
					isFileComplete = true;
				else
					isFileComplete = false;
		*/
				// skip all .done and .list and .lck and in-completed files
				if ( (isListFile==true) || (isDoneFile==true) || 
							(isLockFile==true) || (isFileComplete == false) ) {
					isListFile = false;
					isDoneFile = false;
				        isLockFile = false;
					if ( debug ) logmessage(inFile.getName() + " done/list/lck/in-complete file skipped.");
					continue; //skip this iteration in the loop
				}
		
				//Take out directories, list files, done files and hidden files from the filelist
				if ( (inFile.isHidden() == true) || 
							(inFile.isFile() == false) || (inFile.canRead() == false) ) {
					if ( debug ) logmessage(inFile.getName() + " hidden/directory/can't read file skipped.");
					continue; //skip this iteration in the loop			
				}
		
				// lock the file and put the file name into the list
				if ( fileLocked(dirPathname, inFile.getName()) == false ) {
					if ( lockFile(dirPathname, inFile.getName()) ) {
						fileCount++;
						totalListFileSize += inFile.length();
						fileListVector.add(inFile.getName());
						if ( debug ) logmessage(inFile.getName() + " added.");
						if ( ( chkSize && (totalListFileSize > maxListFileSize)) || ((batchCnt > 0) && (fileCount >= batchCnt)) ) {
							if ( debug )logmessage("break");
							break;
						}
					}
					else {
						logmessage("Warning: FxMailBoxManager:getFileList() exception: unable to lock file: " + inFile.getName() + " in " + dirname );
					}
				}
				
			} // for block
			

			
			// convert the file list vector into string array and 
			// update: save file list to output
			// release file lck for future tests
			
			filelist = new String[fileListVector.size()];
			filelist =  (String[])fileListVector.toArray( filelist );
            if ( debug ) logmessage("size: " + fileListVector.size());
			
			if ( debug ) logmessage("########################");
			
			// save result to output file for debugging
			if (debug) {
				if (filelist.length > 0) {
					System.out.println(
							"Total numbers of files found: " + filelist.length + "\n" + "Save results to output.txt");
					saveResult(filelist, dirname); // save results and
															// release lock

				}
			}
		
			return filelist;
		}
		else
		{
			String msg = "Error: FxMailBoxManager.private:getFileList(): directory " + dirPathname + " does not exist.";
			logmessage(msg);
			return null;
		}
		
	}
	
	public static void saveResult(String[] filelist, String dirname) throws IOException{
		// delete existing output files
		String output = dirname+"output.txt";
		File out = new File(dirname);
		if(out.exists()){
			out.delete();
		}
		try {
			RandomAccessFile outFile  = new RandomAccessFile(output, "rw");
			//System.out.println(output);
			FileChannel outchannel = outFile.getChannel();
			String header = "List of filenames: \n";
			outchannel.write(ByteBuffer.wrap(header.getBytes()));
			ByteBuffer buffer = ByteBuffer.allocate(filelist.length*256);
			buffer.clear();
			for(String s : filelist){
				buffer.put((s+"\n").getBytes());
				releaseFileLock(dirname, s);
			}
			buffer.flip();
			while(buffer.hasRemaining()){
			outchannel.write(buffer);
			}
			
			outchannel.close();
			outFile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// clean up locked files for iterative experimenting
	public void cleanup(String dirname){
		File dir = new File(dirname);
		String[] filelist = dir.list();
		for(String s : filelist){
			releaseFileLock(dirname, s);
		}
	}
	
	// --- <<IS-BEGIN-SHARED-SOURCE-AREA>> ---
	
	static String mailboxDefinition = null;
	static long mailboxControlLastModifiedTime = 0;
	static String txmOnHoldList 	= null;
	private static final String ENCODING_UTF8 = "UTF-8";
	private static final String ENCODING_8859_1 = "ISO-8859-1";
	static	BufferedInputStream getPartialFileFin = null;	
	
	private static synchronized String getTimeStamp(String uniqueId)
	{
		java.util.Date 	tDate;
		//String 			pattern 	= "MMddyyyyHHmmssSSS";
	    String 			pattern 	= "MMddyyyyHHmmss";
		SimpleDateFormat formatter 	= new SimpleDateFormat(pattern);
		
		tDate = new java.util.Date();
		String ts = formatter.format(tDate)+uniqueId; // use the unique id to replcae the milli-second value.
		try { Thread.sleep(2); } catch ( Exception e ) {}; // delay for 2 milisec to avoid dupliocated ts generated.
	
		return ts;
	}
	
	private static synchronized String getTimeStamp()
	{
		java.util.Date 	tDate;
		String 			pattern 	= "MMddyyyyHHmmssSSS";
	
		SimpleDateFormat formatter 	= new SimpleDateFormat(pattern);
		
		tDate = new java.util.Date();
		String ts = formatter.format(tDate);
		try { Thread.sleep(2); } catch ( Exception e ) {}; // delay for 2 milisec to avoid dupliocated ts generated.
	
		return ts;
	}
	
	private static int nextIndexOf(String filedata, String delim[])
	{
		int currindex = 0;
		int nextindex = 0;
		for(int i=0; i<delim.length; i++)
		{
			nextindex = filedata.indexOf(delim[i], 3);
			if(currindex ==0 || (currindex > nextindex && nextindex != -1) || (nextindex == -1 && currindex == 0) || (currindex == -1 && nextindex > currindex))
				currindex = nextindex;
		}
		return currindex;
	}
	
	
	public static void logmessage(String msgText)
	{/*
	  IData input = IDataFactory.create();
	  IDataHashCursor inputCursor = input.getHashCursor();
	  inputCursor.last();
	  inputCursor.insertAfter( "msgTxt", msgText );
	  inputCursor.destroy();
	  IData 	output = IDataFactory.create();
	  try{
	  	    output = Service.doInvoke( "FxUtility", "LogDebugMsg", input );
	     }catch( Exception e){}
	     */
		System.out.println(msgText+"\n");
	 }
	
	
	
	/**
	 * Sort a list of file in asc. order using last modified date time.
	 */
	private static File[] sortFileListByLastModified(File[] aFileList) {
	
		Arrays.sort(aFileList, FILE_DATE_ORDER);
	
		/*
		logmessage("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww");
		for (int i=0; i < aFileList.length; i++) {
			logmessage(aFileList[i].getName() + " --- " + aFileList[i].lastModified());
		}
		logmessage("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww");
		*/
	
		return aFileList;
	}
	
	/**
	 * Sort Comparator for File last modified date time
	 */
	static final Comparator FILE_DATE_ORDER = new Comparator() {
	        public int compare(Object o1, Object o2) {
	            File f1 = (File) o1;
	            File f2 = (File) o2;
				java.util.Date d1 = new java.util.Date(f1.lastModified());
				java.util.Date d2 = new java.util.Date(f2.lastModified());
				if ( d1.equals(d2) ) return 0;
				if ( d1.after(d2) ) return 1;
				else return -1;
	        }
	};
	
	//=============== File Locking methods =================================
	
	private static synchronized String getLockIDold()
	{
		String lockID = null;
		try {
			String hostIPAddress = InetAddress.getLocalHost().getHostAddress();
			String currentTime = Long.toString(System.currentTimeMillis());
	
			lockID = hostIPAddress + "-" + currentTime;
		}
		catch (Exception e) {
		}
		return lockID;
	}
	
	/**
	 * synchronized lockFile to avoid more than one thread try to create
	 * the same lock file
	 */
	private static synchronized boolean lockFileold(String path, String fileName)
	{
	        String lockFileName = fileName+".lck";
	
	        try
	        {
	            if ( fileLocked(path, fileName) == false ) 
	            {
	                File 	flck 	= new File(path, lockFileName);
					String 	lockID 	= getLockID();
	
					logmessage("locked ID:'"+lockID+"'"+"len:"+lockID.length());
	
					if ( lockID == null ) return false;
	                if ( flck.createNewFile() == false ) return false;
	
					RandomAccessFile rf = new RandomAccessFile(flck.getAbsoluteFile(),"rw");
					rf.seek(rf.length());
					rf.writeUTF(lockID);
					//rf.flush();
					rf.seek(0);
					//String savedLockID = rf.readLine();
					String savedLockID = rf.readUTF();
					rf.close();
					//logmessage("locked ID:'"+lockID+"'");
					logmessage("Saved locked ID:'"+savedLockID+"'"+"len:"+savedLockID.length());
					if ( savedLockID.compareTo(lockID) == 0 ) {
						logmessage("Lock IDs are the same");
						return true;
					}
					else {
						logmessage("Lock IDs are not the same");
						logmessage("compare value:" + savedLockID.compareTo(lockID));
						return false;
					}
	            }
	            else
	                return false;
	        }
	        catch ( IOException e ) 
	        {
	            return false;
	        }
	}
	
	private static synchronized String getLockID()
	{
		String lockID = null;
		try {
			String hostIPAddress = InetAddress.getLocalHost().getHostAddress();
			String currentTime = Long.toString(System.currentTimeMillis());
			Random rd = new Random(System.currentTimeMillis());
			//String hubID = SystemProperties.getInstance().getProperty("SID_ID"); //Add hubID to allow more than one IS on the same machine
			//lockID = hostIPAddress + "-" + hubID + "-" + currentTime + "-" + rd.nextDouble();
			lockID = hostIPAddress + "-" + currentTime + "-" + rd.nextDouble();
		}
		catch (Exception e) {
		}
		return lockID;
	}
	
	/**
	 * synchronized lockFile to avoid more than one thread try to create
	 * the same lock file
	 * Please do not modify this function w/o discussion with the original developer
	 * first.
	 */
	private static synchronized boolean lockFile(String path, String fileName)
	{
	        String lockFileName = fileName+".lck";
	
	        try
	        {
	            if ( fileLocked(path, fileName) == false ) 
	            {
	                File 	flck 	= new File(path, lockFileName);
					String 	lockID 	= getLockID();
	
					//logmessage("locked ID:'"+lockID+"'"+"len:"+lockID.length());
	
					if ( lockID == null ) return false;
	                if ( flck.createNewFile() == false ) return false;
	
					//Thread.sleep(50);
	
					flck 	= new File(path, lockFileName);
					if ( flck.length() > 0 ) return false;
	
					RandomAccessFile rf = new RandomAccessFile(flck.getAbsoluteFile(),"rw");
					rf.seek(flck.length());
					rf.writeUTF(lockID);
					rf.close();
					//Thread.sleep(300);
					rf = new RandomAccessFile(flck.getAbsoluteFile(),"r");
					rf.seek(0);
					String savedLockID = rf.readUTF();
					rf.close();
					//logmessage("Saved locked ID:'"+savedLockID+"'"+"len:"+savedLockID.length());
					if ( savedLockID.compareTo(lockID) == 0 ) {
						//logmessage("Lock IDs are the same");
						return true;
					}
					else {
						//logmessage("Lock IDs are not the same");
						//logmessage("compare value:" + savedLockID.compareTo(lockID));
						return false;
					}
	            }
	            else
	                return false;
	        }
	        catch ( IOException e ) 
	        {
	            return false;
	        }
			catch( Exception e )
			{
				return false;
			}
	}
	
	/**
	 * Check if a file is locked by another thread or process
	 */
	private static boolean fileLocked(String path, String fileName)
	{
	        String lockFileName = fileName+".lck";
	        boolean rv = false;
			
			File f = new File(path, fileName);
			if ( f.exists() == true ) {
		        File flck = new File(path, lockFileName);
	    	    if ( flck.exists() == true )
	        	{
	            	if ( fileLockExpired(path, fileName) == true )
	            	{
	                	releaseFileLock(path, fileName);
	                	rv = false;
	            	}
	            	else
	                	rv = true;
	        	}
	        	else
	            	rv = false;
			}
			else
				rv = false;
	
	        return rv;
	}
	    
	/**
	 * Release a lock file
	 */
	private static boolean releaseFileLock(String path, String fileName)
	{
			String lockFileName = null;
			if ( fileName.endsWith(".lck") == true )
				lockFileName = fileName;
			else
	        	lockFileName = fileName+".lck";
	
			//logmessage("deleteing lock file: " + path+"/"+lockFileName);
	        File flck = new File(path, lockFileName);
	        flck.delete();
	
	        return true;
	}
	
	private static boolean renewFileLock(String path, String fileName)
	{
		try {
			String lockFileName = null;
			if ( fileName.endsWith(".lck") == true )
				lockFileName = fileName;
			else
	        	lockFileName = fileName+".lck";
	
			//logmessage("deleteing lock file: " + path+"/"+lockFileName);
	        File flck = new File(path, lockFileName);
			if (flck.exists())
		        flck.setLastModified(System.currentTimeMillis());
			else
				flck.createNewFile(); 
	
	        return true;
		}
		catch (Exception e) {
			logmessage("FxMailboxManager.Private.Shared:renewFileLock(): Uable to renew lock file: " + 
				path + "/"+ fileName + " Exception: " + e.toString());
	
			return false;
		}
	}
	
	/**
	 * Check if a lock is expired.
	 */
	private static boolean fileLockExpired(String path, String fileName)
	{
			String lockFileName = null;
			if ( fileName.endsWith(".lck") == true ) {
				lockFileName = fileName;
				fileName = lockFileName.substring(0,lockFileName.length()-4);
			}
			else
	        	lockFileName = fileName+".lck";
	
	        //int     lockLifeTime = 40; // in minutes
			//int     lockLifeTime = 15; // in minutes
			int     lockLifeTime = 30; // in minutes
	
	        File flck = new File(path, lockFileName);
	
	        long lastModifiedTime = flck.lastModified();
	        long curTime = System.currentTimeMillis();
	
	        // if the lock file is older than lockLifeTime
	        // return true, so file lock can be release.
	        if ( ((curTime - lastModifiedTime)/1000 ) > (60*lockLifeTime) )
	            return true;
	        else {
				File f = new File(path, fileName);
				if ( (f.exists() == false) || 
					 ((f.lastModified() - flck.lastModified()) > 5000) ) // data file is 5+ seconds newer than lock file
					return true;
				else
		            return false;
			}
	}
	
	// --- <<IS-END-SHARED-SOURCE-AREA>> ---
	
}
