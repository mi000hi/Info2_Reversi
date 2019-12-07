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
	private final long SAVE_TO_FILE_INTERVAL = 60000; // interval of saving the rating data to a file
	private long fileIntervalStartTime; // time of last data saving
	
	// name of the file where the ratings are stored
	private final static String FILENAME_RANDOM_VS_RANDOM = "boardRatings_randomPlayer_vs_randomPlayer.txt";
	private DataWriter dataWriter; // writes the board rating data to a file
	
	private Terminator terminator; // if we play until the user stops the program, the terminator will know when to terminate the program
	private Thread terminatorThread; // thread that runs the terminator runnable
	private static boolean terminateProgram = true; // the Terminator will set this to false and later to true to terminate the program

	/*
	 * the two reversi players
	 */
	ReversiPlayer[] players = new ReversiPlayer[3]; // those players will play against each other

	/*
	 * game variables
	 */
	private final int BOARD_SIZE = 8; // size of the gameboard
	private final long MOVE_TIME; // time a player has to make its move; we trust the players here :)
	private final int NUMBER_OF_GAMES = 0; // number of games to be played, set to 0 to play infinitely

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
		
		// standard values
		String filename = "boardRatings_RandomPlayer_vs_RandomPlayer.txt";
		long moveTime = 100;
		ReversiPlayer player01 = new RandomPlayer(), player02 = new RandomPlayer();

		// create new GameCoordinator
		System.out.println("creating new GameCoordinator");
		System.out.println("we have " + args.length + " arguments");
		
		// read the players from args
		// first argument is moveTime, second and third the two players
		if(args.length > 0 && args[0] != "") {
			moveTime = (long) Integer.parseInt(args[0]);
		}
		if(args.length > 1 && args[1].equals("DutyCalls")) {
			player01 = new DutyCalls();
			filename = "boardRatings_DutyCalls_vs_RandomPlayer.txt";
		}
		if(args.length > 2 && args[2].equals("DutyCalls")) {
			player02 = new DutyCalls();
			filename = "boardRatings_RandomPlayer_vs_DutyCalls.txt";
			
			if(args[1] == "DutyCalls") {
				System.out.println("do not use 2 times DutyCalls as player!");
				System.exit(0);
			}
		}
		
		GameCoordinator gameCoordinator = new GameCoordinator(player01, player02,
				filename, moveTime);

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
	public GameCoordinator(ReversiPlayer player01, ReversiPlayer player02, String boardRatingsDataFilename, long moveTime) {

		// initialize variables
		players[1] = player01;
		players[2] = player02;
		MOVE_TIME = moveTime;

		// initialize players
		players[1].initialize(GameBoard.RED, MOVE_TIME);
		players[2].initialize(GameBoard.GREEN, MOVE_TIME);

		// initialize boardRatings
		for (int i = 0; i < 60; i++) {
			boardRatings.add(new double[8][8]);
		}
		
		System.out.println("moveTime = " + MOVE_TIME);
		System.out.println("filename = " + boardRatingsDataFilename);

		// initialize data writer
		dataWriter = new DataWriter(players, boardRatingsDataFilename, false, BOARD_SIZE);
		
		// initialize the terminator
		
		if(NUMBER_OF_GAMES == 0) {
			terminator = new Terminator();
			terminatorThread = new Thread(terminator);
			terminatorThread.start();
		}

	}

	/**
	 * plays a game between the redPlayer and the greenPlayer
	 */
	private void playGames() {

		// take time to save after SAVE_TO_FILE_INTERVAL
		fileIntervalStartTime = System.currentTimeMillis();

		for (int gameIndex = 0; gameIndex < NUMBER_OF_GAMES || !terminateProgram; gameIndex++) {

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

//			System.out.println("Game " + gameIndex + " finished.");
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
			if (System.currentTimeMillis() - fileIntervalStartTime - SAVE_TO_FILE_INTERVAL > 0) {

				if(NUMBER_OF_GAMES == 0) {
					System.out.println("===== SAVING DATA: \nGamesPlayed: " + numberOfGames + " - playing to infinity");
				} else {
					System.out.println("===== SAVING DATA: \nGamesPlayed: " + numberOfGames + " out of " + NUMBER_OF_GAMES);
				}
				
				try {
					saveData(boardRatings, numberOfGames, FILENAME_RANDOM_VS_RANDOM);
					fileIntervalStartTime = System.currentTimeMillis();

					// reset variables and arrays
					numberOfGames = 0;

					boardRatings = new ArrayList<>();
					for (int i = 0; i < 60; i++) {
						boardRatings.add(new double[8][8]);
					}
					
					System.out.println("rating for all " + dataWriter.getNumberOfGames() + " games for board 59 is now: ");
					printRatingsBoard(dataWriter.getBoardRating(60 - 1));
					
					// update the terminate message
					if(terminator != null) {
						terminator.printInputRequest();
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

		System.out.println(numberOfGames + " games finished, added to ratings and saved. rating for board 59:");
		printRatingsBoard(boardRatings, 60 - 1);

		System.out.println("rating for all " + dataWriter.readDataFromFile() + " games for board 59 is now: ");
		printRatingsBoard(dataWriter.readRatingsFromFile(), 60 - 1);

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
		dataWriter.writeFileHeader(false);

		// write ratings data to file
		dataWriter.writeRatingsData(ratings, numberOfGames, true);

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
	 * prints the given GameBoard to the console like this:
	 */
	private void printRatingsBoard(double[][] ratingBoard) {

		double[][] boardToPrint = normalize(ratingBoard);

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
	 * sets the parameter terminateProgram
	 * 
	 * @param value
	 */
	public static void setTerminateProgram(boolean value) {
		terminateProgram = value;
	}

}
