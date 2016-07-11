package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Manager {
	

	public static void main(String[] args) throws InterruptedException {
		if (args.length < 4) {
			throw new IllegalArgumentException("Not enough args");
		}
		
		System.out.println("Testing new method: ");
		JobQueue job = new JobQueue();
		long timeoutS = 100; // timeout in seconds
		
		// Experiment1: test single file
		// args[0]: path of testing file
		System.out.println("\nExperiment 1: test a single file with new method: ");
		LinkedBlockingQueue<String> jobq1 = job.buildJobQueue(args[0]);
		System.out.println("Total number of jobs to be processed: "+jobq1.size());
		WorkQueue workq1 = new WorkQueue(1, jobq1);
		workq1.getFileListOfSrcDir(timeoutS);
		
		
		// Experiment2: test a small folder
		// args[1]: path of testing folder
		System.out.println("\nExperiment 2: test a small folder with new method: ");
		LinkedBlockingQueue<String> jobq2 = job.buildJobQueue(args[1]);
		System.out.println("Total number of jobs to be processed: "+jobq2.size());
		WorkQueue workq2 = new WorkQueue(1, jobq2);
		workq2.getFileListOfSrcDir(timeoutS);
		
		/*
		// Experiment3: test a large folder
		// args[2]: path of testing folder
		System.out.println("\nExperiment 3: test a large folder with new method: ");
		LinkedBlockingQueue<String> jobq3 = job.buildJobQueue(args[2]);
		System.out.println("Total number of jobs to be processed: "+jobq3.size());
		WorkQueue workq3 = new WorkQueue(1, jobq3);
		workq3.getFileListOfSrcDir(timeoutS);
		
		// Experiment4: test multiple folders
		// args[3]: path of testing directory
		// args[4]: number of threads
		System.out.println("\nExperiment 4: test multiple folders with new method: ");
		LinkedBlockingQueue<String> jobq4 = job.buildJobQueue(args[3]);
		System.out.println("Total number of jobs to be processed: "+jobq4.size());
	    int numWorker = Integer.parseInt(args[4]);
		WorkQueue workq4 = new WorkQueue(numWorker, jobq4);
		workq4.getFileListOfSrcDir(timeoutS);
		
		Thread.sleep(10000);
		
		
		// test old method
		System.out.println("Testing old method: ");
		OldWorker oldworker = new OldWorker();
		
		// Experiment1: test single file
		// args[0]: path of testing file
		System.out.println("\nExperiment 1: test a single file with old method: ");
		try {
			oldworker.getFileListFolder(args[0]+"test1/");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Experiment2: test a small folder
		// args[1]: path of testing folder
		System.out.println("\nExperiment 2: test a small folder with old method: ");
		try {
			oldworker.getFileListFolder(args[1]+"test2/");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Experiment3: test a large folder
		// args[2]: path of testing folder
		System.out.println("\nExperiment 3: test a large folder with old method: ");
		try {
			oldworker.getFileListFolder(args[2]+"test3/");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Experiment4: test a directory
		// args[3]: path of testing directory
		System.out.println("\nExperiment 4: test multiple folders with old method: ");
		oldworker.getFileListDir(args[3]);
		
		Thread.sleep(10000);
		
		*/
		System.exit(0);
	}

}
