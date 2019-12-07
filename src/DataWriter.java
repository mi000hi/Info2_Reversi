import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import reversi.ReversiPlayer;

public class DataWriter {

	private final String FILENAME; // file to store the data to
	private final ReversiPlayer[] players; // players that are playing the games
	private boolean dontLookAtOldData = false; // true if you want to write the whole file new
	private final int BOARDSIZE; // size of the gameboard
	private boolean fileExists; // false if we need to create the file

	/*
	 * variables stored already in the file FILENAME
	 */
	private int numberOfGames;
	private ArrayList<double[][]> ratings;

	/**
	 * CONSTRUCTOR
	 * 
	 * @param players
	 * @param filename
	 */
	public DataWriter(ReversiPlayer[] players, String filename, boolean trashOldData, int boardSize) {

		// initialize variables
		this.players = players;
		this.FILENAME = filename;
		dontLookAtOldData = trashOldData;
		BOARDSIZE = boardSize;

		// if we merge the data together
		fileExists = (new File(filename)).exists();
		if (!dontLookAtOldData && fileExists) {
			readDataFromFile();
		} else {
			
			// else initialize these variables with zeros
			numberOfGames = 0;
			
			ratings = new ArrayList<>();
			for(int i = 0; i < 60; i++) {
				ratings.add(new double[BOARDSIZE][BOARDSIZE]);
			}
		}

	}

	/**
	 * reads and saves the data currently stored in the file FILENAME
	 */
	public int readDataFromFile() {

		// read oldNumberOfGames
		numberOfGames = readNumberOfGames();

		// read oldRatings
		ratings = new ArrayList<>();
		ratings = readRatingsFromFile();
//		printRatingsBoard(ratings, 0);
		
		return numberOfGames;

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

		try (BufferedReader br = new BufferedReader(new FileReader(FILENAME))) {

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

			System.out.println("boardRatings SUCCESSFULLY read from file " + FILENAME);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return boardRatings;

	}

	/**
	 * reads the number of games, stored in the file FILENAME, this code is copy
	 * pasted ;)
	 * 
	 * @return
	 */
	private int readNumberOfGames() {

		String nrOfGames;

		try (BufferedReader br = new BufferedReader(new FileReader(FILENAME))) {

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.contains("numberOfGames")) {
					nrOfGames = sCurrentLine.substring(18);
					
					System.out.println("variable <numberOfGames> SUCCESSFULLY read from file " + FILENAME);
					return Integer.parseInt(nrOfGames);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("variable <numberOfGames> not found in the file " + FILENAME);

		return 0;

	}

	/**
	 * writes the given string to the requested file
	 * 
	 * @param relativeFilename
	 * @param output
	 * @throws IOException
	 */
	private void writeToFile(String output, boolean append) throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(FILENAME, append));
		writer.write(output);
		writer.close();

	}

	/**
	 * writes the board rating data to the file
	 * 
	 * @param relativeFilename
	 * @param append
	 * @throws IOException
	 */
	public void writeRatingsData(ArrayList<double[][]> ratings, int numberOfGames, boolean append) throws IOException {

		this.numberOfGames += numberOfGames;
		this.ratings = mergeRatings(this.ratings, ratings);

		// write statistic info
		writeToFile("\n-- numberOfGames: " + this.numberOfGames + "\n\n", true);

		// write board ratings
		for (int i = 0; i < ratings.size(); i++) {

			writeToFile("MOVE " + i + "\n", true);

			writeToFile(ratingsToString(this.ratings.get(i)) + "\n\n", true);

		}

	}

	/**
	 * merges the two given ratings matrices together
	 * 
	 * @param ratings01
	 * @param ratings02
	 * @return
	 */
	private ArrayList<double[][]> mergeRatings(ArrayList<double[][]> ratings01, ArrayList<double[][]> ratings02) {

		// if ratings file doesnt exist, return ratings02
		if(!fileExists) {
			return ratings02;
		}
		
		// return null if ratings dont match their dimensions
		if(ratings01.size() != ratings02.size() || ratings01.get(0).length != ratings02.get(0).length) {
			System.out.println("RATINGS MATRICES DONT MATCH!!!");
			return null;
		}
		
		double[][] currentBoardRatings, currentBoardRatings02;

		for (int i = 0; i < ratings01.size(); i++) {

			currentBoardRatings = ratings01.get(i);
			currentBoardRatings02 = ratings02.get(i);

			for(int x = 0; x < currentBoardRatings.length; x++) {
				for(int y = 0; y < currentBoardRatings.length; y++) {
					
					currentBoardRatings[x][y] += currentBoardRatings02[x][y];
					
				}
			}
			
			ratings01.set(i, currentBoardRatings);
			
		}
		
		return ratings01;

	}

	/**
	 * writes the header of the data file into the file (AND POSSIBLY OVERWRITES THE
	 * WHOLE FILE!)
	 * 
	 * @param relativeFilename
	 * @param append
	 * @throws IOException
	 */
	public void writeFileHeader(boolean append) throws IOException {

		// title
		writeToFile("         +==============================================+\n", append);
		writeToFile("         |   BOARD RATING DATA for our reversi player   |\n", true);
		writeToFile("         +==============================================+\n\n", true);

		// playing players
		writeToFile("    " + players[1].getClass().getName() + " vs " + players[2].getClass().getName() + "\n\n", true);

		writeToFile("+================================================================+\n", true);
		writeToFile("+================================================================+\n", true);

		// write the date and time to the file
		Date today = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.YYYY, HH:mm:SS");
		String date = dateFormat.format(today);
		writeToFile("last saved at: " + date + "\n", true);

		writeToFile("+================================================================+\n", true);
		writeToFile("+================================================================+\n", true);

	}

	/**
	 * returns a string containing all the ratings
	 * 
	 * @param ratings
	 * @return
	 */
	private String ratingsToString(double[][] ratings) {

		String result = "";

		for (int y = 0; y < ratings.length; y++) {
			for (int x = 0; x < ratings.length; x++) {

				// add the actual doubles to the string
				result += adjustStringLength(Double.toString(ratings[x][y]), 25) + "\t";

			}

			result += "\n";
		}

		result += "\n";

		return result;

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
	
	/**
	 * prints the given GameBoard to the console like this:
	 */
	private void printRatingsBoard(ArrayList<double[][]> ratingBoards, int index) {

		double[][] boardToPrint = ratingBoards.get(index);//normalize(ratingBoards.get(index));

		System.out.println("+=================+");

		for (int y = 0; y < BOARDSIZE; y++) {

			System.out.print("| ");

			for (int x = 0; x < BOARDSIZE; x++) {

				// print the numbers between 0 and 10
				System.out.print((int) Math.round(boardToPrint[x][y] * 10) + " ");

			}

			System.out.println("|");
		}

		System.out.println("+=================+");

	}

	/**
	 * adjusts the values to a range of 0 to 1
	 * 
	 * @param values
	 * @return
	 */
	private double[][] normalize(double[][] values) {

		double maxValue = maxValue(values);
		double minValue = minValue(values);

		for (int x = 0; x < values.length; x++) {
			for (int y = 0; y < values[0].length; y++) {

				// make the minimum value 0
				values[x][y] -= minValue;

				// make the maximum value 1
				values[x][y] /= maxValue;

			}
		}

		return values;

	}

	/**
	 * returns the maximum value of this 2d array
	 * 
	 * @param values
	 * @return
	 */
	private double maxValue(double[][] values) {

		double max = values[0][0];

		for (int x = 0; x < values.length; x++) {
			for (int y = 0; y < values[0].length; y++) {

				if (values[x][y] > max) {
					max = values[x][y];
				}

			}
		}

		return max;

	}

	/**
	 * returns the minimum value of this 2d array
	 * 
	 * @param values
	 * @return
	 */
	private double minValue(double[][] values) {

		double min = values[0][0];

		for (int x = 0; x < values.length; x++) {
			for (int y = 0; y < values[0].length; y++) {

				if (values[x][y] < min) {
					min = values[x][y];
				}

			}
		}

		return min;

	}
	
	/**
	 * returns the ratings board for the given move index
	 * 
	 * @param index
	 * @return
	 */
	public double[][] getBoardRating(int index) {
		
		return ratings.get(index);
		
	}

	/**
	 * returns the number of games played in total
	 * 
	 * @return
	 */
	public int getNumberOfGames() {
		
		return numberOfGames;
	}
}
