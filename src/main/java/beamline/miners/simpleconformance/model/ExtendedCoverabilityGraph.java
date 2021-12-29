package beamline.miners.simpleconformance.model;

import java.io.Serializable;

import org.processmining.models.graphbased.directed.transitionsystem.CoverabilityGraph;

public class ExtendedCoverabilityGraph extends CoverabilityGraph implements Serializable {

	private static final long serialVersionUID = 6896074574773349237L;
	private int costActivityNotInProcess = 1;

	public ExtendedCoverabilityGraph(String label, int costActivityNotInProcess) {
		super(label);
		this.costActivityNotInProcess = costActivityNotInProcess;
	}

	public int getCostActivityNotInProcess() {
		return costActivityNotInProcess;
	}
}