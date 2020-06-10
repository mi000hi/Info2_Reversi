package corona;

public class SkipCalculationException extends Exception {
	
	public Move move;
	
	public SkipCalculationException(Move move) {
		this.move = move;
	}

}
