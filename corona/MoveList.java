package corona;

import java.util.ArrayList;

public class MoveList implements Comparable<MoveList> {

	Move move;
	ArrayList<MoveList> followingMoves;
	double alpha, beta;
	
	public MoveList(Move move) {
		
		this.move = move;
		followingMoves = new ArrayList<>();
		
		alpha = -Double.MAX_VALUE;
		beta = Double.MAX_VALUE;
		
	}
	
	public MoveList getLastMoveList() {
		
		if(followingMoves == null) {
			return null;
		}
		return followingMoves.get(followingMoves.size() - 1);
	}
	
	@Override
	public int compareTo(MoveList ml) {
		return (int) (100 * (ml.move.rating - move.rating));
	}
}
