package reversiPlayers;

import java.util.ArrayList;

import reversi.*;

public class AlphaBetaPlayer implements ReversiPlayer {
	private int myColor;
	private int otherColor;

	private int depth = 0; // current depth
	private int move = 0; // current move (0,59)

	// name of the file where the ratings are stored
	private final static String FILENAME_RANDOM_VS_RANDOM = "boardRatings_randomPlayer_vs_randomPlayer.txt";
	private DataWriter dataWriter = new DataWriter(null, FILENAME_RANDOM_VS_RANDOM, false, 8);
	ArrayList<double[][]> ratings = dataWriter.readRatingsFromFile();

	private long timeLimit;

	class Timeout extends Throwable {
		private static final long serialVersionUID = 1L;
	}

	public void initialize(int myColor, long timeLimit) {
		this.myColor = myColor;
		this.otherColor = Utils.other(myColor);
		this.timeLimit = timeLimit;
	}

	public Coordinates nextMove(GameBoard gb) {
		long timeout = System.currentTimeMillis() + timeLimit - 10;
		move = gb.countStones(myColor) + gb.countStones(otherColor) - 4;
		BestMove bestMove = null;
		try {
			bestMove = max(1, timeout, gb, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
		} catch (Timeout e) {
			throw new AssertionError("Hmm, not enough time for recursion depth 1");
		}

		try {
			for (int i = 2;; i++) {
				depth = i;
				bestMove = max(i, timeout, gb, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
			}
		} catch (Timeout e) {
		}
		return bestMove.coord;
	}

	/**
	 * Result of the alpha-beta analysis
	 */
	class BestMove {
		/**
		 * The coordinates of the proposed next move
		 */
		public Coordinates coord;

		/**
		 * The value of the callers game board according to the alpha-beta analysis
		 */
		public double value;

		public BestMove(double value, Coordinates coord) {
			this.value = value;
			this.coord = coord;
		}
	}

	/**
	 * Performs an alpha-beta analysis
	 * 
	 * @param maxDepth the maximum recursion depth
	 * @param timeout  a hard timeout for the analysis
	 * @param gb       the game situation which is analysed
	 * @param depth    the current recursion depth, starting at 0
	 * @param alpha    the alpha bound
	 * @param alpha    the alpha bound
	 * @param beta     the beta bound
	 * @return the best move according to the alpha-beta analysis.
	 * @throws Timeout if system time passes the given timeout.
	 */
	private BestMove max(int maxDepth, long timeout, GameBoard gb, int depth, double alpha, double beta)
			throws Timeout {
		if (System.currentTimeMillis() > timeout) {
			throw new Timeout();
		}

		if (depth == maxDepth) {
			return new BestMove(eval(gb), null);
		}

		ArrayList<Coordinates> availableMoves = new ArrayList<Coordinates>(gb.getSize() * gb.getSize());

		for (int x = 1; x <= gb.getSize(); x++) {
			for (int y = 1; y <= gb.getSize(); y++) {
				Coordinates coord = new Coordinates(x, y);
				if (gb.checkMove(myColor, coord)) {
					availableMoves.add(coord);
				}
			}
		}

		if (availableMoves.isEmpty()) {
			if (gb.isMoveAvailable(otherColor)) {
				BestMove result = min(maxDepth, timeout, gb, depth + 1, alpha, beta);
				return new BestMove(result.value, null);
			} else {
				return new BestMove(finalResult(gb), null);
			}
		}

		Coordinates bestCoord = null;
		for (Coordinates coord : availableMoves) {
			GameBoard hypothetical = gb.clone();
			hypothetical.makeMove(myColor, coord);
			BestMove result = min(maxDepth, timeout, hypothetical, depth + 1, alpha, beta);

			if (result.value > alpha) {
				alpha = result.value;
				bestCoord = coord;
			}
			if (alpha >= beta) {
				return new BestMove(alpha, null);
			}
		}

		return new BestMove(alpha, bestCoord);
	}

	private BestMove min(int maxDepth, long timeout, GameBoard gb, int depth, double alpha, double beta)
			throws Timeout {
		if (System.currentTimeMillis() > timeout) {
			throw new Timeout();
		}

		if (depth == maxDepth) {
			return new BestMove(eval(gb), null);
		}

		ArrayList<Coordinates> availableMoves = new ArrayList<Coordinates>(gb.getSize() * gb.getSize());

		for (int x = 1; x <= gb.getSize(); x++) {
			for (int y = 1; y <= gb.getSize(); y++) {
				Coordinates coord = new Coordinates(x, y);
				if (gb.checkMove(otherColor, coord)) {
					availableMoves.add(coord);
				}
			}
		}

		if (availableMoves.isEmpty()) {
			if (gb.isMoveAvailable(myColor)) {
				BestMove result = max(maxDepth, timeout, gb, depth + 1, alpha, beta);
				return new BestMove(result.value, null);
			} else {
				return new BestMove(finalResult(gb), null);
			}
		}

		Coordinates bestCoord = null;
		for (Coordinates coord : availableMoves) {
			GameBoard hypothetical = gb.clone();
			hypothetical.makeMove(otherColor, coord);
			BestMove result = max(maxDepth, timeout, hypothetical, depth + 1, alpha, beta);

			if (result.value < beta) {
				beta = result.value;
				bestCoord = coord;
			}
			if (beta <= alpha) {
				return new BestMove(beta, null);
			}
		}

		return new BestMove(beta, bestCoord);
	}

	/**
	 * Returns the value of a finished game
	 * 
	 * @param gb the situation
	 * @return the value of the finished game from the perspective of the player.
	 */
	private double finalResult(GameBoard gb) {
		/*
		 * final int myStones = gb.countStones(myColor); final int otherStones =
		 * gb.countStones(otherColor); if (myStones > otherStones) return maxEval(gb);
		 * if (otherStones > myStones) return minEval(gb);
		 */
		return eval(gb);
	}

	/**
	 * Estimate the value of a game situation.
	 * 
	 * @param gb the situation to consider
	 * @return the value of the current game board from the perspective of the
	 *         player
	 */
	private double eval(GameBoard gb) {
		// endgame
		double rating = 0;
		if (move + depth >= 59) {
			return gb.countStones(myColor) - gb.countStones(otherColor);
		}
		else {
			for (int row = 0; row < 8; ++row) {
				for (int col = 0; row < 8; ++row) {
					Coordinates coord = new Coordinates(col, row);
					try {
						if (gb.getOccupation(coord) == myColor) {
							rating += ratings.get(depth)[col][row];
						}
					} catch (OutOfBoundsException e) {
					}
				}
			}
			return rating;
		}
	}

	/**
	 * Get the upper bound for the value of a game situation.
	 * 
	 * @param gb a game board
	 * @return the maximum value possible for any situation on the given game board
	 */
	/*
	 * private int maxEval(GameBoard gb) { return gb.getSize() * gb.getSize(); }
	 */
	/**
	 * Get the lower bound for the value of a game situation.
	 * 
	 * @param gb a game board
	 * @return the minimum value possible for any situation on the given game board
	 */
	/*
	 * private int minEval(GameBoard gb) { return -1 * maxEval(gb); }
	 */
	/**
	 * Get the value of a draw game
	 * 
	 * @param gb a game board
	 * @return the value of a draw game on the given board
	 */
	/*
	 * private int draw(GameBoard gb) { return 0; }
	 */
}
