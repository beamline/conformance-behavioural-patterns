/* 
 * Copyright (C) 2010 - Artem Polyvyanyy, Luciano Garcia Banuelos
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package andbur.au.qut.nets.unfolding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.processmining.framework.util.Pair;

import com.google.common.collect.HashMultimap;

import andbur.hub.top.uma.DNode;
import andbur.hub.top.uma.DNodeBP;
import andbur.hub.top.uma.DNodeSys;
import andbur.hub.top.uma.Options;


/**
 * This is a wrapper of the UMA implementation of complete prefix unfolding
 * specifically designed to derive prefixes suitable for structuring
 */
public class BPstructBP extends DNodeBP {
	public enum MODE {
		ESPARZA, EQUAL_PREDS, EQUAL_DFS
	};

	protected MODE mode = MODE.ESPARZA;

	public static boolean option_printAnti = true;

	public HashSet<String> silentTransitions = new HashSet<>();

	public BPstructBP(DNodeSys system, Options options) {
		super(system, options);
	}

	public void setSilent(HashSet<String> silent) {
		this.silentTransitions = silent;
	}

	public void setMode(MODE mode) {
		this.mode = mode;
	}

	public Map<DNode, DNode> getElementary_ccPair() {
		return super.getCutOffEquivalentEvent();
	}

	/**
	 * The search strategy for
	 * {@link #equivalentCuts_conditionSignature_history(byte[], DNode[], DNode[])}
	 * . A size-based search strategy ({@link Options#searchStrat_size}).
	 * 
	 * The method has been extended to determine cut-off events by a
	 * lexicographic search strategy.
	 * 
	 * @param newEvent
	 *            event that has been added to the branching process
	 * @param newCut
	 *            the cut reached by 'newEvent'
	 * @param eventsToCompare
	 * @return <code>true</code> iff <code>newEvent</code> is a cut-off event
	 *         because of some equivalent event in <code>eventsToCompare</code>.
	 */
	protected boolean findEquivalentCut_bpstruct(int newCutConfigSize,
			DNode newEvent, DNode[] newCut, Iterable<DNode> eventsToCompare) {
		// all extensions to support the size-lexicographic search-strategy
		// or marked with LEXIK

		// optimization: determine equivalence of reached cuts by the
		// help of a 'condition signature, initialize 'empty' signature
		// for 'newEvent', see #equivalentCuts_conditionSignature_history

		byte[] newCutSignature = cutSignature_conditions_init255();

		// compare the cut reached by 'newEvent' to the initial cut
		// if (newCut.length == bp.initialCut.length)
		// if (equivalentCuts_conditionSignature_history(newCutSignature,
		// newCut, bp.initialCut)) {
		// // yes, newEvent reaches the initial cut again
		// setCutOffConditions(newEvent, newCut, bp.initialCut);
		// return true; // 'newEvent' is a cut-off event
		// }

		// Check whether 'eventsToCompare' contains a "smaller" event that
		// reaches
		// the same cut as 'newEvent'.

		// Optimization: to quickly avoid comparing configurations of events
		// that
		// do not reach the same cut as 'newEvent', compare and store hash
		// values
		// of the reached configurations.

		// get hash value of the cut reached by 'newEvent'
		int newEventHash = getPrimeConfiguration_CutHash().get(newEvent);

		// check whether 'newEvent' is a cut-off event by comparing to all other
		// given events
		Iterator<DNode> it = eventsToCompare.iterator();

		while (it.hasNext()) {
			DNode e = it.next();

			// retrieve the cut reached by the old event 'e'
			DNode[] oldCut = bp.getPrimeCut(e,
					getOptions().searchStrat_lexicographic,
					getOptions().searchStrat_lexicographic);

			HashSet<String> oldCLabel = new HashSet<String>();
			for (int i = 0; i < oldCut.length; i++)
				oldCLabel.add(properName(oldCut[i]));

			HashSet<String> newCLabel = new HashSet<String>();
			for (int i = 0; i < newCut.length; i++)
				newCLabel.add(properName(newCut[i]));

			// do not check the event that has just been added, the cuts would
			// be equal...
			if (e == newEvent)
				continue;

			// newCut is only equivalent to oldCut if the configuration of
			// newCut
			// is (lexicographically) larger than the configuration of oldCut
			if (!getPrimeConfiguration_Size().containsKey(e)) {
				// the old event 'e' has incomplete information about its prime
				// configuration, cannot be used to check for cutoff
				continue;
			}

			// optimization: compared hashed values of the sizes of the prime
			// configurations
			if (newCutConfigSize < getPrimeConfiguration_Size().get(e)) {
				// the old one is larger, not equivalent
				continue;
			}

			// optimization: compare reached states by their hash values
			// only if hash values are equal, 'newEvent' and 'e' could be
			// equivalent
			if (getPrimeConfiguration_CutHash().get(e) != newEventHash)
				continue;

			// cuts of different lengths cannot reach the same state, skip
			if (newCut.length != oldCut.length)
				continue;

			// if both configurations have the same size:
			if (newCutConfigSize == bp.getPrimeConfiguration_size) {
				// and if not lexicographic, then the new event cannot be
				// cut-off event
				if (!getOptions().searchStrat_lexicographic)
					continue;
				// LEXIK: otherwise compare whether the old event's
				// configuration is
				// lexicographically smaller than the new event's configuration
				if (!isSmaller_lexicographic(
						getPrimeConfigurationString().get(e),
						getPrimeConfigurationString().get(newEvent))) {
					// Check whether 'e' was just added. If this is the case,
					// then 'e' and 'newEvent'
					// were added in the wrong order and we check again whether
					// 'e' is a cut-off event
					if (e.post != null && e.post.length > 0 && e.post[0]._isNew) {
						// System.out.println("smaller event added later, should switch cut-off");
						checkForCutOff_again(e);
					}
					continue;
				}
			}

			// System.out.println("ZERO UNFOLDING");

			boolean doRestrict = true;
			// Abel's code
			boolean equalPredecessors = true;

			/*
			 * (Abel's code). Cutting context 2. A pair of configurations are
			 * considered as equivalent if the set of tokens in the markings
			 * have been generated by the same visible configurations.
			 */
			
			if (mode == MODE.EQUAL_PREDS) 
				equalPredecessors = checkPredecessor(newEvent, e);
			if(mode == MODE.EQUAL_DFS)
				equalPredecessors = checkDirectlyFollows(newCut, oldCut);
			
			// The prime configuration of 'e' is either smaller or
			// lexicographically
			// smaller than the prime configuration of 'newEvent'. Further, both
			// events
			// reach cuts of the same size. Check whether both cuts reach the
			// same histories
			// by comparing their condition signatures

			if (equalPredecessors
					&& doRestrict
					&& equivalentCuts_conditionSignature_history(
							newCutSignature, newCut, oldCut)) {
				// yes, equivalent cuts, make events and conditions equivalent
				setCutOffEvent(newEvent, e);
				setCutOffConditions(newEvent, newCut, oldCut);
				// and yes, 'newEvent' is a cut-off event
				return true;
			}
		}

		// no smaller equivalent has been found
		return false;
	}

	private boolean checkDirectlyFollows(DNode[] newCut, DNode[] oldCut) {
		HashSet<String> labelsNewCut = getMaximalLabels(newCut);
		HashSet<String> labelsOldCut = getMaximalLabels(oldCut);
		
		if(labelsNewCut.size() != labelsOldCut.size())
			return false;
		else if(!(labelsNewCut.containsAll(labelsOldCut) && labelsOldCut.containsAll(labelsNewCut)))
			return false;
		
		HashMultimap<Short, Short> reference_cutoff = getRelations(newCut);
		HashMultimap<Short, Short> reference_corr = getRelations(oldCut);
		
		if(reference_corr.isEmpty() || reference_cutoff.isEmpty())
			return false;
		
		return reference_corr.equals(reference_cutoff);
	}

	private HashSet<String> getMaximalLabels(DNode[] cut) {
		HashSet<String> maximalLabels = new HashSet<>();
		
		for(int i =0; i < cut.length; i++) {
			if(cut[i].isEvent)
				maximalLabels.add(properName(cut[i]));
			else 
				maximalLabels.add(properName(cut[i].pre[0]));
		}
			
		
		return maximalLabels;
	}

	private HashMultimap<Short, Short> getRelations(DNode[] cut) {
		HashMultimap<Short, Short> relations = HashMultimap.<Short, Short> create();
		LinkedList<DNode> indexesNodes = new LinkedList<DNode>();
		
		for(int i = 0; i < cut.length; i++) {
			Set<DNode> pred = cut[i].getAllPredecessors();
			
			for(DNode node : pred) 
				if(node != cut[i] && !node.isEvent && node.pre!=null && node.post != null && node.pre.length > 0 && node.post.length > 0) {
					DNode correctPost = null;
					for(int k = 0; k < node.post.length; k++)
						if(pred.contains(node.post[k])) {
							correctPost = node.post[k];
							break;
						}
					
					relations.put(node.pre[0].id, correctPost.id);
					
					if(!indexesNodes.contains(node.pre[0]))
						indexesNodes.add(node.pre[0]);
					if(!indexesNodes.contains(correctPost))
						indexesNodes.add(correctPost);
				}
		}
		
		short[][] graph = new short[indexesNodes.size()][indexesNodes.size()];
		short[][] graphInv = new short[indexesNodes.size()][indexesNodes.size()];
		for(int i = 0; i < indexesNodes.size(); i++)
			for(int j = 0; j < indexesNodes.size(); j++)
				if(relations.containsEntry(indexesNodes.get(i).id, indexesNodes.get(j).id) || i == j) 
					graph[i][j] = 1;
		
		short[][] tClosure = getTransitiveClosure(graph);
		for(int i = 0; i < tClosure.length; i++)
			for(int j = 0; j < tClosure.length; j++)
				if(tClosure[i][j] != 1 && tClosure[j][i] != 1 && !relations.containsEntry(indexesNodes.get(i).id, indexesNodes.get(j).id))
					relations.put(indexesNodes.get(i).id, indexesNodes.get(j).id);
		
		return relations;
	}

	private short[][] getTransitiveClosure(short[][] graph) {
		printGraph(graph);
		int V = graph.length;
		
		short reach[][] = new short[V][V];
        int  i, j, k;
 
        for (i = 0; i < V; i++)
            for (j = 0; j < V; j++)
                reach[i][j] = graph[i][j];
 
        for (k = 0; k < V; k++)
            for (i = 0; i < V; i++)
                for (j = 0; j < V; j++)
                		if((reach[i][j]!=0) || ((reach[i][k]!=0) && (reach[k][j]!=0)))
                    reach[i][j] = 1;

        printGraph(reach);
        
        return reach;
	}

	private void printGraph(short[][] reach) {
		System.out.println("-----------");
		for (int i = 0; i < reach.length; i++){
            for (int j = 0; j < reach.length; j++)
                System.out.print(reach[i][j]+" ");
            System.out.println();
        }
		System.out.println("-----------");
	}

	/**
	 * Restrict cutoff criterion for acyclic case
	 * 
	 * A cutoff is acyclic if neither cutoff nor its corresponding event do not
	 * refer to a transition of the originative net that is part of some cyclic
	 * path of the net
	 * 
	 * @param cutoff
	 *            Cutoff event
	 * @param corr
	 *            Corresponding event
	 * @param cutoff_cut
	 *            Cutoff cut
	 * @param corr_cut
	 *            Corresponding cut
	 * @return <code>true</code> if acyclic cutoff criterion holds; otherwise
	 *         <code>false</code>
	 */
	protected boolean checkAcyclicCase(DNode cutoff, DNode corr,
			DNode[] cutoff_cut, DNode[] corr_cut) {

		return checkConcurrency(cutoff, corr, cutoff_cut, corr_cut)
		// && checkGateway(cutoff,corr)
		// <<< LUCIANO: It seems that the condition above is redundant and it
		// only serves as a hack to deal with acyclic xor rigids (cf. to
		// complete the occurrence net)
		;
	}

	/**
	 * Restrict cutoff criterion for cyclic case
	 * 
	 * A cutoff is cyclic if either cutoff or its corresponding event refer to a
	 * transition of the originative net that is part of some cyclic path of the
	 * net
	 * 
	 * @param cutoff
	 *            Cutoff event
	 * @param corr
	 *            Corresponding event
	 * @param cutoff_cut
	 *            Cutoff cut
	 * @param corr_cut
	 *            Corresponding cut
	 * @return <code>true</code> if cyclic cutoff criterion holds; otherwise
	 *         <code>false</code>
	 */
	protected boolean checkContainment(DNode cutoff, DNode corr,
			DNode[] cutoff_cut, DNode[] corr_cut) {

		Set<DNode> lconf_cutoff = getLocalConfig(cutoff);
		Set<DNode> lconf_corr = getLocalConfig(corr);

		Set<Integer> transSet_cutoff = new HashSet<Integer>();
		for (DNode ev : lconf_cutoff)
			if (ev.isEvent && !silentTransitions.contains(properName(ev)))
				transSet_cutoff.add((int) ev.id);

		Set<Integer> transSet_corr = new HashSet<Integer>();
		for (DNode ev : lconf_corr)
			if (ev.isEvent && !silentTransitions.contains(properName(ev)))
				transSet_corr.add((int) ev.id);

		return transSet_corr.containsAll(transSet_cutoff)
				&& transSet_cutoff.containsAll(transSet_corr);
	}

	protected boolean checkPredecessor(DNode cutoff, DNode corr) {
		Set<DNode> lconf_cutoff = getLocalConfig(cutoff);
		Set<DNode> lconf_corr = getLocalConfig(corr);

		Set<Integer> transSet_cutoff = new HashSet<Integer>();
		for (DNode ev : lconf_cutoff)
			if (ev.isEvent && !silentTransitions.contains(properName(ev)))
				transSet_cutoff.add((int) ev.id);

		Set<Integer> transSet_corr = new HashSet<Integer>();
		for (DNode ev : lconf_corr)
			if (ev.isEvent && !silentTransitions.contains(properName(ev))) 
				transSet_corr.add((int) ev.id);

		return transSet_corr.containsAll(transSet_cutoff) && transSet_cutoff.containsAll(transSet_corr);
	}
	
	protected boolean checkDirectlyFollows(DNode cutoff, DNode corr) {
		if(!properName(corr).equals(properName(cutoff)))
			return false;
			
		LinkedHashSet<Pair<DNode, DNode>> lconf_cutoff = getLocalDFS(cutoff);
		LinkedHashSet<Pair<DNode, DNode>> lconf_corr = getLocalDFS(corr);
		HashMultimap<Integer, Integer> reference_cutoff = HashMultimap.<Integer, Integer> create();
		
		if(lconf_corr.size() < 1 || lconf_cutoff.size() < 1)
			return false;
		
		for (Pair<DNode,DNode> ev : lconf_cutoff)
//			if (!silentTransitions.contains(properName(ev.getFirst())) && !silentTransitions.contains(properName(ev.getSecond())))
				reference_cutoff.put((int) ev.getFirst().id, (int) ev.getSecond().id);

		HashMultimap<Integer, Integer> reference_corr = HashMultimap.<Integer, Integer> create();
		for (Pair<DNode,DNode> ev : lconf_corr)
//			if (!silentTransitions.contains(properName(ev.getFirst())) && !silentTransitions.contains(properName(ev.getSecond())))
				reference_corr.put((int) ev.getFirst().id, (int) ev.getSecond().id);
		
		if(reference_corr.keySet().containsAll(reference_cutoff.keySet()) && reference_corr.values().containsAll(reference_cutoff.values())) {
			for(Integer key : reference_cutoff.keySet())
				if(!reference_cutoff.get(key).equals(reference_corr.get(key)))
					return false;
			
			for(Integer key : reference_corr.keySet())
				if(!reference_corr.get(key).equals(reference_cutoff.get(key)))
					return false;
			
			return true;
		}
		
		return false;
	}

	public Set<DNode> getLocalConfig(DNode event) {
		Stack<DNode> stack = new Stack<DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		Set<DNode> events = new HashSet<DNode>();
		stack.push(event);
		while (!stack.isEmpty()) {
			DNode curr = stack.pop();
			visited.add(curr);
			if (curr.isEvent)
				events.add(curr);
			if (curr.pre == null)
				continue;
			for (DNode p : curr.pre)
				if (!visited.contains(p) && !stack.contains(p))
					stack.push(p);
		}
		return events;
	}
	
	public LinkedHashSet<Pair<DNode, DNode>> getLocalDFS(DNode event) {
		Stack<DNode> stack = new Stack<DNode>();
		Set<DNode> visited = new HashSet<DNode>();
		Set<DNode> events = new HashSet<DNode>();
		
		LinkedHashSet<Pair<DNode, DNode>> pairsDF = new LinkedHashSet<>();
		
		DNode target = event;
		stack.push(event);
		while (!stack.isEmpty()) {
			DNode curr = stack.pop();
			visited.add(curr);
			
			if(curr.isEvent)
				events.add(curr);
			
			if(!curr.isEvent && curr.pre != null && curr.pre.length > 0) { 
				pairsDF.add(new Pair<DNode, DNode>(curr.pre[0], target));
				target = curr.pre[0];
			}
			
			for (DNode p : curr.pre)
				if (!visited.contains(p) && !stack.contains(p))
					stack.push(p);
		}
		
		for(DNode n : events)
			for(DNode n1 : events)
				if(n != n1 && !isCorrInLocalConfig(n, n1) && !isCorrInLocalConfig(n1, n))
					pairsDF.add(new Pair<DNode, DNode>(n, n1));
					
		return pairsDF;
	}
	
	/**
	 * Check whether cutoff or its corresponding event refer to a gateway node
	 * in the originative net; false otherwise
	 * 
	 * @param cutoff
	 *            Cutoff event
	 * @param corr
	 *            Corresponding event
	 * @return <code>true</code> if cutoff or its corresponding event refer to a
	 *         gateway node; otherwise <code>false</code>
	 */
	protected boolean checkGateway(DNode cutoff, DNode corr) {
		if (cutoff.post.length > 1 || cutoff.pre.length > 1
				|| corr.post.length > 1 || corr.pre.length > 1)
			return true;

		return false;
	}

	/**
	 * Check if conditions in cuts of cutoff and corresponding events are
	 * shared, except of postsets
	 * 
	 * @param cutoff
	 *            Cutoff event
	 * @param corr
	 *            Corresponding event
	 * @param cutoff_cut
	 *            Cutoff cut
	 * @param corr_cut
	 *            Corresponding cut
	 * @return <code>true</code> if shared; otherwise <code>false</code>
	 */
	protected boolean checkConcurrency(DNode cutoff, DNode corr,
			DNode[] cutoff_cut, DNode[] corr_cut) {
		Set<Integer> cutoffSet = new HashSet<Integer>();
		Set<Integer> corrSet = new HashSet<Integer>();

		int i = 0;
		for (i = 0; i < cutoff_cut.length; i++)
			cutoffSet.add(cutoff_cut[i].globalId);
		for (i = 0; i < corr_cut.length; i++)
			corrSet.add(corr_cut[i].globalId);
		for (i = 0; i < cutoff.post.length; i++)
			cutoffSet.remove(cutoff.post[i].globalId);
		for (i = 0; i < corr.post.length; i++)
			corrSet.remove(corr.post[i].globalId);

		if (cutoffSet.size() != corrSet.size())
			return false;
		for (Integer n : cutoffSet) {
			if (!corrSet.contains(n))
				return false;
		}

		return true;
	}

	/**
	 * Check if corresponding event is in the local configuration of the cutoff
	 * 
	 * @param cutoff
	 *            Cutoff event
	 * @param corr
	 *            Corresponding event
	 * @return <code>true</code> if corresponding event is in the local
	 *         configuration of the cutoff; otherwise <code>false</code>
	 */
	protected boolean isCorrInLocalConfig(DNode cutoff, DNode corr) {
		List<Integer> todo = new ArrayList<Integer>();
		Map<Integer, DNode> i2d = new HashMap<Integer, DNode>();
		for (DNode n : Arrays.asList(cutoff.pre)) {
			todo.add(n.globalId);
			i2d.put(n.globalId, n);
		}
		Set<Integer> visited = new HashSet<Integer>();

		while (!todo.isEmpty()) {
			Integer n = todo.remove(0);
			visited.add(n);

			if (n.equals(corr.globalId))
				return true;

			for (DNode m : i2d.get(n).pre) {
				if (!visited.contains(m.globalId)) {
					todo.add(m.globalId);
					i2d.put(m.globalId, m);
				}
			}
		}

		return false;
	}

	public HashMap<DNode, Set<DNode>> getConcurrentConditions() {
		return co;
	}

	public boolean isCutOffEvent(DNode event) {

		if (findEquivalentCut_bpstruct(getPrimeConfiguration_Size().get(event),
				event, currentPrimeCut, bp.getAllEvents()))
			return true;

		return false;
	}

	public String properName(DNode n) {
		return dNodeAS.properNames[n.id];
	}

	/**
	 * Create a GraphViz' dot representation of this branching process.
	 */
	public String toDot() {
		StringBuilder b = new StringBuilder();
		b.append("digraph BP {\n");

		// standard style for nodes and edges
		b.append("graph [fontname=\"Helvetica\" nodesep=0.3 ranksep=\"0.2 equally\" fontsize=10];\n");
		b.append("node [fontname=\"Helvetica\" fontsize=8 fixedsize width=\".3\" height=\".3\" label=\"\" style=filled fillcolor=white];\n");
		b.append("edge [fontname=\"Helvetica\" fontsize=8 color=white arrowhead=none weight=\"20.0\"];\n");

		// String tokenFillString =
		// "fillcolor=black peripheries=2 height=\".2\" width=\".2\" ";
		String cutOffFillString = "fillcolor=gold";
		String antiFillString = "fillcolor=red";
		String impliedFillString = "fillcolor=violet";
		String hiddenFillString = "fillcolor=grey";

		// first print all conditions
		b.append("\n\n");
		b.append("node [shape=circle];\n");
		for (DNode n : bp.allConditions) {
			if (!option_printAnti && n.isAnti)
				continue;
			/*
			 * - print current marking if (cutNodes.contains(n))
			 * b.append("  c"+n.localId+" ["+tokenFillString+"]\n"); else
			 */
			if (n.isAnti && n.isHot)
				b.append("  c" + n.globalId + " [" + antiFillString + "]\n");
			else if (n.isCutOff)
				b.append("  c" + n.globalId + " [" + cutOffFillString + "]\n");
			else
				b.append("  c" + n.globalId + " []\n");

			// String auxLabel = "";

			b.append("  c" + n.globalId + "_l [shape=none];\n");
			// Diagrams
			// b.append("  c"+n.globalId+"_l -> c"+n.globalId+" [headlabel=\""+n+" "+auxLabel+"\"]\n");
		}

		// then print all events
		b.append("\n\n");
		b.append("node [shape=box];\n");
		for (DNode n : bp.allEvents) {
			if (!option_printAnti && n.isAnti)
				continue;
			if (n.isAnti && n.isHot)
				b.append("  e" + n.globalId + " [" + antiFillString + "]\n");
			else if (n.isAnti && !n.isHot)
				b.append("  e" + n.globalId + " [" + hiddenFillString + "]\n");
			else if (n.isImplied)
				b.append("  e" + n.globalId + " [" + impliedFillString + "]\n");
			else if (n.isCutOff)
				b.append("  e" + n.globalId + " [" + cutOffFillString + "]\n");
			else
				b.append("  e" + n.globalId + " []\n");

			String auxLabel = "";

			b.append("  e" + n.globalId + "_l [shape=none]\n");
			// Diagrams
			b.append("  e" + n.globalId + "_l -> e" + n.globalId
					+ " [headlabel=\"" + properName(n) + " " + auxLabel + "\"]\n");
			// b.append(" label=\""+n+"\"];\n");
		}

		// finally, print all edges
		b.append("\n\n");
		b.append(" edge [fontname=\"Helvetica\" fontsize=8 arrowhead=normal color=black];\n");
		for (DNode n : bp.allConditions) {
			String prefix = n.isEvent ? "e" : "c";
			for (int i = 0; i < n.pre.length; i++) {
				if (n.pre[i] == null)
					continue;
				if (!option_printAnti && n.isAnti)
					continue;

				if (n.pre[i].isEvent)
					b.append("  e" + n.pre[i].globalId + " -> " + prefix
							+ n.globalId + " [weight=10000.0]\n");
				else
					b.append("  c" + n.pre[i].globalId + " -> " + prefix
							+ n.globalId + " [weight=10000.0]\n");
			}
		}

		for (DNode n : bp.allEvents) {
			String prefix = n.isEvent ? "e" : "c";
			for (int i = 0; i < n.pre.length; i++) {
				if (n.pre[i] == null)
					continue;
				if (!option_printAnti && n.isAnti)
					continue;
				if (n.pre[i].isEvent)
					b.append("  e" + n.pre[i].globalId + " -> " + prefix
							+ n.globalId + " [weight=10000.0]\n");
				else
					b.append("  c" + n.pre[i].globalId + " -> " + prefix
							+ n.globalId + " [weight=10000.0]\n");
			}
		}

		// and add links from cutoffs to corresponding events (exclusive case)
		b.append("\n\n");
		b.append(" edge [fontname=\"Helvetica\" fontsize=8 arrowhead=normal color=red];\n");
		for (DNode n : bp.allEvents) {
			if (n.isCutOff
					&& futureEquivalence().getElementary_ccPair().get(n) != null) {
				if (!this.isCorrInLocalConfig(n, futureEquivalence()
						.getElementary_ccPair().get(n)))
					b.append("  e"
							+ n.globalId
							+ " -> e"
							+ futureEquivalence().getElementary_ccPair().get(n).globalId
							+ " [weight=10000.0]\n");
			}
		}

		// and add links from cutoffs to corresponding events (causal case)
		b.append("\n\n");
		b.append(" edge [fontname=\"Helvetica\" fontsize=8 arrowhead=normal color=blue];\n");
		for (DNode n : bp.allEvents) {
			if (n.isCutOff
					&& futureEquivalence().getElementary_ccPair().get(n) != null) {
				if (this.isCorrInLocalConfig(n, futureEquivalence()
						.getElementary_ccPair().get(n)))
					b.append("  e"
							+ n.globalId
							+ " -> e"
							+ futureEquivalence().getElementary_ccPair().get(n).globalId
							+ " [weight=10000.0]\n");
			}
		}

		b.append("}");
		return b.toString();
	}
}
