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

public class AB_rate4allStones implements ReversiPlayer {

	int myColor; // color of this player
	int BOARDSIZE; // size (= width = length) of the game board
	int freeFields; // amount of free fields left on the board
	final int NOT_INITIALIZED = -12345; // fixed value to have uninitialized integers

	long timeLimit; // time for our player to make a move
	double timeToUse; // amount of available time, that we will use for calculation (percentage)

	boolean noTimeLeft; // is put on true if we need to cancel our calculation
	boolean playForWin = false; // true if we can already determine the end of the game

	Coordinates[] corners = { new Coordinates(1, 1), new Coordinates(1, 8), new Coordinates(8, 1),
			new Coordinates(8, 8) };
	GameBoard actualBoard;

	// name of the file where the ratings are stored
	private final static String BASE_FILENAME = "boardRatings/11122019_0030Random_vs_Random.txt";
	private DataReader dataReader = new DataReader(8);
	ArrayList<double[][]> stoneRatings;
	ArrayList<double[][]> mobilityRatings;
	ArrayList<double[][]> moveRatings;
	double[][] nrOfFieldColorChange;

	@Override
	public void initialize(int myColor, long timeLimit) {
		this.myColor = myColor;
		this.timeLimit = timeLimit;
		timeToUse = 0.999;
		noTimeLeft = false;

		// do not use this if the gamecoordinator plays the game. he will give the
		// ratings to the player
		// readDataFromFiles(BASE_FILENAME);

	}

	public void readDataFromFiles(String baseFilename) {
		// TODO:
	}

	@Override
	public Coordinates nextMove(GameBoard gb) {

		System.out.println("hello world!");

		actualBoard = gb;

		// start timer to measure how long we needed for our move
		long startTime = System.currentTimeMillis();

		BOARDSIZE = gb.getSize(); // or hardcode this to 8

		// count empty fields
		freeFields = BOARDSIZE * BOARDSIZE - gb.countStones(1) - gb.countStones(2);

		// Check if the player has any legal moves
		if (gb.isMoveAvailable(myColor)) {

			// The Coordinates that our player chooses
			Coordinates bestMove = new Coordinates(-1, -1);
			bestMove = findBestMove(gb.clone(), startTime);

			return bestMove;

		} else {
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

							// searching the maximum rating
							if (currentDepthBestRating == NOT_INITIALIZED || currentRating > currentDepthBestRating) {
								currentDepthBestRating = currentRating;
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

			// if no best move found, return the first possible one
			if (bestCoordinates.getCol() == -1) {
				if (currentDepthBestCoordinates.getCol() == -1) {
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

		System.out.println("maximum depth was: " + depth);
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
			double rating = rating(board, oldBoard, player);
			return rating;
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
		double[][] currentMoveRating = moveRatings.get(moveNumber);
		// TODO: correct ratings indexes?

		/*
		 * TODO: something fancy here
		 */

		// add up ratings
		double stoneRatingSum = 0;
		double mobilityRatingSum = 0;
		double moveRatingSum = 0;

		for (int x = 0; x < BOARDSIZE; ++x) {
			for (int y = 0; y < BOARDSIZE; ++y) {
				try {
					if (currentBoard.getOccupation(new Coordinates(y, x)) == myColor) {
						stoneRatingSum += currentStoneRating[x][y];
						mobilityRatingSum += currentMobilityRating[x][y];
						moveRatingSum += currentMoveRating[x][y];
					} else if (currentBoard.getOccupation(new Coordinates(y, x)) == Utils.other(myColor)) {
						stoneRatingSum -= 5 * currentStoneRating[x][y];
						mobilityRatingSum -= 5 * currentMobilityRating[x][y];
						moveRatingSum -= currentMoveRating[x][y];
					}
				} catch (OutOfBoundsException e) {
				}
			}
		}

		// put different ratings together
		rating = stoneRatingSum + 2 * moveRatingSum + mobilityRatingSum * 2;
		System.out.println("rating: " + rating);

		return rating;

	}

	/**
	 * sets the ratings
	 * 
	 * @param boardRatings
	 */
	public void setRatings(ArrayList<double[][]> stoneRatings, ArrayList<double[][]> moveRatings,
			ArrayList<double[][]> mobilityRatings, double[][] nrOfFieldColorChange) {

		this.stoneRatings = normalize(stoneRatings); // TODO: maybe unnecessary because pointer
		this.moveRatings = normalize(moveRatings);
		this.nrOfFieldColorChange = normalize(nrOfFieldColorChange);
		this.mobilityRatings = normalize(mobilityRatings);

	}

	/**
	 * normalizes the ratings to interval [-1,1]
	 * 
	 * TODO: maybe even round the ratings or make a threshold that neglects the
	 * values close to 0
	 * 
	 * (change perhaps interval, if absolute value of ratings is too high)
	 */
	private ArrayList<double[][]> normalize(ArrayList<double[][]> ratings) {

		ArrayList<double[][]> normalized = new ArrayList<>();
		double maximumValue;
		double[][] currentRatings;
		double[][] currentNormalizedRatings; // copying all values so we dont destroy them (because pointer)

		// find the maximum value per move and divide the ratings from this move by that
		// value
		for (int i = 0; i < ratings.size(); i++) {

			// find maximum value
			currentRatings = ratings.get(i);
			maximumValue = currentRatings[0][0];
			for (int x = 0; x < currentRatings.length; x++) {
				for (int y = 0; y < currentRatings.length; y++) {

					if (currentRatings[x][y] > maximumValue) {
						maximumValue = currentRatings[x][y];
					}

				}
			}

			// divide all values by maximum value
			currentNormalizedRatings = new double[currentRatings.length][currentRatings.length];
			for (int x = 0; x < currentRatings.length; x++) {
				for (int y = 0; y < currentRatings.length; y++) {

					currentNormalizedRatings[x][y] = currentRatings[x][y] / maximumValue;

				}
			}
			normalized.add(currentNormalizedRatings);

		}

		return normalized;

	}

	/**
	 * normalizes the ratings to interval [-1,1]
	 * 
	 * (change perhaps interval, if absolute value of ratings is too high)
	 */
	private double[][] normalize(double[][] ratings) {

		double[][] normalized = new double[ratings.length][ratings.length];
		double maximumValue;

		// find the maximum value per move and divide the ratings from this move by that
		// value

		// find maximum value
		maximumValue = ratings[0][0];
		for (int x = 0; x < ratings.length; x++) {
			for (int y = 0; y < ratings.length; y++) {

				if (ratings[x][y] > maximumValue) {
					maximumValue = ratings[x][y];
				}

			}
		}

		// divide all values by maximum value
		for (int x = 0; x < ratings.length; x++) {
			for (int y = 0; y < ratings.length; y++) {

				normalized[x][y] = ratings[x][y] / maximumValue;

			}
		}

		return normalized;

	}

}
