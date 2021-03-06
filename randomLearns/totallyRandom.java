package randomLearns;

import java.awt.Point;
import java.util.ArrayList;

import dataAccess.DataReader;
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

public class totallyRandom implements ReversiPlayer {

	int myColor; // color of this player
	int BOARDSIZE; // size (= width = length) of the game board
	int freeFields; // amount of free fields left on the board
	final int NOT_INITIALIZED = -12345; // fixed value to have uninitialized integers
	int calculationDepth; // depth that the alpha-beta-algorithm will consider

	long timeLimit; // time for our player to make a move
	double timeToUse; // amount of available time, that we will use for calculation (percentage)

	boolean noTimeLeft; // is put on true if we need to cancel our calculation
	boolean playForWin = false; // true if we can already determine the end of the game

	Coordinates[] corners = { new Coordinates(1, 1), new Coordinates(1, 8), new Coordinates(8, 1),
			new Coordinates(8, 8) };
	GameBoard actualBoard;

	// name of the file where the ratings are stored
	private final static String BASE_FILENAME = "12122019_1300_Random_vs_Random";
	private DataReader dataReader = new DataReader(8);
	ArrayList<double[][]> stoneRatings;
	ArrayList<double[][]> mobilityRatings;
	ArrayList<double[][]> moveRatings;
	double[][] nrOfFieldColorChange;
	ArrayList<Coordinates> hypotheticalMoves;
	ArrayList<GameBoard> hypotheticalBoards;

	@Override
	public void initialize(int myColor, long timeLimit) {

		this.myColor = myColor;
		this.timeLimit = timeLimit;
		timeToUse = 0.999;
		noTimeLeft = false;

		// initialize hypotheticalMoves with 15 coordinates, so that we will not have a
		// out of bounds index error
		hypotheticalMoves = new ArrayList<>();
		hypotheticalBoards = new ArrayList<>();
		for (int i = 0; i < 15; i++) {
			hypotheticalMoves.add(null);
			hypotheticalBoards.add(null);
		}

		// do not use this if the gamecoordinator plays the game. he will give the
		// ratings to the player
		readDataFromFiles(BASE_FILENAME);

	}

	public void readDataFromFiles(String baseFilename) {

		// initialize rating boards
		// myColor begins
		if (myColor == GameBoard.RED) {
			stoneRatings = normalize(dataReader.readRatingsFromFile(this.getClass(), baseFilename + "_stoneRatings_red_wins.txt"));
			mobilityRatings = normalize(dataReader.readRatingsFromFile(this.getClass(), baseFilename + "_mobilityRatings_red_wins.txt"));
			moveRatings = normalize(dataReader.readRatingsFromFile(this.getClass(), baseFilename + "_moveRatings_red_wins.txt"));
		}
		// myColor is second player
		else {
			stoneRatings = normalize(dataReader.readRatingsFromFile(this.getClass(), baseFilename + "_stoneRatings_green_wins.txt"));
			mobilityRatings = normalize(
					dataReader.readRatingsFromFile(this.getClass(), baseFilename + "_mobilityRatings_green_wins.txt"));
			moveRatings = normalize(dataReader.readRatingsFromFile(this.getClass(), baseFilename + "_moveRatings_green_wins.txt"));
		}

		nrOfFieldColorChange = normalize(dataReader.readRatingFromFile(this.getClass(), baseFilename + "_colorChange.txt"));

		// print ratings to see what they look like:
//		dataReader.printRatingsBoard(moveRatings, 0);
//		dataReader.printRatingsBoard(moveRatings, 1);
//		dataReader.printRatingsBoard(moveRatings, 2);

	}

	@Override
	public Coordinates nextMove(GameBoard gb) {

		actualBoard = gb;

		// start timer to measure how long we needed for our move
		long startTime = System.currentTimeMillis();

		BOARDSIZE = gb.getSize(); // or hardcode this to 8

		// count empty fields
		freeFields = BOARDSIZE * BOARDSIZE - gb.countStones(1) - gb.countStones(2);

		// print ratings for current move
//		if (freeFields != 0) {
//			System.out.println("stoneRatings(" + (60 - freeFields) + "):");
//			dataReader.printRatingsBoard(stoneRatings, 60 - freeFields);
//			System.out.println("mobilityRatings(" + (60 - freeFields) + "):");
//			dataReader.printRatingsBoard(mobilityRatings, 60 - freeFields);
//			System.out.println("moveRatings(" + (60 - freeFields) + "):");
//			dataReader.printRatingsBoard(moveRatings, 60 - freeFields);
//		}

		// Check if the player has any legal moves
		if (gb.isMoveAvailable(myColor)) {

//			System.out.println("calculating move");
			// The Coordinates that our player chooses
			Coordinates bestMove = new Coordinates(-1, -1);
			bestMove = findBestMove(gb.clone(), startTime);

//			System.out.println("took us " + (System.currentTimeMillis() - startTime) + "ms to find a move");

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
		calculationDepth = 1; // start depth for the alpha-beta-algorithm
		Coordinates currentDepthBestCoordinates = new Coordinates(-1, -1);
		Coordinates bestCoordinates = new Coordinates(-1, -1);
		Coordinates currentCoordinates;

		hypotheticalBoards.set(0, board.clone());

		// calculate ratings for increasing depths while the depth does not reach the
		// full game board
		try { // catch Exception that is thrown, if there was not enough time left to finish
				// the calculation
			while (/* calculationDepth <= 2 && */calculationDepth <= freeFields || bestCoordinates.getCol() == -1) {

//				System.out.println("calculationDepth = " + calculationDepth);

				currentDepthBestRating = NOT_INITIALIZED;
				currentDepthBestCoordinates = new Coordinates(-1, -1);

				// iterate over all board fields
				for (int y = 1; y <= board.getSize(); y++) {
					for (int x = 1; x <= board.getSize(); x++) {

						currentCoordinates = new Coordinates(y, x);
						hypotheticalMoves.set(0, currentCoordinates);

//						System.out.println("checking move " + x + "/" + y);
						if (board.checkMove(myColor, currentCoordinates)) {

//							System.out.println("last checked move is available");
							// find rating for this move and save move if it has the better rating than all
							// the moves tested before from this depth
							currentRating = findRating(board.clone(), currentCoordinates, calculationDepth - 1, myColor,
									currentDepthBestRating, startTime);

//							System.out.println("currentRating = " + currentRating);

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

				calculationDepth++;
				// play for the win -- positive stone difference
				if (calculationDepth == freeFields) {
					playForWin = true;
				}
			}
		} catch (Exception e) {
			// do not update bestCoordinates, just return the best ones so far
//			System.out.println("got the exception");

			if (e.getMessage() != null && !e.getMessage().equals("no time for calculation left!")) {
				e.printStackTrace();
			}

			// if no best move found, return the first possible one
			if (bestCoordinates.getCol() == -1) {

				System.out.println("no coordinates!");
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

//		System.out.println("maximum depth was: " + calculationDepth);
//		System.out.println("rating for this game is: " + bestRating);

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

//		System.out.println("finding rating");

		double lastBestRating = NOT_INITIALIZED; // best rating we got so far
		double currentRating; // rating for the move from the current iteration
		Coordinates currentCoordinates; // coordinates for the next move

		// make the move, save old board
		GameBoard oldBoard = board.clone();
		board.makeMove(player, move);

		hypotheticalBoards.set(calculationDepth - depth, board.clone());

		// recursion termination clause
		if (depth <= 0 || board.isFull()) {
//			System.out.println("getting rating");
			double rating = rating(board, oldBoard, player);
//			System.out.println("returning rating");
			return rating;
		}

		// iterate over each field
		for (int y = 1; y <= BOARDSIZE; y++) {
			for (int x = 1; x <= BOARDSIZE; x++) {

				currentCoordinates = new Coordinates(y, x);
				hypotheticalMoves.set(calculationDepth - depth, currentCoordinates);

				// if move is possible, calculate rating
				if (board.checkMove(Utils.other(player), currentCoordinates)) {

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
				hypotheticalMoves.set(calculationDepth - depth, null);
				lastBestRating = findRating(board.clone(), null, depth - 1, Utils.other(player), lastBestRating,
						startTime);
			} else {
				throw new Exception("no time left!");
			}

		}

		return lastBestRating;

	}

	private double rating(GameBoard currentBoard, GameBoard previousBoard, int whoDidLastMove) {

//		System.out.println("in ratings function");

		int currentOccupation;
		double rating = 0;
		int moveNumber = currentBoard.countStones(1) + currentBoard.countStones(2) - 4;

		if (moveNumber == 60)
			return 0;

		double[][] currentStoneRating = stoneRatings.get(moveNumber);
		double[][] currentMobilityRating = mobilityRatings.get(moveNumber);
		double[][] currentMoveRating = moveRatings.get(moveNumber);

//		System.out.println("got all current datas");
		// TODO: correct ratings indexes?

		/*
		 * TODO: something fancy here
		 */

		// add up ratings
		double stoneRatingSum = 0;
		double mobilityRatingSum = 0;
		double moveRatingSum = 0;
		double cornerRating = 0;
		double saveStoneRating = 0;
		Coordinates currentCoordinates;
		GameBoard hypotheticalBoard;

		for (int x = 0; x < BOARDSIZE; ++x) {
			for (int y = 0; y < BOARDSIZE; ++y) {
				try {

					
					
					
					
					currentCoordinates = new Coordinates(y, x);

					
					
					// rate for possession of the field for all hypothetical fields
					for (int i = calculationDepth; i > 0; i--) {

						hypotheticalBoard = hypotheticalBoards.get(i - 1);
						if (hypotheticalBoard.getOccupation(currentCoordinates) == myColor) {
							stoneRatingSum += stoneRatings.get(moveNumber + i - calculationDepth)[x][y]
									/ Math.max(nrOfFieldColorChange[x][y], 0.01);
							// mobilityRatingSum += currentMobilityRating[x][y];
							// moveRatingSum += currentMoveRating[x][y];
						} else if (hypotheticalBoard.getOccupation(currentCoordinates) == Utils.other(myColor)) {
							stoneRatingSum -= 2 * stoneRatings.get(moveNumber + i - calculationDepth)[x][y]
									/ Math.max(nrOfFieldColorChange[x][y], 0.01);
							// mobilityRatingSum -= 2 * currentMobilityRating[x][y];
							// moveRatingSum -= currentMoveRating[x][y];
						}
//						System.out.println("looking the hypothetical boards");
					}

					
					if (currentBoard.getOccupation(currentCoordinates) == myColor) {
						saveStoneRating += 100 * isSaveStone(currentBoard, new Coordinates(y, x));
					} else {
						saveStoneRating -= 100 * isSaveStone(currentBoard, new Coordinates(y, x));
					}
					
					
					if (currentBoard.getOccupation(currentCoordinates) == 0) {

						// no one has this field => it could be a possible move
						if (whoDidLastMove == Utils.other(myColor)) {
							// test for our possible move
							if (currentBoard.checkMove(myColor, currentCoordinates)) {
								mobilityRatingSum += currentMobilityRating[x][y]
										/ Math.max(nrOfFieldColorChange[x][y], 0.01);
							} else if (previousBoard.checkMove(Utils.other(myColor), currentCoordinates)) {
								mobilityRatingSum -= 2 * currentMobilityRating[x][y]
										/ Math.max(nrOfFieldColorChange[x][y], 0.01);
							}
						} else {
							// test for enemys possible move
							if (currentBoard.checkMove(Utils.other(myColor), currentCoordinates)) {
								mobilityRatingSum -= 2 * currentMobilityRating[x][y]
										/ Math.max(nrOfFieldColorChange[x][y], 0.01);
							} else if (previousBoard.checkMove(myColor, currentCoordinates)) {
								mobilityRatingSum += currentMobilityRating[x][y]
										/ Math.max(nrOfFieldColorChange[x][y], 0.01);
							}
						}
					}

				} catch (OutOfBoundsException e) {
				}
			}
		}

		// sum the move ratings over all done moves since the current gameboard
		Coordinates currentMove;
		int moduloOffset = 1;
		if (whoDidLastMove == myColor) {
			moduloOffset = 0;
		}
		for (int i = calculationDepth; i > 0; i--) {
			// count enemy moves negative
			if ((currentMove = hypotheticalMoves.get(i - 1)) != null) {
				if ((i + moduloOffset) % 2 == calculationDepth % 2) {
					moveRatingSum += moveRatings.get(moveNumber + i - calculationDepth)[currentMove.getCol()
							- 1][currentMove.getRow() - 1];
				} else {
					moveRatingSum -= 2 * moveRatings.get(moveNumber + i - calculationDepth)[currentMove.getCol()
							- 1][currentMove.getRow() - 1];
				}
			}

		}

		// rate corners
		try {
			for (Coordinates corner : corners) {
				if (currentBoard.getOccupation(corner) == Utils.other(myColor)
						|| currentBoard.checkMove(Utils.other(myColor), corner)) {
					cornerRating -= 1000;
				} else {
					cornerRating += 100;
				}
			}
		} catch (OutOfBoundsException e) {
		}
		;

		// look that our possible moves are more than the others and our possible moves
		// are further outside
		double mobilityRating02 = 0;
		if (whoDidLastMove == myColor) {
			double divisor = Math.max(currentBoard.mobility(Utils.other(whoDidLastMove)), 0.05);
			mobilityRating02 += previousBoard.mobility(whoDidLastMove) / divisor;
			divisor = Math.max(countPossibleMovesDistance(currentBoard, Utils.other(whoDidLastMove)), 0.05);
			mobilityRating02 += countPossibleMovesDistance(previousBoard, whoDidLastMove) / divisor;
		} else {
			double divisor = Math.max(previousBoard.mobility(whoDidLastMove), 0.05);
			mobilityRating02 += (currentBoard.mobility(myColor)) / divisor;
			divisor = Math.max(countPossibleMovesDistance(previousBoard, whoDidLastMove), 0.05);
			mobilityRating02 += countPossibleMovesDistance(currentBoard, myColor) / divisor;

		}

		// if we have a corner, better rating means more stones near our corner
		double cornerCenterX = 0, cornerCenterY = 0;
		int ourCorners = 0;
		try {
			for (Coordinates c : corners) {
				if (actualBoard.getOccupation(c) == myColor
						|| (whoDidLastMove == Utils.other(myColor) && currentBoard.checkMove(myColor, c))) {

					// if its not the opposite corner, add it to corner center
					// use maximum 2 corners
					if (ourCorners < 2
							&& (ourCorners == 0 || c.getCol() == cornerCenterX || c.getRow() == cornerCenterY)) {
						cornerCenterX += c.getCol();
						cornerCenterY += c.getRow();
						ourCorners++;
					} else {
//					System.out.println("do not look at diagonal corners");
					}
					break;
				}
			}
		} catch (OutOfBoundsException o) {
		};

		cornerCenterX /= Math.max(ourCorners, 1);
		cornerCenterY /= Math.max(ourCorners, 1);

		double cornerRating02 = 0;
		if (cornerCenterX != 0) {

			cornerRating02 += countStoneDistance(currentBoard, oppositeCorner(cornerCenterX, cornerCenterY));

			cornerRating02 = cornerRating02 / 30;

			rating += cornerRating02;

//					System.out.println("additional rating for having stones near our Corner is: " + additionalRating);
		}

//		System.out.println("stoneRatingSum = " + 0.1 * stoneRatingSum * (64 - freeFields) / 15);
//		System.out.println("moveRatingSum = " + 10 * moveRatingSum);
//		System.out.println("mobilityRatingSum = " + freeFields * mobilityRatingSum / 15);
//		System.out.println("cornerRaring = " + cornerRating);
//		System.out.println("saveStoneRating = " + saveStoneRating);

		// put different ratings together
		rating = 0.1 * stoneRatingSum * (64 - freeFields) / 20 + 4 * moveRatingSum + freeFields * mobilityRatingSum / 10
				+ 2 * cornerRating;
		rating /= Math.max(0.1, nrOfFieldColorChange[hypotheticalMoves.get(calculationDepth - 1).getCol()
				- 1][hypotheticalMoves.get(calculationDepth - 1).getRow() - 1]);
		rating /= 150;
		rating += 20 * mobilityRating02 + 2 * cornerRating02 + 2*saveStoneRating;
//		rating *= -(currentBoard.mobility(myColor) - previousBoard.mobility(Utils.other(myColor)));
		/*
		 * if (moveNumber > 35) { rating = (stoneRatingSum + moveRatingSum * 2) * 2 *
		 * mobilityRatingSum; } else { rating = (stoneRatingSum + moveRatingSum * 2) *
		 * mobilityRatingSum * 3; }
		 */
//		System.out.println("Rating: " + rating);
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

		// find the maximum value per move and divide the ratings from this move by that
		// value

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

	private double countPossibleMovesDistance(GameBoard board, int player) {

		double centerX = 4.5, centerY = 4.5;
		double distance = 0;
		Coordinates c;
		for (int y = 1; y <= BOARDSIZE; y++) {
			for (int x = 1; x <= BOARDSIZE; x++) {

				try {
					c = new Coordinates(y, x);
					if (board.getOccupation(c) == myColor) {
						distance += Math.sqrt(Math.pow(c.getCol() - centerX, 2) + Math.pow(c.getRow() - centerY, 2));
					}
				} catch (OutOfBoundsException oe) {

				}

			}
		}

		return distance;

	}

	private Point oppositeCorner(double x, double y) {

		Point p = new Point();
		p.setLocation(-x + 9, -y + 9);

		return p;

	}

	private double countStoneDistance(GameBoard board, Point center) {

		double distance = 0;
		Coordinates c;

		for (int y = 1; y <= BOARDSIZE; y++) {
			for (int x = 1; x <= BOARDSIZE; x++) {

				try {
					c = new Coordinates(y, x);
					if (board.getOccupation(c) == myColor) {
						distance += Math.sqrt(Math.pow(c.getCol() - center.x, 2) + Math.pow(c.getRow() - center.y, 2));
					}
				} catch (OutOfBoundsException oe) {

				}

			}
		}

		return distance;

	}
	
	private int isSaveStone(GameBoard board, Coordinates field) {

		try {

			int occupation = board.getOccupation(field);
			boolean noSaveStoneForThisCorner = false;

			// only rate if the player has already a corner
			Coordinates corner;
			for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
				corner = corners[cornerIndex];

				if(board.getOccupation(corner) == myColor) {
					noSaveStoneForThisCorner = false;
	
	//					System.out.println("corner is: " + corner.getCol() + "/" + corner.getRow());
	
					// check horizontal stones towards corner
					boolean doOneMore = false;
					for (int x = field.getCol(); x != corner.getCol() || doOneMore; x += (corner.getCol() - field.getCol())
							/ Math.abs(corner.getCol() - field.getCol())) {
	
	//						System.out.println("checking field: " + x + "/" + field.getRow());
						if (board.getOccupation(new Coordinates(field.getRow(), x)) != occupation) {
							// is not a save stone
							noSaveStoneForThisCorner = true;
							break;
						}
	
						if (doOneMore) {
							break;
						}
	
						doOneMore = x + (corner.getCol() - field.getCol())
								/ Math.abs(corner.getCol() - field.getCol()) == corner.getCol();
	
					}
	
					if (noSaveStoneForThisCorner) {
						continue;
					}
	
					// check vertical stones towards corner
					doOneMore = false;
					for (int y = field.getRow(); y != corner.getRow(); y += (corner.getRow() - field.getRow())
							/ Math.abs(corner.getRow() - field.getRow())) {
	
	//						System.out.println("checking field: " + field.getCol() + "/" + y);
						if (board.getOccupation(new Coordinates(y, field.getCol())) != occupation) {
							// is not a save stone
							noSaveStoneForThisCorner = true;
							break;
						}
	
						if (doOneMore) {
							break;
						}
	
						doOneMore = y + (corner.getRow() - field.getRow())
								/ Math.abs(corner.getRow() - field.getRow()) == corner.getRow();
	
					}
	
					if (noSaveStoneForThisCorner) {
						continue;
					}
	
					// check diagonal towards corner
					doOneMore = false;
					for (int x = field.getCol(), y = field.getRow(); x != corner.getCol()
							&& y != corner.getRow(); x += (corner.getCol() - field.getCol())
									/ Math.abs(corner.getCol() - field.getCol()), y += (corner.getRow() - field.getRow())
											/ Math.abs(corner.getRow() - field.getRow())) {
	
	//						System.out.println("checking field: " + x + "/" + y);
						if (board.getOccupation(new Coordinates(y, x)) != occupation) {
							// is not a save stone
							noSaveStoneForThisCorner = true;
							break;
						}
	
						if (doOneMore) {
							break;
						}
	
						doOneMore = x + (corner.getCol() - field.getCol())
								/ Math.abs(corner.getCol() - field.getCol()) == corner.getCol()
								|| y + (corner.getRow() - field.getRow())
										/ Math.abs(corner.getRow() - field.getRow()) == corner.getRow();
	
					}
	
					if (noSaveStoneForThisCorner) {
						continue;
					}
	
					// check diagonal 90 degree to corner01
					Coordinates corner01, corner02;
					if (cornerIndex == 3) {
						corner01 = corners[0];
						corner02 = corners[2];
					} else if (cornerIndex == 0) {
						corner01 = corners[1];
						corner02 = corners[3];
					} else {
						corner01 = corners[cornerIndex + 1];
						corner02 = corners[cornerIndex - 1];
					}
					doOneMore = false;
					boolean firstIsFalse = false;
					for (int x = field.getCol(), y = field.getRow(); x != corner01.getCol()
							&& y != corner01.getRow(); x += (corner01.getCol() - field.getCol()) / Math
									.abs(corner01.getCol() - field.getCol()), y += (corner01.getRow() - field.getRow())
											/ Math.abs(corner01.getRow() - field.getRow())) {
	
	//						System.out.println("checking field: " + x + "/" + y);
						if (board.getOccupation(new Coordinates(y, x)) != occupation) {
							// is not a save stone
							firstIsFalse = true;
						}
	
						if (doOneMore) {
							break;
						}
	
						doOneMore = x
								+ (corner01.getCol() - field.getCol())
										/ Math.abs(corner01.getCol() - field.getCol()) == corner01.getCol()
								|| y + (corner01.getRow() - field.getRow())
										/ Math.abs(corner01.getRow() - field.getRow()) == corner01.getRow();
	
					}
	
					doOneMore = false;
					for (int x = field.getCol(), y = field.getRow(); x != corner02.getCol()
							&& y != corner02.getRow(); x += (corner02.getCol() - field.getCol()) / Math
									.abs(corner02.getCol() - field.getCol()), y += (corner02.getRow() - field.getRow())
											/ Math.abs(corner02.getRow() - field.getRow())) {
	
	//						System.out.println("checking field: " + x + "/" + y);
						if (board.getOccupation(new Coordinates(y, x)) != occupation) {
							// is not a save stone
							if (firstIsFalse) {
								noSaveStoneForThisCorner = true;
								break;
							}
						}
	
						if (doOneMore) {
							break;
						}
	
						doOneMore = x
								+ (corner02.getCol() - field.getCol())
										/ Math.abs(corner02.getCol() - field.getCol()) == corner02.getCol()
								|| y + (corner02.getRow() - field.getRow())
										/ Math.abs(corner02.getRow() - field.getRow()) == corner02.getRow();
	
					}
	
	//					System.out.println("it is a save stone");
					
					if(noSaveStoneForThisCorner) {
						continue;
					}
					
					return 1;
	
				}
				
			}

		} catch (OutOfBoundsException e) {
			e.printStackTrace();
		}

		return 0;

	}

}
