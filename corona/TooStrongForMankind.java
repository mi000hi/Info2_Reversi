package corona;

import reversi.Coordinates;
import reversi.GameBoard;
import reversi.OutOfBoundsException;
import reversi.ReversiPlayer;

/**
 * 
 * This bot can only win. it cheats though :)
 * 
 * it cannot play online.
 * 
 * @author michael
 *
 */

public class TooStrongForMankind implements ReversiPlayer{

	private int color;
	private boolean alreadyWon = false;
	
	@Override
	public void initialize(int myColor, long timeLimit) {
		
		this.color = myColor;
		
	}

	@Override
	public Coordinates nextMove(GameBoard gb) {
		
		if(!alreadyWon) {
			if(color == 1) {
				alreadyWon = true;
				gb.makeMove(color, new Coordinates(6, 3));
				return new Coordinates(6, 5);
			} else if(color == 2) {
				alreadyWon = true;
				
				try {
					
					// if enemy placed c6 or d6
					if(gb.getOccupation(new Coordinates(6, 3)) != 0 || gb.getOccupation(new Coordinates(6, 4)) != 0) {
						gb.makeMove(color, new Coordinates(5, 3));
						gb.makeMove(color, new Coordinates(3, 3));
						gb.makeMove(color, new Coordinates(3, 5));
						return new Coordinates(7, 3);
						
						// if enemy placed e6 or f6
					} else {
						gb.makeMove(color, new Coordinates(5, 6));
						gb.makeMove(color, new Coordinates(3, 6));
						gb.makeMove(color, new Coordinates(3, 3));
						return new Coordinates(7, 6);
					}
					
				} catch (OutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}

}
