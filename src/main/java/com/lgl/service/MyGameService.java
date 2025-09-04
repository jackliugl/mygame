package com.lgl.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.lgl.model.CanvasMessage;
import com.lgl.model.ChatMessage;
import com.lgl.model.GameMessage;
import com.lgl.model.GameStatus;
import com.lgl.model.GameStatus.ActiveUser;

public interface MyGameService extends MyWebSocketService {

	// shared map to store all chat users (<sim sessionid, ActiveUser>)
	static Map<String, ActiveUser> activeUserMap = new ConcurrentHashMap<String, ActiveUser>();

	static List<CanvasMessage> syncList = Collections.synchronizedList(new ArrayList<CanvasMessage>());
	
	static GameStatus gameStatus = new GameStatus();
	
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

	public void sendMessage(String sessionID, GameMessage message);

	public void sendMessageToAllOthers(String sessionID, GameMessage message);
	
	public void sendMessageToUser(String sessionID, GameMessage message);
	
	default public void join(String sessionID, String userName) {
		//sendMessage(sessionID, new CanvasMessage(userName, sessionID));
		activeUserMap.put(sessionID, new ActiveUser(userName, getUserIcon(userName.toLowerCase()), sessionID));
	}
	
	default public void leave(String sessionID) {
		if (activeUserMap.containsKey(sessionID)) {
			activeUserMap.remove(sessionID);
		}
	}

	public GameMessage processGameMessage(String sessionID, GameMessage req);
	
	public String getUserIcon(String name);
	
}
