package com.lgl.sudoku;

public class testSudoku {

	public static void main(String[] args) {
		Generator generator = new Generator();
		Grid grid = generator.generate(28);

		System.out.println(grid);
		
		Solver solver = new Solver();
		solver.solve(grid);
		System.out.println(grid);
		System.out.println(grid.toStringGrid());
	}

}
