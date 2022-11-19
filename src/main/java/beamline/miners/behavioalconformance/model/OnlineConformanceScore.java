package beamline.miners.behavioalconformance.model;

import beamline.events.BEvent;
import beamline.models.responses.Response;

/**
 * This class models the score of local online conformance
 * 
 * @author Andrea Burattin
 */
public class OnlineConformanceScore extends Response {

	private static final long serialVersionUID = 6895821762421722787L;
	private Double conformance = 0d;
	private Double completeness = 0d;
	private Double confidence = 0d;
	private BEvent lastEvent = null;
	private Double processingTime = 0d;
	private boolean isLastObservedViolation = false;
	
	public Double getConformance() {
		return conformance;
	}

	public void setConformance(Double conformance) {
		this.conformance = conformance;
	}

	public Double getCompleteness() {
		return completeness;
	}

	public void setCompleteness(Double completeness) {
		this.completeness = completeness;
	}

	public Double getConfidence() {
		return confidence;
	}

	public void setConfidence(Double confidence) {
		this.confidence = confidence;
	}
	
	public boolean isLastObservedViolation() {
		return isLastObservedViolation;
	}
	
	public void isLastObservedViolation(boolean isLastObservedVilation) {
		this.isLastObservedViolation = isLastObservedVilation;
	}
	
	public BEvent getLastEvent() {
		return lastEvent;
	}
	
	public void setLastEvent(BEvent lastEvent) {
		this.lastEvent = lastEvent;
	}

	public void setProcessingTime(double l) {
		this.processingTime = l;
	}
	
	public double getProcessingTime() {
		return processingTime;
	}

	@Override
	public String toString() {
		return "last-case-id = " + lastEvent.getTraceName() + "; last-activity = " + lastEvent.getEventName() + "; conformance = " + getConformance() + "; completeness = " + getCompleteness() + "; confidence = " + getConfidence() + "; processing-time = " + getProcessingTime();
	}
}
