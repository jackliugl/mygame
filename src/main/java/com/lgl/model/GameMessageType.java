package com.lgl.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GameMessageType {
	DUMMY 					(0),
	USER_LOGIN_REQ 			(1),
	USER_LOGIN_RESP 		(2),
	
	USER_START_GAME_REQ		(3),
	USER_START_GAME_RESP	(4),
	
	USER_WORD_LIST_REQ		(5),
	USER_WORD_LIST_RESP		(6),
	
	USER_CHOOSE_WORD_REQ	(7),
	USER_CHOOSE_WORD_RESP	(8),
	
	USER_GUESS_WORD_REQ		(9),
	USER_GUESS_WORD_RESP	(10),

	SERVER_NEXT_USER_REQ	(11),
	SERVER_GAME_STATUS		(12),
	
	SERVER_GAME_OVER		(13),
	
	SERVER_USER_DISCONNECT	(14),
	
	USER_SERVER_HEARTBEAT   (15);
	
	private int value;
	
	GameMessageType(int value) {
		this.value = value;
	}

	@JsonValue
	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}
