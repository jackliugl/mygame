package com.lgl.model;

import java.util.ArrayList;
import java.util.List;

public class GameStatus {

	private boolean started = false;
	private String startedUser = "";
	
	private int totalRounds = 5;
	private int currentRound = 0;
	
	private List<ActiveUser> users = null;
	
	private List<String> guessedUsers = new ArrayList<String>();
	
	private String drawingUser = "";
	private String drawingWord = "";

	private int timeRemaining = 0;
	private String drawingWordMask = "";
	
	public GameStatus() {
	}
	
	public String getStartedUser() {
		return startedUser;
	}

	public void setStartedUser(String startedUser) {
		this.startedUser = startedUser;
	}

	public List<ActiveUser> getUsers() {
		return users;
	}

	public void setUsers(List<ActiveUser> users) {
		this.users = users;
	}

	public List<String> getGuessedUsers() {
		return guessedUsers;
	}

	public void setGuessedUsers(List<String> guessedUsers) {
		this.guessedUsers = guessedUsers;
	}

	public String getDrawingUser() {
		return drawingUser;
	}

	public void setDrawingUser(String drawingUser) {
		this.drawingUser = drawingUser;
	}

	public String getDrawingWord() {
		return drawingWord;
	}

	public void setDrawingWord(String drawingWord) {
		this.drawingWord = drawingWord;
	}

	public int getTimeRemaining() {
		return timeRemaining;
	}

	public void setTimeRemaining(int timeRemaining) {
		this.timeRemaining = timeRemaining;
	}

	public String getDrawingWordMask() {
		return drawingWordMask;
	}

	public void setDrawingWordMask(String drawingWordMask) {
		this.drawingWordMask = drawingWordMask;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public int getTotalRounds() {
		return totalRounds;
	}

	public void setTotalRounds(int totalRounds) {
		this.totalRounds = totalRounds;
	}

	public int getCurrentRound() {
		return currentRound;
	}

	public void setCurrentRound(int currentRound) {
		this.currentRound = currentRound;
	}

	static public class ActiveUser {
		private String name;
		private String sessionID;
		
		// calculated based on time taken and if he is the drawing person
		private int score;
		
		private boolean done = false;
		private long lastTS = 0;
		private boolean ghost = false;
		private String icon = "";

		public ActiveUser(String name, String icon, String sessionID) {
			this.name = name;
			this.sessionID = sessionID;
			this.score = 0;
			this.done = false;
			this.lastTS = System.currentTimeMillis();
			this.ghost = false;
			this.icon = icon;
		}

		public String getIcon() {
			return icon;
		}

		public void setIcon(String icon) {
			this.icon = icon;
		}

		public boolean isGhost() {
			return ghost;
		}

		public void setGhost(boolean ghost) {
			this.ghost = ghost;
		}

		public long getLastTS() {
			return lastTS;
		}

		public void setLastTS(long lastTS) {
			this.lastTS = lastTS;
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

		public int getScore() {
			return score;
		}

		public void setScore(int score) {
			this.score = score;
		}

		public boolean isDone() {
			return done;
		}

		public void setDone(boolean done) {
			this.done = done;
		} 
		
	}
	
}
