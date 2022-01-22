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
            if((actTime - startTime)*1000000 <= 100) break;
            Board boardAfterMove = board.clone();
            boardAfterMove.doMove(m);

            Long heur = alphaBeta(boardAfterMove, 3, Long.MIN_VALUE, Long.MAX_VALUE, color);

            if(mini > heur){
                best = m;
                mini = heur;
            }
        }
        return best;
    }

    private Long alphaBeta(Board board, int depth, Long alpha, Long beta, Color color){
        if( depth == 0 ) return 0L; //ocena szczepan!!

        Color enemyColor = Color.PLAYER1;
        if(color.equals(Color.PLAYER1)) enemyColor = Color.PLAYER2;
        List<Move> moves = makeListOfMoves(board, enemyColor);
        Move move;
        while((move = getMove(moves)) != null){
            //OGRANICZENIE CZASOWE
            Board boardAfterMove = board.clone();
            boardAfterMove.doMove(move);
            Long eval = alphaBeta(board, depth - 1, -beta, -alpha, enemyColor);
            alpha = Math.max(alpha, eval);
            if(alpha >= beta) return beta;
        }
        return alpha;
    }


    @Override
    public String getName() {
        return "Maciej Walczykowski 145389 Szczepan Mierzejewski ______";
    }//indeks szczepan!


    @Override
    public Move nextMove(Board b) {
        Color playerColor = getColor();
        Long time = getTime();
        return whatMoveToDo(b, playerColor, time);
    }
}
