package corona;

public class SkipCalculationException extends Exception {
	
	Move move;
	
	public SkipCalculationException(Move move) {
		this.move = move;
	}

}
