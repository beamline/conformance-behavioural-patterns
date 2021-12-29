package beamline.miners.simpleconformance.ui;

import java.util.Map;

import org.apache.commons.collections15.map.HashedMap;
import org.processmining.models.graphbased.directed.DirectedGraphElementWeights;
import org.processmining.models.graphbased.directed.transitionsystem.State;
import org.processmining.models.graphbased.directed.transitionsystem.Transition;

import beamline.graphviz.Dot;
import beamline.graphviz.DotEdge;
import beamline.graphviz.DotNode;
import beamline.miners.simpleconformance.model.ExtendedCoverabilityGraph;

public class GraphvizConverter {

	public static Dot get(ExtendedCoverabilityGraph cg, DirectedGraphElementWeights weights) {
		Dot dot = new Dot();
		Map<State, DotNode> states = new HashedMap<State, DotNode>();
		
		int i = 0;
		for (State s : cg.getNodes()) {
			states.put(s, dot.addNode("" + i++));
		}
		
		for (Transition t : cg.getEdges()) {
			String label = t.getLabel();
			boolean error = weights.get(t.getSource().getIdentifier(), t.getTarget().getIdentifier(), t.getIdentifier(), 0) > 0;
			if (error) {
				label += " (error)";
			}
			DotEdge e = dot.addEdge(states.get(t.getSource()), states.get(t.getTarget()), label);
			if (error) {
				e.setOption("color", "red");
			}
		}
		
		return dot;
	}
}