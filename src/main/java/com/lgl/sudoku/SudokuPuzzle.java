package com.lgl.sudoku;

public class SudokuPuzzle {

	private String puzzle;
	private String solved;
	
	public String getPuzzle() {
		return puzzle;
	}
	
	public void setPuzzle(String puzzle) {
		this.puzzle = puzzle;
	}
	
	public String getSolved() {
		return solved;
	}
	
	public void setSolved(String solved) {
		this.solved = solved;
	}
	
	public SudokuPuzzle(String puzzle, String solved) {
		super();
		this.puzzle = puzzle;
		this.solved = solved;
	}

	@Override
	public String toString() {
		return "SudokuPuzzle [puzzle=" + puzzle + ", solved=" + solved + "]";
	}
	
}
