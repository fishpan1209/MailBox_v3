package com.fishpan1209.mailbox_v3;
import java.io.File;
// a MailslotWorker processes all mailslots of a owner(get list first then copy)
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public class MailslotWorker implements Callable {
	private String owner;
	private LinkedBlockingQueue<String> mailslots;
	private MysqlConnection conn;
	private String ownerDest;
	private boolean debug;
	private getFileListNew listWorker;
	

	public MailslotWorker(String owner, LinkedBlockingQueue<String> mailslots, MysqlConnection conn, String ownerDest, boolean debug) {
		this.owner = owner;
		this.mailslots = mailslots;
		this.conn = conn;
		this.ownerDest = ownerDest;
		this.debug = debug;
		this.listWorker = new getFileListNew();
	}
    
	@Override
	public Long call() {
		long start = System.currentTimeMillis();
		// get filelist, lock files
		while (!mailslots.isEmpty()) {
			try {
				String mailslot = mailslots.take();
				String fullPathName = conn.getFullPath(owner, mailslot);
				
				if(debug) System.out.println("\n Processing mailslot located at: "+fullPathName);

				String[] fileList = this.listWorker.getFileList(fullPathName);
				
		        // copy fileList, if empty, simply create mailslot folder
				String mailslotDest = ownerDest+"/"+mailslot;
				int numCopyWorkers = fileList.length / 200 + 1;
				long timeoutS = 1000;
				
				try {
					CopyWorker cpWorker = new CopyWorker(numCopyWorkers, fullPathName, mailslotDest, fileList, debug);
					cpWorker.fileCopy(timeoutS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}

		long end = System.currentTimeMillis();
		return (end - start);
	}

}
