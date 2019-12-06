package reversiPlayers;

import reversi.Coordinates;
import reversi.GameBoard;
import reversi.ReversiPlayer;
import reversi.Utils;

public class RandomPlayer implements ReversiPlayer {

	int color;
	int BOARDSIZE;
	
	@Override
	public void initialize(int myColor, long timeLimit) {
		color = myColor;
		
	}

	@Override
	public Coordinates nextMove(GameBoard gb) {
		
		BOARDSIZE = gb.getSize();
		
		// Check if the player has any legal moves
		if (gb.isMoveAvailable(color)) {

//			System.out.print(String.format("RandomPlayer %s is calculating a random move.\n", Utils.toString(color)));

			// The Coordinates that the random player chooses
			Coordinates where = new Coordinates(-1, -1);

			// Read in coordinates until the user inputs a valid move
			while (!gb.checkMove(color, where)) {
				where = new Coordinates((int) Math.abs(BOARDSIZE * Math.random()) + 1, (int) Math.abs(BOARDSIZE * Math.random()) + 1);

				// Print an error message if the provided position is invalid
				if (where != null && !gb.checkMove(color, where)) {
//					System.out.println(
//							String.format("RandomPlayer %s choose an invalid move: %s", Utils.toString(color),
//									where.toMoveString()));
				}
			}
			
//			System.out.println(String.format("RandomPlayer %s moves: %s", Utils.toString(color), where.toMoveString()));
			
			return where;
		} else {
//			System.out.println(String.format("RandomPlayer %s has no legal moves, passes.", Utils.toString(color)));
			return null;
		}
		
	}

}
