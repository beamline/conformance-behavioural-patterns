package beamline.miners.behavioalconformance;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;

import beamline.events.BEvent;
import beamline.miners.behavioalconformance.model.LocalConformanceTracker;
import beamline.miners.behavioalconformance.model.LocalModelStructure;
import beamline.miners.behavioalconformance.model.OnlineConformanceScore;
import beamline.models.algorithms.StreamMiningAlgorithm;

public class BehavioralConformance extends StreamMiningAlgorithm<OnlineConformanceScore> {

	private static final long serialVersionUID = 6287730078016220573L;
	private LocalConformanceTracker lct = null;
	
	public BehavioralConformance(Petrinet net, Marking marking, int maxCasesToStore) throws Exception {
		this(net, marking, maxCasesToStore, true);
	}
	
	public BehavioralConformance(Petrinet net, Marking marking, int maxCasesToStore, boolean quietPreProcessing) throws Exception {
		System.out.println("Preprocessing started... ");
		PrintStream oldSysout = System.out;
		if (quietPreProcessing) {
			System.setOut(new PrintStream(new OutputStream() { public void write(int arg0) throws IOException {} }));
		}
		
		LocalModelStructure lms = new LocalModelStructure(net, marking);
		this.lct = new LocalConformanceTracker(lms, maxCasesToStore);
		
		System.setOut(oldSysout);
		System.out.println("Preprocessing complete!");
	}
	
	@Override
	public OnlineConformanceScore ingest(BEvent event) {
		String caseId = event.getTraceName();
		String activityName = event.getEventName();
		return lct.replayEvent(caseId, activityName);
	}
}
