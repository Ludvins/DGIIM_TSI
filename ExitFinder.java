package practica_busqueda;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.List;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExitFinder extends PathFinder {
    public ExitFinder(ArrayList<Integer> obstacleItypes)
    {
        super( obstacleItypes );
    }
    
    
    private boolean monsterNearby(int x, int y, int r){
        boolean result = false;

        for( int i = 0; i <= r; ++i)
        {
            for( int j = 0; j <=i; ++j)
            {
                int type1 = grid[x+i-j][y+j].get(0).itype;
                
                int type2 = grid[x-i+j][y-j].get(0).itype;

                if( type1 ==  10 || type1 == 11  ||
                    type2 ==  10 || type2 == 11 )
                        result = true;
            }
        }

        return result;
    }
      

    
    protected boolean isObstacle(int row, int col)
    {
        if(row<0 || row>=grid.length) return true;
        if(col<0 || col>=grid[row].length) return true;

        for(Observation obs : grid[row][col])
        {
            if(obstacleItypes.contains(obs.itype))
                return true;
            
            if( monsterNearby( row, col, 2) )
                return true;
        }

        return false;
    }

}