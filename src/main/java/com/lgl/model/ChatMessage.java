package com.lgl.model;

import java.util.Date;

public class ChatMessage {

	private Date timestamp;
	private String name;
	private String message;
	private boolean isAdminMessage = false;
	
	public ChatMessage() {
	}

	public ChatMessage(String name, String message) {
		this.timestamp = new Date();
		this.name = name;
		this.message = message;
	}

	// for admin message
	public ChatMessage(String message) {
		this.timestamp = new Date();
		this.name = "";
		this.isAdminMessage = true;
		this.message = message;
	}
	
	public boolean isAdminMessage() {
		return isAdminMessage;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
