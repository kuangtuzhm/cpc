package com.jimi.cpc.util.mq;

public enum JMSMode {
	TOPIC("topic"), QUEUE("queue");
	
	String name;
	
	JMSMode(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
