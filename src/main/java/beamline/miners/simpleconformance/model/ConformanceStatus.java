package beamline.miners.simpleconformance.model;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;
import org.processmining.models.graphbased.directed.DirectedGraphElementWeights;
import org.processmining.models.graphbased.directed.transitionsystem.AcceptStateSet;
import org.processmining.models.graphbased.directed.transitionsystem.StartStateSet;
import org.processmining.models.graphbased.directed.transitionsystem.State;
import org.processmining.models.graphbased.directed.transitionsystem.Transition;

import com.google.common.collect.EvictingQueue;

import beamline.miners.simpleconformance.utils.Quadruple;

public class ConformanceStatus {

	protected ExtendedCoverabilityGraph model;
	protected DirectedGraphElementWeights weights;
	protected StartStateSet ss;
	protected AcceptStateSet as;

	protected int costNoActivity;
	protected int totalCost;
	protected EvictingQueue<State> lastStates;
	protected Date lastUpdate;
	protected EvictingQueue<Quadruple<State, String, State, Integer>> lastErrors; // from state, which action, target state, cost
	protected Quadruple<State, String, State, Integer> lastError;

	public ConformanceStatus(ExtendedCoverabilityGraph model, DirectedGraphElementWeights weights, StartStateSet ss, AcceptStateSet as,
			int statesToStore, int errorsToStore) {

		this.model = model;
		this.weights = weights;
		this.ss = ss;
		this.as = as;
		this.costNoActivity = model.getCostActivityNotInProcess();
		this.totalCost = 0;
		this.lastStates = EvictingQueue.create(statesToStore);
		this.lastUpdate = new Date();
		this.lastErrors = EvictingQueue.create(errorsToStore);
	}

	public Queue<Quadruple<State, String, State, Integer>> getLastErrors() {
		return lastErrors;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public int getTotalCost() {
		return totalCost;
	}

	public void refreshUpdateTime() {
		lastUpdate.setTime(System.currentTimeMillis());
	}

	/**
	 *
	 * @return the error associated to the last event; or <tt>null</tt> if the last event does not kick an error
	 */
	public Quadruple<State, String, State, Integer> getErrorForLastEvent() {
		return lastError;
	}

	public Pair<State, Integer> replayEvent(String newEventName) {
		Object usedState = null;
		Pair<State, Integer> bestSoFar = Pair.of(null, Integer.MAX_VALUE);

		// try to replay the current event starting from the start states or,
		// from the last states reached. only the configuration with lowest
		// local cost is kept
		if (lastStates.size() == 0) {
			// this is the first move of the trace, we have to select the first
			// move starting from the set of initial states
			for (Object stastStateId : ss) {
				State state = model.getNode(stastStateId);
				Pair<State, Integer> r = replayEventFromState(newEventName, state);
				if (r.getRight() < bestSoFar.getRight()) {
					bestSoFar = r;
					usedState = stastStateId;
				}
			}
		} else {
			// this is not the first move of the trace, we have to select the
			// transition from the transitions available starting from the last
			// states reached
			List<State> l = new LinkedList<State>(lastStates);
			Collections.reverse(l);
			for (State state : l) {
				Pair<State, Integer> r = replayEventFromState(newEventName, state);
				if (r.getRight() < bestSoFar.getRight()) {
					bestSoFar = r;
					usedState = state.getIdentifier();
				}
			}
		}

		// we can now "perform" the actual replay for the best configuration
		lastStates.add(bestSoFar.getLeft());
		totalCost += bestSoFar.getRight();

		// log the error
		if (bestSoFar.getRight() > 0) {
			lastErrors.add(Quadruple.of(
					model.getNode(usedState),
					newEventName,
					bestSoFar.getLeft(),
					bestSoFar.getRight()));
			lastError = Quadruple.of(
					model.getNode(usedState),
					newEventName,
					bestSoFar.getLeft(),
					bestSoFar.getRight());
		} else {
			lastError = null;
		}

		return bestSoFar;
	}

	protected Pair<State, Integer> replayEventFromState(String newEventName, State state) {
		for (Transition t : model.getOutEdges(state)) {
			if (t.getLabel().equals(newEventName)) {
				// we just found a transition labeled as we need. if the
				// transition has no cost associated, it means it is a "correct"
				// transition, so default cost is set to 0
				return Pair.of(
						t.getTarget(),
						weights.get(t.getSource().getIdentifier(), t.getTarget().getIdentifier(), t.getIdentifier(), 0));
			}
		}

		// if we did not find any transition it means that newEventName does not
		// belong to the alphabet of the process provided. no move from the
		// current state, and cost as for no activity
		return Pair.of(state, costNoActivity);
	}

	public boolean traceReachedAcceptState() {
		for (State s : lastStates) {
			if (as.contains(s.getIdentifier())) {
				return true;
			}
		}
		return false;
	}
}