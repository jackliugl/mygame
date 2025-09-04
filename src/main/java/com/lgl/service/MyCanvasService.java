package com.lgl.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.lgl.model.CanvasMessage;
import com.lgl.model.ChatMessage;

public interface MyCanvasService extends MyWebSocketService {

	// shared map to store all chat users (<sim sessionid, chat username>)
	static Map<String, String> activeUserMap = new ConcurrentHashMap<String, String>();

	static List<CanvasMessage> syncList = Collections.synchronizedList(new ArrayList<CanvasMessage>());
	
	SimpMessagingTemplate simpMessagingTemplate = null;
	
	public void setSimpMessagingTemplate(SimpMessagingTemplate simpMessagingTemplate);

	public SimpMessagingTemplate getSimpMessagingTemplate();

	/*
	 * Process received CanvasMessage
	 * return a new response CanvasMessage to be sent to all clients
	 * return null if nothing needs to be sent to clients
	 * 
	 */
	public CanvasMessage processCanvasMessage(String sessionID, CanvasMessage message);
	
	public void sendMessage(String sessionID, CanvasMessage message);
	
	default public void join(String sessionID, String userName) {
		sendMessage(sessionID, new CanvasMessage(userName, sessionID));
		activeUserMap.put(sessionID, userName);
	}
	
	default public void leave(String sessionID) {
		if (activeUserMap.containsKey(sessionID)) {
			//sendMessage(sessionID, new CanvasMessage(String.format("<b>%s</b> left the chat.", activeChatUserMap.get(sessionID))));
			activeUserMap.remove(sessionID);
		}
	}
	
}
