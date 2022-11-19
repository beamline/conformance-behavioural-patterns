package beamline.miners.behavioalconformance.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import beamline.events.BEvent;

/**
 * This class keeps track of the conformance status for a single process instance
 * 
 * @author Andrea Burattin
 */
public class LocalConformanceStatus implements Serializable {

	private static final long serialVersionUID = -3488601130608912097L;
	protected int correctObservedDirectFollowingRelations = 0;
	protected int incorrectObservedDirectFollowingRelations = 0;
	protected String lastActivityForCase = null;
	protected LocalModelStructure lms;
	protected Date lastUpdate;
	protected Set<DirectFollowingRelation> observedRelations = new HashSet<DirectFollowingRelation>();
	protected OnlineConformanceScore last = new OnlineConformanceScore();
	
	public LocalConformanceStatus (LocalModelStructure lms) {
		this.lms = lms;
		this.lastUpdate = new Date();
	}
	
	/**
	 * This method performs the replay of a single event
	 * 
	 * @param newEventName the event to replay
	 * @return
	 */
	public OnlineConformanceScore replayEvent(BEvent event) {
		String newEventName = event.getEventName();
		if (lastActivityForCase != null) {
			// this is not the first relation in the case
			DirectFollowingRelation relation = new DirectFollowingRelation(lastActivityForCase, newEventName);
			
			// count relations based on whether it is allowed or not
			if (lms.isAllowed(relation)) {
				if (!observedRelations.contains(relation)) {
					correctObservedDirectFollowingRelations++;
					observedRelations.add(relation);
				}
				last.isLastObservedViolation(true);
			} else {
				incorrectObservedDirectFollowingRelations++;
				last.isLastObservedViolation(false);
			}
			
			// compute the conformance
			last.setConformance((double) correctObservedDirectFollowingRelations /
					(correctObservedDirectFollowingRelations + incorrectObservedDirectFollowingRelations));
			
			// compute the completeness
			if (lms.isAllowed(relation)) {
				Pair<Integer, Integer> minMax = lms.getMinMaxRelationsBefore(relation);
				int observed = observedRelations.size();
				if (observed >= minMax.getLeft() &&  observed <= minMax.getRight()) {
					last.setCompleteness(1d);
				} else {
					double comp = observed / (minMax.getLeft() + 1d);
					if (observed > (minMax.getLeft() + 1d)) {
						comp = observed / (minMax.getRight() + 1d);
					}
					if (comp > 1) {
						comp = 1;
					}
					last.setCompleteness(comp);
				}
			}
			
			// compute the confidence
			if (lms.isAllowed(relation)) {
				last.setConfidence(1d - (lms.getMinRelationsAfter(relation) / lms.getMaxOfMinRelationsAfter()));
			}
		}
		last.setLastEvent(event);
		lastActivityForCase = newEventName;
		refreshUpdateTime();
		return last;
	}
	
	public OnlineConformanceScore getCurrentScore() {
		return last;
	}
	
	public void refreshUpdateTime() {
		lastUpdate.setTime(System.currentTimeMillis());
	}
	
	public Date getLastUpdate() {
		return lastUpdate;
	}
}
