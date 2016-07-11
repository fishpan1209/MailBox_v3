package com.fishpan1209.mailbox_v3;
/*
Transfer7:
directory creation: java IO
header copying: combine header and body first
file copying: java NIO byteBuffer 
*/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SingleCopyer {

	public SingleCopyer() {

	}

	public void transferDir(File srcDir, File destDir) {
		// make sure source exists
		if (!srcDir.exists()) {
			System.out.println("Directory does not exist.");
			System.exit(0);
		} else {
			if (srcDir.isDirectory()) {
				// if destination directory not exists, create it
				if (!destDir.exists()) {
					destDir.mkdir();
				}else{
					destDir.delete();
					destDir.mkdir();
				}

				// list all the directory contents
				String files[] = srcDir.list();

				for (String file : files) {
					// construct the src and dest file structure
					File srcFile = new File(srcDir, file);
					File destFile = new File(destDir, file);
					// recursive copy
					transferDir(srcFile, destFile);
				}
			} else {
				// if file, then copy it
				// File file = new File(srcDir.getAbsolutePath());
				ProcessFile processer = new ProcessFile(srcDir.toPath());
				String header = processer.getHeader();
				sendFile(srcDir, header, destDir);
				// System.out.println("File copied from " + srcDir + " to " +
				// destDir);
			}
		}
	}

	public void sendFile(File file, String header, File destDir) {
		try {
			// System.out.println("Sending file: " + file.getName());
			// System.out.println(destDir.getPath());

			try {
				// input file
				RandomAccessFile inFile = new RandomAccessFile(file, "rw");
				FileChannel inchannel = inFile.getChannel();
				long fileSize = inchannel.size();
				
				// create output file
				RandomAccessFile outFile = new RandomAccessFile(destDir, "rw");
				FileChannel outchannel = outFile.getChannel();
				
				// get file size
			
				ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
				
				// write header first
				//System.out.println(header);
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
/*
	public static void main(String[] args) {
		String srcPath = "/users/aojing/documents/workspace-mars/FileTransfer_v0/Input/test";
		String destPath = "/users/aojing/documents/workspace-mars/FileTransfer_v0/Output/";
		File srcDir = new File(srcPath);
		File destDir = new File(destPath);
		Transfer7 t7 = new Transfer7();
		long startTime = System.currentTimeMillis();
		t7.transferDir(srcDir, destDir);
		long endTime = System.currentTimeMillis();
		System.out.println("Total Time of transfer7 is  " + (endTime - startTime) + "ms");
	}
*/
}
