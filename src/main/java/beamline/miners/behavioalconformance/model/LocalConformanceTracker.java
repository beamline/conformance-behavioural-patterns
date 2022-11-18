package beamline.miners.behavioalconformance.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * This class keeps track of the conformance status of a whole stream, by dispatching the events based on their
 * process instance. Also it keeps track of the different cases and is in charge of removing old ones.
 * 
 * @author Andrea Burattin
 */
public class LocalConformanceTracker extends HashMap<String, LocalConformanceStatus> implements Serializable {

	private static final long serialVersionUID = -7453522111588238137L;

	protected Queue<String> caseIdHistory;
	protected LocalModelStructure lms;

	protected int maxCasesToStore;
	protected int statesToStore;
	protected int costNoActivity;
	protected int errorsToStore;

	public LocalConformanceTracker(LocalModelStructure lms, int maxCasesToStore) {
		this.caseIdHistory = new LinkedList<String>();
		this.lms = lms;
		this.maxCasesToStore = maxCasesToStore;
	}
	
	/**
	 * This method performs the replay of an event and keeps track of corresponding process instance.
	 * 
	 * @param caseId
	 * @param newEventName
	 * @return
	 */
	public OnlineConformanceScore replayEvent(String caseId, String newEventName) {
		double time = System.nanoTime();
		OnlineConformanceScore currentScore;
		
		if (containsKey(caseId)) {
			// now we can perform the replay
			currentScore = get(caseId).replayEvent(newEventName);
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
			LocalConformanceStatus lcs = new LocalConformanceStatus(lms);
			currentScore = lcs.replayEvent(newEventName);
			put(caseId, lcs);
		}
		// put the replayed case as first one
		caseIdHistory.add(caseId);
		
		// set the processing time
		currentScore.setProcessingTime(System.nanoTime() - time);

		return currentScore;
	}

	public Set<String> getHandledCases() {
		return keySet();
	}
}
