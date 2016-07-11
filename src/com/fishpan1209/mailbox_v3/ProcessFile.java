package com.fishpan1209.mailbox_v3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class ProcessFile {
	private Path file;
	public ProcessFile(Path file){
		this.file = file;
	}
	
	public String getHeader(){
		StringBuilder data = new StringBuilder();
		data.append("File name: ");
		data.append(file.getFileName());
		try {
			BasicFileAttributes attr;
			attr = Files.readAttributes(this.file, BasicFileAttributes.class);
			long size = attr.size();
			data.append(" ; file size: ");
			data.append(size);
			FileTime time = attr.creationTime();
			data.append(" ; file creation time: ");
			data.append(time);
			data.append("\n");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("File metadata: "+data.toString());
		return data.toString();
	}

}
