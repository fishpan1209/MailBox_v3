package com.fishpan1209.mailbox_v3;

import java.util.concurrent.LinkedBlockingQueue;

public class MailslotWorker {
	private LinkedBlockingQueue<String> mailslots;
	public MailslotWorker(LinkedBlockingQueue<String> mailslots){
		this.mailslots = mailslots;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
