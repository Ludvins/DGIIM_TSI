package practica_busqueda;

// General java imports
import java.util.ArrayList;
import java.util.*;
// General game imports
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.plaf.nimbus.State;
import java.lang.*;
/**
 * Agent class
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
    private Observation exit;

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
        
        gems = this.Gems( so );
        for (Observation gem : gems) {
            System.out.println("Posicion" + gem.getX() + " " + gem.getY());
        }
        actual = States.NEED_NEW_OBJETIVE;
    }

    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {


        Node nowPos = new Node(new Vector2d(getPlayer(stateObs).getX(), getPlayer(stateObs).getY()));
        System.out.println("[ACT]: Posicion actual: " + getPlayer(stateObs).getX() + " " + getPlayer(stateObs).getY());
        PlayerObservation avatar = getPlayer(stateObs);

        // Get current position and clear path if needed
        if (((avatar.getX() != lastPosition.getX()) || (avatar.getY() != lastPosition.getY()))
                && !path.isEmpty()) {
            path.remove(0);
        }
        if (local_gem_counter != getNumGems(stateObs)){
            actual = States.JUST_GOT_GEM;
        }


        Types.ACTIONS ret_action;

        while (true) {
            try{
                Thread.sleep(1000);
            }
            catch(Exception e){}

            System.out.println("[ACT]: Estado actual " + actual);
            System.out.println("[ACT]: El vector de gemas tiene tamaño " + this.gems.size());
            System.out.println("[ACT]: Numero de gemas obtenidas: " + local_gem_counter + " " + getNumGems(stateObs));
            switch (actual) {

                case NEAR_WANTED_GEM:

                    if (nowPos.position.x == next_gem.getX() && nowPos.position.y - 1 == next_gem.getY()) {
                        System.out.println("[ACT - NEAR_WANTED_GEM]: Debajo de la gema deseada");
                        return Types.ACTIONS.ACTION_UP;
                    }
                    if (nowPos.position.y == next_gem.getY() && nowPos.position.x + 1 == next_gem.getX()){
                        System.out.println("[ACT - NEAR_WANTED_GEM]: A la izquierda de la gema deseada");
                       return Types.ACTIONS.ACTION_RIGHT;
                    }
                    if (nowPos.position.y == next_gem.getY() && nowPos.position.x - 1 == next_gem.getX()){
                        System.out.println("[ACT - NEAR_WANTED_GEM]: A la derecha de la gema deseada");
                       return Types.ACTIONS.ACTION_LEFT;
                    }
                    actual = States.NEED_NEW_OBJETIVE;
                    break;

                case LOOKING_FOR_GEM:

                    System.out.println("[ACT - LOOKING_FOR_GEM]: Objetivo: " + next_gem.getX() + " " + next_gem.getY());
                    Node nextPos;
                    if (path != null && !path.isEmpty()) {
                        nextPos = path.get(0);
                    } else {
                        this.gems.add(this.gems.size(), next_gem);
                        actual = States.NEED_NEW_OBJETIVE;
                        break;
                    }
                    ret_action = computeNextAction(avatar, nextPos);

                    if ((nowPos.position.x == next_gem.getX() && nowPos.position.y - 1 == next_gem.getY())
                            || nowPos.position.y  == next_gem.getY() && ( nowPos.position.x + 1 == next_gem.getX() || nowPos.position.x -1  == next_gem.getX())
                    ){
                        System.out.println("[ACT - LOOKING_FOR_GEM]: Al lado de la gema deseada");
                        actual = States.NEAR_WANTED_GEM;
                        break;
                    }

                    if (monsterNearby(nextPos, stateObs)) {
                        System.out.println("[ACT - LOOKING_FOR_GEM]: HAY UN MONSTRUO CERCAAAAAA");
                        path.clear();
                        actual = States.ESCAPING;
                        this.gems.add(this.gems.size(), next_gem);
                        break;
                    }
                    System.out.println("[ACT - LOOKING_FOR_GEM]: La accion computada es " + ret_action);
                    if (action_implies_death(stateObs, ret_action)) {
                        System.out.println("[ACT - LOOKING_FOR_GEM]: La siguiente accion implica la muerte");
                        path.clear();
                        this.gems.add(this.gems.size(), next_gem);
                        actual = States.ESCAPING;
                        break;
                    }
                    if (!isSafe(nextPos, stateObs)) {
                        System.out.println("[ACT - LOOKING_FOR_GEM]: La siguiente posición no es segura");
                        path.clear();
                        this.gems.add(this.gems.size(), next_gem);
                        actual = States.ESCAPING;
                        break;
                    }
                        System.out.println("[ACT - LOOKING_FOR_GEM]: Acción a devolver: " + ret_action);
                        lastPosition = avatar;
                        return ret_action;

                case JUST_GOT_GEM:
                    local_gem_counter += 1;
                    System.out.println("[ACT - JUST_GOT_GEM]: Gema conseguida.");

                    if (nowPos.position.x == next_gem.getX() && nowPos.position.y == next_gem.getY()) { // Esto arregla la siguiente situacion: Coger una gema de camino a otra.
                        actual = States.NEED_NEW_OBJETIVE;
                    }
                    else {
                        actual = States.LOOKING_FOR_GEM;
                    }
                    break;

                case SETTING_PATH_FOR_GEM:
                    System.out.println("La gema objetivo es :" + next_gem.getX() + " " + next_gem.getY());

                        pf.state = stateObs;
                        pf.grid = stateObs.getObservationGrid();

                        path = pf.astar._findPath(nowPos, new Node(new Vector2d(next_gem.getX(), next_gem.getY())));
                        if (path == null || path.isEmpty()){
                            System.out.println("[ACT - SETTING_PATH]: No existe camino a la siguiente gema.");
                            actual = States.NEED_NEW_OBJETIVE;
                            this.gems.add(this.gems.size(), next_gem);
                        } else {
                            actual = States.LOOKING_FOR_GEM;
                        }

                    break;

                case NEED_NEW_OBJETIVE:

                    if (local_gem_counter >= NUM_GEMS_FOR_EXIT || getNumGems(stateObs) >= NUM_GEMS_FOR_EXIT) {
                        actual = States.GOT_ALL_GEMS;
                    } else {

                        next_gem = this.gems.get(0);
                        this.gems.remove(0);
                        actual = States.SETTING_PATH_FOR_GEM;
                    }
                    break;

                case ESCAPING:
                    actual = States.NEED_NEW_OBJETIVE;
                    Types.ACTIONS ret = escape_from_current_position(stateObs);
                    System.out.println("[ACT - SCAPING]: La accion de escape es " + ret);
                    return ret;

                case GOT_ALL_GEMS:
                    path.clear();
                    if (!setAstarPath(avatar, exit)) {

                        System.out.println("[ACT - GOT_ALL_GEMS]: No existe camino a la salida");

                        pf.state = stateObs;
                        pf.grid = stateObs.getObservationGrid();
                        path = pf.astar._findPath(nowPos, new Node(new Vector2d(exit.getX(), exit.getY())));
                        if (path == null || path.isEmpty()) {
                            System.out.println("NO EXISTE CAMINO A LA SALIDA!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            return Types.ACTIONS.ACTION_NIL;
                        }

                    }
                    actual = States.GOING_TO_EXIT;
                    break;

                case GOING_TO_EXIT:

                    System.out.println("[ACT - GOING_EXIT]: Objetivo: " + exit.getX() + " " + exit.getY());
                    if (path != null && !path.isEmpty()) {
                        nextPos = path.get(0);
                    }
                    else {
                        actual = States.GOT_ALL_GEMS;
                        break;
                    }

                    ret_action = computeNextAction(avatar, nextPos);

                    System.out.println("[ACT - GOING_EXIT]: La accion computada es " + ret_action);

                    if (monsterNearby(nextPos, stateObs)) {
                        System.out.println("[ACT - GOING_EXIT]: HAY UN MONSTRUO CERCAAAAAA");
                        path.clear();
                        actual = States.ESCAPING;
                        break;
                    }
                    if (action_implies_death(stateObs, ret_action)) {
                        System.out.println("[ACT - GOING_EXIT]: La siguiente accion implica la muerte");
                        path.clear();
                        actual = States.ESCAPING;
                        break;
                    }
                    if (!isSafe(nextPos, stateObs)) {
                        System.out.println("[ACT - GOING_EXIT]: La siguiente posición no es segura");
                        path.clear();
                        actual = States.ESCAPING;
                        break;
                    }

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
                if( isBoulderAbove(  gem.get(i).getX(),gem.get(i).getY(),stateObs) )
                {
                    h+=10;
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

    private boolean boulderOverGem(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;
        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        ObservationType uptype = getObservationGrid(stateObs)[x][y-1].get(0).getType();
        return type == ObservationType.GEM && uptype == ObservationType.BOULDER;
    }

    private boolean boulderOverNothing(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;
        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        ObservationType uptype = getObservationGrid(stateObs)[x][y-1].get(0).getType();
        return type == ObservationType.GROUND && uptype == ObservationType.BOULDER;
    }

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

    /*************************************************
     * Safety Methods
     *************************************************/
   private boolean monsterNearby(Node Pos, StateObservation so){

        for (Node n : pf.getNeighbours(Pos)){
            ObservationType t = getObservationGrid(so)[ (int) n.position.x][ (int) n.position.y].get(0).getType();
            if (t == ObservationType.BAT || t == ObservationType.SCORPION){
                return true;
            }
        }
        return false;
    }
    // Comprueba si la posicion es segura.
    private boolean isSafe(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;
        boolean nearMonster = false;

        //in type is the pos asked, in uptype is the pos above de current one.
        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        ObservationType uptype = getObservationGrid(stateObs)[x][y-1].get(0).getType();
        System.out.println("[isSafe]: x: " + x + ", y: " + y + ", tipo: " + type );
        
        
        for (Node m : pf.getNeighbours(node)){
        for (Node n : pf.getNeighbours(m)){
             if( monsterNearby(n, stateObs))
             { nearMonster = true;
                 System.err.println("Monstruo cerca");
             }
        }
        }

        if (type == ObservationType.BOULDER || type == ObservationType.SCORPION || type == ObservationType.BAT || nearMonster)
            return false;
        if (uptype ==  ObservationType.BOULDER) {
            return type == ObservationType.GEM;
        }
        return true;
    }

    // Calcula una accion de escape.
    private Types.ACTIONS escape_from_current_position(StateObservation stateObs){
        int x = getPlayer(stateObs).getX();
        int y = getPlayer(stateObs).getY();
        Node actual = new Node(new Vector2d(x,y));
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
                boolean encerrado = pf.astar._findPath(neighbour, new Node( new Vector2d(exit.getX(), exit.getY()))) == null;
                pf.grid = stateObs.getObservationGrid();
                pf.state = stateObs;
                if (!encerrado) {
                    System.out.println("El vecino seguro es: " + neighbour.position.x + " " + neighbour.position.y);
                    return ret;
                }
            }
        }
        System.out.println("[Escape]: El jugador muere de todas formas");
        return Types.ACTIONS.reverseACTION(getLastAction());
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
        if (path == null || path.isEmpty()) {
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
