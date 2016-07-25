package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.io.FileNotFoundException;
// a MailslotWorker processes all mailslots of a owner(get list first then copy)
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public class MailslotWorker implements Callable {
	private String tableName;
	private String owner;
	private LinkedBlockingQueue<String> mailslots;
	private MysqlConnection conn;
	private String ownerDest;
	private boolean debug;
	private getFileListNew listWorker;
	

	public MailslotWorker(String tableName, String owner, LinkedBlockingQueue<String> mailslots, MysqlConnection conn, String ownerDest, boolean debug) {
		this.tableName = tableName;
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
		long copyTime = 0;
		// get filelist, lock files
		while (!mailslots.isEmpty()) {
			
			try {
				String mailslot = mailslots.take();
				String fullPathName = conn.getFullPath(tableName, owner, mailslot);
				
				if(debug) System.out.println("\n Processing mailslot " +mailslot+" located at: "+fullPathName);

				String[] fileList = this.listWorker.getFileList(fullPathName);
				
				if(debug){
					System.out.println("\nGet file list done, number of files to be processed: "+fileList.length);
					saveResult(fileList, mailslot, ownerDest);
				}
				
		        // copy fileList, if empty, simply create mailslot folder
				String mailslotDest = ownerDest+"/"+mailslot;
				int numCopyWorkers = fileList.length / 200 + 1;
				
				long timeoutS = 10000;
				
			
				try {
					CopyWorker cpWorker = new CopyWorker(numCopyWorkers, fullPathName, mailslotDest, fileList, copyTime, debug);
					copyTime = cpWorker.fileCopy(timeoutS);
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
		return (end - start)+copyTime;
	}

	private void saveResult(String[] fileList, String mailslot, String ownerDest) throws IOException {

		// delete existing output files
		String output = ownerDest+"/"+mailslot + "output.txt";
		
		try {
			RandomAccessFile outFile = new RandomAccessFile(output, "rw");
			// System.out.println(output);
			FileChannel outchannel = outFile.getChannel();
			String header = "List of filenames: \n";
			outchannel.write(ByteBuffer.wrap(header.getBytes()));
			ByteBuffer buffer = ByteBuffer.allocate(fileList.length * 256);
			buffer.clear();
			for (String s : fileList) {
				buffer.put((s + "\n").getBytes());
			}
			buffer.flip();
			while (buffer.hasRemaining()) {
				outchannel.write(buffer);
			}

			outchannel.close();
			outFile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
