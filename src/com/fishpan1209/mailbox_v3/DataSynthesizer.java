package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DataSynthesizer {
/*
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String dir = "/users/aojing/dropbox/liaison/project/data/Input/Input2/";
		for(int i=1; i<=100; i++){
			String fname = dir+"test"+i+".txt";
			
				RandomAccessFile rf;
				try {
					rf = new RandomAccessFile(fname,"rw");
					FileChannel outchannel = rf.getChannel();
					String body = "This is test file "+i;
					outchannel.write(ByteBuffer.wrap(body.getBytes()));
					outchannel.close();
					rf.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
		}
		
	}
	*/

}
