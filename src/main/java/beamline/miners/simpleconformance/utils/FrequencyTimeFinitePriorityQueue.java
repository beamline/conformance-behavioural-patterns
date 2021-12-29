package beamline.miners.simpleconformance.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class FrequencyTimeFinitePriorityQueue<E> {

	private Map<E, Pair<Integer, Date>> dataStructure;
	private PriorityQueue<Triple<E, Integer, Date>> sortedElements;
	private int maxSize;

	public FrequencyTimeFinitePriorityQueue(int maxSize) {
		this.maxSize = maxSize;

		this.dataStructure = new HashMap<E, Pair<Integer, Date>>();
		this.sortedElements = new PriorityQueue<Triple<E, Integer, Date>>(maxSize, new Comparator<Triple<E, Integer, Date>>() {
			@Override
			public int compare(Triple<E, Integer, Date> o1, Triple<E, Integer, Date> o2) {
				int frequencyComparison = o2.getSecond().compareTo(o1.getSecond());
				int timeComparison = o2.getThird().compareTo(o1.getThird());
				if (frequencyComparison != 0) {
					return frequencyComparison;
				} else {
					return timeComparison;
				}
			}
		});
	}

	public synchronized void add(E element) {
		int newFrequency = 1;
		Date newDate = new Date();

		if (dataStructure.containsKey(element)) {
			// just update the existing data
			Pair<Integer, Date> e = dataStructure.get(element);
			newFrequency = dataStructure.get(element).getFirst() + 1;
			sortedElements.remove(new Triple<E, Integer, Date>(element, e.getFirst(), e.getSecond()));
		} else {
			if (dataStructure.size() == maxSize) {
				// new element and no more space
				Triple<E, Integer, Date> toRemove = sortedElements.poll();
				dataStructure.remove(toRemove.getFirst());
			}
			dataStructure.put(element, new Pair<Integer, Date>(newFrequency, newDate));
		}
		// add the elements to both internal data structures
		sortedElements.add(new Triple<E, Integer, Date>(element, newFrequency, newDate));
		dataStructure.put(element, new Pair<Integer, Date>(newFrequency, newDate));
	}

	public synchronized List<Pair<Integer, E>> getList() {
		LinkedList<Pair<Integer, E>> toReturn = new LinkedList<Pair<Integer, E>>();

		List<Triple<E, Integer, Date>> elements = new LinkedList<Triple<E, Integer, Date>>(sortedElements);
		Collections.sort(elements, sortedElements.comparator());

		for (Triple<E, Integer, Date> e : elements) {
			toReturn.add(new Pair<Integer, E>(e.getSecond(), e.getFirst()));
		}

		return toReturn;
	}
}