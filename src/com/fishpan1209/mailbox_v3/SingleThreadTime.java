package com.fishpan1209.mailbox_v3;

import java.lang.management.*;

class SingleThreadTime {
	private ThreadMXBean bean;
	
	public SingleThreadTime(){
		this.bean = ManagementFactory.getThreadMXBean();
	}
	
	/** Get CPU time in nanoseconds. */
	public long getCpuTime() {
		return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0L;
	}

	/** Get user time in nanoseconds. */
	public long getUserTime() {
		return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadUserTime() : 0L;
	}

	/** Get system time in nanoseconds. */
	public long getSystemTime() {
		return bean.isCurrentThreadCpuTimeSupported() ? (bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime())
				: 0L;
	}
}
