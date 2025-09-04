package com.lgl.controller;

import javax.websocket.Session;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lgl.sudoku.Generator;
import com.lgl.sudoku.Grid;
import com.lgl.sudoku.Solver;
import com.lgl.sudoku.SudokuPuzzle;

@RestController
@RequestMapping("/sudoku")
public class SudokuController {

	@GetMapping("/generate")
	public ResponseEntity<SudokuPuzzle> generate(@RequestParam(required = true) Integer numOfCells) {
		
		Grid grid = null;
		String puzzle = "";
		SudokuPuzzle result = null;
		while (true) {
			Generator generator = new Generator();
			grid = generator.generate(81-numOfCells);
	
			puzzle = grid.toString();
			Solver solver = new Solver();
			solver.solve(grid);
			
			result = new SudokuPuzzle(puzzle, grid.toString());
			System.out.println(result);
			
			if (!grid.toString().contains(".")) {
				break;
			}
		}
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

}
