package corona;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import dataAccess.DataReader;
import reversi.Coordinates;
import reversi.GameBoard;
import reversi.OutOfBoundsException;
import reversi.ReversiPlayer;
import reversi.Utils;

public class StayHomeStayHealthy implements ReversiPlayer {

	private int MY_COLOR, ENEMY_COLOR;
	private long TIME_LIMIT;

	private final int BOARDSIZE = 8;
	private final static String BASE_FILENAME = "12122019_1300_Random_vs_Random"; // name of the file where the ratings
																					// are stored
	private DataReader dataReader = new DataReader(8);
	ArrayList<double[][]> stoneRatings;
	ArrayList<double[][]> mobilityRatings;
	ArrayList<double[][]> moveRatings;
	double[][] nrOfFieldColorChange;

	// save the previous board to determine the enemy his move
	GameBoard previousBoard = null;

	int lastCompletedDepth;
	int numberOfCuts;
	int NUMBER_OF_FOLLOWING_MOVES_ME = 20;
	int NUMBER_OF_FOLLOWING_MOVES_ENEMY = 20;
	Move bestMove;

	@Override
	public void initialize(int myColor, long timeLimit) {

		MY_COLOR = myColor;
		ENEMY_COLOR = Utils.other(MY_COLOR);

		TIME_LIMIT = (long) (0.95 * timeLimit);

		readDataFromFiles();

		lastCompletedDepth = 0 + 2; // + 2 because there was no move before

	}

	@Override
	public Coordinates nextMove(GameBoard gb) {

		// start time-measurement
		long endTime = System.currentTimeMillis() + TIME_LIMIT;

		// save current Gameboard
		GameBoard currentGameBoard = gb.clone();

		System.out.println("we have " + countSaveStones(currentGameBoard, MY_COLOR) + " safestones");
		bestMove = null;

		int freeFields = 64 - currentGameBoard.countStones(1) - currentGameBoard.countStones(2);
		Move myMove = new Move(null); // TODO: take actually available move

		// find the best available move
		// TODO: start where we left off in the last move -- not needed because
		// alpha-beta cuts?
		try {
			for (lastCompletedDepth -= 2; System.currentTimeMillis() < endTime; lastCompletedDepth++) {

				// if theres no further depth to calculate, just play the best values
				if (lastCompletedDepth >= freeFields) {
					System.out.println("can calculate to the finish of the game");
					lastCompletedDepth = freeFields - 1;
				}

				System.out.println("starting with depth: " + Math.max(1, lastCompletedDepth));

//				numberOfCuts = 0;

				myMove = alphaBetaFindMove(endTime, Math.max(1, lastCompletedDepth), gb.clone());

				System.out.println("finished depth " + Math.max(1, lastCompletedDepth) + "; we did " + numberOfCuts
						+ " cuts so far");

				// if theres no further depth to calculate, just play the best values
				if (lastCompletedDepth >= freeFields - 1) {
					break;
				}

			}
		} catch (NoTimeLeftException e) {
			// ignore exception, it means we have no time left for calculations or reached
			// the maximum depth available on this gameboard
			// return the best move!
			// e.printStackTrace();
		} catch (FullGameBoardException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (SkipCalculationException e) {
			// TODO Auto-generated catch block
			myMove = e.move;
//				e.printStackTrace();
		}

		// save previous gameboard
		currentGameBoard.makeMove(MY_COLOR, myMove.coord);
		previousBoard = currentGameBoard;

		if (myMove.coord == null) {
			if (bestMove != null) {
				myMove = bestMove;
			} else {
				System.out.println("\nWe need to pass for rating " + myMove.rating + "\n");
			}
		} else {
			System.out.println("\nWe will move " + myMove.coord.toMoveString() + " for rating " + myMove.rating + "\n");
		}

		return myMove.coord;
	}

	private Move alphaBetaFindMove(final long endTime, int depth, GameBoard gb)
			throws NoTimeLeftException, FullGameBoardException, SkipCalculationException {

		// if theres no move available, pass
		if (!gb.isMoveAvailable(MY_COLOR)) {
			throw new SkipCalculationException(new Move(null)); // pass
			// TODO: do not return
		}

		// find available moves if we have not done so before
		ArrayList<Move> possibleMoves = new ArrayList<>();
		Coordinates coord; // iteration variable
		for (int x = 1; x <= BOARDSIZE; x++) {
			for (int y = 1; y <= BOARDSIZE; y++) {
				coord = new Coordinates(y, x);
				if (gb.checkMove(MY_COLOR, coord)) {
					possibleMoves.add(new Move(coord));
//						System.out.println("adding possible move " + move.toMoveString());
				}
			}
		}

		// skip calculation if only one more is available
		if (possibleMoves.size() == 1) {
			throw new SkipCalculationException(possibleMoves.get(0));
		}

		// sort moves
		possibleMoves = sortMovesByStones(possibleMoves, gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4,
				MY_COLOR, gb);

//		System.out.print("We have " + possibleMoves.size() + " moves to choose from: ");
//		printPossibleMoves(possibleMoves);

		// initialize bestmove for a new depth search
		bestMove = possibleMoves.get(0);

		// reset alpha and beta values
		double maximalAlpha = -Double.MAX_VALUE;
		double minimalBeta = Double.MAX_VALUE; // TODO: would not be needed

		// test the moves
		for (int i = 0; i < possibleMoves.size(); i++) {

			// make the move
			GameBoard afterMove = gb.clone();
			afterMove.makeMove(MY_COLOR, possibleMoves.get(i).coord);

			// calculate the alpha-beta rating for this move
			double moveRating = alphaBeta(endTime, depth - 1, afterMove, possibleMoves.get(i), ENEMY_COLOR,
					maximalAlpha, minimalBeta);

			// if move rating is higher than alpha-rating, use this move!
			if (moveRating > maximalAlpha) {
				bestMove = possibleMoves.get(i);
				maximalAlpha = moveRating;
				bestMove.rating = moveRating;
			}
		}

//		System.out.println("prediction for this game is " + bestMove);

		return bestMove;
	}

	private double alphaBeta(final long endTime, int depth, GameBoard gb, Move previousMove, int activePlayer,
			double maxAlpha, double minBeta) throws NoTimeLeftException, FullGameBoardException {

		// return ratings value if depth is reached
		if (depth == 0) {
			return rating(gb, Utils.other(activePlayer), true);
		}

		// if theres no time left, throw exception
		if (System.currentTimeMillis() > endTime) {
			throw new NoTimeLeftException();
		}

		// if theres no move left, throw exception
		if (gb.isFull()) {
			throw new FullGameBoardException();
		}

		ArrayList<Move> possibleMoves = new ArrayList<>();

		// if we need to pass:
		if (gb.mobility(activePlayer) == 0) {

			// see if we do not have stones left, return rating
			if (gb.countStones(activePlayer) == 0) {
				if (activePlayer == MY_COLOR) {
					return -Double.MAX_VALUE;
				} else {
					return Double.MAX_VALUE;
				}
			}

			possibleMoves.add(new Move(null));
		}

		// find the possible moves
		Coordinates coord; // iteration variable
		for (int x = 1; x <= BOARDSIZE; x++) {
			for (int y = 1; y <= BOARDSIZE; y++) {
				coord = new Coordinates(y, x);
				if (gb.checkMove(activePlayer, coord)) {
					possibleMoves.add(new Move(coord));
				}
			}
		}

		// sort moves
		possibleMoves = sortMovesByStones(possibleMoves, gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4,
				activePlayer, gb);

		// find the best move
		for (int i = 0; i < possibleMoves.size(); i++) {

			// make the move
			GameBoard afterMove = gb.clone();
			afterMove.makeMove(activePlayer, possibleMoves.get(i).coord);

			// go one move further
			if (activePlayer == MY_COLOR) {
				double moveRating = alphaBeta(endTime, depth - 1, afterMove, possibleMoves.get(i), ENEMY_COLOR,
						maxAlpha, minBeta);

				// if move rating is higher than alpha-rating, use this move!
				if (moveRating > maxAlpha) {
					maxAlpha = moveRating;
				}

				// if alpha is bigger than beta, cut
				if (maxAlpha >= minBeta) {
					numberOfCuts++;
					break;
				}
			} else {
				double moveRating = alphaBeta(endTime, depth - 1, afterMove, possibleMoves.get(i), MY_COLOR, maxAlpha,
						minBeta);

				// if move rating is less than beta-rating, use this move!
				if (moveRating < minBeta) {
					minBeta = moveRating;
				}

				// if beta is smaller than alpha, cut
				if (minBeta <= maxAlpha) {
					numberOfCuts++;
					break;
				}
			}
		}

		// return rating value
		if (activePlayer == MY_COLOR) {
			return maxAlpha;
		} else {
			return minBeta;
		}
	}

	private double rating(GameBoard gb, int lastMoveBy, boolean longRating) {

		int moveNumber = Math.min(59, gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4);
		double[][] ratingsFieldOccupation = stoneRatings.get(Math.max(0, moveNumber - 1)); // we need to look at the
																							// past field
		double[][] ratingsFieldMobility = mobilityRatings.get(moveNumber); // we need to look at the upcoming field
		double occupationRating = 0;
		double mobilityRating = 0;
		double savestoneRating = 0;

		Coordinates field;

		for (int x = 0; x < BOARDSIZE; x++) {
			for (int y = 0; y < BOARDSIZE; y++) {

				field = new Coordinates(y + 1, x + 1);

				try {

					/** RATING NO MATTER WHOSE MOVE IT WAS **/

					// rating from stonelocations
					if (gb.getOccupation(field) == MY_COLOR) {

//						occupationRating += ratingsFieldOccupation[x][y];

					} else if (gb.getOccupation(field) == ENEMY_COLOR) {

//						occupationRating -= ratingsFieldOccupation[x][y];
					}

					/** RATING THAT DEPENDS ON ACTIVE PLAYER **/

					if (lastMoveBy != MY_COLOR) {

						// rating from mobility
						if (gb.checkMove(MY_COLOR, field)) {
							// or +10 +20
							mobilityRating += 5 * ratingsFieldMobility[x][y];
						}
					} else {

						// rating from mobility
						if (gb.checkMove(ENEMY_COLOR, field)) {
							mobilityRating -= 10 * ratingsFieldMobility[x][y];
						}
					}
				} catch (OutOfBoundsException e) {
					e.printStackTrace();
				}
			}
		}

		// rating from save stones that cant change its color anymore
		if (longRating) {
			savestoneRating = countSaveStones(gb, MY_COLOR) - 20 * countSaveStones(gb, ENEMY_COLOR);
		}

		// print ratings for comparison
//		System.out.println("Occupation: " + occupationRating);
//		System.out.println("Mobility: " + mobilityRating);
		// ==> occupationRating

		return occupationRating + mobilityRating + savestoneRating;

	}

	private double countSaveStones(GameBoard gb, int playerColor) {

		Coordinates field;
		int result = 0;

		try {
			for (int x = 0; x < BOARDSIZE; x++) {
				for (int y = 0; y < BOARDSIZE; y++) {

					field = new Coordinates(y + 1, x + 1);

					if (gb.getOccupation(field) == playerColor) {
						if (isSaveStone(gb, field)) {
							result++;
						}
					}
				}
			}
		} catch (OutOfBoundsException e) {
		}

		return result;
	}

	private boolean isSaveStone(GameBoard gb, Coordinates field) throws OutOfBoundsException {

		int x = field.getCol();
		int y = field.getRow();

		int rightColorChange = 0, leftColorChange = 0;
		int upColorChange = 0, downColorChange = 0;
		int upRightColorChange = 0, downLeftColorChange = 0;
		int upLeftColorChange = 0, downRightColorChange = 0;

		boolean rightOccupied = true, leftOccupied = true;
		boolean upOccupied = true, downOccupied = true;
		boolean upRightOccupied = true, downLeftOccupied = true;
		boolean upLeftOccupied = true, downRightOccupied = true;

		boolean horizSafe, vertSafe, diag01Safe, diag02Safe;

		int occupation = gb.getOccupation(field);
		int lastOccupationRight = occupation;
		int lastOccupationLeft = occupation;
		int lastOccupationUp = occupation;
		int lastOccupationDown = occupation;
		int lastOccupationUpRight = occupation;
		int lastOccupationDownLeft = occupation;
		int lastOccupationUpLeft = occupation;
		int lastOccupationDownRight = occupation;

		for (int distance = 1; distance < BOARDSIZE; distance++) {

			// right direction
			if (x + distance <= BOARDSIZE && rightOccupied) {
				occupation = gb.getOccupation(new Coordinates(y, x + distance));
				rightOccupied = occupation != 0;
				if (lastOccupationRight != occupation) {
					lastOccupationRight = occupation;
					rightColorChange++;
				}
			}

			// left direction
			if (x - distance >= 1 && leftOccupied) {
				occupation = gb.getOccupation(new Coordinates(y, x - distance));
				leftOccupied = occupation != 0;
				if (lastOccupationLeft != occupation) {
					lastOccupationLeft = occupation;
					leftColorChange++;
				}
			}

			// up direction
			if (y - distance >= 1 && upOccupied) {
				occupation = gb.getOccupation(new Coordinates(y - distance, x));
				upOccupied = occupation != 0;
				if (lastOccupationUp != occupation) {
					lastOccupationUp = occupation;
					upColorChange++;
				}
			}

			// down direction
			if (y + distance <= BOARDSIZE && downOccupied) {
				occupation = gb.getOccupation(new Coordinates(y + distance, x));
				downOccupied = occupation != 0;
				if (lastOccupationDown != occupation) {
					lastOccupationDown = occupation;
					downColorChange++;
				}
			}

			// up right direction
			if (y - distance >= 1 && x + distance <= BOARDSIZE && upRightOccupied) {
				occupation = gb.getOccupation(new Coordinates(y - distance, x + distance));
				upRightOccupied = occupation != 0;
				if (lastOccupationUpRight != occupation) {
					lastOccupationUpRight = occupation;
					upRightColorChange++;
				}
			}

			// down left direction
			if (y + distance <= BOARDSIZE && x - distance >= 1 && downLeftOccupied) {
				occupation = gb.getOccupation(new Coordinates(y + distance, x - distance));
				downLeftOccupied = occupation != 0;
				if (lastOccupationDownLeft != occupation) {
					lastOccupationDownLeft = occupation;
					downLeftColorChange++;
				}
			}

			// up left direction
			if (y - distance >= 1 && x - distance >= 1 && upLeftOccupied) {
				occupation = gb.getOccupation(new Coordinates(y - distance, x - distance));
				upLeftOccupied = occupation != 0;
				if (lastOccupationUpLeft != occupation) {
					lastOccupationUpLeft = occupation;
					upLeftColorChange++;
				}
			}

			// down right direction
			if (y + distance <= BOARDSIZE && x + distance <= BOARDSIZE && downRightOccupied) {
				occupation = gb.getOccupation(new Coordinates(y + distance, x + distance));
				downRightOccupied = occupation != 0;
				if (lastOccupationDownRight != occupation) {
					lastOccupationDownRight = occupation;
					downRightColorChange++;
				}
			}
		}

		// horizontally safe?
		horizSafe = (rightColorChange % 2 == 0 && rightOccupied) || (leftColorChange % 2 == 0 && leftOccupied)
				|| (rightOccupied && leftOccupied);

		// vertically safe?
		vertSafe = (upColorChange % 2 == 0 && upOccupied) || (downColorChange % 2 == 0 && downOccupied)
				|| (upOccupied && downOccupied);

		// diag01, (right up) safe?
		diag01Safe = (upRightColorChange % 2 == 0 && upRightOccupied)
				|| (downLeftColorChange % 2 == 0 && downLeftOccupied) || (upRightOccupied && downLeftOccupied);

		// diag02, (right down) safe?
		diag02Safe = (upLeftColorChange % 2 == 0 && upLeftOccupied)
				|| (downRightColorChange % 2 == 0 && downRightOccupied) || (upLeftOccupied && downRightOccupied);

		return horizSafe && vertSafe && diag01Safe && diag02Safe;
	}

	/**
	 * 
	 * @param moves
	 * @param moveNumber
	 * @param activePlayer the player that can choose one of the given moves
	 * @return
	 */
	private ArrayList<Move> sortMovesByStones(ArrayList<Move> moves, int moveNumber, int activePlayer, GameBoard gb) {

		double[][] ratingsField = moveRatings.get(moveNumber);
		PriorityQueue<Move> sortingMoves = new PriorityQueue<>();
		ArrayList<Move> sortedMoves = new ArrayList<>();
		int maxNumberOfMoves;

		if (activePlayer == MY_COLOR) {
			maxNumberOfMoves = NUMBER_OF_FOLLOWING_MOVES_ME;
		} else {
			maxNumberOfMoves = NUMBER_OF_FOLLOWING_MOVES_ENEMY;
		}

		// do not sort if size == 1
		if (moves.size() == 1) {
			return moves;
		}

		Coordinates move;
		for (int i = 0; i < moves.size(); i++) {
			move = moves.get(i).coord;

			// split per player because field ratings state a negative value for good enemy
			// moves ==> move needs to be highly considered
			if (activePlayer == MY_COLOR) {
				sortingMoves.add(new Move(move,
						ratingsField[move.getCol() - 1][move.getRow() - 1] * rating(gb, Utils.other(activePlayer), false)));
			} else {
				// TODO: minussign?
				sortingMoves.add(new Move(move,
						-ratingsField[move.getCol() - 1][move.getRow() - 1] * rating(gb, Utils.other(activePlayer), false)));
			}
		}

		for (int i = 0; i < moves.size() && i < maxNumberOfMoves; i++) {
			sortedMoves.add(sortingMoves.poll());
		}

		return sortedMoves;
	}

	private void readDataFromFiles() {

		// initialize rating boards
		// myColor begins
		if (MY_COLOR == GameBoard.RED) {
			stoneRatings = normalize(
					dataReader.readRatingsFromFile(this.getClass(), BASE_FILENAME + "_stoneRatings_red_wins.txt"));
			mobilityRatings = normalize(
					dataReader.readRatingsFromFile(this.getClass(), BASE_FILENAME + "_mobilityRatings_red_wins.txt"));
			moveRatings = normalize(
					dataReader.readRatingsFromFile(this.getClass(), BASE_FILENAME + "_moveRatings_red_wins.txt"));
		}
		// myColor is second player
		else {
			stoneRatings = normalize(
					dataReader.readRatingsFromFile(this.getClass(), BASE_FILENAME + "_stoneRatings_green_wins.txt"));
			mobilityRatings = normalize(
					dataReader.readRatingsFromFile(this.getClass(), BASE_FILENAME + "_mobilityRatings_green_wins.txt"));
			moveRatings = normalize(
					dataReader.readRatingsFromFile(this.getClass(), BASE_FILENAME + "_moveRatings_green_wins.txt"));
		}

		nrOfFieldColorChange = normalize(
				dataReader.readRatingFromFile(this.getClass(), BASE_FILENAME + "_colorChange.txt"));

		// print ratings to see what they look like:
//		dataReader.printRatingsBoard(moveRatings, 0);
//		dataReader.printRatingsBoard(moveRatings, 1);
//		dataReader.printRatingsBoard(moveRatings, 2);

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
			maximumValue = Math.abs(currentRatings[0][0]);
			for (int x = 0; x < currentRatings.length; x++) {
				for (int y = 0; y < currentRatings.length; y++) {

					if (Math.abs(currentRatings[x][y]) > maximumValue) {
						maximumValue = Math.abs(currentRatings[x][y]);
					}
				}
			}
			// divide all values by maximum value
			currentNormalizedRatings = new double[currentRatings.length][currentRatings.length];
			for (int x = 0; x < currentRatings.length; x++) {
				for (int y = 0; y < currentRatings.length; y++) {

					currentNormalizedRatings[x][y] = currentRatings[x][y] / Math.max(1, maximumValue);

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

		// find maximum value
		maximumValue = Math.abs(ratings[0][0]);
		for (int x = 0; x < ratings.length; x++) {
			for (int y = 0; y < ratings.length; y++) {

				if (Math.abs(ratings[x][y]) > maximumValue) {
					maximumValue = Math.abs(ratings[x][y]);
				}
			}
		}
		// divide all values by maximum value
		for (int x = 0; x < ratings.length; x++) {
			for (int y = 0; y < ratings.length; y++) {

				normalized[x][y] = ratings[x][y] / Math.max(1, maximumValue);

			}
		}
		return normalized;
	}

}
