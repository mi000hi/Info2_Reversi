package dataAccess;

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

	private final ReversiPlayer[] players; // players that are playing the games
	private boolean dontLookAtOldData = false; // true if you want to write the whole file new
	private final int BOARDSIZE; // size of the gameboard
	private boolean fileExists; // false if we need to create the file

	/**
	 * CONSTRUCTOR
	 * 
	 * @param players
	 * @param filename
	 */
	public DataWriter(ReversiPlayer[] players, int boardSize) {

		// initialize variables
		this.players = players;
		BOARDSIZE = boardSize;

	}

	/**
	 * writes the given string to the requested file
	 * 
	 * @param relativeFilename
	 * @param output
	 * @throws IOException
	 */
	private void writeToFile(String filename, String output, boolean append) throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(filename, append));
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
	public void writeRatingsData(String baseFilename, ArrayList<double[][]> ratings_red,
			ArrayList<double[][]> ratings_green, int numberOfGames_red, int numberOfGames_green) throws IOException {

		/*
		 * write data for red player
		 */
		writeToFile(baseFilename + "_red_wins.txt", "\n-- numberOfGames: " + numberOfGames_red + "\n\n", true);

		for (int i = 0; i < ratings_red.size(); i++) {

			writeToFile(baseFilename + "_red_wins.txt", "MOVE " + i + "\n", true);
			writeToFile(baseFilename + "_red_wins.txt", ratingsToString(ratings_red.get(i)) + "\n\n", true);

		}

		/*
		 * write data for green player
		 */
		writeToFile(baseFilename + "_green_wins.txt", "\n-- numberOfGames: " + numberOfGames_green + "\n\n", true);

		for (int i = 0; i < ratings_green.size(); i++) {

			writeToFile(baseFilename + "_green_wins.txt", "MOVE " + i + "\n", true);
			writeToFile(baseFilename + "_green_wins.txt", ratingsToString(ratings_green.get(i)) + "\n\n", true);

		}

	}

	/**
	 * writes the board rating data to the file
	 * 
	 * @param relativeFilename
	 * @param append
	 * @throws IOException
	 */
	public void writeRatingData(String filename, double[][] ratings, int numberOfGames)
			throws IOException {

		writeToFile(filename, "\n-- numberOfGames: " + numberOfGames + "\n\n", true);

			writeToFile(filename, "FIELD \n", true);
			writeToFile(filename, ratingsToString(ratings) + "\n\n", true);

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
		if (!fileExists) {
			return ratings02;
		}

		// return null if ratings dont match their dimensions
		if (ratings01.size() != ratings02.size() || ratings01.get(0).length != ratings02.get(0).length) {
			System.out.println("RATINGS MATRICES DONT MATCH!!!");
			return null;
		}

		double[][] currentBoardRatings, currentBoardRatings02;

		for (int i = 0; i < ratings01.size(); i++) {

			currentBoardRatings = ratings01.get(i);
			currentBoardRatings02 = ratings02.get(i);

			for (int x = 0; x < currentBoardRatings.length; x++) {
				for (int y = 0; y < currentBoardRatings.length; y++) {

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
	public void writeFileHeader(String filename) throws IOException {

		// title
		writeToFile(filename, "         +==============================================+\n", false);
		writeToFile(filename, "         |   BOARD RATING DATA for our reversi player   |\n", true);
		writeToFile(filename, "         +==============================================+\n\n", true);

		// playing players
		writeToFile(filename,
				"    " + players[1].getClass().getName() + " vs " + players[2].getClass().getName() + "\n\n", true);

		writeToFile(filename, "+================================================================+\n", true);
		writeToFile(filename, "+================================================================+\n", true);

		// write the date and time to the file
		Date today = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.YYYY, HH:mm:SS");
		String date = dateFormat.format(today);
		writeToFile(filename, "last saved at: " + date + "\n", true);

		writeToFile(filename, "+================================================================+\n", true);
		writeToFile(filename, "+================================================================+\n", true);

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
	 * returns a string containing all the ratings
	 * 
	 * @param ratings
	 * @return
	 */
	private String ratingsToString(int[][] ratings) {

		String result = "";

		for (int y = 0; y < ratings.length; y++) {
			for (int x = 0; x < ratings.length; x++) {

				// add the actual doubles to the string
				result += adjustStringLength(Integer.toString(ratings[x][y]), 25) + "\t";

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

		double[][] boardToPrint = ratingBoards.get(index);// normalize(ratingBoards.get(index));

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
				values[x][y] /= (maxValue - minValue);

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

}
