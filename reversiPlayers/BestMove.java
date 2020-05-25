package reversiPlayers;

import dataAccess.DataReader;
import dataAccess.DataWriter;
import java.util.ArrayList;

import reversi.*;

public class BestMove implements ReversiPlayer {
	private int myColor;
	private int otherColor;
	private int move = 0; // current move (0,59)

	// name of the file where the ratings are stored
	private final static String FILENAME_RANDOM_VS_RANDOM = "boardRatings_RandomPlayer_vs_RandomPlayer.txt";
	private DataReader dataReader = new DataReader(8);
	ArrayList<double[][]> ratings = dataReader.readRatingsFromFile(this.getClass(), FILENAME_RANDOM_VS_RANDOM);

	public void initialize(int myColor, long timeLimit) {
		this.myColor = myColor;
		this.otherColor = Utils.other(myColor);
	}

	public Coordinates nextMove(GameBoard gb) {
		move = gb.countStones(myColor) + gb.countStones(otherColor) - 4;

		// Check if the player has any legal moves
		if (gb.isMoveAvailable(myColor)) {

			// List with valid moves
			ArrayList<Coordinates> validMoves = new ArrayList<Coordinates>();
			for (int row = 1; row <= gb.getSize(); row++) {
				for (int col = 1; col <= gb.getSize(); col++) {
					Coordinates coord = new Coordinates(row, col);
					if (gb.checkMove(myColor, coord))
						validMoves.add(coord);
				}
			}
			Coordinates bestcoord = validMoves.get(0);
			int col = bestcoord.getCol();
			int row = bestcoord.getRow();
			double bestrating = ratings.get(move)[col-1][row-1];

			double rating;
			Coordinates coord;
			for (int j = 1; j < validMoves.size(); ++j) {
				coord = validMoves.get(j);
				col = bestcoord.getCol();
				row = bestcoord.getRow();
				rating = ratings.get(move)[col-1][row-1];
				if (rating > bestrating) {
					bestcoord = coord;
					bestrating = rating;
				}
			}

			return bestcoord;
		}
		return null;
	}
}