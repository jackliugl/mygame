package com.lgl.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class CanvasMessage {

	private String name;
	private String sessionID;
	private boolean clear;
	private List<Line> lines = null;
	
	public CanvasMessage() {
	}
	
	public CanvasMessage(String name, String sessionID) {
		this.name = name;
		this.sessionID = sessionID;
	}

	public boolean isClear() {
		return clear;
	}

	public void setClear(boolean clear) {
		this.clear = clear;
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

	public List<Line> getLines() {
		return lines;
	}

	public void setLines(List<Line> lines) {
		this.lines = lines;
	}

	@Setter
    @Getter
    static class Line {
		private Position start;
		private Position stop;
		private String color;
		private int width;
		
		public Line() {
		}

        @Setter
        @Getter
        static class Position {
			private int x;
			private int y;

			public Position() {
			}

        }
	}
	
}
