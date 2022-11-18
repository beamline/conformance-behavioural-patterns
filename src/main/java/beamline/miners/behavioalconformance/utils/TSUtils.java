package beamline.miners.behavioalconformance.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.processmining.models.graphbased.directed.analysis.ShortestPathInfo;
import org.processmining.models.graphbased.directed.transitionsystem.State;
import org.processmining.models.graphbased.directed.transitionsystem.Transition;
import org.processmining.models.graphbased.directed.transitionsystem.TransitionSystem;

import beamline.miners.behavioalconformance.model.DirectFollowingRelation;

/**
 * Collection of tools to handle and operate transition systems.
 * 
 * @author Andrea Burattin
 */
public class TSUtils {
	
	/**
	 * Checks if the given transition is a silent transition
	 * 
	 * @param transition
	 * @return <tt>true</tt> if the transition is silent, <tt>false</tt> otherwise
	 */
	public static boolean isTransitionTau(Transition transition) {
		return transition == null || transition.getLabel().startsWith("tau ") || transition.getLabel().startsWith("silent_added") || transition.getLabel().isEmpty();
	}
	
	/**
	 * Get one transition connecting the two given states
	 * 
	 * @param from source state
	 * @param to target state
	 * @return a transition connecting the two states, <tt>null</tt> if such transition does not exist
	 */
	public static Transition getConnection(State from, State to) {
		TransitionSystem ts = from.getGraph();
		for (Transition t : ts.getOutEdges(from)) {
			if (t.getTarget().equals(to)) {
				return t;
			}
		}
		return null;
	}
	
	public static List<DirectFollowingRelation> getShortestPath(ShortestPathInfo<State, Transition> calculator, State from, State to) {
		List<DirectFollowingRelation> result = new LinkedList<DirectFollowingRelation>();
		State prev = null;
		Transition prevTransition = null;
		for (State s : calculator.getShortestPath(from, to)) {
			if (prev != null) {
				Transition t = TSUtils.getConnection(prev, s);
				if (t != null && !TSUtils.isTransitionTau(t)) {
					if (prevTransition != null) {
						DirectFollowingRelation newRel = new DirectFollowingRelation(prevTransition.getLabel(), t.getLabel());
						if (!result.contains(newRel)) {
							result.add(newRel);
						}
					}
					prevTransition = t;
				}
			}
			prev = s;
		}
		return result;
	}
	
	public static String getTransitionLabel(Transition t) {
		String label = t.getLabel();
		if (label.matches("(.*)_copy_\\d+")) {
			label = label.replaceAll("(.*)_copy_\\d+", "$1");
		}
		return label;
	}
	
	public static Set<Transition> getIncomingNonTau(State s, Set<Transition> i) {
		for (Transition t : s.getGraph().getInEdges(s)) {
			if (!TSUtils.isTransitionTau(t)) {
				i.add(t);
			} else {
				i.addAll(getIncomingNonTau(t.getSource(), i));
			}
		}
		return i;
	}
	
	public static Set<Transition> getOutgoingNonTau(State s, Set<Transition> i) {
		for (Transition t : s.getGraph().getOutEdges(s)) {
			if (!TSUtils.isTransitionTau(t)) {
				i.add(t);
			} else {
				i.addAll(getOutgoingNonTau(t.getTarget(), i));
			}
		}
		return i;
	}
}
