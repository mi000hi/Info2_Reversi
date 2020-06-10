package corona;

import reversi.Coordinates;

public class Move implements Comparable<Move> {

	public Coordinates coord;
	public double rating;

	public Move(Coordinates coord, double rating) {
		this.coord = coord;
		this.rating = rating;
	}
	
	public Move(Coordinates coord) {
		this.coord = coord;
		rating = -Double.MAX_VALUE;
	}
	
	public boolean equals(Move m) {
		return coord.toMoveString().equals(m.coord.toMoveString());
	}
	
	public boolean equals(Coordinates c) {
		return coord.toMoveString().equals(c.toMoveString());
	}

	@Override
	public int compareTo(Move m) {
		return (int) (100 * (m.rating - rating));
	}
	
	public String toString() {
		return coord.toMoveString() + " " + rating;
	}
}
