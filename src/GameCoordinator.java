import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import reversi.*;
import reversiPlayers.*;

public class GameCoordinator {

	/*
	 * variables for the GameCoordinator
	 */
	private int[] scores = new int[3]; // scores of the 2 players
	private int winner; // winner of the last game
	private final long SAVE_TO_FILE_INTERVAL = 30; // interval of saving the rating data to a file
	private final int MIN_GAMES_BEFORE_SAVING = 100; // maximum games to play before we save the data to a file
	private long fileIntervalStartTime; // time of last data saving
	private final String FILENAME_RANDOM_VS_RANDOM = "boardRatings_randomPlayer_vs_randomPlayer.txt"; // name of the
																										// file where
																										// the ratings
																										// are stored

	/*
	 * the two reversi players
	 */
	ReversiPlayer[] players = new ReversiPlayer[3]; // those players will play against each other

	/*
	 * game variables
	 */
	private final int BOARD_SIZE = 8; // size of the gameboard
	private final long MOVE_TIME = 100; // time a player has to make its move; we trust the players here :)
	private final int NUMBER_OF_GAMES = 1002; // number of games to be played

	/*
	 * variables to analyze the games
	 */
	private ArrayList<GameBoard> gameBoards = new ArrayList<>();
	private ArrayList<double[][]> boardRatings = new ArrayList<>();
	private int numberOfGames = 0;

	/**
	 * creates the GameCoordinator, who can run reversi games
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// create new GameCoordinator
		System.out.println("creating new GameCoordinator");
		GameCoordinator gameCoordinator = new GameCoordinator(new RandomPlayer(), new RandomPlayer());

		// play a game
		long startTime = System.currentTimeMillis();
		gameCoordinator.playGames();
		System.out.println("playing those games took " + (System.currentTimeMillis() - startTime) + "ms");

	}

	/**
	 * CONSTRUCTOR
	 * 
	 * @param moveTime
	 * @param player01
	 * @param player02
	 */
	public GameCoordinator(ReversiPlayer player01, ReversiPlayer player02) {

		// initialize variables
		players[1] = player01;
		players[2] = player02;

		// initialize players
		players[1].initialize(GameBoard.RED, MOVE_TIME);
		players[2].initialize(GameBoard.GREEN, MOVE_TIME);

		// initialize boardRatings
		for (int i = 0; i < 60; i++) {
			boardRatings.add(new double[8][8]);
		}

	}

	/**
	 * plays a game between the redPlayer and the greenPlayer
	 */
	private void playGames() {

		// take time to save after SAVE_TO_FILE_INTERVAL
		fileIntervalStartTime = System.currentTimeMillis();

		for (int gameIndex = 0; gameIndex < NUMBER_OF_GAMES; gameIndex++) {

			// create the game board
			GameBoard board = new BitBoard();
			int currentPlayer = GameBoard.RED;
			Coordinates move;

			gameBoards = new ArrayList<>();

			// play the game
			while (board.isMoveAvailable(GameBoard.GREEN) || board.isMoveAvailable(GameBoard.RED)) {

				// player makes a move
				move = players[currentPlayer].nextMove(board.clone());
				board.makeMove(currentPlayer, move);

				// add the gameboard to the gameBoards
				if (move != null) {
					gameBoards.add(board.clone());
				}

				// switch player
				currentPlayer = -currentPlayer + 3;

			}

			System.out.println("Game " + gameIndex + " finished.");
			// printBoard(board);
			// printAllBoards(gameBoards);

			winner = getWinner(board);

			if (winner == GameBoard.EMPTY) { // if draw
//				System.out.println("===== DRAW: " + scores[GameBoard.RED] + "(r) - " + scores[GameBoard.GREEN] + "(g)");
			} else {

				// regular win
//				System.out.println("Player " + Utils.toString(winner) + " won the game: " + scores[GameBoard.RED]
//						+ "(r) - " + scores[GameBoard.GREEN] + "(g)");

			}

			// add this game to the boardRatings
			if (winner != GameBoard.EMPTY) {
				addGameToBoardRatings(gameBoards, winner);
				numberOfGames++;
				// printRatingsBoard(boardRatings, 60 - 1);
			}

			// save ratings data to file
//			System.out.println((System.currentTimeMillis() - fileIntervalStartTime - SAVE_TO_FILE_INTERVAL));
			if (MIN_GAMES_BEFORE_SAVING < numberOfGames
					&& System.currentTimeMillis() - fileIntervalStartTime - SAVE_TO_FILE_INTERVAL > 0) {

				System.out.println("===== SAVING DATA =====");

				try {
					saveData(boardRatings, numberOfGames, FILENAME_RANDOM_VS_RANDOM);
					fileIntervalStartTime = System.currentTimeMillis();

					// reset variables and arrays
					numberOfGames = 0;

					boardRatings = new ArrayList<>();
					for (int i = 0; i < 60; i++) {
						boardRatings.add(new double[8][8]);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}

		// were finished with the games, so save the data
		System.out.println("===== SAVING DATA =====");

		try {
			saveData(boardRatings, numberOfGames, FILENAME_RANDOM_VS_RANDOM);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(numberOfGames + " games finished, added to ratings and saved:");
		printRatingsBoard(boardRatings, 60 - 1);

	}

	/**
	 * writes the game rating data to a file
	 * 
	 * @param ratings
	 * @param numberOfGames
	 * @throws IOException
	 */
	private void saveData(ArrayList<double[][]> ratings, int numberOfGames, String relativeFilename)
			throws IOException {

		// writes title, date, names etc to the file, DELETES THE FILE CONTENT!
		writeFileHeader(relativeFilename, false);

		// write ratings data to file
		writeRatingsData(ratings, numberOfGames, relativeFilename, true);

	}

	/**
	 * writes the given string to the requested file
	 * 
	 * @param relativeFilename
	 * @param output
	 * @throws IOException
	 */
	private void writeToFile(String relativeFilename, String output, boolean append) throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(relativeFilename, append));
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
	private void writeRatingsData(ArrayList<double[][]> ratings, int numberOfGames, String relativeFilename,
			boolean append) throws IOException {

		// write statistic info
		writeToFile(relativeFilename, "\n-- numberOfGames: " + numberOfGames + "\n\n", true); // TODO: add old number of
																								// games

		// write board ratings
		for (int i = 0; i < ratings.size(); i++) {

			writeToFile(relativeFilename, "MOVE " + i + "\n", true);

			writeToFile(relativeFilename, ratingsToString(ratings.get(i)) + "\n\n", true);

		}

	}

	/**
	 * writes the header of the data file into the file (AND POSSIBLY OVERWRITES THE
	 * WHOLE FILE!)
	 * 
	 * @param relativeFilename
	 * @param append
	 * @throws IOException
	 */
	private void writeFileHeader(String relativeFilename, boolean append) throws IOException {

		// title
		writeToFile(relativeFilename, "         +==============================================+\n", append);
		writeToFile(relativeFilename, "         |   BOARD RATING DATA for our reversi player   |\n", true);
		writeToFile(relativeFilename, "         +==============================================+\n\n", true);

		// playing players
		writeToFile(relativeFilename,
				"    " + players[1].getClass().getName() + " vs " + players[2].getClass().getName() + "\n\n", true);

		writeToFile(relativeFilename, "+================================================================+\n", true);
		writeToFile(relativeFilename, "+================================================================+\n", true);

	}

	/**
	 * adds the game saved in boards to the boardRatings
	 * 
	 * @param boards
	 */
	private void addGameToBoardRatings(ArrayList<GameBoard> boards, int winner) {

		GameBoard currentBoard; // saves the current board for that iteration
		double[][] currentRatings; // saves the current board ratings for that move
		int occupation; // saves the occupation of one field on the gameboard
		int nrOfMovesMade = boards.size(); // the number of moves done that game
//		System.out.println("nrOfMovesMade = " + nrOfMovesMade);
		double weight = 1.0 / nrOfMovesMade; // weight that will be counted to the boardRatings
//		System.out.println("weight = " + weight);

		try {
			for (int moveIndex = 0; moveIndex < nrOfMovesMade; moveIndex++) {

				currentBoard = boards.get(moveIndex);
				currentRatings = boardRatings.get(moveIndex);

				for (int x = 0; x < BOARD_SIZE; x++) {
					for (int y = 0; y < BOARD_SIZE; y++) {

						occupation = currentBoard.getOccupation(new Coordinates(y + 1, x + 1));

						// add 1 for winner, add -1 for looser, weight it with number of moves made that
						// game
						if (occupation == winner) {
							currentRatings[x][y] += weight;
//							System.out.println(currentRatings[x][y]);
						} else if (occupation == -winner + 3) {
							currentRatings[x][y] -= weight;
//							System.out.println(currentRatings[x][y]);
						}
						// do nothing for draw games (we should not call this method for draw games)

					}
				}

				// save the new boardRating
				boardRatings.set(moveIndex, currentRatings);

			}
		} catch (OutOfBoundsException o) {
			o.printStackTrace();
		}

	}

	/**
	 * returns the winner of the given gameboard
	 * 
	 * @param board
	 * @return
	 */
	private int getWinner(GameBoard board) {

		scores[1] = board.countStones(GameBoard.RED);
		scores[2] = board.countStones(GameBoard.GREEN);

		if (scores[1] > scores[2])
			return GameBoard.RED;
		if (scores[2] > scores[1])
			return GameBoard.GREEN;
		return GameBoard.EMPTY;

	}

	/**
	 * prints the given GameBoard to the console like this:
	 */
	private void printBoard(GameBoard board) {

		char occupation;

		System.out.println("+=================+");

		try {
			for (int y = 1; y <= BOARD_SIZE; y++) {

				System.out.print("| ");

				for (int x = 1; x <= BOARD_SIZE; x++) {

					switch (board.getOccupation(new Coordinates(y, x))) {

					case 1:
						occupation = 'r';
						break;
					case 2:
						occupation = 'g';
						break;
					default:
						occupation = ' ';

					}

					System.out.print(occupation + " ");

				}

				System.out.println("|");
			}
		} catch (OutOfBoundsException o) {
		}

		System.out.println("+=================+");

	}

	/**
	 * prints all gameboards from the list to the console
	 * 
	 * @param boards
	 */
	private void printAllBoards(ArrayList<GameBoard> boards) {

		for (GameBoard b : boards) {
			printBoard(b);
		}

	}

	/**
	 * prints the given GameBoard to the console like this:
	 */
	private void printRatingsBoard(ArrayList<double[][]> ratingBoards, int index) {

		double[][] boardToPrint = normalize(ratingBoards.get(index));

		System.out.println("+=================+");

		for (int y = 0; y < BOARD_SIZE; y++) {

			System.out.print("| ");

			for (int x = 0; x < BOARD_SIZE; x++) {

				// print the numbers between 0 and 10
				System.out.print((int) Math.round(boardToPrint[x][y] * 10) + " ");

			}

			System.out.println("|");
		}

		System.out.println("+=================+");

	}

	/**
	 * returns a string containing all the ratings
	 * 
	 * @param ratings
	 * @return
	 */
	private String ratingsToString(double[][] ratings) { // TODO: add already stored ratings to these ones
		
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
	 * returns the str with |str.length - newLength| spaces before it
	 * 
	 * @param str
	 * @param newLength
	 * @return
	 */
	private String adjustStringLength(String str, int newLength) {
		
		String spaces = "";
		
		for(int length = str.length(); length <= newLength; length++) {
			spaces += " ";
		}
		
		return spaces + str;
		
	}

}
