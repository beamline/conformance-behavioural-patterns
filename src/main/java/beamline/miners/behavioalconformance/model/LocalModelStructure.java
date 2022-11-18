package beamline.miners.behavioalconformance.model;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.processmining.models.graphbased.directed.analysis.ShortestPathFactory;
import org.processmining.models.graphbased.directed.analysis.ShortestPathInfo;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.transitionsystem.CoverabilityGraph;
import org.processmining.models.graphbased.directed.transitionsystem.State;
import org.processmining.models.graphbased.directed.transitionsystem.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import beamline.miners.behavioalconformance.utils.CGHelper;
import beamline.miners.behavioalconformance.utils.PetrinetHelper;
import beamline.miners.behavioalconformance.utils.TSUtils;

/**
 * This class is a container of all structures needed for local online conformance checking.
 * 
 * @author Andrea Burattin
 */
public class LocalModelStructure implements Serializable {

	private static final long serialVersionUID = -7127507687303931081L;
	private Set<DirectFollowingRelation> allowedDirectFollowingRelations = new HashSet<>();
	private Map<DirectFollowingRelation, Pair<Integer, Integer>> minMaxRelationsBefore = new HashMap<>();
	private Map<DirectFollowingRelation, Integer> minRelationsAfter = new HashMap<>();
	
	// cached values
	private Double maxOfMinRelationsAfter = null;

	/**
	 * Initializes the local model structure
	 * 
	 * @param context
	 * @param net
	 * @param initMarking
	 * @throws Exception
	 */
	public LocalModelStructure(Petrinet net, Marking initMarking) throws Exception {
		populateStructure(net, initMarking);
	}
	
	/**
	 * Initializes the local model structure
	 * 
	 * @param coverabilityGraph
	 * @param coverabilityGraphUnfolded
	 * @param coverabilityGraphDualUnfolded
	 */
	public LocalModelStructure(CoverabilityGraph coverabilityGraph, CoverabilityGraph coverabilityGraphUnfolded, CoverabilityGraph coverabilityGraphDualUnfolded) {
		populateStructure(coverabilityGraph, coverabilityGraphUnfolded, coverabilityGraphDualUnfolded);
	}
	
	/**
	 * This method checks if the given relation is allowed by the model or not
	 * 
	 * @param relation
	 * @return
	 */
	public boolean isAllowed(DirectFollowingRelation relation) {
		return allowedDirectFollowingRelations.contains(relation);
	}
	
	public Pair<Integer, Integer> getMinMaxRelationsBefore(DirectFollowingRelation relation) {
		return minMaxRelationsBefore.get(relation);
	}
	
	public Integer getMinRelationsAfter(DirectFollowingRelation relation) {
		return minRelationsAfter.get(relation);
	}
	
	public Double getMaxOfMinRelationsAfter() {
		if (maxOfMinRelationsAfter == null) {
			maxOfMinRelationsAfter = Double.MIN_VALUE;
			for (Integer v : minRelationsAfter.values()) {
				maxOfMinRelationsAfter = Math.max(maxOfMinRelationsAfter, v);
			}
		}
		return maxOfMinRelationsAfter;
	}
	
	protected void populateStructure(Petrinet net, Marking initMarking) throws Exception {
		// build coverability graphs
		CoverabilityGraph coverabilityGraph = CGHelper.generate(net, initMarking);
		
		// build coverability graph of unfolded net
		Pair<Petrinet, Marking> unfoldedTotal = PetrinetHelper.unfold(net);
		CoverabilityGraph coverabilityGraphUnfolded = CGHelper.generate(unfoldedTotal.getLeft(), unfoldedTotal.getRight());
		
		// build coverability graph of dual net
		Pair<Petrinet, Marking> dualNet = PetrinetHelper.computeDual(net);
		Pair<Petrinet, Marking> unfoldedDualNet = PetrinetHelper.unfold(dualNet.getLeft());
		CoverabilityGraph coverabilityGraphDualUnfolded = CGHelper.generate(unfoldedDualNet.getLeft(), unfoldedDualNet.getRight());
		
		populateStructure(coverabilityGraph, coverabilityGraphUnfolded, coverabilityGraphDualUnfolded);
	}
	
	protected void populateStructure(
			CoverabilityGraph coverabilityGraph,
			CoverabilityGraph coverabilityGraphUnfolded,
			CoverabilityGraph coverabilityGraphDualUnfolded) {
		
		populateDirectFollowingRelations(coverabilityGraph);
		populateMinMaxBefore(coverabilityGraphUnfolded);
		populateMinAfter(coverabilityGraphDualUnfolded);
	}
	
	protected void populateDirectFollowingRelations(CoverabilityGraph coverabilityGraph) {
		// populate allowed direct following relations
		for (State s : coverabilityGraph.getNodes()) {
			for (Transition first : TSUtils.getIncomingNonTau(s, new HashSet<Transition>())) {
				for (Transition second : TSUtils.getOutgoingNonTau(s, new HashSet<Transition>())) {
					allowedDirectFollowingRelations.add(new DirectFollowingRelation(first.getLabel(), second.getLabel()));
				}
			}
		}
	}
	
	protected void populateMinMaxBefore(CoverabilityGraph coverabilityGraphUnfolded) {
		ShortestPathInfo<State, Transition> shortestPathCalculatorUnfolded = ShortestPathFactory.calculateAllShortestDistanceDijkstra(coverabilityGraphUnfolded);
		
		// populate min/max relations BEFORE from unfolded model
		State startState = null;
		for (Object s : coverabilityGraphUnfolded.getStates()) {
			if (coverabilityGraphUnfolded.getInEdges(coverabilityGraphUnfolded.getNode(s)).isEmpty()) {
				startState = coverabilityGraphUnfolded.getNode(s);
				break;
			}
		}
		
		for (State s : coverabilityGraphUnfolded.getNodes()) {
			for (Transition first : TSUtils.getIncomingNonTau(s, new HashSet<Transition>())) {
				for (Transition second : TSUtils.getOutgoingNonTau(s, new HashSet<Transition>())) {
					String firstLabel = TSUtils.getTransitionLabel(first);
					String secondLabel = TSUtils.getTransitionLabel(second);
					
					DirectFollowingRelation relation = new DirectFollowingRelation(firstLabel, secondLabel);
					State targetState = first.getTarget();
					List<DirectFollowingRelation> path = TSUtils.getShortestPath(shortestPathCalculatorUnfolded, startState, targetState);
					Integer min = path.size();
					Integer max = path.size();
					if (minMaxRelationsBefore.containsKey(relation)) {
						Pair<Integer, Integer> minMax = minMaxRelationsBefore.get(relation);
						min = Math.min(min, minMax.getLeft());
						max = Math.max(max, minMax.getRight());
					}
					minMaxRelationsBefore.put(relation, Pair.of(min, max));
				}
			}
		}
	}
	
	protected void populateMinAfter(CoverabilityGraph coverabilityGraphDualUnfolded) {
		ShortestPathInfo<State, Transition> shortestPathCalculatorDualUnfolded = ShortestPathFactory.calculateAllShortestDistanceDijkstra(coverabilityGraphDualUnfolded);
		
		// populate min/max relations AFTER the current one
		State startStateDual = null; // start state on dual is end state on original
		for (Object s : coverabilityGraphDualUnfolded.getStates()) {
			if (coverabilityGraphDualUnfolded.getInEdges(coverabilityGraphDualUnfolded.getNode(s)).isEmpty()) {
				startStateDual = coverabilityGraphDualUnfolded.getNode(s);
				break;
			}
		}
		
		for (State s : coverabilityGraphDualUnfolded.getNodes()) {
			if (!coverabilityGraphDualUnfolded.getInEdges(s).isEmpty() && !coverabilityGraphDualUnfolded.getOutEdges(s).isEmpty()) {
				for (Transition first : TSUtils.getIncomingNonTau(s, new HashSet<Transition>())) {
					for (Transition second : TSUtils.getOutgoingNonTau(s, new HashSet<Transition>())) {
						String firstLabel = TSUtils.getTransitionLabel(first);
						String secondLabel = TSUtils.getTransitionLabel(second);
						
						State targetState = second.getSource();
						List<DirectFollowingRelation> path = TSUtils.getShortestPath(shortestPathCalculatorDualUnfolded, startStateDual, targetState);
						DirectFollowingRelation relation = new DirectFollowingRelation(secondLabel, firstLabel);
						Integer min = path.size();
						if (minRelationsAfter.containsKey(relation)) {
							min = Math.min(min, minRelationsAfter.get(relation));
						}
						minRelationsAfter.put(relation, min);
					}
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return allowedDirectFollowingRelations.toString();
	}
	
	public void printNicely(PrintStream out) {
		List<String> alphabet = new LinkedList<String>();
		for (DirectFollowingRelation rel : allowedDirectFollowingRelations) {
			if (!alphabet.contains(rel.getLeft())) {
				alphabet.add(rel.getLeft());
			}
			if (!alphabet.contains(rel.getRight())) {
				alphabet.add(rel.getRight());
			}
		}
		Collections.sort(alphabet);
		
		// print direct following relations
		out.println("DIRECT FOLLOWING RELATIONS");
		out.println("==========================");
		out.print("\t");
		for (String a : alphabet) {
			out.print(a + "\t");
		}
		out.println("");
		for (String a : alphabet) {
			out.print(a + "\t");
			for (String b : alphabet) {
				if (allowedDirectFollowingRelations.contains(new DirectFollowingRelation(a, b))) {
					out.print(">");
				} else {
					out.print(" ");
				}
				out.print("\t");
			}
			out.println("");
		}
		
		// print min/max from start

		out.println("MIN/MAX RELATIONS FROM START");
		out.println("============================");
		out.print("\t");
		for (String a : alphabet) {
			out.print(a + "\t");
		}
		out.println("");
		for (String a : alphabet) {
			out.print(a + "\t");
			for (String b : alphabet) {
				DirectFollowingRelation r = new DirectFollowingRelation(a, b);
				if (minMaxRelationsBefore.containsKey(r)) {
					out.print(minMaxRelationsBefore.get(r).getLeft() + "/" + minMaxRelationsBefore.get(r).getRight());
				} else {
					out.print(" ");
				}
				out.print("\t");
			}
			out.println("");
		}
		
		// print min to end
		out.println("MIN RELATIONS TO END");
		out.println("====================");
		out.print("\t");
		for (String a : alphabet) {
			out.print(a + "\t");
		}
		out.println("");
		for (String a : alphabet) {
			out.print(a + "\t");
			for (String b : alphabet) {
				DirectFollowingRelation r = new DirectFollowingRelation(a, b);
				if (minRelationsAfter.containsKey(r)) {
					out.print(minRelationsAfter.get(r));
				} else {
					out.print(" ");
				}
				out.print("\t");
			}
			out.println("");
		}
	}
}
