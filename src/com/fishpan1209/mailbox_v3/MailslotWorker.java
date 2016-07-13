package com.fishpan1209.mailbox_v3;
// a MailslotWorker processes all mailslots of a owner(get list first then copy)
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public class MailslotWorker implements Callable {
	private String owner;
	private LinkedBlockingQueue<String> mailslots;
	private MysqlConnection conn;
	private String dest;
	private getFileListNew listWorker;

	public MailslotWorker(String owner, LinkedBlockingQueue<String> mailslots, MysqlConnection conn, String dest) {
		this.owner = owner;
		this.mailslots = mailslots;
		this.conn = conn;
		this.dest = dest;
		this.listWorker = new getFileListNew();
	}

	public Long call() {
		long start = System.currentTimeMillis();
		// get filelist, lock files
		while (!mailslots.isEmpty()) {
			try {
				String mailslot = mailslots.take();
				String fullPathName = conn.getFullPath(owner, mailslot);

				String[] fileList = this.listWorker.getFileList(fullPathName);
				// then copy files
				int numCopyWorkers = fileList.length / 200 + 1;
				String mailslotDest = dest + mailslot;
				long timeoutS = 1000;
				try {
					CopyWorker cpWorker = new CopyWorker(numCopyWorkers, fullPathName, dest, fileList);
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

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
