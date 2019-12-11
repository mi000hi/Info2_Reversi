/* Dezember 2019, Moritz Huesser
 * 
 * Alpha-beta algorithm copied from solutions
 *
 */

package reversiPlayers;

import reversi.Coordinates;
import reversi.GameBoard;
import reversi.OutOfBoundsException;
import reversi.ReversiPlayer;
import reversi.Utils;
import java.util.ArrayList;
import java.util.Arrays;

public class ABHugi implements ReversiPlayer {
	private int myColor;
	private int otherColor;
	private long timeLimit;
	// Koordinaten Ecken
	private ArrayList<Coordinates> corner = new ArrayList<Coordinates>(
			Arrays.asList(new Coordinates(1, 1), new Coordinates(1, 8), new Coordinates(8, 1), new Coordinates(8, 8)));
	// Koordinaten erstes Feld diagonal zu Ecken
	private ArrayList<Coordinates> diagcorner = new ArrayList<Coordinates>(
			Arrays.asList(new Coordinates(2, 2), new Coordinates(2, 7), new Coordinates(7, 2), new Coordinates(7, 7)));
	// Koordinaten erstes Feld neben Ecken 1
	private ArrayList<Coordinates> cornbo1 = new ArrayList<Coordinates>(
			Arrays.asList(new Coordinates(1, 2), new Coordinates(1, 7), new Coordinates(7, 1), new Coordinates(7, 8)));
	// Koordinaten erstes Feld neben Ecken 2
	private ArrayList<Coordinates> cornbo2 = new ArrayList<Coordinates>(
			Arrays.asList(new Coordinates(2, 1), new Coordinates(2, 8), new Coordinates(8, 2), new Coordinates(8, 7)));
	// Gibt an, wie weit das Spiel schon ist
	private int earlygame;
	// Koordinaten Eckregionen
	private ArrayList<Coordinates> cornreg1 = new ArrayList<Coordinates>(Arrays.asList(new Coordinates(1, 1),
			new Coordinates(1, 2), new Coordinates(1, 3), new Coordinates(2, 1), new Coordinates(2, 2),
			new Coordinates(2, 3), new Coordinates(3, 1), new Coordinates(3, 2), new Coordinates(3, 3)));
	private ArrayList<Coordinates> cornreg2 = new ArrayList<Coordinates>(Arrays.asList(new Coordinates(1, 8),
			new Coordinates(2, 8), new Coordinates(3, 8), new Coordinates(1, 7), new Coordinates(2, 7),
			new Coordinates(3, 7), new Coordinates(1, 6), new Coordinates(2, 6), new Coordinates(3, 6)));
	private ArrayList<Coordinates> cornreg3 = new ArrayList<Coordinates>(Arrays.asList(new Coordinates(8, 1),
			new Coordinates(8, 2), new Coordinates(8, 3), new Coordinates(7, 1), new Coordinates(7, 2),
			new Coordinates(7, 3), new Coordinates(6, 1), new Coordinates(6, 2), new Coordinates(6, 3)));
	private ArrayList<Coordinates> cornreg4 = new ArrayList<Coordinates>(Arrays.asList(new Coordinates(8, 8),
			new Coordinates(8, 7), new Coordinates(8, 6), new Coordinates(7, 8), new Coordinates(7, 7),
			new Coordinates(7, 6), new Coordinates(6, 8), new Coordinates(6, 7), new Coordinates(6, 6)));

	class Timeout extends Throwable {
		private static final long serialVersionUID = 1L;
	}

	public void initialize(int myColor, long timeLimit) {
		this.myColor = myColor;
		this.otherColor = Utils.other(myColor);
		this.timeLimit = timeLimit;
		// this.timeLimit = 5000;
		this.earlygame = 0;
	}

	public Coordinates nextMove(GameBoard gb) {
		long timeout = System.currentTimeMillis() + timeLimit - 20;

		// Check corners
		if (earlygame < 50) {
			for (int i = 0; i < 4; ++i) {
				Coordinates c = corner.get(i);
				if (gb.checkMove(myColor, c)) {
					return c;
				}
			}
		}

		BestMove bestMove = null;
		try {
			bestMove = max(1, timeout, gb, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
		} catch (Timeout e) {
			throw new AssertionError("Hmm, not enough time for recursion depth 1");
		}
		int depth = 0;
		try {
			for (int i = 2;; i++) {
				bestMove = max(i, timeout, gb, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
				depth = i;
			}
		} catch (Timeout e) {
		}

		System.out.println(depth);
		earlygame += 2;
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
		 * The value of the callers game board according to the min-max analysis
		 */
		public int value;

		/**
		 * Flag that indicates,if the analysis was cut at the maximum recursion depth
		 */
		public boolean cut;

		public BestMove(int value, Coordinates coord, boolean cut) {
			this.value = value;
			this.coord = coord;
			this.cut = cut;
		}
	}

	/**
	 * Performs a min-max analysis.
	 * 
	 * @param maxDepth the maximum recursion depth
	 * @param timeout  a hard timeout for the analysis
	 * @param gb       the game situation which is analysed
	 * @param depth    the current recursion depth, starting at 0
	 * @return the best move for the given player according to the min-max analysis.
	 * @throws Timeout if system time passes the given timeout.
	 */
	private BestMove max(int maxDepth, long timeout, GameBoard gb, int depth, int alpha, int beta) throws Timeout {
		if (System.currentTimeMillis() > timeout) {
			throw new Timeout();
		}

		if (depth == maxDepth) {
			return new BestMove(eval(gb), null, true);
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
				return new BestMove(result.value, null, result.cut);
			} else {
				return new BestMove(finalResult(gb), null, false);
			}
		}

		boolean cut = false;
		BestMove bestMove = new BestMove(Integer.MIN_VALUE, null, false);
		for (Coordinates coord : availableMoves) {
			GameBoard hypothetical = gb.clone();
			hypothetical.makeMove(myColor, coord);
			BestMove result = min(maxDepth, timeout, hypothetical, depth + 1, alpha, beta);

			if (result.value > alpha) {
				alpha = result.value;
				bestMove.coord = coord;
				bestMove.value = result.value;
			}
			if (alpha >= beta) {
				return new BestMove(alpha, null, false);
			}
			cut = cut || result.cut;
		}

		return new BestMove(alpha, bestMove.coord, cut);
	}

	private BestMove min(int maxDepth, long timeout, GameBoard gb, int depth, int alpha, int beta) throws Timeout {
		if (System.currentTimeMillis() > timeout) {
			throw new Timeout();
		}

		if (depth == maxDepth) {
			return new BestMove(eval(gb), null, true);
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
				return new BestMove(result.value, null, result.cut);
			} else {
				return new BestMove(finalResult(gb), null, false);
			}
		}

		boolean cut = false;
		BestMove bestMove = new BestMove(Integer.MAX_VALUE, null, false);
		for (Coordinates coord : availableMoves) {
			GameBoard hypothetical = gb.clone();
			hypothetical.makeMove(otherColor, coord);
			BestMove result = max(maxDepth, timeout, hypothetical, depth + 1, alpha, beta);

			if (result.value < beta) {
				beta = result.value;
				bestMove.coord = coord;
				bestMove.value = result.value;
			}
			if (beta <= alpha) {
				return new BestMove(beta, null, false);
			}
			cut = cut || result.cut;
		}

		return bestMove;
	}

	/**
	 * Returns the value of a finished game
	 * 
	 * @param gb the situation
	 * @return the value of the finished game from the perspective of the player.
	 */
	private int finalResult(GameBoard gb) {

		final int myStones = gb.countStones(myColor);
		final int otherStones = gb.countStones(otherColor);
		if (myStones > otherStones)
			return maxEval(gb);
		if (otherStones > myStones)
			return minEval(gb);
		return draw(gb);
	}

	/**
	 * Estimate the value of a game situation.
	 * 
	 * @param gb the situation to consider
	 * @return the value of the current game board from the perspective of the
	 *         player
	 */
	private int eval(GameBoard gb) {

		if (gb.countStones(otherColor) == 0) {
			return 100000;
		}
		if (gb.countStones(myColor) == 0) {
			return -100000;
		}

		// Unterschied Anzahl Steine
		int rating;
		if (gb.countStones(myColor) + gb.countStones(otherColor) >= 64) {
			if (gb.mobility(otherColor) != 0) {
				return (gb.countStones(myColor) - gb.countStones(otherColor)) * gb.mobility(myColor)
						/ gb.mobility(otherColor);
			} else {
				return 1000;
			}
		} else if (earlygame < 40) {
			// rating = (gb.countStones(myColor) - gb.countStones(otherColor)) * 10;
			rating = (gb.countStones(myColor) - gb.countStones(otherColor)) * 5;
		} else {
			rating = (gb.countStones(myColor) - gb.countStones(otherColor)) * 40;
		}

		// Steine zentriert am Anfang --> funktioniert so nicht wirklich
		/*
		 * // Steine zentriert if (earlygame < 23) { double middle = 0; int stones = 0;
		 * for (int col = 1; col < 9; ++col) { for (int row = 1; row < 9; ++row) {
		 * Coordinates coord = new Coordinates(col, row); try { if
		 * (gb.getOccupation(coord) == myColor) { ++stones; middle +=
		 * Math.pow(Math.abs(4.5 - col), 2) + Math.pow(Math.abs(4.5 - row), 2); } }
		 * catch (OutOfBoundsException e) { } } } middle /= stones; middle = 9/(middle);
		 * rating += 7 * middle; }
		 */

		int mobilityfactor = 1;

		// Selber kein Zug mehr uebrig
		int mymobility = gb.mobility(myColor);
		if (mymobility == 0) {
			return -50000;
		}
		// Corner
		if (earlygame < 50) { // evtl. weiter runter setzen 44 oder 45
			for (int i = 0; i < 4; ++i) {
				try {
					Coordinates corn = corner.get(i);
					if (gb.getOccupation(corn) == myColor) {
						rating += 200;
						// An Ecken grenzende Raender
						if (i == 1) {
							for (int j = 2; j < 8; ++j) {
								Coordinates coord1 = new Coordinates(1, j);
								if (gb.getOccupation(coord1) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
							for (int j = 2; j < 8; ++j) {
								Coordinates coord2 = new Coordinates(j, 1);
								if (gb.getOccupation(coord2) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
						} else if (i == 2) {
							for (int j = 2; j < 8; ++j) {
								Coordinates coord1 = new Coordinates(j, 8);
								if (gb.getOccupation(coord1) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
							for (int j = 1; j < 7; ++j) {
								Coordinates coord2 = new Coordinates(1, 8 - j);
								if (gb.getOccupation(coord2) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
						} else if (i == 3) {
							for (int j = 2; j < 8; ++j) {
								Coordinates coord1 = new Coordinates(8, j);
								if (gb.getOccupation(coord1) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
							for (int j = 1; j < 7; ++j) {
								Coordinates coord2 = new Coordinates(8 - j, 1);
								if (gb.getOccupation(coord2) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
						} else {
							for (int j = 1; j < 7; ++j) {
								Coordinates coord1 = new Coordinates(8 - j, 8);
								if (gb.getOccupation(coord1) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
							for (int j = 1; j < 7; ++j) {
								Coordinates coord2 = new Coordinates(8, 8 - j);
								if (gb.getOccupation(coord2) == myColor) {
									rating += 50;
								} else {
									break;
								}
							}
						}

					} else {
						// Ecken Gegner
						if (gb.getOccupation(corn) == otherColor) {
							// rating -= 300;
							rating -= 3000;
							if (i == 1) {
								for (int j = 2; j < 8; ++j) {
									Coordinates coord1 = new Coordinates(1, j);
									if (gb.getOccupation(coord1) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
								for (int j = 2; j < 8; ++j) {
									Coordinates coord2 = new Coordinates(j, 1);
									if (gb.getOccupation(coord2) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
							} else if (i == 2) {
								for (int j = 2; j < 8; ++j) {
									Coordinates coord1 = new Coordinates(j, 8);
									if (gb.getOccupation(coord1) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
								for (int j = 1; j < 7; ++j) {
									Coordinates coord2 = new Coordinates(1, 8 - j);
									if (gb.getOccupation(coord2) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
							} else if (i == 3) {
								for (int j = 2; j < 8; ++j) {
									Coordinates coord1 = new Coordinates(8, j);
									if (gb.getOccupation(coord1) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
								for (int j = 1; j < 7; ++j) {
									Coordinates coord2 = new Coordinates(8 - j, 1);
									if (gb.getOccupation(coord2) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
							} else {
								for (int j = 1; j < 7; ++j) {
									Coordinates coord1 = new Coordinates(8 - j, 8);
									if (gb.getOccupation(coord1) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
								for (int j = 1; j < 7; ++j) {
									Coordinates coord2 = new Coordinates(8, 8 - j);
									if (gb.getOccupation(coord2) == otherColor) {
										rating -= 50;
									} else {
										break;
									}
								}
							}
						} else {
							boolean checkcorner = false;
							// X-Squares
							Coordinates diagcorn = diagcorner.get(i);
							if (gb.getOccupation(diagcorn) == myColor) {
								rating -= 150;
								checkcorner = true;
							}
							if (gb.getOccupation(diagcorn) == otherColor) {
								rating += 150;
							}
							// C-Squares
							if (gb.getOccupation(cornbo1.get(i)) == myColor) {
								rating -= 150;
								checkcorner = true;
							}
							if (gb.getOccupation(cornbo1.get(i)) == otherColor) {
								rating += 150;
								mobilityfactor += 1;
							}
							// C-Squares
							if (gb.getOccupation(cornbo2.get(i)) == myColor) {
								rating -= 150;
								checkcorner = true;
							}
							if (gb.getOccupation(cornbo2.get(i)) == otherColor) {
								rating += 150;
								mobilityfactor += 1;
							}
							if (checkcorner == true) {
								if (gb.checkMove(otherColor, corn)) {
									rating -= 2000;
								}
							}
						}
					}
				} catch (OutOfBoundsException exception) {
				}
			}
		}

		if (earlygame < 35) {
			// Einzelne stabile Steine
			for (int x = 3; x < 7; ++x) {
				for (int y = 3; y < 7; ++y) {
					int counter = 0;
					boolean opponent = false;
					Coordinates coordinate = new Coordinates(y, x);
					try {
						if (gb.getOccupation(coordinate) == myColor) {
							if (gb.getOccupation(new Coordinates(y - 1, x - 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y, x - 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y + 1, x - 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y - 1, x)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y + 1, x)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y - 1, x + 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y, x + 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y + 1, x + 1)) == otherColor)
								++counter;
						} else if (gb.getOccupation(coordinate) == otherColor) {
							if (gb.getOccupation(new Coordinates(y - 1, x - 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y, x - 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y + 1, x - 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y - 1, x)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y + 1, x)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y - 1, x + 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y, x + 1)) == otherColor)
								++counter;
							if (gb.getOccupation(new Coordinates(y + 1, x + 1)) == otherColor)
								++counter;
							opponent = true;
						}
						if (counter == 8) {
							rating += 500;
						} else if (counter == 8 && opponent == true)
							rating -= 300;
					} catch (OutOfBoundsException exception) {
					}
				}
			}
		}

		// Paritaet um Ecken ueberpruefen (nur gegen Schluss des Spiels)
		// Gerade Anzahl leere Felder uebrig ist gut (Nachteil, in eine Region mit
		// gerader Anzahl Felder setzen zu muessen)
		int counter = 0;
		for (int i = 0; i < 9; ++i) {
			try {
				if (gb.getOccupation(cornreg1.get(i)) == GameBoard.EMPTY) {
					++counter;
				}
			} catch (OutOfBoundsException e) {
			}
		}
		if (counter % 2 == 0) {
			rating += 100;
		}
		counter = 0;
		for (int i = 0; i < 9; ++i) {
			try {
				if (gb.getOccupation(cornreg2.get(i)) == GameBoard.EMPTY) {
					++counter;
				}
			} catch (OutOfBoundsException e) {
			}
		}
		if (counter % 2 == 0) {
			rating += 100;
		}
		counter = 0;
		for (int i = 0; i < 9; ++i) {
			try {
				if (gb.getOccupation(cornreg3.get(i)) == GameBoard.EMPTY) {
					++counter;
				}
			} catch (OutOfBoundsException e) {
			}
		}
		if (counter % 2 == 0) {
			rating += 100;
		}
		counter = 0;
		for (int i = 0; i < 9; ++i) {
			try {
				if (gb.getOccupation(cornreg4.get(i)) == GameBoard.EMPTY) {
					++counter;
				}
			} catch (OutOfBoundsException e) {
			}
		}
		if (counter % 2 == 0) {
			rating += 100;
		}

		// Mobilitaet
		int othermobility = gb.mobility(otherColor);

		// Spielanfang
		if (earlygame < 30) {
			if (othermobility == 0) {
				rating += 100;
			}
			// double mobility = (double) othermobility / (double) mymobility;
			// if ((int) mobility != 0) {
			// rating *= (int) mobility;
			// }
			// if (othermobility != 0) {
			// rating /= (int) othermobility;
			// }
			rating -= othermobility * 4;
			rating += mymobility * mobilityfactor;
		} else { // endgame
			if (othermobility == 0) {
				rating += 6000;
			} else {
				double mobility = (double) mymobility / (double) othermobility;
				if ((int) mobility != 0) {
					// rating *= 6 * mobilityfactor * (int) mobility;
					rating *= 10 * (int) mobility;
				}
			}
		}

		// System.out.println(rating); // print ratings to console

		return rating;
	}

	/**
	 * Get the upper bound for the value of a game situation.
	 * 
	 * @param gb a game board
	 * @return the maximum value possible for any situation on the given game board
	 */

	private int maxEval(GameBoard gb) {
		return gb.getSize() * gb.getSize();
	}

	/**
	 * Get the lower bound for the value of a game situation.
	 * 
	 * @param gb a game board
	 * @return the maximum value possible for any situation on the given game board
	 */

	private int minEval(GameBoard gb) {
		return -1 * maxEval(gb);
	}

	/**
	 * Get the value of a draw game
	 * 
	 * @param gb a game board
	 * @return the value of a draw game on the given board
	 */

	private int draw(GameBoard gb) {
		return 0;
	}

}