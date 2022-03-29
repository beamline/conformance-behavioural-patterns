package beamline.miners.simpleconformance.utils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class FrequencyTimeFinitePriorityQueue<E extends Serializable> implements Serializable {

	private static final long serialVersionUID = 6704338693616129449L;
	private HashMap<E, Pair<Integer, Date>> dataStructure;
	private transient PriorityQueue<Triple<E, Integer, Date>> sortedElements;
	private int maxSize;

	public FrequencyTimeFinitePriorityQueue(int maxSize) {
		this.maxSize = maxSize;

		this.dataStructure = new HashMap<>();
		this.sortedElements = new PriorityQueue<>(maxSize, (Triple<E, Integer, Date> o1, Triple<E, Integer, Date> o2) -> {
			int frequencyComparison = o2.getRight().compareTo(o1.getRight());
			int timeComparison = o2.getRight().compareTo(o1.getRight());
			if (frequencyComparison != 0) {
				return frequencyComparison;
			} else {
				return timeComparison;
			}
		});
	}

	public synchronized void add(E element) {
		int newFrequency = 1;
		Date newDate = new Date();

		if (dataStructure.containsKey(element)) {
			// just update the existing data
			Pair<Integer, Date> e = dataStructure.get(element);
			newFrequency = dataStructure.get(element).getLeft() + 1;
			sortedElements.remove(Triple.of(element, e.getLeft(), e.getRight()));
		} else {
			if (dataStructure.size() == maxSize) {
				// new element and no more space
				Triple<E, Integer, Date> toRemove = sortedElements.poll();
				dataStructure.remove(toRemove.getLeft());
			}
			dataStructure.put(element, Pair.of(newFrequency, newDate));
		}
		// add the elements to both internal data structures
		sortedElements.add(Triple.of(element, newFrequency, newDate));
		dataStructure.put(element, Pair.of(newFrequency, newDate));
	}

	public synchronized List<Pair<Integer, E>> getList() {
		LinkedList<Pair<Integer, E>> toReturn = new LinkedList<>();

		List<Triple<E, Integer, Date>> elements = new LinkedList<>(sortedElements);
		Collections.sort(elements, sortedElements.comparator());

		for (Triple<E, Integer, Date> e : elements) {
			toReturn.add(Pair.of(e.getMiddle(), e.getLeft()));
		}

		return toReturn;
	}
}