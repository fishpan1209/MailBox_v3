package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fishpan1209.mailbox_v3.WorkQueue.Worker;

public class Manager {
	private LinkedBlockingQueue<String> owners;
	private int numWorker;
	private MysqlConnection conn;

	public Manager(LinkedBlockingQueue<String> owners, int numWorker, MysqlConnection conn){
		this.owners = owners;
		this.numWorker = numWorker;
		this.conn = conn;
	}
	
	public void getListAndCopy(String dest, boolean debug, long timeoutS) {
		// create dest folder if not exists
		File destDir = new File(dest);
		if(!destDir.exists()){
			destDir.mkdirs();
		}
		// initiate numWorkers for all owners
		if(debug) System.out.println("\n Prepare getting file list and copying of mailbox to destination "+dest);
		ExecutorService service = Executors.newFixedThreadPool(this.numWorker);
		ScheduledExecutorService canceller = Executors.newSingleThreadScheduledExecutor();
		List<Future<Long>> jobList = new ArrayList<Future<Long>>();
		int totalTime = 0;
		
		if(debug) System.out.println("\n Number of owners to process: "+owners.size());

		while (!owners.isEmpty()) {
			try {
				String owner = owners.take();
				if(debug) System.out.println("\n Processing owner : "+owner);
				
				// create folder for each owner if not exists
				String ownerDest = dest+"/"+owner;
				File ownerDir = new File(ownerDest);
				if(!ownerDir.exists()){
					ownerDir.mkdirs();
				}
				
				// get list of all mailslots belong to the owner
				LinkedBlockingQueue<String> mailslots = conn.getMailslotList(owner);
				MailslotWorker worker = new MailslotWorker(owner,mailslots,conn, ownerDest, debug);
				Future<Long> future = service.submit(worker);
				jobList.add(future);
				canceller.schedule(new Callable<Void>() {
					public Void call() {
						future.cancel(true);
						return null;
					}
				}, timeoutS, TimeUnit.SECONDS);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			for (Future<Long> fur : jobList) {
				try {
					totalTime += fur.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		try {
			service.shutdown();
			service.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // wait until all threads terminate

		// measure wall-clock elasped time
		System.out.println("All get list and copy task completed, wall-clock elapsed time: " + totalTime + "ms");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* parameters
    args[0]: mailbox path
	args[1]: number of threads for getFileList for all owners
	args[2]: copy destination
	args[3]: timeout in seconds
	args[4]: debug
	*/
	public static void main(String[] args) throws InterruptedException {
		if (args.length < 3) {
			throw new IllegalArgumentException("Not enough args");
		}
		
		System.out.println("Mailbox manager starts scanning mailbox: "+args[0]);
		// open a mysql connection, get owner information
		String db_driver = "com.mysql.jdbc.Driver";
		String dbURL = "jdbc:mysql://localhost:3306/MailBox";
		String user = "test";
		String password = "123456";
		MysqlConnection conn = new MysqlConnection(db_driver, dbURL, user, password);
		
		LinkedBlockingQueue<String> owners = conn.getOwnerList();
		int numWorker = Integer.parseInt(args[1]);
		Manager manager = new Manager(owners, numWorker, conn);
		// timeout in seconds
		String dest = args[2];
		long timeoutS = Long.parseLong(args[3]);
		boolean debug = args[4]=="Y"? true : false;
		
		// for each owner, get file list and copy files to destination
		manager.getListAndCopy(dest, debug, timeoutS);
		System.exit(0);
	}

}
