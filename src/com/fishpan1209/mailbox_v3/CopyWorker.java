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
	private String[] filelist;
	private LinkedBlockingQueue<String> jobq;
	private String dest;
	private boolean debug;
	
	
	public CopyWorker(int numWorkers, String folder, String dest, String[] filelist){
		this.numWorkers = numWorkers;
		this.dest = dest+folder;
		this.jobq = new LinkedBlockingQueue<String>();
		for(String file : filelist){
			jobq.add(file);
		}
		
	}
	
	public void fileCopy(long timeoutS) throws InterruptedException{
		    boolean debug = true;
		
			// make dest dir
			File destDir = new File(this.dest);
			if (!destDir.exists()) {
				destDir.mkdir();
			}else{
				destDir.delete();
				destDir.mkdir();
			}
			
			// initiate multiple workers & start copying files from list
			ExecutorService service = Executors.newFixedThreadPool(this.numWorkers);
			ScheduledExecutorService canceller = Executors.newSingleThreadScheduledExecutor();
			List<Future<Long>> jobList = new ArrayList<Future<Long>>();
		    long totalTime = 0;
			while (!this.jobq.isEmpty()) {

				try {
					String file = jobq.take();
					if(debug) System.out.println("Processing file: " + file);
					Copyer copyer = new Copyer(file, dest);
					@SuppressWarnings("unchecked")
					Future<Long> future = service.submit(copyer);
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
			System.out.println("All copying task completed, wall-clock elapsed time: "+totalTime+"ms");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

		class Copyer implements Callable {
			private final String file;
			private final String dest;
			private SingleCopyer singleCP;

			public Copyer(String file, String dest) {
				this.file = file;
				this.dest = dest;
				this.singleCP = new SingleCopyer();
			}

			// for each worker, get file list of assigned folder
			// initiate number of copy workers by size/200+1; copy that folder to destination
			@Override
			public Long call() {
				if(debug) System.out.println("Current thread: "+Thread.currentThread().getName());
				long start = System.currentTimeMillis();
				
				copyFile( file, dest);
				
				long end = System.currentTimeMillis();
				return (end-start);
			}

		
	}
		
		public void copyFile(String file, String dest) {
			try {
				String out = dest+file;
				try {
					// input file
					RandomAccessFile inFile = new RandomAccessFile(file, "rw");
					FileChannel inchannel = inFile.getChannel();
					long fileSize = inchannel.size();
					
					// create output file
					RandomAccessFile outFile = new RandomAccessFile(out, "rw");
					FileChannel outchannel = outFile.getChannel();
					
					// get file size
				
					ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
					
					// write header first
					//System.out.println(header);
					ProcessFile processfile = new ProcessFile(Paths.get(file));
					String header = processfile.getHeader();
									
					outchannel.write(ByteBuffer.wrap(header.getBytes()));
					
					// write file body
					int bytesRead = inchannel.read(buffer);
					while(bytesRead != -1){
						buffer.flip(); // make buffer ready for read
						while(buffer.hasRemaining()){
							outchannel.write(buffer); // write to output file
						}
						buffer.clear();
						bytesRead = outchannel.read(buffer); // reset
			
					}
					
					//System.out.println("End of file reached.."+"\n");
					inFile.close();
					outFile.close();

				} catch (FileNotFoundException e) {
					System.out.println("FILE NOT FOUND EXCEPTION");
					e.getMessage();
				}
			} catch (IOException e) {
				System.out.println("IO EXCEPTION");
				e.getMessage();
			}
		}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String[] filelist = new String[10];
		for(int i=0; i<filelist.length; i++){
			filelist[i] = "test"+i+".txt";
		}
		int numWorkers = 2;
		String src = "/users/aojing/dropbox/liaison/project/data/Input/Input3/test";
		String dest = "/users/aojing/dropbox/liaison/project/data/Output/";
		CopyWorker test = new CopyWorker(numWorkers, src, dest, filelist);
		int timeoutS = 100;
		try {
			test.fileCopy(timeoutS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
