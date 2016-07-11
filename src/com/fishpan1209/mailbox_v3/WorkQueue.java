package com.fishpan1209.mailbox_v3;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.org.glassfish.gmbal.ParameterNames;

import java.util.concurrent.Future;

public class WorkQueue
{
	private static boolean debug = true;   // debug purposes
	private int numWorkers;   // num of threadpool workers for getFileList
	private String dest;  // output destination for file copy
	// private int numCopyWorkers; // num of copy workers
	private LinkedBlockingQueue<String> jobq;  // job queue of folders to be processed
	
	// initiate num of workder for a job
	public WorkQueue(int numWorkers, LinkedBlockingQueue<String> jobq, String dest){
		this.numWorkers = numWorkers;
		this.jobq = jobq;
		this.dest = dest;
	}
	
    // get file list for source directory
	// tiemoutS: timeout in seconds
	public void getFileListOfSrcDir(long timeoutS) throws InterruptedException{
		
		ExecutorService service = Executors.newFixedThreadPool(this.numWorkers);
		ScheduledExecutorService canceller = Executors.newSingleThreadScheduledExecutor();
		List<Future<Long>> jobList = new ArrayList<Future<Long>>();
		int totalTime = 0;
		while (!jobq.isEmpty()) {

			try {
				String folder = jobq.take();
				if(debug) System.out.println("Processing folder: " + folder);
				Worker worker = new Worker(folder, dest);
				@SuppressWarnings("unchecked")
				Future<Long> future = service.submit(worker);
				jobList.add(future);
				canceller.schedule(new Callable<Void>() {
					public Void call() {
						future.cancel(true);
						return null;
					}
				}, timeoutS, TimeUnit.SECONDS);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
			
				for (Future<Long> fur : jobList) {
					try {
						totalTime+=fur.get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		
		service.shutdown();
		service.awaitTermination(1, TimeUnit.HOURS);  // wait until all threads terminate
		
		// measure wall-clock elasped time
		System.out.println("All task completed, wall-clock elapsed time: "+totalTime+"ms");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	class Worker implements Callable {
		private String folderTask;
		private String dest;
		private getFileListNew listWorkerNew;

		public Worker(String folderTask, String dest) {
			this.folderTask = folderTask;
			this.dest = dest;
			this.listWorkerNew = new getFileListNew();
		}

		// for each worker, get file list of assigned folder
		// initiate number of copy workers by size/200+1; copy that folder to destination
		@Override
		public Long call() {
			if(debug) System.out.println("Current thread: "+Thread.currentThread().getName());
			long start = System.currentTimeMillis();
			
			// then process files
			try {
				String[] fileList = this.listWorkerNew.getFileList(folderTask);
				int numCopyWorkers = fileList.length/200+1;
				dest += folderTask;
				int timeoutS = 100;
				try {
					CopyWorker cpWorker = new CopyWorker(numCopyWorkers, folderTask, dest, fileList);
					cpWorker.fileCopy(timeoutS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			long end = System.currentTimeMillis();
			return (end-start);
		}

	
	}

}
