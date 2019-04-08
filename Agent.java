package practica_busqueda;

// General java imports
import java.util.ArrayList;
import java.util.; 
import java.util.Random;
import java.util.stream.Stream;
import java.awt.List;
import static java.util.Arrays.*;
// General game imports
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.*;
import javax.swing.plaf.nimbus.State;
import java.lang.*;
/**
 * Agent class
 * @author Luis Antonio Ortega Andrés
 */


public class Agent extends BaseAgent {
    // Basic A* agent
    private PathFinder pf;
    private ArrayList<Node> path = new ArrayList<>();
    private PlayerObservation lastPosition;
    private int local_gem_counter;
    private Observation next_gem;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        super(so, elapsedTimer);

        ArrayList<Integer> tiposObs = new ArrayList();
        tiposObs.add(0);  // <- Muros
        tiposObs.add(7);  // <- Piedras

        // Init pathfinder
        pf = new PathFinder(tiposObs);
        pf.run(so);

        // Get last known position
        lastPosition = getPlayer(so);
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        System.out.println("[ACT]: Posicion actual: " + getPlayer(stateObs).getX() + " " + getPlayer(stateObs).getY());

        Types.ACTIONS action;

        if (getNumGems(stateObs) != local_gem_counter){
            local_gem_counter+=1;
            System.out.println("[ACT]: Gema conseguida.");
            path.clear();
        }

        // Get current position and clear path if needed
        PlayerObservation avatar = getPlayer(stateObs);
        if (((avatar.getX() != lastPosition.getX()) || (avatar.getY() != lastPosition.getY()))
                && !path.isEmpty()) {
            System.out.println("[ACT]: Entra en función 1.");
            path.remove(0);
        }

        // Get current gem count
        int gems = getNumGems(stateObs);

        // Update path
        if (path.isEmpty()) {
            System.out.println("[ACT]: El camino esta vacio");
            // Look for the exit (all gems collected)
            if (gems == NUM_GEMS_FOR_EXIT) {

                Observation exit = this.getExit(stateObs);

                // Calculate shortest path to nearest exit
                setAstarPath(avatar, exit);
            }
            // Look for another gem
            else {
                System.out.println("[ACT]: Buscamos la siguiente gema.");
                // Select nearest gem
                ArrayList<core.game.Observation>[] gemList
                        = stateObs.getResourcesPositions(stateObs.getAvatarPosition());

                next_gem = new Observation(gemList[0].get(0), stateObs.getBlockSize());
                System.out.println("[ACT]: Posicion de la siguiente gema: " + next_gem.getX() + ", " + next_gem.getY());
                // Calculate shortest path to nearest exit
                if (!setAstarPath(avatar, next_gem)) {

                    System.out.println("[ACT]: No existe camino a la siguiente gema.");

                    //TODO: work here
                    //Can't stop to think on an unsecure position
                    //if(action_implies_death(stateObs, Types.ACTIONS.ACTION_NIL)){
                    //    System.out.println("[ACT]: La posicion actual no es segura.");

                    //}


                    pf.state = stateObs;
                    pf.grid = stateObs.getObservationGrid();
                    //pf.astar = new AStar(pf);
                    if (pf.astar == null){
                        System.out.println("Astar nulo");
                    }
                    Node pos = new Node(new Vector2d(getPlayer(stateObs).getX(),  getPlayer(stateObs).getY()));
                    System.out.println(pos.position);
                    //Node g = new Node(new Vector2d(gem.getX(), gem.getY()));
                    //System.out.println(g.position);
                    //path = pf.astar._findPath(pos, g);
                    //if (path == null || path.isEmpty()){
                    //    System.out.println("No ha funcionado");
                    //}
                    pf.runAll((int) pos.position.x, (int) pos.position.y);

                }

            }
        }

        Node nowPos = new Node(new Vector2d(getPlayer(stateObs).getX(), getPlayer(stateObs).getY()));
        // Calculate next action
        Node nextPos;
        if (path != null && !path.isEmpty()) {
            nextPos = path.get(0);
        }
        else{
            nextPos = nowPos;
        }
        action = computeNextAction(avatar, nextPos);


        if (nextPos.position.x == next_gem.getX() && nextPos.position.y == next_gem.getY()){
            System.out.println("[ACT]: La siguiente posicion es una gema");
            path.clear();
            return action;
        }


        //if( boulderComing( nowPos,stateObs ))
        //{
        //    System.out.println("[ACT]: Está una piedra para caernos");
         //   return escape_from_current_position(stateObs);
       // }

        if (action_implies_death(stateObs, action)){
            System.out.println("[ACT]: La siguiente accion implica la muerte");
            path.clear();
            action = escape_from_current_position(stateObs);
        }

        if (!isSafe(nextPos, stateObs)){
            System.out.println("[ACT]: La siguiente posición no es segura");
            path.clear();
            action = escape_from_current_position(stateObs);

        }

        lastPosition = avatar;

        System.out.println("[ACT]: Acción a devolver: " + action);
        return action;

    }

    //Making it a function only for debugging issues

    private boolean areBoulberAbove(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;

        return ObservationType.BOULDER ==   getObservationGrid(stateObs)[x][y-1].get(0).getType();
    }

        
    private boolean isBoulberAbove(int x, int y, StateObservation stateObs){
        return ObservationType.BOULDER ==   getObservationGrid(stateObs)[x][y-1].get(0).getType();
    }


    private ArrayList<Node> Gems(Node node, StateObservation stateObs){
        ArrayList<core.game.Observation> gemList
            = stateObs.getResourcesPositions(stateObs.getAvatarPosition())[0];
        

        ArrayList<Observation> gem = new ArrayList<Observation>();
        java.util.List<java.util.Map.Entry<Integer,Integer>> heuristicList = new java.util.ArrayList<>();
        ArrayList<Node> OrderedGem = new ArrayList<Node>();
    
        for( int i = 0; i < gemList.size(); ++i)
        {
            gem.add( new Observation(gemList.get(i), stateObs.getBlockSize()));
        } 
        
        for( int i = 0; i < gemList.size(); ++i)
        {
            int h = 0;
            //if( isBoulderAbove(  gem.get(i).getX(),gem.get(i).getY(),stateObs) )
            // intente ponerlo en una función para que no fuera todo horrible y no lo conseguí
            int x = gem.get(i).getX();
            int y = gem.get(i).getY();
            boolean boulderAbove = ObservationType.BOULDER ==   getObservationGrid(stateObs)[x][y-1].get(0).getType();
            if (boulderAbove)
                h += 30;
            
            
            //Calculate Manhattan Metric to each gem
            Node nowPos = new Node(new Vector2d(getPlayer(stateObs).getX(), getPlayer(stateObs).getY()));
            h += Math.abs( nowPos.position.x - x);
            h += Math.abs( nowPos.position.y - y);
            
            
            java.util.Map.Entry<Integer,Integer> pair1=new java.util.AbstractMap.SimpleEntry<>(i,h);
            heuristicList.add(pair1);
        } 

        //ordena en funcion de las heursiticas
        heuristicList.sort( (p1,p2) -> p1.getValue().compareTo(p2.getValue()));
        
        for( java.util.Map.Entry<Integer,Integer> p : heuristicList )
        {
            System.out.println("practica_busqueda.Agent.Gems()"+p.toString()+"\n");
            int i = p.getKey();
            int x = gem.get(i).getX();
            int y = gem.get(i).getY();
            
            OrderedGem.add( new Node( new Vector2d(x,y)));
        }
        
        return OrderedGem;
    }

    
    private boolean areBoulberNearby(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;
        boolean result = true;
        
        int[] num = {-1,0,1};
        for( int i : num )
        {
        ObservationType type1 = getObservationGrid(stateObs)[x+i][y].get(0).getType();
        ObservationType type2 = getObservationGrid(stateObs)[x][y+i].get(0).getType();
        
        if( type1 == ObservationType.BOULDER)
                result = false;
        if(type2 == ObservationType.BOULDER ) 
                result = false;
            
        }
        

        return result;
    }
    
    private boolean boulderComing(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;

        //
        if( y == 0 )
        {
            return true;
        }
        else
        {
        y = y-1;
        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        System.out.println("[isComing]: x: " + x + ", y: " + y + ", tipo: " + type );
        return type == ObservationType.BOULDER;
        }
    }

    // Comprueba si la posicion es segura.
    private boolean isSafe(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;


        //in type is the pos asked, in uptype is the pos above de current one.
        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        ObservationType uptype = getObservationGrid(stateObs)[x][y-1].get(0).getType();
        System.out.println("[isSafe]: x: " + x + ", y: " + y + ", tipo: " + type );
        System.out.println("[isSafe]: x: " + x + ", y: " + (y-1) + ", tipo: " + uptype );
        return type != ObservationType.WALL && type != ObservationType.BOULDER
                && uptype != ObservationType.BOULDER;
    }

    // Calcula una accion de escape.
    private Types.ACTIONS escape_from_current_position(StateObservation stateObs){
        int x = getPlayer(stateObs).getX();
        int y = getPlayer(stateObs).getY();
        Node actual = new Node(new Vector2d(x,y));
        System.out.println("[Escape]: Buscando ruta de escape de posible muerte");
        ArrayList<Node> neighbours = new ArrayList<>();

        neighbours.add(new Node(actual.position.copy().add(1,0)));
        neighbours.add(new Node(actual.position.copy().add(-1,0)));
        neighbours.add(new Node(actual.position.copy().add(0,1)));
        neighbours.add(new Node(actual.position.copy().add(0,-1)));

        for (Node neighbour: neighbours) {
            if (isSafe(neighbour, stateObs)) {
                return computeNextAction(getPlayer(stateObs), neighbour);
            }
        }
        System.out.println("[Escape]: El jugador muere de todas formas");
        return Types.ACTIONS.ACTION_NIL;
    }

    // Comprueba si la accion correspondiente implica la muerte segun el juego.
    private boolean action_implies_death(StateObservation stateObs, Types.ACTIONS action){
        StateObservation next_state = stateObs.copy();
        next_state.advance(action);

        return !next_state.isAvatarAlive();
    }

    /**
     * *********************************************
     * Test act methods
     * *********************************************
     */

    private boolean setAstarPath(PlayerObservation initial, Observation goal){
        System.out.println("[setAstarPath]: Calculando camino.");
        path = pf.getPath(new Vector2d(initial.getX(), initial.getY()),
                                  new Vector2d(goal.getX(), goal.getY()));
        if (path == null) {
            path = new ArrayList<Node>();
            return false;
        }
        System.out.println("[setAstarPath]: Camino calculado de tamaño: " + path.size());
            return true;
    }

    private Types.ACTIONS computeNextAction(PlayerObservation avatar, Node nextPos) {

        if (nextPos.position.x != avatar.getX()) {
            if (nextPos.position.x > avatar.getX())
                return Types.ACTIONS.ACTION_RIGHT;

            return Types.ACTIONS.ACTION_LEFT;
        }

        if (nextPos.position.y > avatar.getY())
            return Types.ACTIONS.ACTION_DOWN;

        return Types.ACTIONS.ACTION_UP;

    }

}
