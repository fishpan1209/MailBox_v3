package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class CopyWorker {
	private int numWorkers;
	private String mailslotPath;
	private String mailslotDest;
	private String[] filelist;
	private boolean debug;
	private LinkedBlockingQueue<String> copyq;
	
	
	public CopyWorker(int numWorkers, String mailslotPath, String mailslotDest, String[] filelist, boolean debug){
		this.numWorkers = numWorkers;
		this.mailslotPath = mailslotPath;
		this.mailslotDest = mailslotDest;
		this.debug = debug;
		
		for(String file : filelist){
			this.copyq.add(file);
		}	
	}
	
	public void fileCopy(long timeoutS) throws InterruptedException {
		if (debug)
			System.out.println("Copying mailslot from " + mailslotPath + " to " + mailslotDest);
		// make dest dir
		File destDir = new File(this.mailslotDest);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		// handle empty filelist
		if (filelist.length == 0)
			return;

		// initiate multiple workers & start copying files from list
		ExecutorService service = Executors.newFixedThreadPool(this.numWorkers);
		ScheduledExecutorService canceller = Executors.newSingleThreadScheduledExecutor();
		List<Future<Long>> jobList = new ArrayList<Future<Long>>();
		long totalTime = 0;
		while (!this.copyq.isEmpty()) {

			try {
				String fileName = copyq.take();
				if (debug)
					System.out.println("Processing file: " + fileName);
				String fileSrc = mailslotPath + "/" + fileName;
				String fileDest = mailslotDest + "/" + fileName;
				singleFileCopyer singleCP = new singleFileCopyer(fileSrc, fileDest);
				@SuppressWarnings("unchecked")
				Future<Long> future = service.submit(singleCP);
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
					totalTime += fur.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		service.shutdown();
		service.awaitTermination(1, TimeUnit.HOURS); // wait until all threads
														// terminate

		// measure wall-clock elasped time
		System.out.println("All copying task completed, wall-clock elapsed time: " + totalTime + "ms");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
