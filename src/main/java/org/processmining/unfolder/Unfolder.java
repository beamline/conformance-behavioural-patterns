package org.processmining.unfolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.base.Pnml;

import andbur.au.qut.nets.unfolding.BPstructBP;
import andbur.au.qut.nets.unfolding.Multiplicity;
import andbur.au.qut.nets.unfolding.Unfolder_PetriNet;
import andbur.au.qut.pnml.PNMLReader;
import andbur.hub.top.petrinet.Arc;
import andbur.hub.top.petrinet.Node;
import andbur.hub.top.petrinet.PetriNet;
import andbur.hub.top.petrinet.Place;
import andbur.hub.top.petrinet.Transition;

class CustomPrintStream extends PrintStream {
	public CustomPrintStream(OutputStream out) {
		super(out, true);
	}
}

public class Unfolder {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Error. Please provide source and target PNML file paths as parameters.");
			System.exit(-1);
		}
		String file = args[0];
		String target = args[1];
//		String file = "C:\\Users\\andbur\\Desktop\\model.pnml";
//		String target = file + "_unfolded";

		PipedOutputStream pipeOut = new PipedOutputStream();
		System.setOut(new PrintStream(pipeOut));

		PetriNet net = PNMLReader.parse(new File(file));
		write2File(unfold(net), target);
	}
	
	public static Petrinet unfold(PetriNet net) throws Exception {
		HashSet<String> commonLabels = new HashSet<String>();
		HashSet<String> silent = new HashSet<String>();
		for (Transition t : net.getTransitions()) {
			if (t.getName().startsWith("tau ") || t.getName().isEmpty() || t.getName() == null) {
				silent.add(t.getName());
			} else {
				commonLabels.add(t.getName());
			}
		}
		
		Unfolder_PetriNet unfolder = new Unfolder_PetriNet(net, BPstructBP.MODE.EQUAL_DFS, silent);
		unfolder.computeUnfolding();

		HashMap<Node, Multiplicity> repetitions = new HashMap<>();
		PetriNet unfolded = unfolder.getUnfoldingAsPetriNet(commonLabels, repetitions, new HashMap<>());
		return translateNet(unfolded);
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

	private static void write2File(Petrinet net, String file) throws Exception {
		Marking marking = getInitialM(net);
		
		GraphLayoutConnection layout = new GraphLayoutConnection(net);
		
		Map<PetrinetGraph, Marking> markedNets = new HashMap<>();
		markedNets.put(net, marking);
		Pnml pnml = new Pnml().convertFromNet(markedNets, layout);
		pnml.setType(Pnml.PnmlType.PNML);
		String text = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + pnml.exportElement(pnml);
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		bw.write(text);
		bw.close();
	}

	private static Petrinet translateNet(PetriNet net) {
		HashMap<Object, Object> map = new HashMap<>();
		Petrinet newNet = PetrinetFactory.newPetrinet("name-of-net");

		for (Transition n : net.getTransitions())
			map.put(n, newNet.addTransition(n.getName()));

		for (Place n : net.getPlaces())
			map.put(n, newNet.addPlace(n.getName()));

		for (Arc arc : net.getArcs()) {
			if (arc.getSource() instanceof Place)
				newNet.addArc(
						(org.processmining.models.graphbased.directed.petrinet.elements.Place) map.get(arc.getSource()),
						(org.processmining.models.graphbased.directed.petrinet.elements.Transition) map
								.get(arc.getTarget()));
			else
				newNet.addArc(
						(org.processmining.models.graphbased.directed.petrinet.elements.Transition) map
								.get(arc.getSource()),
						(org.processmining.models.graphbased.directed.petrinet.elements.Place) map
								.get(arc.getTarget()));
		}

		return newNet;
	}
}
