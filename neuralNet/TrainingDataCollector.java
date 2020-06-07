package neuralNet;

import java.util.ArrayList;

import reversi.BitBoard;
import reversi.Coordinates;
import reversi.GameBoard;
import reversi.ReversiPlayer;
import reversiPlayers.RandomPlayer;
import data.Terminatable;
import data.Terminator;

public class TrainingDataCollector implements Terminatable {

	private String trainingDataFilename = "neuralNet_reversi_trainingData.txt";
	private static boolean terminateProgram = true;

	public static void main(String[] args) {
		
		TrainingDataCollector tdc = new TrainingDataCollector();

		Terminator terminator = new Terminator(tdc);
		Thread terminatorThread = new Thread(terminator);
		terminatorThread.start();
		tdc.setTerminateProgram(false);
		
		while(!terminateProgram) {
			
		}
		System.out.println("program terminated");

	}

	/**
	 * returns a TrainingSample object that contains a reversi gameboard and the
	 * result value of the outcome. a maximal Depth of 12 can already take a pretty
	 * long time. a maximal Depth of 11 takes around 3 seconds to calculate
	 * 
	 * @param maximalDepth the maximal number of moves that can be made until the
	 *                     game ends.
	 * @return a object containing the winrates together with its gameboard
	 */
	public TrainingSample findTrainingSample(int maximalDepth) {

		/**
		 * the game must be finished after maximalDepth moves
		 */

		long startTime = System.currentTimeMillis();

		ReversiPlayer[] players = new ReversiPlayer[2];
		players[0] = new RandomPlayer();
		players[1] = new RandomPlayer();
		players[0].initialize(GameBoard.RED, 0);
		players[1].initialize(GameBoard.GREEN, 0);

		// generate a gameboard with maximalDepth free fields
		TrainingSample trainingSample = playGame(players, 60 - maximalDepth);

		// find the winrate
		trainingSample = winRate(trainingSample, trainingSample.gb.clone(), trainingSample.nextPlayer);

		// find the winrate accroding to the player who has to make a turn
		trainingSample.setGameResult();

		System.out.println("the trainingSample has " + trainingSample.numberOfOutcomes + " possible outcomes"
				+ "\nwith a winPrediction of " + trainingSample.gameResult + " for the player "
				+ trainingSample.nextPlayer);
		System.out.print("\n" + trainingSample.gb);
		System.out.println("finding this trainingSample took " + (System.currentTimeMillis() - startTime) + " ms\n");

		// return the trainingSample
		return trainingSample;
	}

	/**
	 * edits the win-parameters of the given {@code TrainingSample sample} according
	 * to the games outcome by checking all possible outcomes through recursion.
	 * 
	 * @param sample the trainingsample that gets modified
	 * @param board  the current gameboard -- this changes through the recursion
	 * @param player the player that has to make the next move
	 * @return the same trainingsample that was provided, but its internal
	 *         win-numbers will have changed
	 */
	private TrainingSample winRate(TrainingSample sample, GameBoard board, int player) {

		/*
		 * TODO: add a failsave that exits if recursion for this gameboard is too far,
		 * so that we cannot finish the calculations in appropriate time
		 */

		// go further in recursion
		if (board.isMoveAvailable(GameBoard.GREEN) || board.isMoveAvailable(GameBoard.RED)) {

			Coordinates move;
			GameBoard nextBoard;
			for (int x = 1; x <= 8; x++) {
				for (int y = 1; y <= 8; y++) {

					move = new Coordinates(y, x);
					if (board.checkMove(player, move)) {
						nextBoard = board.clone();
						nextBoard.makeMove(player, move);
						sample = winRate(sample, nextBoard, reversi.Utils.other(player));
					}
				}
			}
			return sample;

			// exit recursion
		} else {
			int stoneDifference = board.countStones(GameBoard.RED) - board.countStones(GameBoard.GREEN);
			if (stoneDifference > 0) {
				// red wins
				sample.addRedWin();
			} else if (stoneDifference < 0) {
				// green wins
				sample.addGreenWin();
			} else {
				// draw
				sample.addDraw();
			}
			return sample;
		}
	}

	/**
	 * plays a game of reversi for {@code numberOfMoves} moves
	 * 
	 * @param players       the two reversiplayers used to play the game
	 * @param numberOfMoves the number of moves played (passing is not counted)
	 * @return the gameboard after {@code numberOfMoves} moves
	 */
	private TrainingSample playGame(ReversiPlayer[] players, int numberOfMoves) {

		// create the game board
		GameBoard board = new BitBoard();
		int currentPlayer = GameBoard.RED;
		Coordinates currentMove;

		// play the game
		while ((board.isMoveAvailable(GameBoard.GREEN) || board.isMoveAvailable(GameBoard.RED)) && numberOfMoves > 0) {

			// player makes a move
			currentMove = players[currentPlayer - 1].nextMove(board.clone());
			board.makeMove(currentPlayer, currentMove);

			// switch player
			currentPlayer = -currentPlayer + 3;
			numberOfMoves--;
		}

		// create the trainingSample
		TrainingSample trainingSample = new TrainingSample(board, currentPlayer);

		return trainingSample;
	}

	@Override
	public void setTerminateProgram(boolean value) {
		terminateProgram = value;
	}

}
