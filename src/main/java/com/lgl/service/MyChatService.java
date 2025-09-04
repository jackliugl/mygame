package com.lgl.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.lgl.model.ChatMessage;

public interface MyChatService extends MyWebSocketService {

	// shared map to store all chat users (<sim sessionid, chat username>)
	static Map<String, String> activeChatUserMap = new ConcurrentHashMap<String, String>();

	SimpMessagingTemplate simpMessagingTemplate = null;
	
	public void setSimpMessagingTemplate(SimpMessagingTemplate simpMessagingTemplate);

	public SimpMessagingTemplate getSimpMessagingTemplate();

	/*
	 * Process received ChatMessage
	 * return a new response ChatMessage to be sent to all clients
	 * return null if nothing needs to be sent to clients
	 * 
	 */
	public ChatMessage processChatMessage(String sessionID, ChatMessage message);
	
	public void sendMessage(String sessionID, ChatMessage message);
	
	default public void join(String sessionID, String userName) {
		sendMessage(sessionID, new ChatMessage(String.format("<b>%s</b> joined the chat.", userName)));
		activeChatUserMap.put(sessionID, userName);
	}
	
	default public void leave(String sessionID) {
		if (activeChatUserMap.containsKey(sessionID)) {
			sendMessage(sessionID, new ChatMessage(String.format("<b>%s</b> left the chat.", activeChatUserMap.get(sessionID))));
			activeChatUserMap.remove(sessionID);
		}
	}
	
}
