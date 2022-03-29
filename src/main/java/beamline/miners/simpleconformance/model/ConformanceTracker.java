package beamline.miners.simpleconformance.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.processmining.models.graphbased.directed.DirectedGraphElementWeights;
import org.processmining.models.graphbased.directed.transitionsystem.AcceptStateSet;
import org.processmining.models.graphbased.directed.transitionsystem.StartStateSet;
import org.processmining.models.graphbased.directed.transitionsystem.State;

import beamline.miners.simpleconformance.utils.FrequencyTimeFinitePriorityQueue;
import beamline.miners.simpleconformance.utils.Quadruple;

public class ConformanceTracker extends HashMap<String, ConformanceStatus> {

	private static final long serialVersionUID = -7453522111588238137L;
	protected LinkedList<String> caseIdHistory;
	protected ExtendedCoverabilityGraph model;
	protected DirectedGraphElementWeights weights;
	protected StartStateSet ss;
	protected AcceptStateSet as;
	protected FrequencyTimeFinitePriorityQueue<Quadruple<State, String, State, Integer>> topErrors;

	protected int maxCasesToStore;
	protected int statesToStore;
	protected int costNoActivity;
	protected int errorsToStore;

	public ConformanceTracker(
			ExtendedCoverabilityGraph model,
			DirectedGraphElementWeights weights,
			StartStateSet ss,
			AcceptStateSet as,
			int statesToStore,
			int maxCasesToStore,
			int errorsToStore,
			int topErrorsToStore) {
		this.caseIdHistory = new LinkedList<>();
		this.model = model;
		this.weights = weights;
		this.ss = ss;
		this.as = as;
		this.maxCasesToStore = maxCasesToStore;
		this.statesToStore = statesToStore;
		this.costNoActivity = model.getCostActivityNotInProcess();
		this.errorsToStore = errorsToStore;
		this.topErrors = new FrequencyTimeFinitePriorityQueue<>(topErrorsToStore);
	}

	public Set<String> getHandledCases() {
		return keySet();
	}

	public Pair<State, Integer> replay(String caseId, String newEventName) {
		Pair<State, Integer> returned = null;
		Quadruple<State, String, State, Integer> lastError = null;

		if (containsKey(caseId)) {
			// now we can perform the replay
			returned = get(caseId).replayEvent(newEventName);
			// last error for statistical purposes
			lastError = get(caseId).getErrorForLastEvent();
			// need to refresh the cache
			caseIdHistory.remove(caseId);
		} else {
			// check if we can store the new case
			if (caseIdHistory.size() >= maxCasesToStore) {
				// we have no room for the case, we need to remove the case id
				// with most far update time
				String toRemove = caseIdHistory.poll();
				remove(toRemove);
			}
			// now we can perform the replay
			ConformanceStatus cs = new ConformanceStatus(model, weights, ss, as, statesToStore, errorsToStore);
			returned = cs.replayEvent(newEventName);
			put(caseId, cs);
			// last error for statistical purposes
			lastError = cs.getErrorForLastEvent();

		}
		// put the replayed case as first one
		caseIdHistory.add(caseId);
		// last error for statistical purposes
		if (lastError != null) {
			topErrors.add(lastError);
		}

		return returned;
	}

	public FrequencyTimeFinitePriorityQueue<Quadruple<State, String, State, Integer>> getTopErrors() {
		return topErrors;
	}
	
	public ExtendedCoverabilityGraph getModel() {
		return model;
	}
}