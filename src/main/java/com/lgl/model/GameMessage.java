package com.lgl.model;

import java.util.ArrayList;
import java.util.List;

import com.lgl.service.MyGameService;

public class GameMessage {

	private GameMessageType type;
	
	// sender name, null if sent by server
	private String name;
	
	// sender's sessionID, null if sent by server
	private String sessionID;

	private String message = null;
	
	private List<String> wordList = null;
	
	private String wordChosen = "";
	
	private String wordGuessed = "";

	private GameStatus gameStatus = null;
	
	public GameMessage() {
	}

	public GameMessage(GameMessageType type) {
		this.type = type;

		MyGameService.gameStatus.setUsers(new ArrayList<>(MyGameService.activeUserMap.values()));
		this.gameStatus = MyGameService.gameStatus;		
	}
	
	public GameStatus getGameStatus() {
		return gameStatus;
	}

	public void setGameStatus(GameStatus gameStatus) {
		this.gameStatus = gameStatus;
	}

	public GameMessage(String name, String sessionID) {
		this.name = name;
		this.sessionID = sessionID;
	}

	public String getWordChosen() {
		return wordChosen;
	}

	public void setWordChosen(String wordChosen) {
		this.wordChosen = wordChosen;
	}

	public String getWordGuessed() {
		return wordGuessed;
	}

	public void setWordGuessed(String wordGuessed) {
		this.wordGuessed = wordGuessed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public GameMessageType getType() {
		return type;
	}

	public void setType(GameMessageType type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<String> getWordList() {
		return wordList;
	}

	public void setWordList(List<String> wordList) {
		this.wordList = wordList;
	}
}
