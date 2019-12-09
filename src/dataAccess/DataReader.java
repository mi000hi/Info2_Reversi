package dataAccess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DataReader {
	
	private String filename;
	private final int BOARDSIZE;
	
	public DataReader(String filename, int boardsize) {
		this.filename = filename;
		BOARDSIZE = boardsize;
	}
	
	/**
	 * reads the board ratings for the different moves, stored in the file FILENAME
	 * 
	 * @return
	 */
	public ArrayList<double[][]> readRatingsFromFile() {

		double[][] currentBoardRatings = new double[BOARDSIZE][BOARDSIZE];
		ArrayList<double[][]> boardRatings = new ArrayList<>();
		boolean readingRatingsNow = false;
		int currentY = 0;
		String sCurrentLine;
		String[] ratingsString;

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

			while ((sCurrentLine = br.readLine()) != null) {
				
				if (sCurrentLine.contains("MOVE")) {

					// prepare variables to read the ratings matrix
					readingRatingsNow = true;
					currentY = 0;
					continue; // goto next line to read the ratings
				}

				// read the boardRatings
				if (readingRatingsNow) {

					// split line into ratings
					ratingsString = sCurrentLine.split("\\t");

					// get the double values
					for (int x = 0; x < currentBoardRatings.length; x++) {
						currentBoardRatings[x][currentY] = Double.parseDouble(ratingsString[x]);
						
//						if(boardRatings.size() == 0) {
//							System.out.print(ratingsString[x]);
////							System.out.print(Double.parseDouble(ratingsString[x]) + "\t");
//						}
					}
					
//					if(boardRatings.size() == 0) {
//						System.out.println("");
//					}

					currentY++;

					// add the board to the boardRatings
					if (currentY >= currentBoardRatings.length) {
						readingRatingsNow = false;

						boardRatings.add(currentBoardRatings);
						
						currentBoardRatings = new double[BOARDSIZE][BOARDSIZE]; // need to do this because this is on the heap!!!!!!
						
//						System.out.println("read board from file: board nr " + (boardRatings.size() - 1));
//						if(boardRatings.size() == 1) {
//							printRatingsBoard(boardRatings, 0);
//						}
					}

				}

			}

			System.out.println("boardRatings SUCCESSFULLY read from file " + filename);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return boardRatings;

	}

}
