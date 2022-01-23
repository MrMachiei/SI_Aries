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
            if(diff <= time*10000) return best;
            Board boardAfterMove = board.clone();
            boardAfterMove.doMove(m);

            Long heur = alphaBeta(boardAfterMove, 1, Long.MIN_VALUE, Long.MAX_VALUE, color, diff);

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
        if(color == Color.PLAYER1) enemyColor = Color.PLAYER2;
        List<Move> moves = makeListOfMoves(board, enemyColor);
        Move move;
        while((move = getMove(moves)) != null){
            //OGRANICZENIE CZASOWE
            Board boardAfterMove = board.clone();
            boardAfterMove.doMove(move);
            Long eval = -alphaBeta(board, depth - 1, -beta, -alpha, enemyColor, timeR);
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
        int size = board.getSize();

        Field start = getStart(color, size);
        Field target = getTarget(color, size);

        Field playerClosest = getClosestPawn(color, board, size, target);
        Field enemyClosest = getClosestPawn(getOpponent(color), board, size, start);

        int playerDistance = getDistance(playerClosest, target);
        int enemyDistance = getDistance(enemyClosest, start);

        int playerDanger = countEndangered(board, color, size);
        int enemyDanger = countEndangered(board, getOpponent(color), size);

        if (playerDistance > 0) playerDistance = size * size;
        else playerDistance = size - playerDistance;
        if (wayIsEmpty(playerClosest, target, board, color)) playerDistance += 2 * size;

        if (enemyDistance == 0) enemyDistance = size * size;
        else enemyDistance = size - enemyDistance;
        if (wayIsEmpty(enemyClosest, target, board, color)) enemyDistance += 2 * size;

        int playerEvaluation = playerDistance  * countPawns(board, color, size) - playerDanger;

        int enemyEvaluation = enemyDistance  * countPawns(board, getOpponent(color), size) - enemyDanger;

        if(board.getWinner(getOpponent(color)) == getOpponent(color)) enemyEvaluation += size * size;
        return (long)(enemyEvaluation - playerEvaluation);
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
        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                if(canBePushedOff(new Field(x,y), board, color, size)) count++;
                if(canBeSquished(new Field(x,y), board, color, size)) count++;
            }
        }
        return count;
    }

    private boolean canBePushedOff(Field currentPawn, Board board, Color color, int size){
        if(currentPawn.getX() == 0)
            if(anythingFromLine(currentPawn, board, color, size, 0))return true;
        if(currentPawn.getX() == size - 1)
            if(anythingFromLine(currentPawn, board, color, size, 1))return true;
        if(currentPawn.getY() == 0)
            if(anythingFromLine(currentPawn, board, color, size, 2))return true;
        if(currentPawn.getY() == size - 1)
            if(anythingFromLine(currentPawn, board, color, size, 3))return true;
        return false;
    }

    private boolean canBeSquished(Field currentPawn, Board board, Color color, int size){
        if(anythingNextto(currentPawn,board,color,size,0) && anythingFromLine(currentPawn, board, color, size, 1)) return true;
        if(anythingNextto(currentPawn,board,color,size,1) && anythingFromLine(currentPawn, board, color, size, 0)) return true;
        if(anythingNextto(currentPawn,board,color,size,2) && anythingFromLine(currentPawn, board, color, size, 3)) return true;
        if(anythingNextto(currentPawn,board,color,size,3) && anythingFromLine(currentPawn, board, color, size, 2)) return true;
        return false;
    }
    private boolean anythingNextto(Field currentPawn, Board board, Color color, int size, int direction){ //direction = 0 - x+; 1 - x-; 2 - y+; 3 - y-
        if(direction < 2) {
            int y = currentPawn.getY();
            if (direction % 2 == 0) {
                int x = currentPawn.getX() + 1;
                if(x < size)
                    if(board.getState(x,y) == getOpponent(color)) return true;
            }
            else {
                int x = currentPawn.getX() - 1;
                if(x >= 0)
                    if(board.getState(x,y) == getOpponent(color)) return true;
            }
        }
        else{
            int x = currentPawn.getX();
            if (direction % 2 == 0) {
                int y = currentPawn.getY() + 1;
                if(y < size)
                    if(board.getState(x,y) == getOpponent(color)) return true;
            }
            else {
                int y = currentPawn.getY() - 1;
                if(y >= 0)
                    if(board.getState(x,y) == getOpponent(color)) return true;
            }
        }
        return false;
    }

    private boolean anythingFromLine(Field currentPawn, Board board, Color color, int size, int direction){ //direction = 0 - x+; 1 - x-; 2 - y+; 3 - y-
        if(direction < 2){
            int y = currentPawn.getY();
            boolean friendlyLine = true;
            if(direction%2 == 0){
                for(int x = currentPawn.getX() + 1; x < size; x++){
                    Color lookupFieldColor = board.getState(x,y);
                    if(friendlyLine){
                        if(lookupFieldColor==Color.EMPTY) friendlyLine = false;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                    else{
                        if(lookupFieldColor==color) break;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                }
            }
            else{
                for(int x = currentPawn.getX() - 1; x > 0; x--){
                    Color lookupFieldColor = board.getState(x,y);
                    if(friendlyLine){
                        if(lookupFieldColor==Color.EMPTY) friendlyLine = false;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                    else{
                        if(lookupFieldColor==color) break;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                }
            }
        }
        else{
            boolean friendlyLine = true;
            int x = currentPawn.getX();
            if(direction%2 == 0){
                for(int y = currentPawn.getY() + 1; y < size; y++){
                    Color lookupFieldColor = board.getState(x,y);
                    if(friendlyLine){
                        if(lookupFieldColor==Color.EMPTY) friendlyLine = false;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                    else{
                        if(lookupFieldColor==color) break;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                }
            }
            else{
                for(int y = currentPawn.getY() - 1; y > 0; y--){
                    Color lookupFieldColor = board.getState(x,y);
                    if(friendlyLine){
                        if(lookupFieldColor==Color.EMPTY) friendlyLine = false;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                    else{
                        if(lookupFieldColor==color) break;
                        if(lookupFieldColor==getOpponent(color)) return true;
                    }
                }
            }
        }
        return false;
    }

    private Field getClosestPawn(Color color, Board board, int size, Field target) {
        Field closest = null;
        for (int i=0; i<size; i++){
            for (int j=0; j<size; j++){
                Color currentColor = board.getState(i, j);
                if (currentColor==color) {
                    Field currentField = new Field(i, j);
                    int distanceCurrent = getDistance(currentField, target);
                    if (distanceCurrent == 0) return currentField;
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

    private boolean wayIsEmpty(Field from, Field to, Board board, Color color) {
        if(from.getX() == to.getX()){
            if(from.getY() < to.getY()) {
                for (int i = from.getY(); i <= to.getY(); i++)
                    if (board.getState(from.getX(), i)==getOpponent(color))
                        return false;
            }
            else {
                for (int i = to.getY(); i <= from.getY(); i++)
                    if (board.getState(from.getX(), i)==getOpponent(color))
                        return false;
            }
        }
        else if(from.getY() == to.getY()){
            if(from.getX() < to.getX()) {
                for (int i = from.getX(); i <= to.getX(); i++)
                    if (board.getState(i, from.getY())==getOpponent(color))
                        return false;
            }
            else {
                for (int i = to.getX(); i <= from.getX(); i++)
                    if (board.getState(i, from.getY())==getOpponent(color))
                        return false;
            }
        }
        return true;
    }

    private Field getStart(Color color, int size) {
        if (color==Color.PLAYER1) return new Field(0, 0);
        return new Field(size - 1, size - 1);
    }

    private Field getTarget(Color color, int size) {
        if (color==Color.PLAYER2) return new Field(0, 0);
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


