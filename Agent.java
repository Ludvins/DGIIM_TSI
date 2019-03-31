package practica_busqueda;

// General java imports
import java.util.ArrayList;
import java.util.Random;

// General game imports
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.swing.*;
import javax.swing.plaf.nimbus.State;

/**
 * Agent class
 * @author Luis Antonio Ortega Andrés
 */
public class Agent extends BaseAgent {

    // Random agent
    private Random randomGenerator;

    // Basic A* agent
    private PathFinder pf;
    private ArrayList<Node> path = new ArrayList<>();
    private PlayerObservation lastPosition;
    private int local_gem_counter;

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

        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;

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
                Observation gem = new Observation(gemList[0].get(0), stateObs.getBlockSize());
                System.out.println("[ACT]: Posicion de la siguiente gema: " + gem.getX() + ", " + gem.getY());
                // Calculate shortest path to nearest exit
                if (!setAstarPath(avatar, gem)) {

                    System.out.println("[ACT]: No existe camino a la siguiente gema.");

                    //TODO: work here
                    //Can't stop to think on an unsecure position
                    if(action_implies_death(stateObs, Types.ACTIONS.ACTION_NIL)){
                        System.out.println("[ACT]: La posicion actual no es segura.");

                    }

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

        if( boulderComing( nowPos,stateObs ))
        {
            System.out.println("[ACT]: Está una piedra para caernos");
            return escape_from_current_position(stateObs);
        }
        
        if (!isSafe(nextPos, stateObs) || action_implies_death(stateObs, action)){
            System.out.println("[ACT]: La siguiente accion implica la muerte");
            action = escape_from_current_position(stateObs);
            path.clear();
        }

        lastPosition = avatar;

        System.out.println("[ACT]: Acción a devolver: " + action);
        return action;

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

    // Comprueba si la posicion pasada es un muero o piedra.
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
