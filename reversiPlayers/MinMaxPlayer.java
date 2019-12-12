package reversiPlayers;

import reversi.Coordinates;
import reversi.GameBoard;
import reversi.ReversiPlayer;
import reversi.Utils;
import java.util.ArrayList;

public class MinMaxPlayer implements ReversiPlayer {
        private int myColor;
        private int otherColor;
  private long timeLimit;
  
  class Timeout extends Throwable {
    private static final long serialVersionUID = 1L;
  }

  public void initialize(int myColor, long timeLimit) {
    this.myColor = myColor;
        this.otherColor = Utils.other(myColor);
    this.timeLimit = timeLimit;
  }

  public Coordinates nextMove(GameBoard gb) {
    long timeout = System.currentTimeMillis() + timeLimit - 10;

    BestMove bestMove = null;
    try {
      bestMove = max(1, timeout, gb, 0, 0);
    } catch (Timeout e) {
      throw new AssertionError("Hmm, not enough time for recursion depth 1");
    }

    int maxDepth = 0;
    try {
      for (int i = 2; ; i++) {
    	maxDepth = i;
        bestMove = max(i, timeout, gb, 0, bestMove.value);
      }
    } catch (Timeout e) {
    	
    	System.out.println("max depth was " + maxDepth);
    }
    return bestMove.coord;
  }

  /**
   * Result of the min-max analysis
   */
  class BestMove {
        /**
         * The coordinates of the proposed next move
         */
    public Coordinates coord;
    
    /**
     * The value of the callers game board according to the min-max analysis
     */
    public int value;
    
    public BestMove(int value, Coordinates coord) {
      this.value = value;
      this.coord = coord;
    }
  }

  /**
   * Performs a min-max analysis.
   * 
   * @param maxDepth the maximum recursion depth
   * @param timeout a hard timeout for the analysis
   * @param gb the game situation which is analysed
   * @param depth the current recursion depth, starting at 0
   * @return the best move for the given player according to the min-max analysis.
   * @throws Timeout if system time passes the given timeout.
   */
  private BestMove max(int maxDepth, long timeout, GameBoard gb,
      int depth, int referenceRating) throws Timeout 
   {
    if (System.currentTimeMillis() > timeout) {
      throw new Timeout();
    }
    
    if (depth == maxDepth) {
      return new BestMove(eval(gb), null);
    }

    ArrayList<Coordinates> availableMoves = 
        new ArrayList<Coordinates>(gb.getSize() * gb.getSize());

    for (int x = 1; x <= gb.getSize(); x++) {
      for (int y = 1; y <= gb.getSize(); y++) {
        Coordinates coord = new Coordinates(x, y);
        if (gb.checkMove(myColor, coord)) {
          availableMoves.add(coord);
        }
      }
    }

    if (availableMoves.isEmpty()) {
      if (gb.isMoveAvailable(otherColor)) {
        BestMove result = min(maxDepth, timeout, gb, depth+1, 0);
        return new BestMove(result.value, null);
      } else {
        return new BestMove(finalResult(gb), null);
      }
    }

    BestMove bestMove = new BestMove(Integer.MIN_VALUE, null);
    for (Coordinates coord : availableMoves) {
      GameBoard hypothetical = gb.clone();
      hypothetical.makeMove(myColor, coord);
      BestMove result = min(maxDepth, timeout, hypothetical, depth + 1, bestMove.value);
      
      if (result.value > bestMove.value) {
        bestMove.coord = coord;
        bestMove.value = result.value;
      }
      
      if(bestMove.value >= referenceRating) {
    	  return bestMove;
      }
    }

    return bestMove;
  }

  private BestMove min(int maxDepth, long timeout, GameBoard gb,
      int depth, int referenceRating) throws Timeout 
   {
    if (System.currentTimeMillis() > timeout) {
      throw new Timeout();
    }
    
    if (depth == maxDepth) {
      return new BestMove(eval(gb), null);
    }

    ArrayList<Coordinates> availableMoves = 
        new ArrayList<Coordinates>(gb.getSize()* gb.getSize());

    for (int x = 1; x <= gb.getSize(); x++) {
      for (int y = 1; y <= gb.getSize(); y++) {
        Coordinates coord = new Coordinates(x, y);
        if (gb.checkMove(otherColor, coord)) {
          availableMoves.add(coord);
        }
      }
    }

    if (availableMoves.isEmpty()) {
      if (gb.isMoveAvailable(myColor)) {
        BestMove result = max(maxDepth, timeout, gb, depth+1, 0); // TODO: 0 for referenceRating?
        return new BestMove(result.value, null);
      } else {
        return new BestMove(finalResult(gb), null);
      }
    }

    BestMove bestMove = new BestMove(Integer.MAX_VALUE, null);
    for (Coordinates coord : availableMoves) {
      GameBoard hypothetical = gb.clone();
      hypothetical.makeMove(otherColor, coord);
      BestMove result = max(maxDepth, timeout, hypothetical, depth + 1, bestMove.value);
      
      if (result.value < bestMove.value) {
        bestMove.coord = coord;
        bestMove.value = result.value;
      }
      
      if(bestMove.value <= referenceRating) {
    	  return bestMove;
      }
    }

    return bestMove;
  }

  /**
   * Returns the value of a finished game
   * @param gb the situation
   * @return the value of the finished game from the perspective of the player.
   */
  private int finalResult(GameBoard gb)
  {
//    final int myStones = gb.countStones(myColor);
//    final int otherStones = gb.countStones(otherColor);
//    if (myStones > otherStones) return maxEval(gb);
//    if (otherStones > myStones) return minEval(gb);
//    return draw(gb);
	  
	  return eval(gb);
  }
  
  /**
   * Estimate the value of a game situation.
   * 
   * @param gb
   *            the situation to consider
   * @return the value of the current game board from the perspective of the player
   */
  private int eval(GameBoard gb) {
    return gb.countStones(myColor) - gb.countStones(otherColor);
  }
  
  /**
   * Get the upper bound for the value of a game situation.
   * 
   * @param gb a game board
   * @return the maximum value possible for any situation on the given game board 
   */
//  private int maxEval(GameBoard gb) {
//    return gb.getSize() * gb.getSize();
//  }

  /**
   * Get the lower bound for the value of a game situation.
   * 
   * @param gb a game board
   * @return the maximum value possible for any situation on the given game board 
   */
//  private int minEval(GameBoard gb) {
//    return -1 * maxEval(gb);
//  }
  
  /**
   * Get the value of a draw game
   * @param gb a game board
   * @return the value of a draw game on the given board
   */
//  private int draw(GameBoard gb) {
//    return 0;
//  }
}