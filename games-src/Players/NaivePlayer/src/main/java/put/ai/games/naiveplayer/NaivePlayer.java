/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.naiveplayer;

import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class NaivePlayer extends Player {

    private Random random = new Random(0xdeadbeef);

    private List<Move> makeListOfMoves(Board board, Color color){
        return board.getMovesFor(color);
    }

    private Move getMove(List<Move> moves){
        int numberOfMoves = moves.size();
        if(numberOfMoves == 0) return null;
        Move move = moves.get(Math.abs(random.nextInt()) % numberOfMoves);
        moves.remove(move);
        return move;
    }
    private Move whatMoveToDo(Board board, Color color, Long time){
        Long startTime = System.nanoTime();
        Long actTime;
        List<Move> moves = makeListOfMoves(board, color);
        Move best = null;
        Long mini = Long.MAX_VALUE;

        for(Move m: moves){
            //dodac ograniczenie czasowe
            actTime = System.nanoTime();
            Long diff = time*1000000 - (actTime - startTime);
            if(diff <= time*100000) return best;
            Board boardAfterMove = board.clone();
            boardAfterMove.doMove(m);

            Long heur = alphaBeta(boardAfterMove, 3, Long.MIN_VALUE, Long.MAX_VALUE, color, diff);

            if(mini > heur){
                best = m;
                mini = heur;
            }
        }
        return best;
    }

    private Long alphaBeta(Board board, int depth, Long alpha, Long beta, Color color, Long time){
        if( depth == 0 || time <= 50*1000000) return heuristicEvaluation(color, board);
        Long timeR = time*1000000 - System.nanoTime();
        Color enemyColor = Color.PLAYER1;
        if(color.equals(Color.PLAYER1)) enemyColor = Color.PLAYER2;
        List<Move> moves = makeListOfMoves(board, enemyColor);
        Move move;
        while((move = getMove(moves)) != null){
            //OGRANICZENIE CZASOWE
            Board boardAfterMove = board.clone();
            boardAfterMove.doMove(move);
            Long eval = alphaBeta(board, depth - 1, -beta, -alpha, enemyColor, timeR);
            alpha = Math.max(alpha, eval);
            if(alpha >= beta) return beta;
        }
        return alpha;
    }


    @Override
    public String getName() {
        return "Maciej Walczykowski 145389 Szczepan Mierzejewski 140748";
    }


    @Override
    public Move nextMove(Board b) {
        Color playerColor = getColor();
        Long time = getTime();
        return whatMoveToDo(b, playerColor, time);
    }

    private Long heuristicEvaluation(Color color, Board board) {
        // get board size
        int size = board.getSize();
        // get start and goal for current player
        Field start = getStart(color, size);
        Field target = getTarget(color, size);
        // find the best pawn for current player
        Field playerClosest = getClosestPawn(color, board, size, target);
        // find the best pawn for other player
        Field enemyClosest = getClosestPawn(getOpponent(color), board, size, start);
        // check, basing on distance who is closer to win
        int playerDistance = getDistance(playerClosest, target);
        int enemyDistance = getDistance(enemyClosest, start);
        // calculate initial evaluation based on distance from goal and
        //  all player pawns - all player endangered pawns
        int playerEvaluation = size - playerDistance + countPawns(board, color, size) - countEndangered(board, color, size);
        // calculation done for both sides
        int enemyEvaluation = size - enemyDistance + countPawns(board, getOpponent(color), size) - countEndangered(board, getOpponent(color), size);
        // check if enemy can win in one move
        if(board.getWinner(getOpponent(color)) == getOpponent(color)) enemyEvaluation += board.getSize();
        // final evaluation is player evaluation - enemy evaluation
        return (long)(playerEvaluation - enemyEvaluation);
    }

    private int countPawns(Board board, Color color, int size) {
        int count = 0;
        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                if (board.getState(i,j) == color) count++;
            }
        }
        return count;
    }

    private int countEndangered(Board board, Color color, int size){
        int count = 0;
        //checking all edges - if pawn can be 'pushed off'
        for(int i = 0; i < size; i++) {
            if(board.getState(0,i) == color &&
                    board.getState(1,i) == getOpponent(color)) count++;
            if(board.getState(i,0) == color &&
                    board.getState(i,1) == getOpponent(color)) count++;
            if(board.getState(size - 1,i) == color &&
                    board.getState(size - 2,i) == getOpponent(color)) count++;
            if(board.getState(i, size - 1) == color &&
                    board.getState(i, size - 2) == getOpponent(color)) count++;
        }
        //checking rest of board - if pawn can be 'squished'
        for(int i = 1; i < size - 1; i++) {
            for(int j = 1; j < size - 1; j++) {
                if(board.getState(i, j) == color && (board.getState(i-1,j) == getOpponent(color) && board.getState(i+1,j) == getOpponent(color) ||
                        board.getState(i,j-1) == getOpponent(color) && board.getState(i,j+1) == getOpponent(color))) count++;
            }
        }
        return count;
    }

    private Field getClosestPawn(Color color, Board board, int size, Field target) {
        Field closest = null;
        for (int i=0; i<size; i++){
            for (int j=0; j<size; j++){
                Color currentColor = board.getState(i, j);
                if (currentColor == color) {
                    Field currentField = new Field(i, j);
                    int distanceCurrent = getDistance(currentField, target);
                    if (closest == null)
                        closest = currentField;
                    else{
                        int distanceBest = getDistance(closest, target);
                        if (distanceBest > distanceCurrent) closest = currentField;
                    }
                }
            }
        }
        return closest;
    }

    private int getDistance(Field from, Field to) {
        int distanceX = Math.abs(to.getX() - from.getX());
        int distanceY = Math.abs(to.getY() - from.getY());
        return distanceX + distanceY;
    }

    private Field getStart(Color color, int size) {
        if (color == Color.PLAYER1) return new Field(0, 0);
        return new Field(size - 1, size - 1);
    }

    private Field getTarget(Color color, int size) {
        if (color == Color.PLAYER2) return new Field(0, 0);
        return new Field(size - 1, size - 1);
    }

    private static class Field {
        private final int x;
        private final int y;

        public Field(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {return x;}

        public int getY() {return y;}
    }
}


