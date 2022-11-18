package andbur.au.qut.nets.unfolding;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import andbur.hub.top.petrinet.PetriNet;
import andbur.hub.top.petrinet.Place;
import andbur.hub.top.petrinet.Transition;

/**
 * Created by armascer on 9/11/2017.
 */
public class NetReplayer {
    private HashSet<Execution> executions;
    PetriNet net;

    public NetReplayer(PetriNet net){
        this.net = net;
    }

    public HashSet<LinkedList<String>> getTraces() {
        Execution exec = new Execution();
        exec.setMarking(getInitialMarking());

        Queue<Execution> toAnalyze = new LinkedList<>();
        toAnalyze.add(exec);

        HashSet<LinkedList<String>> traces = new HashSet<>();

        while(!toAnalyze.isEmpty()){
            Execution current = toAnalyze.remove();
            HashSet<Transition> enabled = getEnabledTs(current.marking);

            if(enabled.isEmpty())
                traces.add(current.getTrace());

            for(Transition t : enabled){
                Execution c1 = current.clone();
                c1.addFired(t);
                toAnalyze.add(c1);
            }
        }

        return traces;
    }

    private HashSet<Transition> getEnabledTs(LinkedList<Place> marking) {
        HashSet<Transition> enabled = new HashSet<>();
        for(Transition t : net.getTransitions())
            if(marking.containsAll(t.getPreSet()))
                enabled.add(t);

        return enabled;
    }

    private LinkedList<Place> getInitialMarking() {
        LinkedList<Place> marking = new LinkedList<>();

        for(Place p : net.getPlaces())
            if(p.getPreSet().isEmpty())
                marking.add(p);

        return marking;
    }
}
