import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import reversi.*;
import reversiPlayers.*;
import dataAccess.DataReader;
import dataAccess.DataWriter;

public class GameCoordinator {

	/*
	 * variables for the GameCoordinator
	 */
	private int[] scores = new int[3]; // scores of the 2 players
	private int winner; // winner of the last game
	private long fileIntervalStartTime; // time of last data saving
	private final long SAVE_TO_FILE_INTERVAL = 60000; // interval of saving the rating data to a file
	private final int MAX_NUMBER_OF_GAMES = 0; // number of games to be played, set to 0 to play infinitely
	private final static String DIRECTORYPATH = "boardRatings/"; // path to the ratings files
	private DataWriter dataWriter; // writes the board rating data to a file for red
	private DataReader dataReader; // reads data from files

	/*
	 * variables to terminate the infinite running program
	 */
	private Terminator terminator; // if we play until the user stops the program, the terminator will know when to
									// terminate the program
	private Thread terminatorThread; // thread that runs the terminator runnable
	private static boolean terminateProgram = true; // the Terminator will set this to false and later to true to
													// terminate the program

	/*
	 * game variables
	 */
	private ReversiPlayer[] players = new ReversiPlayer[3]; // those players will play against each other
	private final int BOARD_SIZE = 8; // size of the gameboard
	private final long MOVE_TIME; // time a player has to make its move; we trust the players here :)
	private final int MIN_STONES_ON_BOARD = 58; // min stones on board to add the game to the ratings
	private final int MIN_STONE_DIFFERENCE = 56; // min stone difference to add the game to the ratings

	/*
	 * variables to analyze the games
	 */
	private ArrayList<GameBoard> gameBoards = new ArrayList<>(); // saves the gameboards
	private ArrayList<Coordinates> moves = new ArrayList<>(); // saves the move locations
	private ArrayList<Integer> moveWasMadeBy = new ArrayList<>(); // saves the player who did the move
	private ArrayList<double[][]> stoneRatings_red = new ArrayList<>(); // ratings for red wins
	private ArrayList<double[][]> stoneRatings_green = new ArrayList<>(); // ratings for green wins
	private ArrayList<double[][]> moveRatings_red = new ArrayList<>(); // ratings for one single field as a move
	private ArrayList<double[][]> moveRatings_green = new ArrayList<>(); // ratings for one single field as a move
	private ArrayList<double[][]> mobilityRatings_red = new ArrayList<>();
	private ArrayList<double[][]> mobilityRatings_green = new ArrayList<>();
	private int numberOfGames_red = 0; // number of games that counted to the ratings for red
	private int numberOfGames_green = 0; // number of games that counted to the ratings for green
	private double[][] nrOfFieldColorChange; // number of times a stone gets flipped during the game

	/**
	 * creates the GameCoordinator, who can run reversi games
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		HashMap<String, ReversiPlayer> players = new HashMap<>();
		players.put("Random01", new RandomPlayer());
		players.put("Random02", new RandomPlayer());
		players.put("AB_rate4allStones01", new AB_rate4allStones());
		players.put("AB_rate4allStones02", new AB_rate4allStones());

		// dont let program run without commandline arguments
		if (args.length < 5) {
			System.out.println(
					"PLEASE USE COMMANDLINE ARGUMENTS: {filename} {trashOldFile} {player01} {player02} {moveTime}");
			System.exit(0);
		}
		System.out.println("we have " + args.length + "/5 arguments, looking good so far");

		String filename = DIRECTORYPATH + args[0] + args[2] + "_vs_" + args[3]; // no file extension so far!
		boolean trashOldFile = Boolean.parseBoolean(args[1]);
		long timeToMove = (long) Integer.parseInt(args[4]);
		ReversiPlayer player01 = players.get(args[2] + "01");
		ReversiPlayer player02 = players.get(args[3] + "02");

		// create new GameCoordinator
		System.out.println("creating new GameCoordinator");

		GameCoordinator gameCoordinator = new GameCoordinator(player01, player02, filename, timeToMove, trashOldFile);

		// play a game
		long startTime = System.currentTimeMillis();

		gameCoordinator.playGames(filename);

		System.out.println("playing those games took " + (System.currentTimeMillis() - startTime) + "ms");

	}

	/**
	 * CONSTRUCTOR
	 * 
	 * @param moveTime
	 * @param player01
	 * @param player02
	 */
	public GameCoordinator(ReversiPlayer player01, ReversiPlayer player02, String baseFilename, long timeToMove,
			boolean trashOldFiles) {

		// initialize variables
		players[1] = player01;
		players[2] = player02;
		MOVE_TIME = timeToMove;

		if (players[1] instanceof AB_rate4allStones) {
			((AB_rate4allStones) players[1]).readDataFromFiles(baseFilename);
		}
		if (players[2] instanceof AB_rate4allStones) {
			((AB_rate4allStones) players[2]).readDataFromFiles(baseFilename);
		}

		// initialize players
		players[1].initialize(GameBoard.RED, MOVE_TIME);
		players[2].initialize(GameBoard.GREEN, MOVE_TIME);

		// initialize data writer
		dataWriter = new DataWriter(players, BOARD_SIZE);
		dataReader = new DataReader(BOARD_SIZE);

		// read data from files
		if (trashOldFiles) {
			for (int i = 0; i < 60; i++) {
				stoneRatings_red.add(new double[BOARD_SIZE][BOARD_SIZE]);
				stoneRatings_green.add(new double[BOARD_SIZE][BOARD_SIZE]);
				moveRatings_red.add(new double[BOARD_SIZE][BOARD_SIZE]);
				moveRatings_green.add(new double[BOARD_SIZE][BOARD_SIZE]);
				numberOfGames_red = 0;
				numberOfGames_green = 0;
				nrOfFieldColorChange = new double[BOARD_SIZE][BOARD_SIZE];
				mobilityRatings_red.add(new double[BOARD_SIZE][BOARD_SIZE]);
				mobilityRatings_green.add(new double[BOARD_SIZE][BOARD_SIZE]);
			}
		} else {
			stoneRatings_red = dataReader.readRatingsFromFile(baseFilename + "_stoneRatings_red_wins.txt");
			stoneRatings_green = dataReader.readRatingsFromFile(baseFilename + "_stoneRatings_green_wins.txt");
			moveRatings_red = dataReader.readRatingsFromFile(baseFilename + "_moveRatings_red_wins.txt");
			moveRatings_green = dataReader.readRatingsFromFile(baseFilename + "_moveRatings_green_wins.txt");
			numberOfGames_red = dataReader.readNumberOfGamesFromFile(baseFilename + "_stoneRatings_red_wins.txt");
			numberOfGames_green = dataReader.readNumberOfGamesFromFile(baseFilename + "_stoneRatings_green_wins.txt");
			nrOfFieldColorChange = dataReader.readRatingFromFile(baseFilename + "_colorChange.txt");
			mobilityRatings_red = dataReader.readRatingsFromFile(baseFilename + "_mobilityRatings_red_wins.txt");
			mobilityRatings_green = dataReader.readRatingsFromFile(baseFilename + "_mobilityRatings_green_wins.txt");
		}

		// initialize the terminator
		if (MAX_NUMBER_OF_GAMES == 0) {
			terminator = new Terminator();
			terminatorThread = new Thread(terminator);
			terminatorThread.start();
		}

	}

	/**
	 * plays a game between the redPlayer and the greenPlayer
	 */
	private void playGames(String baseFilename) {

		// take time to save after SAVE_TO_FILE_INTERVAL
		fileIntervalStartTime = System.currentTimeMillis();

		for (int gameIndex = 0; gameIndex < MAX_NUMBER_OF_GAMES || !terminateProgram; gameIndex++) {

			// give ratings data to players
			if (players[1] instanceof AB_rate4allStones) {
				((AB_rate4allStones) players[1]).setRatings(stoneRatings_red, moveRatings_red, mobilityRatings_red,
						nrOfFieldColorChange);
			}
			if (players[2] instanceof AB_rate4allStones) {
				((AB_rate4allStones) players[2]).setRatings(stoneRatings_green, moveRatings_green,
						mobilityRatings_green, nrOfFieldColorChange);
			}

			// create the game board
			GameBoard board = new BitBoard();
			int currentPlayer = GameBoard.RED;
			Coordinates currentMove;

			// reset game state variables
			gameBoards = new ArrayList<>();
			moveWasMadeBy = new ArrayList<>();
			moves = new ArrayList<>();

			// play the game
			while (board.isMoveAvailable(GameBoard.GREEN) || board.isMoveAvailable(GameBoard.RED)) {

				// player makes a move
//				do {
				currentMove = players[currentPlayer].nextMove(board.clone());
//				} while(!board.checkMove(currentPlayer, currentMove));
				board.makeMove(currentPlayer, currentMove);

				// add the gameboard to the gameBoards
				if (currentMove != null) {// && currentMove.getRow() != -1) {
					gameBoards.add(board.clone());
					moveWasMadeBy.add(currentPlayer);
					moves.add(currentMove);
//					System.out.println("add move " + moves.size() + ": " + currentMove.getCol() + "/" + currentMove.getRow());
				}

				// switch player
				currentPlayer = -currentPlayer + 3;

			}

//			System.out.println("Game " + gameIndex + " finished.");
			// printBoard(board);
			// printAllBoards(gameBoards);

			winner = getWinner(board);

//			if (winner == GameBoard.EMPTY) { // if draw
//				System.out.println("===== DRAW: " + scores[GameBoard.RED] + "(r) - " + scores[GameBoard.GREEN] + "(g)");
//			} else {
//
//				// regular win
//				System.out.println("Player " + Utils.toString(winner) + " won the game: " + scores[GameBoard.RED]
//						+ "(r) - " + scores[GameBoard.GREEN] + "(g)");
//			}

			// add this game to the ratings

			GameBoard lastBoard = gameBoards.get(gameBoards.size() - 1);
			int stoneDifference = Math.abs(lastBoard.countStones(1) - lastBoard.countStones(2));
			int stonesOnBoard = lastBoard.countStones(1) + lastBoard.countStones(2);

			if (stoneDifference >= MIN_STONE_DIFFERENCE && stonesOnBoard >= MIN_STONES_ON_BOARD) {
				addGameToRatings(gameBoards, moves, moveWasMadeBy, winner);
				if (winner == 1) {
					numberOfGames_red++;
				} else {
					numberOfGames_green++;
				}
			}

			// printRatingsBoard(boardRatings, 60 - 1);

			// save ratings data to file
//			System.out.println((System.currentTimeMillis() - fileIntervalStartTime - SAVE_TO_FILE_INTERVAL));
			if (System.currentTimeMillis() - fileIntervalStartTime - SAVE_TO_FILE_INTERVAL > 0) {

				if (MAX_NUMBER_OF_GAMES == 0) {
					System.out.println("===== SAVING DATA: \nGamesPlayed: " + (numberOfGames_red + numberOfGames_green)
							+ " - playing to infinity");
				} else {
					System.out.println("===== SAVING DATA: \nGamesPlayed: " + (numberOfGames_red + numberOfGames_green)
							+ " out of " + MAX_NUMBER_OF_GAMES);
				}

				try {
					saveData(baseFilename, stoneRatings_red, stoneRatings_green, numberOfGames_red, numberOfGames_green,
							moveRatings_red, moveRatings_green, mobilityRatings_red, mobilityRatings_green, nrOfFieldColorChange);


					// update the terminate message
					if (terminator != null) {
						terminator.printInputRequest();
					}

					// save current time and continue playing
					fileIntervalStartTime = System.currentTimeMillis();

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}

		// were finished with the games, so save the data
		System.out.println("===== SAVING DATA =====");

		try {
			saveData(baseFilename, stoneRatings_red, stoneRatings_green, numberOfGames_red, numberOfGames_green,
					moveRatings_red, moveRatings_green, mobilityRatings_red, mobilityRatings_green, nrOfFieldColorChange);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println((numberOfGames_red + numberOfGames_green) + " games finished, added to ratings and saved.");
		System.out.println("stoneRating for board 59 is: (red wins)");
		printRatingsBoard(stoneRatings_red, 60 - 1);
		System.out.println("moveRating for board 59 is: (red wins)");
		printRatingsBoard(moveRatings_red, 60 - 1);
		System.out.println("mobilityRating for board 58 is: (red wins)");
		printRatingsBoard(mobilityRatings_red, 60 - 2);
		System.out.println("nrOfFieldColorChange for the gameboard is: ");
		printRatingsBoard(nrOfFieldColorChange);
		
	}

	/**
	 * writes the game rating data to a file
	 * 
	 * @param ratings
	 * @param numberOfGames
	 * @throws IOException
	 */
	private void saveData(String baseFilename, ArrayList<double[][]> stoneRatings_red,
			ArrayList<double[][]> stoneRatings_green, int numberOfGames_red, int numberOfGames_green,
			ArrayList<double[][]> moveRatings_red, ArrayList<double[][]> moveRatings_green,
			ArrayList<double[][]> mobilityRatings_red, ArrayList<double[][]> mobilityRatings_green,
			double[][] nrOfFieldColorChange) throws IOException {

		// writes title, date, names etc to the file, DELETES THE FILE CONTENT!
		dataWriter.writeFileHeader(baseFilename + "_stoneRatings_red_wins.txt");
		dataWriter.writeFileHeader(baseFilename + "_stoneRatings_green_wins.txt");
		dataWriter.writeFileHeader(baseFilename + "_moveRatings_red_wins.txt");
		dataWriter.writeFileHeader(baseFilename + "_moveRatings_green_wins.txt");
		dataWriter.writeFileHeader(baseFilename + "_colorChange.txt");
		dataWriter.writeFileHeader(baseFilename + "_mobilityRatings_red_wins.txt");
		dataWriter.writeFileHeader(baseFilename + "_mobilityRatings_green_wins.txt");

		// write ratings data to file
		dataWriter.writeRatingsData(baseFilename + "_stoneRatings", stoneRatings_red, stoneRatings_green,
				numberOfGames_red, numberOfGames_green);
		dataWriter.writeRatingsData(baseFilename + "_moveRatings", moveRatings_red, moveRatings_green,
				numberOfGames_red, numberOfGames_green);
		dataWriter.writeRatingsData(baseFilename + "_mobilityRatings", mobilityRatings_red, mobilityRatings_green,
				numberOfGames_red, numberOfGames_green);
		dataWriter.writeRatingData(baseFilename + "_colorChange.txt", nrOfFieldColorChange,
				numberOfGames_red + numberOfGames_green);

	}

//	private ArrayList<double[][]> mergeRatings(ArrayList<double[][]> ratings01, ArrayList<double[][]> ratings02) {
//
//		ArrayList<double[][]> result = new ArrayList<>();
//		double[][] currentRatings01, currentRatings02;
//
//		for (int i = 0; i < ratings01.size(); i++) {
//
//			double[][] merged = new double[8][8];
//
//			currentRatings01 = ratings01.get(i);
//			currentRatings02 = ratings02.get(i);
//
//			for (int x = 0; x < 8; x++) {
//				for (int y = 0; y < 8; y++) {
//
//					merged[x][y] = currentRatings01[x][y] + currentRatings02[x][y];
//
//				}
//			}
//
//			result.add(merged);
//		}
//
//		return result;
//
//	}

	/**
	 * adds the game saved in boards to the boardRatings
	 * 
	 * @param boards
	 */
	private void addGameToRatings(ArrayList<GameBoard> boards, ArrayList<Coordinates> moves,
			ArrayList<Integer> moveWasMadeBy, int winner) {

//		System.out.println("adding game to ratings");
		
		GameBoard currentBoard; // saves the current board for that iteration
		int lastPlayer; // saves the player who did the move resulting with this gameboard
		double[][] currentStoneRatings; // saves the current board ratings for that move
		int nrOfMovesMade = boards.size(); // the number of moves done that game
//		System.out.println("nrOfMovesMade = " + nrOfMovesMade);
		double weight = (boards.get(boards.size() - 1).countStones(winner)
				- boards.get(boards.size() - 1).countStones(-winner + 3)); // weight that will be counted to the
		// boardRatings
//		System.out.println("weight = " + weight);

		try {

			/*
			 * for each field, do ... for each board
			 */
			int lastOccupation = 0;
			for (int x = 0; x < 8; x++) {
				for (int y = 0; y < 8; y++) {
					lastOccupation = 0;
					for (int i = 0; i < boards.size(); i++) {
						if (boards.get(i).getOccupation(new Coordinates(y + 1, x + 1)) != lastOccupation) {
							if(lastOccupation != 0) {
								nrOfFieldColorChange[x][y] += 1.0 / 10000;
							}
							lastOccupation = boards.get(i).getOccupation(new Coordinates(y + 1, x + 1));
						}
					}
				}
			}

			/*
			 * for each board, do ... for each field
			 */
			double[][] currentMoveRatings, currentMobilityRatings;
			Coordinates currentMove, currentCoordinates;
			GameBoard previousBoard = new BitBoard();
			for (int moveIndex = 0; moveIndex < nrOfMovesMade; moveIndex++) {

				currentBoard = boards.get(moveIndex);
				lastPlayer = moveWasMadeBy.get(moveIndex);
				if (winner == 1) {
					currentStoneRatings = stoneRatings_red.get(moveIndex);
					currentMoveRatings = moveRatings_red.get(moveIndex);
					currentMobilityRatings = mobilityRatings_red.get(moveIndex);
				} else {
					currentStoneRatings = stoneRatings_green.get(moveIndex);
					currentMoveRatings = moveRatings_green.get(moveIndex);
					currentMobilityRatings = mobilityRatings_green.get(moveIndex);
				}
				currentMove = moves.get(moveIndex);
//				System.out.println("currentMove: " + currentMove.getCol() + "/" + currentMove.getRow());

				for (int x = 0; x < BOARD_SIZE; x++) {
					for (int y = 0; y < BOARD_SIZE; y++) {

						currentCoordinates = new Coordinates(y + 1, x + 1);
						
						currentStoneRatings[x][y] += weight
								* stoneRating(currentBoard, moveIndex, lastPlayer, winner, currentCoordinates) / 10000;

						currentMobilityRatings[x][y] += weight
								* mobilityRating(previousBoard, moveIndex, lastPlayer, winner, currentCoordinates) / 10000;

					}
				}

				currentMoveRatings[currentMove.getCol() - 1][currentMove.getRow() - 1] += moveRating(lastPlayer,
						winner); // TODO

				// save the new boardRating
				if (winner == 1) {
					stoneRatings_red.set(moveIndex, currentStoneRatings);
					moveRatings_red.set(moveIndex, currentMoveRatings);
					mobilityRatings_red.set(moveIndex, currentMobilityRatings);
				} else {
					stoneRatings_green.set(moveIndex, currentStoneRatings);
					moveRatings_green.set(moveIndex, currentMoveRatings);
					mobilityRatings_green.set(moveIndex, currentMobilityRatings);
				}
				
//				System.out.println("previous Board was:");
//				printBoard(previousBoard);
				previousBoard = currentBoard;

			}
		} catch (OutOfBoundsException o) {
			o.printStackTrace();
		}

	}

	/**
	 * returns 1, -1 or 0 according to the move the player made
	 * 
	 * @param lastPlayer
	 * @return
	 */
	private int moveRating(int lastPlayer, int winner) {

		if (lastPlayer == winner) {
			return 1;
		} else if (lastPlayer == -winner + 3) {
			return -1;
		}
		return 0;

	}

	/**
	 * returns 1, 0 or -1 for a given coordinate on the gameboard
	 * 
	 * @return
	 * @throws OutOfBoundsException
	 */
	private double stoneRating(GameBoard board, int boardIndex, int player, int winner, Coordinates field)
			throws OutOfBoundsException {

		int occupation = board.getOccupation(field); // saves the occupation of one field on the gameboard
		player = -player + 3; // switch the player because now the other player can make a move

		// rate the field according to occupation: 1 for winners field, -1 for loosers
		// field, 0 for unoccupied
		if (occupation == winner) {
			return 1.0; // board.countStones(winner); // winners field
		} else if (occupation == -winner + 3) {
			return -1.0; // board.countStones(-winner + 3); // loosers field
			// TODO: can divisor be 0?
			// TODO: * freeFields is unnecessary becuase its just a constant
		}
		return 0; // field unoccupied

	}

	/**
	 * returns 1, 0 or -1 for a given coordinate on the gameboard
	 * 
	 * @param player the player who can now make a move on the field board
	 * @return
	 * @throws OutOfBoundsException
	 */
	private double mobilityRating(GameBoard board, int boardIndex, int player, int winner, Coordinates field)
			throws OutOfBoundsException {
		
		// rate the field according to possible moves: 1 for winners possible move
		if (board.checkMove(player, field)) {
			if (player == winner) { // winner could pick this move
				return 1.0;// / board.mobility(player);
			} else {
				return 0;// -1.0;// / board.mobility(player); // looser could pick this move
			} // TODO: is this the right divisor?, can it be 0?
		}
		return 0; // no player could pick this move

		/*
		 * TODO: is this thinking correct for the possible moves? becuase if it was the
		 * looser his turn and he could move there, wouldnt that make it a good field?
		 * maybe the first if looks out for this case? (line 337 - 11 = 326)
		 */

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

	/**
	 * sets the parameter terminateProgram
	 * 
	 * @param value
	 */
	public static void setTerminateProgram(boolean value) {
		terminateProgram = value;
	}

}
