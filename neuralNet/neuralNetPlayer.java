package neuralNet;

import java.util.ArrayList;

import NeuronalNetwork.Net;
import Utils.DataReader;
import corona.FullGameBoardException;
import corona.Move;
import corona.NoTimeLeftException;
import corona.SkipCalculationException;
import reversi.Coordinates;
import reversi.GameBoard;
import reversi.OutOfBoundsException;
import reversi.ReversiPlayer;
import reversi.Utils;

public class neuralNetPlayer implements ReversiPlayer {

	private int myColor;
	private long timeLimit;
	private Net net;

	private final int BOARDSIZE = 8;
	int lastCompletedDepth;
	int numberOfCuts;
	int NUMBER_OF_FOLLOWING_MOVES_ME = 20;
	int NUMBER_OF_FOLLOWING_MOVES_ENEMY = 20;
	Move bestMove;

	@Override
	public void initialize(int myColor, long timeLimit) {
		this.myColor = myColor;
		this.timeLimit = timeLimit;

		// load the neural net from file
		String filename = "neuralNet_reversi_0_to_10_ff_1000reps_24hidden.txt";
		net = DataReader.readNetFromFile(filename);
//		net = new Net(65, (int) Math.round(2.0 / 3 * 65 + 1), 1, 0.1);
	}

	@Override
	public Coordinates nextMove(GameBoard gb) {

		// start time-measurement
		long endTime = System.currentTimeMillis() + timeLimit;

		// save current Gameboard
		GameBoard currentGameBoard = gb.clone();

		System.out.println("we have " + countSaveStones(currentGameBoard, myColor) + " safestones");
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

//						numberOfCuts = 0;

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
//						e.printStackTrace();
		}

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
		if (!gb.isMoveAvailable(myColor)) {
			throw new SkipCalculationException(new Move(null)); // pass
			// TODO: do not return
		}

		// find available moves if we have not done so before
		ArrayList<Move> possibleMoves = new ArrayList<>();
		Coordinates coord; // iteration variable
		for (int x = 1; x <= BOARDSIZE; x++) {
			for (int y = 1; y <= BOARDSIZE; y++) {
				coord = new Coordinates(y, x);
				if (gb.checkMove(myColor, coord)) {
					possibleMoves.add(new Move(coord));
//						System.out.println("adding possible move " + move.toMoveString());
				}
			}
		}

		// skip calculation if only one more is available
		if (possibleMoves.size() == 1) {
			throw new SkipCalculationException(possibleMoves.get(0));
		}

		// TODO: sort moves
		// sort moves
//		possibleMoves = sortMovesByStones(possibleMoves, gb.countStones(myColor) + gb.countStones(Utils.other(myColor)) - 4,
//				myColor, gb);

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
			afterMove.makeMove(myColor, possibleMoves.get(i).coord);

			// calculate the alpha-beta rating for this move
			double moveRating = alphaBeta(endTime, depth - 1, afterMove, possibleMoves.get(i), Utils.other(myColor),
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
				if (activePlayer == myColor) {
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

		// TODO: sort moves
		// sort moves
//		possibleMoves = sortMovesByStones(possibleMoves, gb.countStones(MY_COLOR) + gb.countStones(ENEMY_COLOR) - 4,
//				activePlayer, gb);

		// find the best move
		for (int i = 0; i < possibleMoves.size(); i++) {

			// make the move
			GameBoard afterMove = gb.clone();
			afterMove.makeMove(activePlayer, possibleMoves.get(i).coord);

			// go one move further
			if (activePlayer == myColor) {
				double moveRating = alphaBeta(endTime, depth - 1, afterMove, possibleMoves.get(i), Utils.other(myColor),
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
				double moveRating = alphaBeta(endTime, depth - 1, afterMove, possibleMoves.get(i), myColor, maxAlpha,
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
		if (activePlayer == myColor) {
			return maxAlpha;
		} else {
			return minBeta;
		}
	}

	private double rating(GameBoard gb, int lastMoveBy, boolean longRating) {

		// create arrays to feed to the neural network
		double[] input = new double[65];
		
		input[0] = Utils.other(lastMoveBy);
		for (int y = 1; y <= 8; y++) {
			for (int x = 1; x <= 8; x++) {
				try {
					input[(y - 1) * 8 + x] = gb.getOccupation(new Coordinates(y, x));
				} catch (OutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// TODO: ratinc cannot be correct. it is always around 3.7679570574766785E-10
		double prediction = net.compute(input)[0];
		System.out.println("prediction: " + (1-prediction));

		// 1 - prediction because the network predicts the winrate for the player who can move now
		return 1 - prediction;

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

}
