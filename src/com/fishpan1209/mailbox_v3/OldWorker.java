package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.io.IOException;

public class OldWorker {
	
	private getFileList_Original gfl;
	
	
	public OldWorker(){
		this.gfl = new getFileList_Original();
		
	}
	
	public void getFileListFolder(String folder) throws IOException{
		boolean debug = false;
		if(debug) System.out.println("Getting file list from: "+folder);
		long start = System.currentTimeMillis();
		gfl.getFileList(folder);
		long end = System.currentTimeMillis();
		System.out.println("All task completed, wall-clock elapsed time: "+(end-start)+"ms");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void getFileListDir(String dir){
		long start = System.currentTimeMillis();
		File srcDir = new File(dir);
		if (!srcDir.exists()) {
			System.out.println("Directory does not exist.");
			System.exit(0);
		} 
		else {
				// list all the directory contents
				String files[] = srcDir.list();

				for (String file : files) {
					if(!file.equals(".DS_Store") && !file.matches("(.*)output.txt")){
						String folder = dir+file;

					    try {
							gfl.getFileList(folder);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		long end = System.currentTimeMillis();
		System.out.println("All task completed, wall-clock elapsed time: "+(end-start)+"ms");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
