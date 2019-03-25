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

        System.out.println("Entra a ACT");

        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;

        // TODO Mirar si esto sirve para algo, lo hizo antonio
        // Get current position and clear path if needed
        PlayerObservation avatar = getPlayer(stateObs);
        if (((avatar.getX() != lastPosition.getX()) || (avatar.getY() != lastPosition.getY()))
                && !path.isEmpty()) {
            path.remove(0);
        }

        // Get current gem count
        int gems = getNumGems(stateObs);

        // Update path
        if (path.isEmpty()) {

            System.out.println("El camino esta vacio");
            // Look for the exit (all gems collected)
            if (gems == NUM_GEMS_FOR_EXIT) {

                Observation exit = this.getExit(stateObs);

                // Calculate shortest path to nearest exit
                setAstarPath(avatar, exit);
            }

            // Look for another gem
            else {
                System.out.println("El camino no esta vacio");
                // Select nearest gem
                ArrayList<core.game.Observation>[] gemList
                        = stateObs.getResourcesPositions(stateObs.getAvatarPosition());
                Observation gem = new Observation(gemList[0].get(0), stateObs.getBlockSize());

                // Calculate shortest path to nearest exit
                setAstarPath(avatar, gem);
            }
        }
        if (!stateObs.isAvatarAlive()){
            System.out.println("Jugador muerto");
        }
        // Calculate next action
        try {
            Node nextPos = path.get(0);

            // TODO Simular accion, si morimos buscar ruta de escape
            action = computeNextAction(avatar, nextPos);

            if (action_implies_death(stateObs, action)){
                System.out.println(stateObs.isAvatarAlive());
                //action = Types.ACTIONS.ACTION_RIGHT;
                //System.out.println("La siguiente accion implica la muerte");
                action = escape_from_current_position(stateObs);
                path.clear();
            }

            lastPosition = avatar;
        }
        catch(IndexOutOfBoundsException ex){
            System.out.println("Path vacio");
        }

        System.out.println(action);
        return action;

    }

    private boolean isSafe(Node node, StateObservation stateObs){
        int x = (int) node.position.x;
        int y = (int) node.position.y;


        ObservationType type = getObservationGrid(stateObs)[x][y].get(0).getType();
        System.out.println("[isSafe]: x: " + x + " y: " + y + " tipo: " + type );
        return type != ObservationType.WALL && type != ObservationType.BOULDER;
    }

    private Types.ACTIONS escape_from_current_position(StateObservation stateObs){
        int x = getPlayer(stateObs).getX();
        int y = getPlayer(stateObs).getY();
        Node actual = new Node(new Vector2d(x,y));
        System.out.println(actual.position);
        System.out.println("Buscando ruta de escape de posible muerte");
        ArrayList<Node> neighbours = new ArrayList<>();

        neighbours.add(new Node(actual.position.copy().add(1,0)));
        neighbours.add(new Node(actual.position.copy().add(-1,0)));
        neighbours.add(new Node(actual.position.copy().add(0,1)));
        neighbours.add(new Node(actual.position.copy().add(0,-1)));

        System.out.println(neighbours.size());
        for (Node neighbour: neighbours) {
            if (isSafe(neighbour, stateObs)) {
                return computeNextAction(getPlayer(stateObs), neighbour);
            }
        }
        System.out.println("El jugador muere de todas formas");
        return Types.ACTIONS.ACTION_NIL;
    }

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

    private void setAstarPath(PlayerObservation initial, Observation goal){
            path = pf.getPath(new Vector2d(initial.getX(), initial.getY()),
                                  new Vector2d(goal.getX(), goal.getY()));
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
