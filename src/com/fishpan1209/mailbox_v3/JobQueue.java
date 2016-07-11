package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;



public class JobQueue {
	final LinkedBlockingQueue<String> jobqueue;
	public JobQueue(){
		this.jobqueue = new LinkedBlockingQueue<String>();
	}
	
	public LinkedBlockingQueue<String> buildJobQueue(String dir){
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
					this.jobqueue.add(dir+file);
					}
				}
			} 	
		return this.jobqueue;
	}
/*	
	public static void main(String args[]) throws InterruptedException{
		JobQueue job = new JobQueue();
		String dir = "/users/aojing/dropbox/liaison/project/data/test/";
		job.buildJobQueue(dir);
		while(!job.jobqueue.isEmpty()){
			System.out.println(job.jobqueue.take());
		}
	}
	
*/
}
