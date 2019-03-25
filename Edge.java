package practica_busqueda;
import ontology.Types;


public class Edge {
        public int cost;
        public Types.ACTIONS action;
        public Types.ACTIONS parentAction;
        public Edge (int cost, Types.ACTIONS action, Types.ACTIONS parentAction){
            this.cost = cost;
            this.action = action;
            this.parentAction = parentAction;
        }
}

