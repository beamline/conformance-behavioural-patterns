package beamline.miners.behavioalconformance.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.unfolder.Unfolder;

import andbur.hub.top.petrinet.PetriNet;

public class PetrinetHelper {

	public static Petrinet unfoldPlugin(Petrinet net) throws Exception {
		return unfold(net).getLeft();
	}

	public static Pair<Petrinet, Marking> unfold(Petrinet net) throws Exception {
		//		File pnmlOriginal = File.createTempFile("petrinet", "original");
		//		String pnmlUnfolded = pnmlOriginal.getAbsolutePath() + "-unfolded";
		//		
		//		PnmlExportNetToPNML exporter = new PnmlExportNetToPNML();
		//		exporter.exportPetriNetToPNMLFile(context, net, pnmlOriginal);
		//		
		//		ProcessBuilder pb = new ProcessBuilder("java", "-jar", "./lib/unfolder-v4.jar", pnmlOriginal.getAbsolutePath(), pnmlUnfolded);
		//		Process proc = pb.start();
		//		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		//		while (reader.readLine() != null) { }
		//		proc.waitFor();

		Marking marking;
//		try {
//			// Try to find corresponding marking
//			marking = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, net)
//					.getObjectWithRole(InitialMarkingConnection.MARKING);
//		} catch (Exception e) {
			// Corresponding marking not found, use empty marking
			marking = new Marking();
//		}
		if (marking.size() == 0) {
			marking = getInitialM(net);
		}
		// Convert ProM net to PNAPI net
		PetriNet pn = toPNAPIFormat(net, marking);
		// Call the unfolder on the PNAPI net
		Petrinet netUnfolded = Unfolder.unfold(pn);
		// Create initial marking 
		marking = getInitialM(netUnfolded);
		// Connect net and initial marking

		//		Petrinet netUnfolded = context.tryToFindOrConstructFirstNamedObject(
		//				Petrinet.class,
		//				PnmlImportNet.class.getAnnotation(Plugin.class).name(),
		//				null,
		//				null,
		//				pnmlUnfolded);
		//		InitialMarkingConnection imc = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, netUnfolded);

		//		pnmlOriginal.deleteOnExit();
		//		return new Pair<Petrinet, Marking>(
		//				(Petrinet) imc.getObjectWithRole("Net"),
		//				(Marking) imc.getObjectWithRole("Marking"));
		return Pair.of(netUnfolded, marking);
	}

	private static Marking getInitialM(Petrinet net) {
		Marking m = new Marking();
		for (org.processmining.models.graphbased.directed.petrinet.elements.Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty()) {
				m.add(p, 1);
			}
		}
		return m;
	}

	public static Pair<Petrinet, Marking> computeDual(Petrinet net) {
		Petrinet dualNet = PetrinetFactory.clonePetrinet(net);
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : dualNet.getEdges()) {
			dualNet.removeEdge(e);
			if (e.getTarget() instanceof Place && e
					.getSource() instanceof org.processmining.models.graphbased.directed.petrinet.elements.Transition) {
				dualNet.addArc((Place) e.getTarget(),
						(org.processmining.models.graphbased.directed.petrinet.elements.Transition) e.getSource());
			} else {
				dualNet.addArc(
						(org.processmining.models.graphbased.directed.petrinet.elements.Transition) e.getTarget(),
						(Place) e.getSource());
			}
		}

		Marking initial = new Marking();
		for (Place p : dualNet.getPlaces()) {
			if (dualNet.getInEdges(p).isEmpty()) {
				initial.add(p);
			}
		}

		return Pair.of(dualNet, initial);
	}

	public static boolean isWorkflowNet(Petrinet net) {
		boolean hasSourcePlace = false;
		boolean hasSinkPlace = false;
		for (Place p : net.getPlaces()) {
			hasSourcePlace = hasSourcePlace || net.getInEdges(p).isEmpty();
			hasSinkPlace = hasSinkPlace || net.getOutEdges(p).isEmpty();
		}
		return (hasSourcePlace && hasSinkPlace);
	}

	public static andbur.hub.top.petrinet.PetriNet toPNAPIFormat(Petrinet net, Marking initMarking) {
		return toPNAPIFormat(net, initMarking, new HashMap<PetrinetNode, andbur.hub.top.petrinet.Transition>());
	}

	public static andbur.hub.top.petrinet.PetriNet toPNAPIFormat(Petrinet net, Marking initMarking,
			Map<PetrinetNode, andbur.hub.top.petrinet.Transition> transitionMap) {
		andbur.hub.top.petrinet.PetriNet umaNet = new andbur.hub.top.petrinet.PetriNet();

		HashMap<PetrinetNode, andbur.hub.top.petrinet.Place> placeMap = new HashMap<PetrinetNode, andbur.hub.top.petrinet.Place>();

		for (PetrinetNode n : net.getPlaces()) {
			andbur.hub.top.petrinet.Place p = umaNet.addPlace(n.getLabel());
			placeMap.put(n, p);
			int num = initMarking.occurrences(n);
			if (num > 0)
				umaNet.setTokens(p, num);
		}

		for (PetrinetNode n : net.getTransitions()) {
			andbur.hub.top.petrinet.Transition t = umaNet.addTransition(n.getLabel());

			transitionMap.put(n, t);

			if (n instanceof org.processmining.models.graphbased.directed.petrinet.elements.Transition) {
				if (((org.processmining.models.graphbased.directed.petrinet.elements.Transition) n).isInvisible()) {
					t.tau = true;
				}
			}

			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> a : net.getInEdges(n)) {
				umaNet.addArc(placeMap.get(a.getSource()), t);
			}
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> a : net.getOutEdges(n)) {
				umaNet.addArc(t, placeMap.get(a.getTarget()));
			}
		}

		return umaNet;
	}

}
