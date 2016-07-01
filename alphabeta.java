// Brian Oh
// Connect Four AI using Iterative Deepening Minimax with Alpha-Beta Pruning

import java.lang.Math;
import java.util.ArrayList;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Random;
import java.util.HashMap;

public class alphabeta extends AIModule{
    
    private int player;
    private long[][] zobHash = new long[42][2];
    private HashMap<Long, Long> ttZobHash = new HashMap<>();
    private HashMap<Long, Integer> ttDepth = new HashMap<>();
    private HashMap<Long, Double> ttUpperBound = new HashMap<>();
    private HashMap<Long, Double> ttLowerBound = new HashMap<>();
    
    @Override
    public void getNextMove(final GameStateModule state){
        int depth = 0;
        if(state.getCoins()%2 == 0)
            player = 1;
        else
            player = 2;
        int bestMove = -1;
        double bestScore;
        Random rng = new Random();
        for (long[] zobHash1 : zobHash) {
            for (int j = 0; j < zobHash1.length; j++) {
                zobHash1[j] = rng.nextLong();
            }
        }
        
        while (!terminate){
            bestScore = Double.MIN_VALUE;
            for(int i=0; i<state.getWidth(); i++){
                if(state.canMakeMove(i)){
                    state.makeMove(i);
                    double moveScore = minimaxAB(state, depth, false, Double.MIN_VALUE, Double.MAX_VALUE);
                    if(moveScore > bestScore)
                    {
                        bestMove = i;
                        bestScore = moveScore;
                    }
                    state.unMakeMove();
                }
            }
            depth++;
        }     
        chosenMove = bestMove; 
    }
    
    private double minimaxAB(GameStateModule node, int depth, boolean isMaxPlayer, double alpha, double beta){
        double currValue;
        
        //If the game state is already in our transposition table, return the values
        long hashKey = zobristHash(node);
        if (ttZobHash.containsKey(hashKey) && hashKey == ttZobHash.get(hashKey)){
            if (ttDepth.get(hashKey)==depth){
                if (ttLowerBound.get(hashKey)>= beta) 
                    return ttLowerBound.get(hashKey);
                if (ttUpperBound.get(hashKey) <= alpha)
                    return ttUpperBound.get(hashKey);
            }
        }
        
        //If running low on time, stop growing the game tree
        //Leaf node, evaluate it and return its value
        if (depth==0 || node.isGameOver() || terminate){
            return evalFct(node, isMaxPlayer);
        }
        
        //Maximizing player
        else if (isMaxPlayer){
            currValue = Double.MIN_VALUE;
            ArrayList<Map.Entry<GameStateModule,Integer>> children = getChildren(node);
            for(int i=0; i<children.size(); i++){
                currValue = Math.max(currValue, minimaxAB(children.get(i).getKey(), depth-1, false, alpha, beta));
                alpha = Math.max(alpha, currValue);
                if (beta<=alpha){
                    break;
                }
            }         
        }
        
        //Minimizing player
        else{ 
            currValue = Double.MAX_VALUE;
            ArrayList<Map.Entry<GameStateModule,Integer>> children = getChildren(node);
            for(int i=0; i<children.size(); i++){
                currValue = Math.min(currValue, minimaxAB(children.get(i).getKey(), depth-1, true, alpha, beta));
                beta = Math.min(beta, currValue);
                if(beta<=alpha)
                    break;
            }
        }
        
        //Record transposition table information
        if (currValue <= alpha)
            ttUpperBound.put(hashKey, currValue);
        if (currValue > alpha && currValue < beta) {
            ttLowerBound.put(hashKey, currValue);
            ttUpperBound.put(hashKey, currValue);
        }
        if (currValue >= beta)
            ttLowerBound.get(hashKey);
        
        return currValue;
    }
    
    private ArrayList<Map.Entry<GameStateModule,Integer>> getChildren(GameStateModule node){
        ArrayList<Map.Entry<GameStateModule,Integer>> children = new ArrayList<>();
        
        //Iterate through every column and if possible, create a new GameStateModule node 
        for(int i=0; i<node.getWidth(); i++){
            if(node.canMakeMove(i)){
                Map.Entry<GameStateModule,Integer> child = new AbstractMap.SimpleEntry<>(node.copy(), i);
                child.getKey().makeMove(i);
                children.add(child);
            }
        }
        
        return children;
    }
    
    private long zobristHash(GameStateModule board){
        long boardHash=0;
        
        for(int x=0; x<board.getWidth(); x++){
            for (int y=0; y<board.getHeight(); y++){
                int coin = board.getAt(x, y);
                if(coin!=0){
                    int oneDIndex = x + y*board.getHeight();
                    boardHash ^= zobHash[oneDIndex][coin-1];
                }
            }
        }
        
        return boardHash;
    }
    
    private double evalFct(GameStateModule node, boolean isMaxPlayer)
    {
        if(node.isGameOver()){
            if (node.getWinner()==player) { //TODO: 1 is hard-coded
                return 900000.0;
            }
            else {
                return -900000.0;
            }
        }
        else{
            //initialize the number of in a rows for p1
            int p1_twoInRow = 0;
            int p1_threeInRow = 0;

            //initialize the number of in a rows for p2
            int p2_twoInRow = 0;
            int p2_threeInRow = 0;

            //initialize prev to 0 (empty) to start
            int prev = 0;

            //CHECK FOR IN A ROW BY ROW ONLY
            for(int i = 0; i < node.getHeight(); i++)
            {
                int tent_p1inARow = 0;
                int tent_p2inARow = 0;
                for(int j = 0; j < node.getWidth(); j++)
                {
                    //if node is empty set tentative in a row for p1 and p2 to 0 and set previous coin value to 0
                    if(node.getAt(i,j) == 0)
                    {
                        tent_p1inARow = 0;
                        tent_p2inARow = 0;
                        prev = 0;
                    }
                    else 
                    {
                        //if the slot is occuped by p1
                        if(node.getAt(i,j) == 1)
                        {
                            //if the previous coin was by p1, we increment the tentative #in a row for p1
                            if(prev == 1)
                            {
                                tent_p1inARow = tent_p1inARow + 1;
                            }
                            //if the previous coin was empty or by p2
                            else if(prev == 0 || prev == 2)
                            {
                                //if the tentative #in a row for p1 is greater than equal to 2, we store it
                                if(tent_p1inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p1inARow == 2)
                                    {
                                        p1_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p1inARow == 3)
                                    {
                                        p1_twoInRow--;
                                        p1_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p1inARow = 1;
                            }

                            //save that the previous value was by p1
                            prev = 1;
                        }
                        //if the slot is occupied by p2
                        else if(node.getAt(i,j) == 2) // node.getAt(i,j) == 2
                        {
                            //if the previous coin was by p2, we increment the tentative #in a row for p2
                            if(prev == 2)
                            {
                                tent_p2inARow = tent_p2inARow + 1;
                            }
                            //if the previous coin was empty or by p1
                            if(prev == 0 || prev == 1)
                            {
                                //if the tentative #in a row for p2 is greater than equal to 2, we store it
                                if(tent_p2inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p2inARow == 2)
                                    {
                                        p2_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p2inARow == 3)
                                    {
                                        p2_twoInRow--;
                                        p2_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p2inARow = 1;
                            }
                            //save that the previous value was by p2
                            prev = 2;
                        }
                    }
                }
            }

            prev = 0;
            //Check for in a row by columns
            for(int i = 0; i < node.getWidth(); i++)
            {
                int tent_p1inARow = 0;
                int tent_p2inARow = 0;
                for(int j = 0; j < node.getHeight() ; j++)
                {
                    //if node is empty set tentative in a row for p1 and p2 to 0 and set previous coin value to 0
                    if(node.getAt(j,i) == 0)
                    {
                        tent_p1inARow = 0;
                        tent_p2inARow = 0;
                        prev = 0;
                    }
                    else
                    {
                        //if the slot is occuped by p1
                        if(node.getAt(j,i) == 1)
                        {
                            //if the previous coin was by p1, we increment the tentative #in a row for p1
                            if(prev == 1)
                            {
                                tent_p1inARow = tent_p1inARow + 1;
                            }
                            //if the previous coin was empty or by p2
                            if(prev == 0 || prev == 2)
                            {
                                //if the tentative #in a row for p1 is greater than equal to 2, we store it
                                if(tent_p1inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p1inARow == 2)
                                    {
                                        p1_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p1inARow == 3)
                                    {
                                        p1_twoInRow--;
                                        p1_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p1inARow = 1;
                            }

                            //save that the previous value was by p1
                            prev = 1;
                        }
                        //if the slot is occupied by p2
                        else if(node.getAt(j,i) == 2) // node.getAt(i,j) == 2
                        {
                            //if the previous coin was by p2, we increment the tentative #in a row for p2
                            if(prev == 2)
                            {
                                tent_p2inARow = tent_p2inARow + 1;
                            }
                            //if the previous coin was empty or by p1
                            else if(prev == 0 || prev == 1)
                            {
                                //if the tentative #in a row for p2 is greater than equal to 2, we store it
                                if(tent_p2inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p2inARow == 2)
                                    {
                                        p2_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p2inARow == 3)
                                    {
                                        p2_twoInRow--;
                                        p2_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p2inARow = 1;
                            }
                            //save that the previous value was by p2
                            prev = 2;
                        }
                    }
                }
            }
            int start=0;
            if (node.getAt(3,0)==player){ //TODO: 1 is hardcoded
                start = 1000;
            }
            else if(node.getAt(4,0) == player)
            {
                start = 500;
            }
            else if(node.getAt(2,0) == player)
            {
                start = 500;
            }


            //check for left diagonal first half
            prev = 0;
            for(int i = 0; i < 7; i++) // column
            {
                int tent_p1inARow = 0;
                int tent_p2inARow = 0;
                for(int j = 1; j < 7; j++) // row
                {
                    if(i + j >= 7)
                        break;
                    //if(field[row][column+row])
                    if(node.getAt(j, i+j) == 0)
                    {
                        tent_p1inARow = 0;
                        tent_p2inARow = 0;
                        prev = 0;
                    }
                    else
                    {
                        if(node.getAt(j, i+j) == 1)
                        {
                            //if the previous coin was by p1, we increment the tentative #in a row for p1
                            if(prev == 1)
                            {
                                tent_p1inARow = tent_p1inARow + 1;
                            }
                            //if the previous coin was empty or by p2
                            if(prev == 0 || prev == 2)
                            {
                                //if the tentative #in a row for p1 is greater than equal to 2, we store it
                                if(tent_p1inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p1inARow == 2)
                                    {
                                        p1_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p1inARow == 3)
                                    {
                                        p1_twoInRow--;
                                        p1_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p1inARow = 1;
                            }

                            //save that the previous value was by p1
                            prev = 1;
                        }
                        else if(node.getAt(j, i+j) == 2)
                        {
                            //if the previous coin was by p2, we increment the tentative #in a row for p2
                            if(prev == 2)
                            {
                                tent_p2inARow = tent_p2inARow + 1;
                            }
                            //if the previous coin was empty or by p1
                            else if(prev == 0 || prev == 1)
                            {
                                //if the tentative #in a row for p2 is greater than equal to 2, we store it
                                if(tent_p2inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p2inARow == 2)
                                    {
                                        p2_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p2inARow == 3)
                                    {
                                        p2_twoInRow--;
                                        p2_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p2inARow = 1;
                            }
                            //save that the previous value was by p2
                            prev = 2;
                        }
                    }
                }
            }


            //check for left diagonal first half
            prev = 0;
            for(int i = 0; i < 7; i++) // column
            {
                int tent_p1inARow = 0;
                int tent_p2inARow = 0;
                for(int j = 1; j < 7; j++) // row
                {
                    if(i + j >= 7)
                        break;
                    //if(field[row][column+row])
                    if(node.getAt(i+j, j) == 0)
                    {
                        tent_p1inARow = 0;
                        tent_p2inARow = 0;
                        prev = 0;
                    }
                    else
                    {
                        if(node.getAt(i+j, j) == 1)
                        {
                            //if the previous coin was by p1, we increment the tentative #in a row for p1
                            if(prev == 1)
                            {
                                tent_p1inARow = tent_p1inARow + 1;
                            }
                            //if the previous coin was empty or by p2
                            if(prev == 0 || prev == 2)
                            {
                                //if the tentative #in a row for p1 is greater than equal to 2, we store it
                                if(tent_p1inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p1inARow == 2)
                                    {
                                        p1_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p1inARow == 3)
                                    {
                                        p1_twoInRow--;
                                        p1_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p1inARow = 1;
                            }

                            //save that the previous value was by p1
                            prev = 1;
                        }
                        else if(node.getAt(i+j, j) == 2)
                        {
                            //if the previous coin was by p2, we increment the tentative #in a row for p2
                            if(prev == 2)
                            {
                                tent_p2inARow = tent_p2inARow + 1;
                            }
                            //if the previous coin was empty or by p1
                            else if(prev == 0 || prev == 1)
                            {
                                //if the tentative #in a row for p2 is greater than equal to 2, we store it
                                if(tent_p2inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p2inARow == 2)
                                    {
                                        p2_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p2inARow == 3)
                                    {
                                        p2_twoInRow--;
                                        p2_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p2inARow = 1;
                            }
                            //save that the previous value was by p2
                            prev = 2;
                        }
                    }
                }
            }

            //check for left diagonal first half
            prev = 0;
            for(int i = 0; i < 7; i++) // column
            {
                int tent_p1inARow = 0;
                int tent_p2inARow = 0;
                for(int j = 1; j < 7; j++) // row
                {
                    if(i - j < 7)
                        break;
                    //if(field[row][column+row])
                    if(node.getAt(j, i-j) == 0)
                    {
                        tent_p1inARow = 0;
                        tent_p2inARow = 0;
                        prev = 0;
                    }
                    else
                    {
                        if(node.getAt(j, i-j) == 1)
                        {
                            //if the previous coin was by p1, we increment the tentative #in a row for p1
                            if(prev == 1)
                            {
                                tent_p1inARow = tent_p1inARow + 1;
                            }
                            //if the previous coin was empty or by p2
                            if(prev == 0 || prev == 2)
                            {
                                //if the tentative #in a row for p1 is greater than equal to 2, we store it
                                if(tent_p1inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p1inARow == 2)
                                    {
                                        p1_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p1inARow == 3)
                                    {
                                        p1_twoInRow--;
                                        p1_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p1inARow = 1;
                            }

                            //save that the previous value was by p1
                            prev = 1;
                        }
                        else if(node.getAt(j, i-j) == 2)
                        {
                            //if the previous coin was by p2, we increment the tentative #in a row for p2
                            if(prev == 2)
                            {
                                tent_p2inARow = tent_p2inARow + 1;
                            }
                            //if the previous coin was empty or by p1
                            else if(prev == 0 || prev == 1)
                            {
                                //if the tentative #in a row for p2 is greater than equal to 2, we store it
                                if(tent_p2inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p2inARow == 2)
                                    {
                                        p2_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p2inARow == 3)
                                    {
                                        p2_twoInRow--;
                                        p2_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p2inARow = 1;
                            }
                            //save that the previous value was by p2
                            prev = 2;
                        }
                    }
                }
            }

                        //check for left diagonal first half
            prev = 0;
            for(int i = 0; i < 7; i++) // column
            {
                int tent_p1inARow = 0;
                int tent_p2inARow = 0;
                for(int j = 5; j >=0; j--) // row
                {
                    if(j - i >= 7)
                        break;
                    //if(field[row][column+row])
                    if(node.getAt(j-i, j) == 0)
                    {
                        tent_p1inARow = 0;
                        tent_p2inARow = 0;
                        prev = 0;
                    }
                    else
                    {
                        if(node.getAt(j-i, j) == 1)
                        {
                            //if the previous coin was by p1, we increment the tentative #in a row for p1
                            if(prev == 1)
                            {
                                tent_p1inARow = tent_p1inARow + 1;
                            }
                            //if the previous coin was empty or by p2
                            if(prev == 0 || prev == 2)
                            {
                                //if the tentative #in a row for p1 is greater than equal to 2, we store it
                                if(tent_p1inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p1inARow == 2)
                                    {
                                        p1_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p1inARow == 3)
                                    {
                                        p1_twoInRow--;
                                        p1_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p1inARow = 1;
                            }

                            //save that the previous value was by p1
                            prev = 1;
                        }
                        else if(node.getAt(j-i, j) == 2)
                        {
                            //if the previous coin was by p2, we increment the tentative #in a row for p2
                            if(prev == 2)
                            {
                                tent_p2inARow = tent_p2inARow + 1;
                            }
                            //if the previous coin was empty or by p1
                            else if(prev == 0 || prev == 1)
                            {
                                //if the tentative #in a row for p2 is greater than equal to 2, we store it
                                if(tent_p2inARow >= 2)
                                {
                                    //store for 2 in a row
                                    if(tent_p2inARow == 2)
                                    {
                                        p2_twoInRow++;
                                    }
                                    //store for 3 in a row
                                    else if(tent_p2inARow == 3)
                                    {
                                        p2_twoInRow--;
                                        p2_threeInRow++;
                                    }
                                }
                                //reset tentative #in a row for p1 to 1
                                tent_p2inARow = 1;
                            }
                            //save that the previous value was by p2
                            prev = 2;
                        }
                    }
                }
            }
            if(player == 1)
                return ((10*p1_threeInRow + p1_twoInRow) - (10*p2_threeInRow + p2_twoInRow)+start);
            else
                return ((10*p2_threeInRow + p2_twoInRow) - (10*p1_threeInRow + p1_twoInRow)+start);

        }
    } //*/
}
    
