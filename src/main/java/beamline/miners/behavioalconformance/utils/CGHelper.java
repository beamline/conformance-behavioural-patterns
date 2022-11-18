package beamline.miners.behavioalconformance.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.transitionsystem.CoverabilityGraph;
import org.processmining.models.graphbased.directed.transitionsystem.State;
import org.processmining.models.graphbased.directed.transitionsystem.TransitionSystem;
import org.processmining.models.graphbased.directed.transitionsystem.TransitionSystemFactory;
import org.processmining.models.graphbased.directed.utils.Node;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.CTMarking;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;

public class CGHelper {

	public static CoverabilityGraph generate(Petrinet net, Marking initial) {
		Semantics<Marking, Transition> semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		semantics.initialize(net.getTransitions(), initial);
		return doBreadthFirst(net.getLabel(), new CTMarking(initial), semantics);
	}
	
	/**
	 * Build a coverability graph from initial state with breadth-first approach
	 * 
	 * @author Arya Adriansyah
	 * 
	 * @param context
	 *            context of the net
	 * @param label
	 *            label of the net
	 * @param state
	 *            Initial state (initial marking)
	 * @param semantics
	 *            semantics obtained from initial state
	 * @return CoverabilityGraph to be displayed
	 */
	public static CoverabilityGraph doBreadthFirst(String label, CTMarking state,
			Semantics<Marking, Transition> semantics) {

		// work using tree and transition system in parallel
		Node<CTMarking> root = new Node<CTMarking>();
		// create a tree to describe the coverability tree

		root.setData(new CTMarking(state));
		root.setParent(null);

		// build transitionSystem based on the root node
		CoverabilityGraph ts = TransitionSystemFactory.newCoverabilityGraph("Coverability Graph of " + label);
		ts.addState(state);

		// expands all
		Queue<Node<CTMarking>> expandedNodes = new LinkedList<Node<CTMarking>>();
		expandedNodes.add(root);

		// if CG is used as an intermediary result, no need to have context as a parameter
		// therefore the context should be null
		// checking context inside of extend methods
		do {
			Collection<? extends Node<CTMarking>> newNodes = extend(expandedNodes.poll(), semantics, ts);
			expandedNodes.addAll(newNodes);
		} while (!expandedNodes.isEmpty());
		return ts;
	}
	
	/**
	 * Extend an input state to get all of its children by executing available
	 * transition. Omega notation is added as needed
	 * 
	 * @author Arya Adriansyah
	 * 
	 * @param root
	 *            state to be extended, represented in form of element of a tree
	 * @param semantics
	 *            semantic of current net
	 * @param context
	 *            Context of the net
	 * @param ts
	 *            transition system associated with the tree
	 * @return list of all nodes need to be further extended
	 */
	private static Collection<? extends Node<CTMarking>> extend(Node<CTMarking> root,
			Semantics<Marking, Transition> semantics, TransitionSystem ts) {
		// init
		Marking rootState = root.getData();
		semantics.setCurrentState(rootState);

		List<Node<CTMarking>> needToBeExpanded = new ArrayList<Node<CTMarking>>();
		// this is the variable to be returned
		//		if (context != null)
		//			context.log("Current root = " + root.getData().toString(), MessageLevel.DEBUG);

		// execute transitions
		for (Transition t : semantics.getExecutableTransitions()) {

			//			if (context != null)
			//				context.log("Transition going to be executed : " + t.getLabel(), MessageLevel.DEBUG);
			semantics.setCurrentState(rootState);
			try {
				/*
				 * [HV] The local variable info is never read
				 * ExecutionInformation info =
				 */semantics.executeExecutableTransition(t);
				//				if (context != null)
				//					context.log(info.toString(), MessageLevel.DEBUG);
			} catch (IllegalTransitionException e) {
				e.printStackTrace();
				assert (false);
			}
			//			if (context != null)
			//				context.log("After execution= " + semantics.getCurrentState().toString(), MessageLevel.DEBUG);

			// convert current state to CTMarking
			// change the place in all the nodes of the tree with omega
			// representation
			CTMarking currStateCTMark = new CTMarking(semantics.getCurrentState());
			if (root.getData().hasOmegaPlace()) {
				currStateCTMark = currStateCTMark.transformToOmega(root.getData().getOmegaPlaces());
			}

			// currStateCTMark node
			Node<CTMarking> currStateCTMarkNode = new Node<CTMarking>();

			// is newState marking identical to a marking on the path from the
			// root?
			CTMarking lessOrEqualMarking = null;
			//			if (context != null)
			//				context.log("currStateCTMark = " + currStateCTMark.toString(), MessageLevel.DEBUG);
			//			if (context != null)
			//				context.log("root = " + root.getData().toString(), MessageLevel.DEBUG);

			for (State node : ts.getNodes()) {
				if (node.getIdentifier().equals(currStateCTMark)) {
					lessOrEqualMarking = currStateCTMark;
				}
			}
			if (lessOrEqualMarking == null) {
				lessOrEqualMarking = getIdenticalOrCoverable(currStateCTMark, root);
			}
			if (lessOrEqualMarking != null) {

				//				if (context != null)
				//					context.log("less or equal is found", MessageLevel.DEBUG);
				//				if (context != null)
				//					context.log(lessOrEqualMarking.toString(), MessageLevel.DEBUG);

				// check if the state is the same
				if (!lessOrEqualMarking.equals(currStateCTMark)) {
					// if not the same, check in case there are places that
					// needs to be changed into omega
					//					if (context != null)
					//						context.log("Equal node is not found. Coverable node is found", MessageLevel.DEBUG);

					// The places that need to be marked as omega, are those places that
					// occur more often in currStateCTMark, than in lessOrEqualMarking
					CTMarking temp = new CTMarking(currStateCTMark);
					temp.removeAll(lessOrEqualMarking);

					Set<Place> listToBeChanged = temp.baseSet();

					// list of places need to be transformed into omega
					currStateCTMark = currStateCTMark.transformToOmega(listToBeChanged);

					// set the node
					currStateCTMarkNode.setData(currStateCTMark);
					currStateCTMarkNode.setParent(root);

					root.addChild(currStateCTMarkNode); // insert the new node
					// to root

					// update transition system
					ts.addState(currStateCTMark);
					// BVD:new Marking(currStateCTMark));
					ts.addTransition(rootState, currStateCTMark, t);
					// BVD: new Marking(currStateCTMarkNode.getData()),
					//					if (context != null)
					//						context.log("Added Child (also need to be expanded): "
					//								+ currStateCTMarkNode.getData().toString(), MessageLevel.DEBUG);

					// node to be expanded
					needToBeExpanded.add(currStateCTMarkNode);
				} else { // exactly the same node is found
					//					if (context != null)
					//						context.log("Equal node is found", MessageLevel.DEBUG);
					// just set the node and add
					// set the node
					currStateCTMarkNode.setData(lessOrEqualMarking);
					currStateCTMarkNode.setParent(root);

					root.addChild(currStateCTMarkNode); // insert the new node
					// to root

					// update transition system
					ts.addState(currStateCTMark);
					// BVD:new Marking(currStateCTMark));
					ts.addTransition(rootState, currStateCTMark, t);
					// BVD: new Marking(currStateCTMarkNode.getData()),

					//					if (context != null)
					//						context.log("Added Child : " + lessOrEqualMarking.toString(), MessageLevel.DEBUG);
				}
				//				if (context != null)
				//					context.log("root after = " + root.toString(), MessageLevel.DEBUG);
			} else {
				// set the node
				currStateCTMarkNode.setData(currStateCTMark);
				currStateCTMarkNode.setParent(root);

				root.addChild(currStateCTMarkNode); // insert the new node to
				// root

				// update transition system
				ts.addState(currStateCTMark);
				// BVD:new Marking(currStateCTMark));
				ts.addTransition(rootState, currStateCTMark, t);
				// BVD: new Marking(currStateCTMarkNode.getData()),

				// node only need to be expanded
				needToBeExpanded.add(currStateCTMarkNode);

				//				if (context != null)
				//					context.log("Added Child (also need to be expanded): " + currStateCTMarkNode.getData().toString(),
				//							MessageLevel.DEBUG);
			}
			//			if (context != null)
			//				context.log("---------------", MessageLevel.DEBUG);
		}
		return needToBeExpanded;
	}
	
	/**
	 * Return an identical node or a node coverable by newState node. The node
	 * should be on the top of tree hierarchy. Return null if there is no
	 * identical node nor coverable node.
	 * 
	 * @author Arya Adriansyah
	 * 
	 * @param newState
	 * @param referenceNode
	 *            node which represent the parent of the newState node
	 * @return
	 */
	private static CTMarking getIdenticalOrCoverable(CTMarking newState, Node<CTMarking> referenceNode) {
		if (referenceNode.getParent() != null) { // checking not against root
			// node
			if (referenceNode.getData().isLessOrEqual(newState)) {
				return referenceNode.getData();
			} else {
				return getIdenticalOrCoverable(newState, referenceNode.getParent());
			}
		} else { // now checking against the root node
			if (referenceNode.getData().isLessOrEqual(newState)) {
				return referenceNode.getData();
			} else {
				return null;
			}
		}
	}
}
