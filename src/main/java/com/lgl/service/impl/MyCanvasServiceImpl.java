package com.lgl.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.lgl.model.CanvasMessage;
import com.lgl.model.ChatMessage;
import com.lgl.service.MyCanvasService;
import com.lgl.service.MyChatService;

@Component
public class MyCanvasServiceImpl implements MyCanvasService {

	SimpMessagingTemplate simpMessagingTemplate = null;
	
	public MyCanvasServiceImpl() {
		super();
	}

	public void setSimpMessagingTemplate(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

	public SimpMessagingTemplate getSimpMessagingTemplate() {
		return simpMessagingTemplate;
	}

	@Override
	public CanvasMessage processCanvasMessage(String sessionID, CanvasMessage message) {
		
		// name only message is a join request
		if (message.getName() != null && message.getLines() == null && !message.isClear()) {
			// this is a join request
			this.join(sessionID, message.getName());
			
			// send all historical CanvasMessages
			synchronized (syncList) {
				syncList.forEach(x -> sendMessage(sessionID, x));
			}
			
			return null;
		} 
		
		// clear request
		if (message.isClear()) {
			synchronized (syncList) {
				// clear history
				syncList.clear();
			}			
		}
		
		// set session id
		message.setSessionID(sessionID);
		
		synchronized (syncList) {
			// add to list to keep history
			syncList.add(message);
		}
		
		return message;
	}

	@Override
	public void sendMessage(String sessionID, CanvasMessage message) {
		simpMessagingTemplate.convertAndSend("/topic/canvas", message);
	}
}
