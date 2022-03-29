package beamline.miners.simpleconformance.model;

import org.processmining.models.graphbased.directed.transitionsystem.CoverabilityGraph;

public class ExtendedCoverabilityGraph extends CoverabilityGraph {

	private int costActivityNotInProcess = 1;
	
	public ExtendedCoverabilityGraph(String label, int costActivityNotInProcess) {
		super(label);
		this.costActivityNotInProcess = costActivityNotInProcess;
	}

	public int getCostActivityNotInProcess() {
		return costActivityNotInProcess;
	}
}