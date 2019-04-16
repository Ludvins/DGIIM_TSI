package practica_busqueda;

// General java imports
import java.util.ArrayList;
import java.util.*;
// General game imports
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
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
    private ArrayList<Observation> gems;

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
        
        gems = this.Gems( so );
        for (Observation gem : gems) {
            System.out.println("Posicion" + gem.getX() + " " + gem.getY());
        }
        next_gem = gems.get(0);
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {


        Node nowPos = new Node(new Vector2d(getPlayer(stateObs).getX(), getPlayer(stateObs).getY()));
        System.out.println("[ACT]: Posicion actual: " + getPlayer(stateObs).getX() + " " + getPlayer(stateObs).getY());

        Types.ACTIONS action;

        //if (getNumGems(stateObs) != local_gem_counter){
        //    local_gem_counter+=1;
        //    System.out.println("[ACT]: Gema conseguida.");
            //path.clear();
            //next_gem = this.gems.get(0);
            //this.gems.remove(0);
            //this.gems.add(this.gems.size(), next_gem);
        //}

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
                if (!setAstarPath(avatar, exit)){
                    pf.state = stateObs;
                    pf.grid = stateObs.getObservationGrid();
                    path = pf.astar._findPath(nowPos, new Node(new Vector2d(exit.getX(), exit.getY())));
                    if (path.isEmpty()){
                        System.out.println("NO EXISTE CAMINO A LA SALIDA");
                    }
                }
            }
            // Look for another gem
            else {
                System.out.println("[ACT]: Buscamos la siguiente gema.");


                /**
                En caso de querer coger la gema mas cercana utilizar este vector.
                 **/
                //Select nearest gem
                //ArrayList<core.game.Observation>[] gemList
                //        = stateObs.getResourcesPositions(stateObs.getAvatarPosition());

                //next_gem = new Observation(gemList[0].get(0), stateObs.getBlockSize());
                /**
                 * Coger la mejor gema segun la heuristica.
                 */
                next_gem = this.gems.get(0);
                this.gems.remove(0);
                this.gems.add(this.gems.size(), next_gem);

                System.out.println("[ACT]: Posicion de la siguiente gema: " + next_gem.getX() + ", " + next_gem.getY());
                // Calculate shortest path to nearest exit
                if (!setAstarPath(avatar, next_gem)) {

                    System.out.println("[ACT]: No existe camino a la siguiente gema.");

                    pf.state = stateObs;
                    pf.grid = stateObs.getObservationGrid();

                    if (pf.astar == null){
                        System.out.println("Astar nulo");
                    }
                    System.out.println(nowPos.position);

                    pf.runAll((int) nowPos.position.x, (int) nowPos.position.y);

                }

            }
        }

        // Calculate next action
        Node nextPos;
        if (path != null && !path.isEmpty()) {
            nextPos = path.get(0);
        }
        else{
            nextPos = nowPos;
        }
        action = computeNextAction(avatar, nextPos);

        System.out.println("La accion a devolver va a ser " + action);

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

        if(monsterNearby(nextPos, stateObs)){
            System.out.println("HAY UN MONSTRUO CERCAAAAAA");
            path.clear();

            action = escape_from_current_position(stateObs);
        }

        lastPosition = avatar;

        System.out.println("[ACT]: Acción a devolver: " + action);

        return action;
    }

    private boolean monsterNearby(Node nextPos, StateObservation so){
        ObservationType type = getObservationGrid(so)[ (int) nextPos.position.x][ (int) nextPos.position.y].get(0).getType();
        if (type == ObservationType.BAT || type == ObservationType.SCORPION){
            return true;
        }

        for (Node n : pf.getNeighbours(nextPos)){
            ObservationType t = getObservationGrid(so)[ (int) n.position.x][ (int) n.position.y].get(0).getType();
            if (t == ObservationType.BAT || t == ObservationType.SCORPION){
                return true;
            }
        }

        return false;
    }

    /*******************************************************
     * Gem Listing Methods
     *******************************************************/

    private ArrayList<Observation> Gems( StateObservation stateObs){
        ArrayList<core.game.Observation> gemList
            = stateObs.getResourcesPositions(stateObs.getAvatarPosition())[0];
        
        //Definicion de variables
        ArrayList<Observation> gem = new ArrayList<>();
        java.util.List<java.util.Map.Entry<Integer,Integer>> heuristicList = new java.util.ArrayList<>();
        ArrayList<Observation> OrderedGem = new ArrayList<>();
    
        // Creacion de vector de variables
        for( int i = 0; i < gemList.size(); ++i)
        {
            gem.add( new Observation(gemList.get(i), stateObs.getBlockSize()));
        } 
        
        int lastPosx = getPlayer(stateObs).getX();
        int lastPosy = getPlayer(stateObs).getY();
        for (int j = 0; j < gemList.size(); ++j)
        {
            for( int i = 0; i < gem.size(); ++i)
            {
                int h = 0;
                if( isBoulderAbove(  gem.get(i).getX(),gem.get(i).getY(),stateObs) )
                {
                    h+=30;
                }

                int x = gem.get(i).getX();
                int y = gem.get(i).getY();
                ArrayList<Node> pa = pf.getPath(new Vector2d( lastPosx, lastPosy ),
                                      new Vector2d(x, y));
                if( null ==  pa )
                {
                    h += 10000;
                    System.out.println("Gema en posición:("+x + ","+y+"), NO es accesible");
                }
                else
                {
                    h += pa.size();
                    System.out.println("Gema en posición:("+x + ","+y+"), SI es accesible");
                }

                java.util.Map.Entry<Integer,Integer> pair1=new java.util.AbstractMap.SimpleEntry<>(i,h);
                heuristicList.add(pair1);
                
                System.out.println("    \n\n\n");
            }
        //Despues de aplicar la heuristica ordena el vector    
        heuristicList.sort(Comparator.comparing(Map.Entry::getValue));
        
        OrderedGem.add(gem.get(heuristicList.get(0).getKey()));
        lastPosx = gem.get(heuristicList.get(0).getKey()).getX();
        lastPosy = gem.get(heuristicList.get(0).getKey()).getY();
        gem.remove(gem.get(heuristicList.get(0).getKey()));
        
        heuristicList = new java.util.ArrayList<>();
        }

        //ordena en funcion de las heursiticas
       /*heuristicList.forEach((p) -> {
            OrderedGem.add( gem.get(p.getKey()) );
        });*/



        return OrderedGem;
    }

    /*************************************************
     * Boulder Nearby Methods
     *************************************************/

    private boolean isBoulderAbove(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;

        return ObservationType.BOULDER ==   getObservationGrid(stateObs)[x][y-1].get(0).getType();
    }


    private boolean isBoulderAbove(int x, int y, StateObservation stateObs){
        return ObservationType.BOULDER == getObservationGrid(stateObs)[x][y-1].get(0).getType();
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

    /*************************************************
     * Safety Methods
     *************************************************/

    // Comprueba si la posicion es segura.
    private boolean isSafe(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;


        //in type is the pos asked, in uptype is the pos above de current one.
        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        ObservationType uptype = getObservationGrid(stateObs)[x][y-1].get(0).getType();
        System.out.println("[isSafe]: x: " + x + ", y: " + y + ", tipo: " + type );
        System.out.println("[isSafe]: x: " + x + ", y: " + (y-1) + ", tipo: " + uptype );
        return type != ObservationType.WALL && type != ObservationType.BOULDER && type != ObservationType.SCORPION && type != ObservationType.BAT
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
            if (isSafe(neighbour, stateObs) && !monsterNearby(neighbour, stateObs) ) {
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
     * Path Methods
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

        if (nextPos.position.y < avatar.getY())
            return Types.ACTIONS.ACTION_UP;

        return Types.ACTIONS.ACTION_NIL;

    }

}
