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

	// Arraylist to save all calculated moves
	ArrayList<MoveList> allMoves;

	// ArrayList to save moves with ratings from one previous move
	ArrayList<Move> calculatedMoves;

	// save the previous board to determine the enemy his move
	GameBoard previousBoard = null;

	boolean moveDoneBefore;

	int lastCompletedDepth;
	int numberOfCuts;
	int NUMBER_OF_FOLLOWING_MOVES_STONERATING, NUMBER_OF_FOLLOWING_MOVES_PREVRATING;

	MoveList bestMove;

	@Override
	public void initialize(int myColor, long timeLimit) {

		MY_COLOR = myColor;
		ENEMY_COLOR = Utils.other(MY_COLOR);

		TIME_LIMIT = (long) (0.99 * timeLimit);

		NUMBER_OF_FOLLOWING_MOVES_STONERATING = 10;
		NUMBER_OF_FOLLOWING_MOVES_PREVRATING = 5;

		readDataFromFiles();

		moveDoneBefore = false;
		lastCompletedDepth = 0 + 2; // + 2 because there was no move before
		allMoves = new ArrayList<>();

	}

	@Override
	public Coordinates nextMove(GameBoard gb) {

		// start time-measurement

		long endTime = System.currentTimeMillis() + TIME_LIMIT;

		// save current Gameboard
		GameBoard currentGameBoard = gb.clone();

		int freeFields = 64 - currentGameBoard.countStones(1) - currentGameBoard.countStones(2);

		MoveList myMove = null; // the move we think is the best one available

		/** THE VERY FIRST MOVE **/
		if (moveDoneBefore) {

			// adjust allMoves arraylist according to the move done by enemy
			Coordinates enemyMove = findMoveDone(currentGameBoard, previousBoard);
			if (enemyMove != null) {
				System.out.println("Enemy player moved " + enemyMove.toMoveString());
			}
			boolean allMovesAdjusted = false; // needed because we can look only at the top moves
			for (int i = 0; i < allMoves.size(); i++) {
				if (allMoves.get(i).move.coord.toMoveString().equals(enemyMove.toMoveString())) {
					allMoves = allMoves.get(i).followingMoves;

					allMovesAdjusted = true;
					break;
				}
			}
			if (!allMovesAdjusted) {
				allMoves = new ArrayList<>();
			}

			// print saved moves
//			printMoves(allMoves, 0);

		} else {
			moveDoneBefore = true;
		}

		// find the best available move
		// TODO: start where we left off in the last move -- not needed because
		// alpha-beta cuts?
		try {
			for (lastCompletedDepth -= 2; System.currentTimeMillis() < endTime; lastCompletedDepth++) {

				Runtime runtime = Runtime.getRuntime();

				NumberFormat format = NumberFormat.getInstance();

				StringBuilder sb = new StringBuilder();
				long maxMemory = runtime.maxMemory();
				long allocatedMemory = runtime.totalMemory();
				long freeMemory = runtime.freeMemory();

				sb.append("free memory: " + format.format(freeMemory / 1024) + "<br/>");
				sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "<br/>");
				sb.append("max memory: " + format.format(maxMemory / 1024) + "<br/>");
				
				System.out.println("\n" + sb + "\n");
			
				// if theres no further depth to calculate, just play the best values
				if (lastCompletedDepth >= freeFields - 1) {
					System.out.println("already calculated to the finish");
					myMove = getBestMove(allMoves);
					break;
				}

				System.out.println("starting with depth: " + (lastCompletedDepth + 1));

				numberOfCuts = 0;

				myMove = alphaBetaFindMove(endTime, lastCompletedDepth + 1, gb.clone());

//				System.out.println("#################");
//				printMoves(allMoves, 0);
//				System.out.println("#################");

				System.out.println("finished depth " + (lastCompletedDepth + 1) + "; we did " + numberOfCuts + " cuts");

				// TODO: remove parameter depth?
			}
		} catch (NoTimeLeftException | FullGameBoardException e) {
			// ignore exception, it means we have no time left for calculations or reached
			// the maximum depth available on this gameboard
			// return the best move!
//			e.printStackTrace();
		}

		// print saved moves
//		printMoves(allMoves, 0);

		// save previous gameboard
		if (myMove == null) {
			System.err.println("myMove == null, should not be\n");
			myMove = bestMove;
			System.err.println("myMove is now " + myMove.move + "\n");
		}
		if (!currentGameBoard.checkMove(MY_COLOR, myMove.move.coord)) {
			System.err.println("move " + myMove.move + " is illegal, should not me\n");
			if(allMoves.size() >= 1) {
				myMove = allMoves.get((int) Math.min(Math.floor(Math.random() * allMoves.size()), allMoves.size() - 1));
				System.err.println("randomly chose move " + myMove.move + "\n");
			} else {
				myMove = new MoveList(new Move(null));
				System.err.println("new move is a pass\n");
			}
		}
		currentGameBoard.makeMove(MY_COLOR, myMove.move.coord);
		previousBoard = currentGameBoard;

		// adjust allMoves arraylist according to our move
		allMoves = myMove.followingMoves;

		if (myMove.move.coord == null) {
			System.out.println("\nWe need to pass for rating " + myMove.move.rating + "\n");
		} else {
			System.out.println(
					"\nWe will move " + myMove.move.coord.toMoveString() + " for rating " + myMove.move.rating + "\n");
		}

		return myMove.move.coord;
	}

	private MoveList alphaBetaFindMove(final long endTime, int depth, GameBoard gb)
			throws NoTimeLeftException, FullGameBoardException {

		// if theres no move available, pass
		if (!gb.isMoveAvailable(MY_COLOR)) {
			return new MoveList(new Move(null)); // pass
			// TODO: do not return
		}

		// find available moves if we have not done so before
		if (allMoves.size() != 0) {

			// sort moves
			allMoves = sortMovesByPrevRating(allMoves, gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4,
					MY_COLOR);

		} else {
			// TODO: we should never need to find a pass here
//			// if we need to pass:
//			if (gb.mobility(MY_COLOR) == 0) {
//				System.out.println("mobility of player " + MY_COLOR + " is " + gb.mobility(MY_COLOR));
//				allMoves.followingMoves.add(new MoveList(new Move(null)));
//
//				// see if we do not have stones left, set rating
//				if (gb.countStones(MY_COLOR) == 0) {
//					System.out.println("we would loose here");
//					bestMove_inProgress.move.rating = -Double.MAX_VALUE;
//
//					// return the rating
//					return bestMove_inProgress;
//				}
//				// else find available moves
//			} else {
			Coordinates move; // iteration variable
			for (int x = 1; x <= BOARDSIZE; x++) {
				for (int y = 1; y <= BOARDSIZE; y++) {
					move = new Coordinates(y, x);
					if (gb.checkMove(MY_COLOR, move)) {
//						possibleMoves.add(new MoveList(new Move(move)));
// goes with A
						allMoves.add(new MoveList(new Move(move)));
//						System.out.println("adding possible move " + move.toMoveString());
					}
				}
			}
//			}

			// sort moves
			allMoves = sortMovesByStones(allMoves, gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4,
					MY_COLOR);
		}

		System.out.print("We have " + allMoves.size() + " moves to choose from: ");
		printPossibleMoves(allMoves);

		// initialize bestmove for a new depth search
		bestMove = allMoves.get(0);

		// reset alpha and beta values
		bestMove.alpha = -Double.MAX_VALUE;
		bestMove.beta = Double.MAX_VALUE;

		// test the moves
		for (int i = 0; i < allMoves.size(); i++) {

//			System.out.println("bestMove is " + bestMove.move);
//			System.out.println("trying move " + allMoves.get(i).move);

			// make the move
			GameBoard afterMove = gb.clone();
			afterMove.makeMove(MY_COLOR, allMoves.get(i).move.coord);

			// calculate the alpha-beta rating for this move
			alphaBeta(endTime, depth - 1, afterMove, allMoves.get(i), ENEMY_COLOR, bestMove.alpha, bestMove.beta);

			// if move rating is higher than alpha-rating, use this move!
			if (allMoves.get(i).move.rating > bestMove.alpha) {
				bestMove = allMoves.get(i);
				bestMove.alpha = allMoves.get(i).move.rating;
			}

//			System.out.println("#################");
//			printMoves(allMoves, 0);
//			System.out.println("#################");
		}

		System.out.println(
				"predicted rating is " + bestMove.move.rating + " with move " + bestMove.move.coord.toMoveString());

		return bestMove;
	}

	private MoveList alphaBeta(final long endTime, int depth, GameBoard gb, MoveList previousMove, int activePlayer,
			double maxAlpha, double minBeta) throws NoTimeLeftException, FullGameBoardException {

		// TODO: is that if right? -- should be okay
		if (depth == 0) {

			previousMove.move.rating = rating(gb, Utils.other(activePlayer));

			return previousMove; // return rating -- not necessary since pointer
		}

		// if theres no time left, throw exception
		if (System.currentTimeMillis() > endTime) {
			throw new NoTimeLeftException();
		}

		// if theres no move left, throw exception
		if (gb.isFull()) {
			throw new FullGameBoardException();
		}

//		ArrayList<MoveList> possibleMoves = previousMove.followingMoves;

		// find available moves if we have not done so before
		if (previousMove.followingMoves.size() != 0) {
			// sort moves
			previousMove.followingMoves = sortMovesByPrevRating(previousMove.followingMoves,
					gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4, activePlayer);

		} else {
			// if we need to pass:
			if (gb.mobility(activePlayer) == 0) {
//				System.out.println("mobility of player " + activePlayer + " is " + gb.mobility(activePlayer));
				previousMove.followingMoves.add(new MoveList(new Move(null)));

				// see if we do not have stones left, set rating
				if (gb.countStones(activePlayer) == 0) {
//					System.out.println("player " + activePlayer + " would loose here");
					if (activePlayer == MY_COLOR) {
						previousMove.move.rating = -Double.MAX_VALUE;
					} else {
						previousMove.move.rating = Double.MAX_VALUE;
					}

					// return the rating
					return previousMove;
				}
				// else find available moves
			} else {
				Coordinates move; // iteration variable
				for (int x = 1; x <= BOARDSIZE; x++) {
					for (int y = 1; y <= BOARDSIZE; y++) {
						move = new Coordinates(y, x);
						if (gb.checkMove(activePlayer, move)) {
							previousMove.followingMoves.add(new MoveList(new Move(move)));
						}
					}
				}
			}

			// sort moves
			previousMove.followingMoves = sortMovesByStones(previousMove.followingMoves,
					gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4, activePlayer);
		}

		// get the best move
		MoveList bestMove = previousMove.followingMoves.get(0);
		bestMove.alpha = maxAlpha;
		bestMove.beta = minBeta;
		for (int i = 0; i < previousMove.followingMoves.size(); i++) {

			// make the move
			GameBoard afterMove = gb.clone();
			afterMove.makeMove(activePlayer, previousMove.followingMoves.get(i).move.coord);

			// go one move further
			MoveList moveRatings;
			if (activePlayer == MY_COLOR) {
				alphaBeta(endTime, depth - 1, afterMove, previousMove.followingMoves.get(i), ENEMY_COLOR,
						bestMove.alpha, bestMove.beta);

//				System.out.println("#################");
//				printMoves(allMoves, 0);
//				System.out.println("#################");

				// if move rating is higher than alpha-rating, use this move!
				if (previousMove.followingMoves.get(i).move.rating > bestMove.alpha) {
					bestMove = previousMove.followingMoves.get(i);
					bestMove.alpha = previousMove.followingMoves.get(i).move.rating;
				}

				// if alpha is bigger than beta, cut
				if (bestMove.alpha >= bestMove.beta) {
//					System.out.println("*** alpha cut at " + bestMove.move.coord.toMoveString());
					numberOfCuts++;
					break;
				}
			} else {
				alphaBeta(endTime, depth - 1, afterMove, previousMove.followingMoves.get(i), MY_COLOR, bestMove.alpha,
						bestMove.beta);

//				System.out.println("#################");
//				printMoves(allMoves, 0);
//				System.out.println("#################");

				// if move rating is less than beta-rating, use this move!
				if (previousMove.followingMoves.get(i).move.rating < bestMove.beta) {
					bestMove = previousMove.followingMoves.get(i);
					bestMove.beta = previousMove.followingMoves.get(i).move.rating;
				}

				// if beta is smaller than alpha, cut
				if (bestMove.beta <= bestMove.alpha) {
//					System.out.println("*** beta cut at " + bestMove.move.coord.toMoveString());
					numberOfCuts++;
					break;
				}
			}
		}

		// return rating value
		previousMove.move.rating = bestMove.move.rating;
		return previousMove; // not necessary since pointer
	}

	private double rating(GameBoard gb, int lastMoveBy) {

		int moveNumber = gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4;
		double[][] ratingsFieldOccupation = stoneRatings.get(Math.max(0, moveNumber - 1)); // we need to look at the
																							// past field
		double[][] ratingsFieldMobility = mobilityRatings.get(moveNumber); // we need to look at the upcoming field
		double occupationRating = 0;
		double mobilityRating = 0;
		Coordinates field;

		for (int x = 0; x < BOARDSIZE; x++) {
			for (int y = 0; y < BOARDSIZE; y++) {

				field = new Coordinates(y + 1, x + 1);

				try {

					/** RATING NO MATTER WHOSE MOVE IT WAS **/

					// rating from stonelocations
					if (gb.getOccupation(field) == MY_COLOR) {

						occupationRating += ratingsFieldOccupation[x][y];

					} else if (gb.getOccupation(field) == ENEMY_COLOR) {

						// TODO: use the other colors ratings files
					}

					/** RATING THAT DEPENDS ON ACTIVE PLAYER **/

					if (lastMoveBy != MY_COLOR) {

						// rating from mobility
						if (gb.checkMove(MY_COLOR, field)) {

							mobilityRating += ratingsFieldMobility[x][y];
						}
					} else {

						// TODO: use the other colors ratings files
					}
				} catch (OutOfBoundsException e) {
					e.printStackTrace();
				}
			}
		}

		// print ratings for comparison
//		System.out.println("Occupation: " + occupationRating);
//		System.out.println("Mobility: " + mobilityRating);
		// ==> occupationRating

		return occupationRating + mobilityRating;

	}

	/**
	 * 
	 * @param moves
	 * @param moveNumber
	 * @param activePlayer the player that can choose one of the given moves
	 * @return
	 */
	private ArrayList<MoveList> sortMovesByStones(ArrayList<MoveList> moves, int moveNumber, int activePlayer) {

		double[][] ratingsField = moveRatings.get(moveNumber);
		PriorityQueue<Move> sortingMoves = new PriorityQueue<>();
		ArrayList<MoveList> sortedMoves = new ArrayList<>();
		int maxNumberOfMoves = NUMBER_OF_FOLLOWING_MOVES_STONERATING;

		// do not sort if size == 1
		if (moves.size() == 1) {
			return moves;
		}

		Coordinates move;
		for (int i = 0; i < moves.size(); i++) {
			move = moves.get(i).move.coord;

			// split per player because field ratings state a negative value for good enemy
			// moves ==> move needs to be highly considered
			if (activePlayer == MY_COLOR) {
				sortingMoves.add(new Move(move, ratingsField[move.getCol() - 1][move.getRow() - 1]));
			} else {
				sortingMoves.add(new Move(move, -ratingsField[move.getCol() - 1][move.getRow() - 1]));
			}
		}

//		System.out.print("Ratings: ");
		for (int i = 0; i < moves.size() && i < maxNumberOfMoves; i++) {
//			System.out.print(sortingMoves.peek().rating + ",");
			sortedMoves.add(new MoveList(sortingMoves.poll()));
		}
//		System.out.println("\n");

		return sortedMoves;

	}

	private ArrayList<MoveList> sortMovesByPrevRating(ArrayList<MoveList> moves, int moveNumber, int activePlayer) {

		PriorityQueue<MoveList> sortingMoves = new PriorityQueue<>();
		ArrayList<MoveList> sortedMoves = new ArrayList<>();
		int maxNumberOfMoves = NUMBER_OF_FOLLOWING_MOVES_PREVRATING;

		// do not sort if size == 1
		if (moves.size() == 1) {
			return moves;
		}

		MoveList move;
		for (int i = 0; i < moves.size(); i++) {
			move = moves.get(i);

			sortingMoves.add(move);
		}

//		System.out.print("Ratings: ");
		for (int i = 0; i < moves.size() && i < maxNumberOfMoves; i++) {
//			System.out.print(sortingMoves.peek().rating + ",");

			// sort increasing we are the active player, else decreasing
			if (activePlayer == MY_COLOR) {
				sortedMoves.add(sortingMoves.poll());
			} else {
				sortedMoves.add(0, sortingMoves.poll());
			}

			// TODO: this should be the wrong direction
//			if(activePlayer == MY_COLOR) {
//				sortedMoves.add(0, sortingMoves.poll());
//			} else {
//				sortedMoves.add(sortingMoves.poll());
//			}
		}

		if (maxNumberOfMoves < sortedMoves.size()) {
			sortedMoves = (ArrayList<MoveList>) sortedMoves.subList(0, maxNumberOfMoves);
			System.out.println("movelist shortened to length " + sortedMoves.size());
		}
//		System.out.println("\n");

		return sortedMoves;

	}

	private Coordinates findMoveDone(GameBoard current, GameBoard previous) {

		try {
			for (int x = 1; x <= BOARDSIZE; x++) {
				for (int y = 1; y <= BOARDSIZE; y++) {

					if (previous.getOccupation(new Coordinates(y, x)) == 0
							&& current.getOccupation(new Coordinates(y, x)) != 0) {
						return new Coordinates(y, x);
					}

				}
			}
		} catch (OutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.err.println("ENEMY MOVE NOT FOUND! Guess he passed...");
		return new Coordinates(0, 0);
	}

	private int doesListContainMove(ArrayList<MoveList> list, Coordinates coord) {

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).move.equals(coord)) {
				return i;
			}
		}
		return -1;

	}

	private int getIndexOfMove(ArrayList<MoveList> list, MoveList move) {

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).move.equals(move.move)) {
				return i;
			}
		}
		return -1;
	}

	private MoveList getBestMove(ArrayList<MoveList> moves) {

		if (moves.size() == 0) {
			return null;
		}

		double bestRating = moves.get(0).move.rating;
		MoveList bestMove = moves.get(0);
		for (int i = 0; i < moves.size(); i++) {
			if (moves.get(i).move.rating > bestRating) {
				bestRating = moves.get(i).move.rating;
				bestMove = moves.get(i);
			}
		}

		return bestMove;
	}

	private void printPossibleMoves(ArrayList<MoveList> moves) {

		for (MoveList ml : moves) {
			System.out.print(ml.move.coord.toMoveString() + " ");
		}
		System.out.println();
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

	private void printMoves(ArrayList<MoveList> moves, int depth) {

		for (MoveList ml : moves) {

			for (int i = 0; i < depth; i++) {
				System.out.print("   ");
			}
			System.out.println(ml.move.toString());
			printMoves(ml.followingMoves, depth + 1);

		}

	}

}
