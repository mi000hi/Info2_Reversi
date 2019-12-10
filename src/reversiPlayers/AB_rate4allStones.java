package reversiPlayers;

import java.awt.Point;
import java.util.ArrayList;

import dataAccess.DataReader;
import dataAccess.DataWriter;
import reversi.Coordinates;
import reversi.GameBoard;
import reversi.OutOfBoundsException;
import reversi.ReversiPlayer;
import reversi.Utils;

public class AB_rate4allStones implements ReversiPlayer {

	int myColor; // color of this player
	int BOARDSIZE; // size (= width = length) of the game board
	int freeFields; // amount of free fields left on the board
	final int NOT_INITIALIZED = -12345; // fixed value to have uninitialized integers

	long timeLimit; // time for our player to make a move
	long bestMoveCalculationTime; // total time we used to make our move

	double timeToUse; // amount of available time, that we will use for calculation (percentage)

	boolean noTimeLeft; // is put on true if we need to cancel our calculation
	boolean playForWin = false; // true if we can already determine the end of the game

	Coordinates[] corners = { new Coordinates(1, 1), new Coordinates(1, 8), new Coordinates(8, 1),
			new Coordinates(8, 8) };

	/*
	 * 
	 * add to ratings if stone difference is bigger than 10-20 set 20 evt gewichtung
	 * freefields egal ~100 / min (random vs random)
	 * 
	 * depth 8-9 2000ms: depth 10-11 1000ms: depth 9-10 500ms: depth 9 300ms: depth
	 * 8 200ms: depth 7-8
	 * 
	 * min stones on board to rate game 40-50 set 50
	 * 
	 * rating *= mobility
	 * 
	 * 
	 * 
	 */

	GameBoard actualBoard;

	// name of the file where the ratings are stored
	private final static String FILENAME_RANDOM_VS_RANDOM = "boardRatings_RandomPlayer_vs_RandomPlayer.txt";
	private final static String FILENAME_DUTYCALLS_VS_RANDOM = "boardRatings_DutyCalls_vs_RandomPlayer.txt";
	private final static String FILENAME_RANDOM_VS_DUTYCALLS = "boardRatings_RandomPlayer_vs_DutyCalls.txt";
//	private DataWriter dataWriter = new DataWriter(null, "boardRatings/Random_vs_Random_stoneLocationRating.txt", false, 8);
	private DataReader dataReader = new DataReader(8);
	ArrayList<double[][]> stoneRatings;
	ArrayList<double[][]> mobilityRatings;
	ArrayList<double[][]> moveRatings;
	int[][] nrOfFieldColorChange;

	@Override
	public void initialize(int myColor, long timeLimit) {
		this.myColor = myColor;
		this.timeLimit = timeLimit;
		timeToUse = 0.999;
		noTimeLeft = false;
	}

	public void readDataFromFiles(String baseFilename) {
		// TODO:
	}

	@Override
	public Coordinates nextMove(GameBoard gb) {

		actualBoard = gb;

		// start timer to measure how long we needed for our move
		long startTime = System.currentTimeMillis();

		BOARDSIZE = gb.getSize(); // or hardcode this to 8

		// count empty fields
		freeFields = 0;
		for (int y = 1; y <= BOARDSIZE; y++) {
			for (int x = 1; x <= BOARDSIZE; x++) {

				try {
					if (gb.getOccupation(new Coordinates(y, x)) == GameBoard.EMPTY) {
						freeFields++;
					}
				} catch (OutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		// Check if the player has any legal moves
		if (gb.isMoveAvailable(myColor)) {

//			System.out
//					.print(String.format("DutyCalls %s is calculating a pretty good move.\n", Utils.toString(myColor)));

			// The Coordinates that our player chooses
			Coordinates bestMove = new Coordinates(-1, -1);
			bestMove = findBestMove(gb.clone(), startTime);

			bestMoveCalculationTime = System.currentTimeMillis() - startTime;

//			System.out.println(
//					String.format("DutyCalls %s needed " + bestMoveCalculationTime + " ms to calculate the move.",
//							Utils.toString(myColor)));
//			System.out
//					.println(String.format("DutyCalls %s moves: %s", Utils.toString(myColor), bestMove.toMoveString()));

			return bestMove;

		} else {
//			System.out.println(String.format("DutyCalls %s has no legal moves, passes.", Utils.toString(myColor)));
			return null;
		}

	}

	/**
	 * returns the coordinates with the best possible move
	 * 
	 * @param board
	 * @param maxDepth
	 * @param startTime
	 * @return
	 */
	private Coordinates findBestMove(GameBoard board, long startTime) {

		double currentDepthBestRating = NOT_INITIALIZED; // best Rating in the current depth of alpha-beta-algorithm
		double bestRating = NOT_INITIALIZED; // the best rating from the last completed depth of alpha-beta-algorithm
		double currentRating; // rating for the current field of the iteration
		int depth = 1; // start depth for the alpha-beta-algorithm
		Coordinates currentDepthBestCoordinates = new Coordinates(-1, -1);
		Coordinates bestCoordinates = new Coordinates(-1, -1);
		Coordinates currentCoordinates;

		// calculate ratings for increasing depths while the depth does not reach the
		// full game board
		try { // catch Exception that is thrown, if there was not enough time left to finish
				// the calculation
			while (depth <= freeFields || bestCoordinates.getCol() == -1) {

				// System.out.println("=== now calculating with depth " + depth);

				currentDepthBestRating = NOT_INITIALIZED;
				currentDepthBestCoordinates = new Coordinates(-1, -1);

				// iterate over all board fields
				for (int y = 1; y <= board.getSize(); y++) {
					for (int x = 1; x <= board.getSize(); x++) {

						currentCoordinates = new Coordinates(y, x);
						if (board.checkMove(myColor, currentCoordinates)) {

							// find rating for this move and save move if it has the better rating than all
							// the moves tested before from this depth
							currentRating = findRating(board.clone(), currentCoordinates, depth - 1, myColor,
									currentDepthBestRating, startTime);

							// System.out.println("rating for field " + (new Coordinates(y,
							// x)).toMoveString() + " is: " + currentRating);

							// searching the maximum rating
							if (currentDepthBestRating == NOT_INITIALIZED || currentRating > currentDepthBestRating) {
								currentDepthBestRating = currentRating;
								// System.out.println("rating for the move " + (new Coordinates(y,
								// x)).toMoveString() + " is: " + currentDepthBestRating);
								currentDepthBestCoordinates = currentCoordinates;
							}

						}
					}
				}

				// use the new ratings from the higher depth
				bestRating = currentDepthBestRating;
				bestCoordinates = currentDepthBestCoordinates;

				depth++;
				// play for the win -- positive stone difference
				if (depth == freeFields) {
					playForWin = true;
				}
			}
		} catch (Exception e) {
			// do not update bestCoordinates, just return the best ones so far

//			System.out.println("got the exception!");
			if (bestCoordinates.getCol() == -1) {
				if (currentDepthBestCoordinates.getCol() == -1) {

//					System.out.println("need to find possible move, take first one, should not be here");
					for (int y = 1; y <= board.getSize(); y++) {
						for (int x = 1; x <= board.getSize(); x++) {
							if (board.checkMove(myColor, new Coordinates(y, x))) {
								return new Coordinates(y, x);
							}
						}
					}
				} else {
					bestCoordinates = currentDepthBestCoordinates;
				}
			}
		}

//		System.out.println("maximum depth was: " + depth);
		System.out.println("rating for this game is: " + bestRating);

		return bestCoordinates;

	}

	/**
	 * finds the rating for the given board, using alpha-beta-algorithm until depth
	 * depth
	 * 
	 * @param board
	 * @param move
	 * @param depth
	 * @param player
	 * @param referenceRating
	 * @param startTime
	 * @return
	 */
	private double findRating(GameBoard board, Coordinates move, int depth, int player, double referenceRating,
			long startTime) throws Exception {

		double lastBestRating = NOT_INITIALIZED; // best rating we got so far
		double currentRating; // rating for the move from the current iteration

		// make the move, save old board
		GameBoard oldBoard = board.clone();
		board.makeMove(player, move);

		// recursion termination clause
		if (depth <= 0 || board.isFull()) {
			return rating(board, oldBoard, player);
		}

		// iterate over each field
		for (int y = 1; y <= BOARDSIZE; y++) {
			for (int x = 1; x <= BOARDSIZE; x++) {

				// if move is possible, calculate rating
				if (board.checkMove(Utils.other(player), new Coordinates(y, x))) {

					if (System.currentTimeMillis() - startTime < timeToUse * timeLimit) {
						currentRating = findRating(board.clone(), new Coordinates(y, x), depth - 1, Utils.other(player),
								lastBestRating, startTime);
					} else {
						throw new Exception("no time for calculation left!");
					}

					// if was our turn, take min, else max
					if (player == myColor) {
						if (lastBestRating == NOT_INITIALIZED) {
							lastBestRating = currentRating;
						} else {
							lastBestRating = Math.min(currentRating, lastBestRating);

							// if lastBestRating is already smaller than our referenceRating, cancel this
							// recursion
							if (referenceRating != NOT_INITIALIZED && lastBestRating <= referenceRating) {
								return lastBestRating;
							}
						}

					} else {
						if (lastBestRating == NOT_INITIALIZED) {
							lastBestRating = currentRating;
						} else {
							lastBestRating = Math.max(currentRating, lastBestRating);

							// if lastBestRating is already greater than our referenceRating, cancel this
							// recursion
							if (referenceRating != NOT_INITIALIZED && lastBestRating >= referenceRating) {
								return lastBestRating;
							}
						}

					}

				}
			}
		}

		if (lastBestRating == NOT_INITIALIZED) {
			// dont cancel recursion if we need to pass!

			if (System.currentTimeMillis() - startTime < timeToUse * timeLimit) {
				lastBestRating = findRating(board.clone(), null, depth - 1, Utils.other(player), lastBestRating,
						startTime);
			} else {
				throw new Exception("no time left!");
			}

		}

		return lastBestRating;

	}

	private double rating(GameBoard currentBoard, GameBoard previousBoard, int whoDidLastMove) {

		int currentOccupation;
		double rating = 0;
		int moveNumber = currentBoard.countStones(1) + currentBoard.countStones(2) - 4;
		double[][] currentStoneRating = stoneRatings.get(moveNumber);
		double[][] currentMobilityRating = mobilityRatings.get(moveNumber);

		try {
			for (int x = 0; x < BOARDSIZE; x++) {
				for (int y = 0; y < BOARDSIZE; y++) {

					currentOccupation = currentBoard.getOccupation(new Coordinates(y + 1, x + 1));
				
					if (currentOccupation == myColor) {
						rating += currentStoneRating[x][y] / currentBoard.countStones(myColor) / 5000 / Math.max(1, (60 - moveNumber));
					} else if(currentOccupation == -myColor + 3) {
						rating -= currentStoneRating[x][y] / currentBoard.countStones(-myColor + 3) / 5000 / Math.max(1, (60 - moveNumber));
					}
					
					// TODO: do something if no player has this field?

//					if(currentBoard.checkMove(-whoDidLastMove+3, new Coordinates(y+1, x+1))) {
//						if(whoDidLastMove == myColor) {
//							rating += 90 / moveNumber * currentMobilityRating[x][y]; // TODO: which player, which move?
//						} else {
//							rating -= 90 / moveNumber * currentMobilityRating[x][y];
//						}
//						
//						
//					}
					
				}
			}
		} catch (OutOfBoundsException e) {
			e.printStackTrace();
		}

		if(myColor != whoDidLastMove) {
			rating *= currentBoard.mobility(whoDidLastMove) * currentBoard.mobility(whoDidLastMove); // TODO
		} else {
			rating /= currentBoard.mobility(whoDidLastMove) * currentBoard.mobility(whoDidLastMove); // TODO
		}
		
		return rating;

	}
	
	
	/**
	 * normalizes the ratings
	 * 
	 */
	/*
	private ArrayList<double[][]> normalize (ArrayList<double[][]> ratingboard) {
		ArrayList<double[][]> normboard = ratingboard;
		for (int i = 0; i < 60; ++i) {
		double max = Double.MIN_VALUE;
		for (int x = 0; x < 8; ++x) {
			for (int y = 0; y < 8; ++y) {
				max = Math.max(max, ratingboard.get[x][y]);
			}
		}
		return normboard;
	}
	*/
	
	/**
	 * sets the ratings
	 * 
	 * @param boardRatings
	 */
	public void setRatings(ArrayList<double[][]> stoneRatings, ArrayList<double[][]> moveRatings, ArrayList<double[][]> mobilityRatings, int[][] nrOfFieldColorChange) {//, ArrayList<double[][]> mobilityRatings) {

		this.stoneRatings = stoneRatings; // TODO: maybe unnecessary because pointer
		this.moveRatings = moveRatings;
		this.nrOfFieldColorChange = nrOfFieldColorChange;
		this.mobilityRatings = mobilityRatings;

	}

}
