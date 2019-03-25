package practica_busqueda;

// General java imports
import java.util.ArrayList;
import java.util.Random;

// General game imports
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

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

        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;

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

            // Look for the exit (all gems collected)
            if (gems == NUM_GEMS_FOR_EXIT) {

                Observation exit = this.getExit(stateObs);

                // Calculate shortest path to nearest exit
                setAstarPath(avatar, exit);
            }

            // Look for another gem
            else {
                // Select nearest gem
                ArrayList<core.game.Observation>[] gemList
                        = stateObs.getResourcesPositions(stateObs.getAvatarPosition());
                Observation gem = new Observation(gemList[0].get(0), stateObs.getBlockSize());

                // Calculate shortest path to nearest exit
                setAstarPath(avatar, gem);
            }
        }

        // Calculate next action
        if (path != null) {
            Node nextPos = path.get(0);

            action = computeNextAction(avatar, nextPos);

            lastPosition = avatar;
        }

        return action;
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
