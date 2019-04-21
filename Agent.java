package practica_busqueda;

// General java imports
import java.util.ArrayList;
import java.util.*;
// General game imports
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Pair;
import tools.Vector2d;

import javax.swing.plaf.nimbus.State;
import java.lang.*;

/**
 * Agent class
 *
 * @author Luis Antonio Ortega Andrés
 * @author Pedro Bonilla Nadal
 */
public class Agent extends BaseAgent {
    // Basic A* agent
    private PathFinder pf;
    private ArrayList<Node> path = new ArrayList<>();
    private PlayerObservation lastPosition;
    private int local_gem_counter;
    private Observation next_gem;
    private ArrayList<Observation> gems;
    private States actual;
    private States last_state;
    private Observation exit;
    private int turnsStoped;

    boolean verbose = true;
  /**
   * Instantiates a new Agent.
   *
   * @param so the so
   * @param elapsedTimer the elapsed timer
   */
  public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        super(so, elapsedTimer);

        ArrayList<Integer> tiposObs = new ArrayList();
        tiposObs.add(0);  // <- Muros
        tiposObs.add(7);  // <- Piedras
        tiposObs.add(10); // <- Escorpión
        tiposObs.add(11); // <- Murcielago

        // Init pathfinder
        pf = new PathFinder(tiposObs);
        pf.run(so);

        // Get last known position
        lastPosition = getPlayer(so);
        exit = getExit(so);
        turnsStoped = 0;
        gems = this.Gems( so );
        if(verbose)
        {
        for (Observation gem : gems) {
            System.out.println("Posicion" + gem.getX() + " " + gem.getY());
        }
        }
        actual = States.NEED_NEW_OBJETIVE;
    }

    /*******************************************************
     * ACT Method
     *******************************************************/

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {


        Node nowPos = new Node(new Vector2d(getPlayer(stateObs).getX(), getPlayer(stateObs).getY()));

        Node nextPos = nowPos;

        if(verbose)
            System.out.println("[ACT]: Posicion actual: " + getPlayer(stateObs).getX() + " " + getPlayer(stateObs).getY());
        PlayerObservation avatar = getPlayer(stateObs);

        if (((avatar.getX() != lastPosition.getX()) || (avatar.getY() != lastPosition.getY()))
                && path != null && !path.isEmpty()) {
            path.remove(0);
            turnsStoped = 0;
        }
        else {
            turnsStoped += 1;
        }

        if (local_gem_counter != getNumGems(stateObs)){
            last_state = actual;
            actual = States.JUST_GOT_GEM;
        }

        Types.ACTIONS ret_action;

        while (true) {

            try{
                Thread.sleep(0);
            }
            catch(Exception ignored){}

            if(verbose)
            {
                System.out.println("[ACT]: Estado actual " + actual);
                System.out.println("[ACT]: El vector de gemas tiene tamaño " + this.gems.size());
                System.out.println("[ACT]: Numero de gemas obtenidas: " + local_gem_counter + " " + getNumGems(stateObs));
            }
            switch (actual) {

                /**
                 *********************************************************************************************************
                 */

                case NEAR_WANTED_GEM:

                    if (nowPos.position.x == next_gem.getX() && nowPos.position.y - 1 == next_gem.getY()) {
                        if(verbose)
                        System.out.println("[ACT - NEAR_WANTED_GEM]: Debajo de la gema deseada");
                        return Types.ACTIONS.ACTION_UP;
                    }
                    if (nowPos.position.y == next_gem.getY() && nowPos.position.x + 1 == next_gem.getX()){
                        if(verbose)
                        System.out.println("[ACT - NEAR_WANTED_GEM]: A la izquierda de la gema deseada");
                       return Types.ACTIONS.ACTION_RIGHT;
                    }
                    if (nowPos.position.y == next_gem.getY() && nowPos.position.x - 1 == next_gem.getX()){
                        if(verbose)
                        System.out.println("[ACT - NEAR_WANTED_GEM]: A la derecha de la gema deseada");
                       return Types.ACTIONS.ACTION_LEFT;
                    }
                    if (nowPos.position.x == next_gem.getX() && nowPos.position.y+ 1 == next_gem.getY()){
                        if(verbose)
                        System.out.println("[ACT - NEAR_WANTED_GEM]: Encima de la gema deseada");
                       return Types.ACTIONS.ACTION_DOWN;
                    }
                    last_state = actual;
                    actual = States.NEED_NEW_OBJETIVE;
                    break;

                /**
                 *********************************************************************************************************
                 */

                case LOOKING_FOR_GEM:

                    if(verbose)
                    System.out.println("[ACT - LOOKING_FOR_GEM]: Objetivo: " + next_gem.getX() + " " + next_gem.getY());
                    if (path != null && !path.isEmpty()) {
                        nextPos = path.get(0);
                    } else {
                        this.gems.add(next_gem);
                        last_state = actual;
                        actual = States.NEED_NEW_OBJETIVE;
                        break;
                    }

                    ret_action = computeNextAction(avatar, nextPos);
                    if(verbose)
                    System.out.println("[ACT - LOOKING_FOR_GEM]: La accion computada es " + ret_action);

                    if (monsterNearby(nextPos, stateObs)) {
                        if(verbose)
                        System.out.println("[ACT - LOOKING_FOR_GEM]: HAY UN MONSTRUO CERCAAAAAA");
                        
                        path.clear();
                        last_state = actual;
                        actual = States.ESCAPING;
                        this.gems.add(next_gem);
                        break;
                    }

                    if (action_implies_death(stateObs, ret_action)) {
                        if(verbose)
                        System.out.println("[ACT - LOOKING_FOR_GEM]: La siguiente accion implica la muerte");
                        
                        path.clear();
                        this.gems.add(next_gem);
                        last_state = actual;
                        actual = States.ESCAPING;
                        break;
                    }
                    if (!isSafe(nextPos, stateObs)) {
                        if(verbose)
                        System.out.println("[ACT - LOOKING_FOR_GEM]: La siguiente posición no es segura");
                        
                        path.clear();
                        this.gems.add(next_gem);
                        last_state = actual;
                        actual = States.ESCAPING;
                        break;
                    }
                    if (path.size() == 1){
                        if(verbose)
                        System.out.println("[ACT - LOOKING_FOR_GEM]: Al lado de la gema deseada");
                        
                        last_state = actual;
                        actual = States.NEAR_WANTED_GEM;
                        break;
                    }

                    if(verbose)
                    System.out.println("[ACT - LOOKING_FOR_GEM]: Acción a devolver: " + ret_action);


                    lastPosition = avatar;
                    return ret_action;

                /**
                 *********************************************************************************************************
                 */

                case JUST_GOT_GEM:
                    local_gem_counter += 1;
                    
                    if(verbose)
                    System.out.println("[ACT - JUST_GOT_GEM]: Gema conseguida.");

                    if (last_state == States.NEAR_WANTED_GEM || local_gem_counter == NUM_GEMS_FOR_EXIT) { // Esto arregla la siguiente situacion: Coger una gema de camino a otra.
                        last_state = actual;
                        actual = States.NEED_NEW_OBJETIVE;
                    }
                    else {
                        last_state = actual;
                        actual = States.LOOKING_FOR_GEM;
                    }
                    break;

                /**
                 *********************************************************************************************************
                 */

                case SETTING_PATH_FOR_GEM:
                    if(verbose)
                    System.out.println("La gema objetivo es :" + next_gem.getX() + " " + next_gem.getY());

                    setPath(stateObs, nowPos, next_gem);
                    // TODO Que pasa si no existe path a ninguna gema.
                    if (path == null || path.isEmpty()){
                        
                        if(verbose)
                        System.out.println("[ACT - SETTING_PATH]: No existe camino a la siguiente gema.");
                        
                        last_state = actual;
                        actual = States.NEED_NEW_OBJETIVE;
                        this.gems.add(next_gem);
                    } else {
                        last_state = actual;
                        actual = States.LOOKING_FOR_GEM;
                    }
                    break;

                /**
                 *********************************************************************************************************
                 */

                case NEED_NEW_OBJETIVE:

                    if (local_gem_counter >= NUM_GEMS_FOR_EXIT || getNumGems(stateObs) >= NUM_GEMS_FOR_EXIT) {
                        last_state = actual;
                        actual = States.GOT_ALL_GEMS;
                    } else {
                        next_gem = this.gems.get(0);
                        this.gems.remove(0);
                        last_state = actual;
                        actual = States.SETTING_PATH_FOR_GEM;
                    }
                    break;

                /**
                 *********************************************************************************************************
                 */

                case ESCAPING:

                    Pair<Types.ACTIONS, Node> ret = escape_from_current_position(stateObs);

                    Types.ACTIONS ret_act = ret.first;
                    nextPos = ret.second;
                    if (monsterNearby(nextPos, stateObs) || action_implies_death(stateObs, ret_act) || !isSafe(nextPos, stateObs)) {
                        
                    if(verbose)
                        System.out.println("[ACT - Escape]: Nos quedamos en modo escapar");
                        return ret.first;
                    }

                    last_state = actual;
                    actual = States.NEED_NEW_OBJETIVE;
                    
                    if(verbose)
                    System.out.println("[ACT - SCAPING]: La accion de escape es " + ret.first);
                    return ret.first;

                /**
                 *********************************************************************************************************
                 */
                case GOT_ALL_GEMS:

                        setPath(stateObs, nowPos, exit);
                        if (path == null || path.isEmpty()) {
                            
                            if(verbose)
                            System.out.println("NO se puede hacer camino seguro");
                            pf.obstacles.clear();
                            return Types.ACTIONS.ACTION_NIL;
                        } else {
                            last_state = actual;
                            actual = States.GOING_TO_EXIT;
                        }
                    break;
                /**
                 *********************************************************************************************************
                 */

                case GOING_TO_EXIT:
                    
                    if(verbose)
                    System.out.println("[ACT - GOING_EXIT]: Objetivo: " + exit.getX() + " " + exit.getY());
                    if (path != null && !path.isEmpty()) {
                        nextPos = path.get(0);
                    }
                    else {
                        last_state = actual;
                        actual = States.GOT_ALL_GEMS;
                        break;
                    }

                    ret_action = computeNextAction(avatar, nextPos);

                    if(verbose)
                    System.out.println("[ACT - GOING_EXIT]: La accion computada es " + ret_action);

                    if (monsterNearby(nextPos, stateObs)) {
                        
                        if(verbose)
                        System.out.println("[ACT - GOING_EXIT]: HAY UN MONSTRUO CERCAAAAAA");
                        
                        path.clear();
                        last_state = actual;
                        actual = States.ESCAPING;
                        
                        if(verbose)
                        System.err.println("SADSADASDASDA");
                        pf.obstacles.add(nextPos);
                        break;
                    }
                    if (action_implies_death(stateObs, ret_action)) {
                        
                        if(verbose)
                        System.out.println("[ACT - GOING_EXIT]: La siguiente accion implica la muerte");
                        
                        path.clear();
                        last_state = actual;
                        actual = States.ESCAPING;
                        break;
                    }
                    if (!isSafe(nextPos, stateObs)) {
                        
                        if(verbose)
                        System.out.println("[ACT - GOING_EXIT]: La siguiente posición no es segura");
                        
                        path.clear();
                        last_state = actual;
                        actual = States.ESCAPING;
                        break;
                    }

                    if (turnsStoped == 4){
                        
                        if(verbose)
                        System.err.println("AYY LMAOOO");
                        
                        pf.obstacles.add(nextPos);
                        path.clear();
                        last_state = States.GOING_TO_EXIT;
                        actual = States.NEED_NEW_OBJETIVE;
                    }
                    
                    if(verbose)
                    System.out.println("[ACT - GOING_EXIT]: Acción a devolver: " + ret_action);
                    
                    lastPosition = avatar;
                    return ret_action;

            }

        }

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
                if( isBoulderAbove(  gem.get(i).getX(),gem.get(i).getY(),stateObs)){
                    h+=10;
                }

                int x = gem.get(i).getX();
                int y = gem.get(i).getY();
                

                if(monsterNearby(x, y, stateObs)){
                        h += 30;
                }
 
                ArrayList<Node> pa = pf.getPath(new Vector2d( lastPosx, lastPosy ),
                                      new Vector2d(x, y));
                if( null ==  pa ){
                    h += 10000;
                    
                    if(verbose)
                    System.out.println("Gema en posición:("+x + ","+y+"), NO es accesible");
                }
                else {
                    h += pa.size();
                    
                    if(verbose)
                    System.out.println("Gema en posición:("+x + ","+y+"), SI es accesible");
                }

                java.util.Map.Entry<Integer,Integer> pair1=new java.util.AbstractMap.SimpleEntry<>(i,h);
                heuristicList.add(pair1);
            }
            heuristicList.sort(Comparator.comparing(Map.Entry::getValue));

            OrderedGem.add(gem.get(heuristicList.get(0).getKey()));
            lastPosx = gem.get(heuristicList.get(0).getKey()).getX();
            lastPosy = gem.get(heuristicList.get(0).getKey()).getY();
            gem.remove(gem.get(heuristicList.get(0).getKey()));

            heuristicList = new java.util.ArrayList<>();
        }
        
        return OrderedGem;
    }

    /*************************************************
     * Boulder Nearby Methods
     *************************************************/

    private boolean isBoulderAbove(int x, int y, StateObservation stateObs){
        return ObservationType.BOULDER == getObservationGrid(stateObs)[x][y-1].get(0).getType();
    }

    /*************************************************
     * Monster Nearby Methods
     *************************************************/

    private boolean isMonster(ObservationType type){
        return type == ObservationType.BAT || type == ObservationType.SCORPION;
    }

    private boolean monsterNearby(Node Pos, StateObservation so){
        int x = (int) Pos.position.x;
        int y = (int) Pos.position.y;
        
        return monsterNearby(x, y, so);
    }
    
    private boolean monsterNearby(int x, int y, StateObservation so){
        ArrayList<Observation>[][] grid = getObservationGrid(so);

        ObservationType[] types = {
                grid[x-1][y-1].get(0).getType(),
                grid[ x ][y-1].get(0).getType(),
                grid[x+1][y-1].get(0).getType(),
                grid[x-1][ y ].get(0).getType(),
                grid[ x ][ y ].get(0).getType(),
                grid[x+1][ y ].get(0).getType(),
                grid[x-1][y+1].get(0).getType(),
                grid[ x ][y+1].get(0).getType(),
                grid[x+1][y+1].get(0).getType(),
        };

        /**
         * 0 1 2
         * 3 4 5
         * 6 7 8
         *
         * Nosotros estamos en 1, 3, 7, o 5 y queremos ir a 4.
         *
         */
        if (isMonster(types[1]) || isMonster(types[3]) || isMonster(types[4]) || isMonster(types[5]) || isMonster(types[7]))
            return true;

        if (isMonster(types[0]) && (types[1] == ObservationType.EMPTY || types[3] == ObservationType.EMPTY))
            return true;

        if (isMonster(types[2]) && (types[1] == ObservationType.EMPTY || types[5] == ObservationType.EMPTY))
            return true;

        if (isMonster(types[6]) && (types[3] == ObservationType.EMPTY || types[7] == ObservationType.EMPTY))
            return true;

        if (isMonster(types[8]) && (types[5] == ObservationType.EMPTY || types[7] == ObservationType.EMPTY))
            return true;

        int x_length = grid.length;
        int y_length = grid[0].length;

        /**
         * O 1 O
         * O 2 O
         * O 3 O
         * O 4 O
         *
         * La idea es que si estamos en 4 y queremos ir a 3, la posicion no es segura si en 1 hay un bicho y 2 esta vacio.
         *
         */

        if ( x - 2 > 1){
            ObservationType type = grid[x-2][y].get(0).getType();
            ObservationType next_type = grid[x-1][y].get(0).getType();
            if (next_type == ObservationType.EMPTY && isMonster(type)) {
                return true;
            }
        }
        if ( x + 2 < x_length - 1){
            ObservationType type = grid[x+2][y].get(0).getType();
            ObservationType next_type = grid[x+1][y].get(0).getType();
            if (next_type == ObservationType.EMPTY && isMonster(type)){
                return true;
            }
        }
        if ( y + 2 < y_length - 1){
            ObservationType type = grid[x][y+2].get(0).getType();
            ObservationType next_type = grid[x][y+1].get(0).getType();
            if (next_type == ObservationType.EMPTY && isMonster(type)){
                return true;
            }
        }
        if ( y - 2 > 1){
            ObservationType type = grid[x][y-2].get(0).getType();
            ObservationType next_type = grid[x][y-1].get(0).getType();
            if (next_type == ObservationType.EMPTY && isMonster(type)){
                return true;
            }
        }

        return false;
    }

    private boolean monsterNearby(int x, int y, StateObservation so, int r){
        boolean result = false;

        for( int i = 0; i <= r; ++i)
        {
            for( int j = 0; j <=i; ++j)
            {
                ObservationType type1 = ObservationType.EMPTY;
                ObservationType type2 = ObservationType.EMPTY;
                
                try{
                    type1 = getObservationGrid(so)[x+i-j][y+j].get(0).getType();
                }catch(ArrayIndexOutOfBoundsException e){ ; }
                
                try{
                    type2 = getObservationGrid(so)[x-i+j][y-j].get(0).getType();
                }catch(ArrayIndexOutOfBoundsException e){ ; }
                
                if( type1 == ObservationType.SCORPION || type1 == ObservationType.BAT ||
                    type2 == ObservationType.SCORPION || type2 == ObservationType.BAT )
                        result = true;
            }
        }
        return result;
    }
    
    private boolean monsterNearby(Node Pos, StateObservation so, int r){
        int x = (int) Pos.position.x;
        int y = (int) Pos.position.y;
        return monsterNearby(x, y, so, r);
    }
      

    /*************************************************
     * Safety Methods
     *************************************************/

    private boolean isSafe(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;
        //boolean monster_nearby = monsterNearby(node, stateObs, 1);

        //in type is the pos asked, in uptype is the pos above de current one.
        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        ObservationType uptype = getObservationGrid(stateObs)[x][y-1].get(0).getType();
        
        if(verbose)
        System.out.println("[isSafe]: x: " + x + ", y: " + y + ", tipo: " + type );

        if (type == ObservationType.BOULDER || type == ObservationType.SCORPION || type == ObservationType.BAT)
            return false;
        if (uptype ==  ObservationType.BOULDER) {
            return type == ObservationType.GEM;
        }
        return true;
    }

    // Comprueba si la accion correspondiente implica la muerte segun el juego.
    private boolean action_implies_death(StateObservation stateObs, Types.ACTIONS action){
        StateObservation next_state = stateObs.copy();
        next_state.advance(action);

        return !next_state.isAvatarAlive();
    }

    // Calcula una accion de escape.
    private Pair<Types.ACTIONS,Node> escape_from_current_position(StateObservation stateObs){
        int x = getPlayer(stateObs).getX();
        int y = getPlayer(stateObs).getY();
        Node actual = new Node(new Vector2d(x,y));
        
        
        if(verbose)
        System.out.println("[Escape]: Buscando ruta de escape de posible muerte");

        ArrayList<Node> neighbours = pf.getNeighbours(actual);

        for (Node neighbour: neighbours) {
            Types.ACTIONS ret = computeNextAction(getPlayer(stateObs), neighbour);
            if (isSafe(neighbour, stateObs)
                    && !action_implies_death(stateObs, ret)
                    && !monsterNearby(neighbour, stateObs)){

                StateObservation copy = stateObs.copy();
                copy.advance(ret);

                pf.grid = copy.getObservationGrid();
                pf.state = copy;
                boolean shut_in  = pf.astar._findPath(neighbour, new Node( new Vector2d(exit.getX(), exit.getY()))) == null;
                pf.grid = stateObs.getObservationGrid();
                pf.state = stateObs;
                if (!shut_in) {
                    
                    if(verbose)
                    System.out.println("El vecino seguro es: " + neighbour.position.x + " " + neighbour.position.y);
                    return new Pair<>(ret, neighbour);
                }
            }
        }
        
        //A esta parte del código solo se llega si no hay acciones validas.
        
        if(verbose)
            System.out.println("[Escape]: El jugador muere de todas formas");
        
        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
 
        
        /*for( int i = -1; i < 2; ++i)
        {
            // Si hay un bicho en una casilla, calcula como llegar a ella y va en dirección contraria
            if( getObservationGrid(stateObs)[x+i][y].get(0).getType() == ObservationType.BAT || 
                    getObservationGrid(stateObs)[x+i][y].get(0).getType() == ObservationType.SCORPION )
            {
                action = Types.ACTIONS.reverseACTION(computeNextAction(getPlayer(stateObs) , new Node( new Vector2d(x+i, y) ) ));
                System.err.println("Estamos huyendo");
            }
            if( getObservationGrid(stateObs)[x][y+i].get(0).getType() == ObservationType.BAT || 
                    getObservationGrid(stateObs)[x][y+i].get(0).getType() == ObservationType.SCORPION )
            {
                action = Types.ACTIONS.reverseACTION(computeNextAction(getPlayer(stateObs) , new Node( new Vector2d(x, y+i) ) ));
                System.err.println("Estamos huyendo");
            }
        }*/

        //En caso de que no haya seguras, pero tampoco haya ningun mostruo colindando, se hace el sistema clásico
        if(action == Types.ACTIONS.ACTION_NIL)
            action = Types.ACTIONS.reverseACTION(getLastAction());
        
        System.err.println("estamos huyendo:" + action.toString());
        return new Pair<>( action, computeAction( action, actual));
        //this was the line pre-commit
        //return new Pair<>(Types.ACTIONS.reverseACTION(getLastAction()), actual);
    }

    Node computeAction(Types.ACTIONS action, Node node)
    {
        switch(action){
            case ACTION_DOWN:
                return new Node( new Vector2d(node.position.x, node.position.y+1) );
            case ACTION_UP:
                return new Node( new Vector2d(node.position.x, node.position.y-1) );
            case ACTION_LEFT:
                return new Node( new Vector2d(node.position.x-1, node.position.y) );
            case ACTION_RIGHT:
                return new Node( new Vector2d(node.position.x+1, node.position.y) );
            default:
                return node;
        }
    }
    /**
     * *********************************************
     * Path Methods
     * *********************************************
     */
     private void setPath(StateObservation stateObs, Node position, Observation goal){
        pf.state = stateObs;
        pf.grid = stateObs.getObservationGrid();
        path = pf.astar._findPath(position, new Node(new Vector2d(goal.getX(), goal.getY())));

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
