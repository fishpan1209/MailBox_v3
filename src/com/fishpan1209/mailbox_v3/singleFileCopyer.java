package com.fishpan1209.mailbox_v3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class singleFileCopyer implements Callable {
	private final String mailslotPath;
	private final String mailslotDest;
	private final String fileName;
	

	public singleFileCopyer(String mailslotPath, String mailslotDest, String fileName) {
		this.mailslotPath = mailslotPath;
		this.mailslotDest = mailslotDest;
		this.fileName = fileName;
	}
	
	@Override
	public Long call() {
        long start = System.currentTimeMillis();
     
		try {
			// input file
			String fileSrc = mailslotPath+"/"+fileName;
			RandomAccessFile inFile = new RandomAccessFile(fileSrc, "rw");
			FileChannel inchannel = inFile.getChannel();
			long fileSize;
			try {
				fileSize = inchannel.size();
				// create output file
				String fileDest = mailslotDest+"/"+fileName;
				RandomAccessFile outFile = new RandomAccessFile(fileDest, "rw");
				FileChannel outchannel = outFile.getChannel();

				// get file size

				ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);

				// write header first
				// System.out.println(header);
				ProcessFile processfile = new ProcessFile(Paths.get(fileSrc));
				String header = processfile.getHeader();

				try {
					outchannel.write(ByteBuffer.wrap(header.getBytes()));
					// write file body
					int bytesRead = inchannel.read(buffer);
					while (bytesRead != -1) {
						buffer.flip(); // make buffer ready for read
						while (buffer.hasRemaining()) {
							outchannel.write(buffer); // write to output file
						}
						buffer.clear();
						bytesRead = outchannel.read(buffer); // reset

					}
					// System.out.println("End of file reached.."+"\n");
					inFile.close();
					outFile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			System.out.println("FILE NOT FOUND EXCEPTION");
			e.getMessage();
		}
		
		// release file lock after copying
		releaseFileLock(mailslotPath, fileName);
		return System.currentTimeMillis()-start;

	}
	
	private static boolean releaseFileLock(String path, String fileName)
	{
			String lockFileName = null;
			if ( fileName.endsWith(".lck") == true )
				lockFileName = fileName;
			else
	        	lockFileName = fileName+".lck";
	
			//logmessage("deleteing lock file: " + path+"/"+lockFileName);
	        File flck = new File(path, lockFileName);
	        flck.delete();
	
	        return true;
	}
}
