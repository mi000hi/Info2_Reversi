package neuralNet;

import reversi.GameBoard;

public class TrainingSample {

	public GameBoard gb;
	public int nextPlayer;
	public double gameResult;
	public int numberOfOutcomes;

	private int numberOfRedWins;
	private int numberOfGreenWins;
	private int numberOfDraws;

	public TrainingSample(GameBoard gb, int nextPlayer) {

		this.gb = gb;
		this.nextPlayer = nextPlayer;

		numberOfRedWins = 0;
		numberOfGreenWins = 0;
		numberOfDraws = 0;
	}

	public double setGameResult() {

		/*
		 * TODO: what should we do with the number of draws?
		 */

		numberOfOutcomes = numberOfRedWins + numberOfGreenWins + numberOfDraws;

		// if there is only this one outcome
		if(numberOfOutcomes == 0) {
			int stoneDifference = gb.countStones(GameBoard.RED) - gb.countStones(GameBoard.GREEN);
			
			// if its a draw
			if(stoneDifference == 0) {
				gameResult = 0.5;
				return gameResult;
			}
			gameResult = stoneDifference / Math.abs(stoneDifference) / 2 + 0.5;
			if (nextPlayer == GameBoard.GREEN) {
				gameResult = 1 - gameResult;
			}
			return gameResult;
		}
		
		if (nextPlayer == GameBoard.RED) {
			gameResult = (double) numberOfRedWins / numberOfOutcomes;
		} else {
			gameResult = (double) numberOfGreenWins / numberOfOutcomes;
		}

		return gameResult;
	}

	public void addRedWin() {
		numberOfRedWins++;
	}

	public void addGreenWin() {
		numberOfGreenWins++;
	}

	public void addDraw() {
		numberOfDraws++;
	}

}
