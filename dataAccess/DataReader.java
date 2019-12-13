package dataAccess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import randomLearns.totallyRandom;

public class DataReader {

	private final int BOARDSIZE;

	public DataReader(int boardsize) {
		BOARDSIZE = boardsize;
	}

	/**
	 * reads the board ratings for the different moves, stored in the file FILENAME
	 * 
	 * @return
	 */
	public ArrayList<double[][]> readRatingsFromFile(Class c, String filename) {

		double[][] currentBoardRatings = new double[BOARDSIZE][BOARDSIZE];
		ArrayList<double[][]> boardRatings = new ArrayList<>();
		boolean readingRatingsNow = false;
		int currentY = 0;
		String sCurrentLine;
		String[] ratingsString;
		
		InputStream input = c.getResourceAsStream(filename);
        if (input == null) {
            // this is how we load file within editor (eg eclipse)
            input = c.getClassLoader().getResourceAsStream(filename);
        }

		try (BufferedReader br = new BufferedReader(new InputStreamReader(input))){//new FileReader(filename))) {

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

						currentBoardRatings = new double[BOARDSIZE][BOARDSIZE]; // need to do this because this is on
																				// the heap!!!!!!

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

	public double[][] readRatingFromFile(Class c, String filename) {

		double[][] rating = new double[BOARDSIZE][BOARDSIZE];
		boolean readingRatingsNow = false;
		int currentY = 0;
		String sCurrentLine;
		String[] ratingsString;
		InputStream input = c.getResourceAsStream(filename);
        if (input == null) {
            // this is how we load file within editor (eg eclipse)
            input = c.getClassLoader().getResourceAsStream(filename);
        }

		try (BufferedReader br = new BufferedReader(new InputStreamReader(input))){//new FileReader(filename))) {

			while ((sCurrentLine = br.readLine()) != null) {

				if (sCurrentLine.contains("FIELD")) {

					// prepare variables to read the ratings matrix
					readingRatingsNow = true;
					currentY = 0;
					continue; // goto next line to read the ratings
				}

				// read the boardRatings
				if (readingRatingsNow) {

					// split line into ratings
					ratingsString = sCurrentLine.split("\\t");

					// get the int values
					for (int x = 0; x < rating.length; x++) {
						rating[x][currentY] = Double.parseDouble(ratingsString[x]);
					}

					currentY++;

					// finished reading, return
					if (currentY >= rating.length) {
						System.out.println("boardRating SUCCESSFULLY read from file " + filename);
						return rating;
					}

				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.err.println("boardRating not found in file " + filename);
		return rating;

	}

	/**
	 * reads the number of games, stored in the file FILENAME, this code is copy
	 * pasted ;)
	 * 
	 * @return
	 */
	public int readNumberOfGamesFromFile(Class c, String filename) {

		String nrOfGames;
		InputStream input = c.getResourceAsStream(filename);
        if (input == null) {
            // this is how we load file within editor (eg eclipse)
            input = c.getClassLoader().getResourceAsStream(filename);
        }

		try (BufferedReader br = new BufferedReader(new InputStreamReader(input))){//new FileReader(filename))) {

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.contains("numberOfGames")) {
					nrOfGames = sCurrentLine.substring(18);

					System.out.println("variable <numberOfGames> SUCCESSFULLY read from file " + filename);
					return Integer.parseInt(nrOfGames);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.err.println("variable <numberOfGames> not found in the file " + filename);
		return 0;

	}

	/**
	 * prints the given GameBoard to the console like this:
	 */
	public void printRatingsBoard(ArrayList<double[][]> ratingBoards, int index) {

		double[][] boardToPrint = ratingBoards.get(index);// normalize(ratingBoards.get(index));

		System.out.println("+=========================================+");

		for (int y = 0; y < BOARDSIZE; y++) {

			System.out.print("| ");

			for (int x = 0; x < BOARDSIZE; x++) {

				// print the numbers between 0 and 10
				System.out.print(
						adjustStringLength(Integer.toString((int) Math.round(boardToPrint[x][y] * 10)), 3) + " ");

			}

			System.out.println("|");
		}

		System.out.println("+=========================================+");

	}

	/**
	 * returns the str with |str.length - newLength| spaces before it
	 * 
	 * @param str
	 * @param newLength
	 * @return
	 */
	private String adjustStringLength(String str, int newLength) {

		String spaces = "";

		for (int length = str.length(); length <= newLength; length++) {
			spaces += " ";
		}

		return spaces + str;

	}

}
