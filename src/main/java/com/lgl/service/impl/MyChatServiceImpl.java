package com.lgl.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.lgl.model.ChatMessage;
import com.lgl.service.MyChatService;

@Component
public class MyChatServiceImpl implements MyChatService {

	SimpMessagingTemplate simpMessagingTemplate = null;
	
	public MyChatServiceImpl() {
		super();
	}

	public void setSimpMessagingTemplate(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

	public SimpMessagingTemplate getSimpMessagingTemplate() {
		return simpMessagingTemplate;
	}

	@Override
	public ChatMessage processChatMessage(String sessionID, ChatMessage message) {
		
		// name only message is a join request
		if (message.getName() != null && message.getMessage() == null) {
			// this is a join request
			this.join(sessionID, message.getName());
			return null;
		} 
		
		// set server timestamp
		message.setTimestamp(new Date());
		return message;
	}

	@Override
	public void sendMessage(String sessionID, ChatMessage message) {
		if (!message.isAdminMessage()) {
			message.setName(activeChatUserMap.get(sessionID));
		}
		simpMessagingTemplate.convertAndSend("/topic/messages", message);
	}
}
