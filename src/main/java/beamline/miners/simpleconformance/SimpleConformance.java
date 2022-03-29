package beamline.miners.simpleconformance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.configuration.Configuration;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.DirectedGraphElementWeights;
import org.processmining.models.graphbased.directed.transitionsystem.AcceptStateSet;
import org.processmining.models.graphbased.directed.transitionsystem.StartStateSet;
import org.processmining.models.graphbased.directed.transitionsystem.State;
import org.processmining.plugins.tsml.Tsml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import beamline.events.BEvent;
import beamline.graphviz.Dot;
import beamline.miners.simpleconformance.SimpleConformance.ConformanceResponse;
import beamline.miners.simpleconformance.model.ConformanceTracker;
import beamline.miners.simpleconformance.model.ExtendedCoverabilityGraph;
import beamline.miners.simpleconformance.ui.GraphvizConverter;
import beamline.models.algorithms.StreamMiningAlgorithm;
import beamline.models.responses.Response;

public class SimpleConformance extends StreamMiningAlgorithm<ConformanceResponse> {

	private static final long serialVersionUID = -2318771541454892749L;
	private static String JAVA_BIN = "";
	private static String OFFLINE_PREPROCESSOR_JAR = "";
	private static final int NUMBER_OF_OBSERVATIONS = 6 * 10;
	private static final long MILLISECONDS_BIN = 10 * 1000;
	public static final DateFormat df = new SimpleDateFormat("HH:mm:ss");
	
	private static int statesToStore = 10;
	private static int maxCasesToStore = 100;
	private static int errorsToStore = 10;
	private static int topErrorsToStore = 10;
	
	private File tpnFile;
	private ConformanceTracker miners = null;
	private CircularFifoQueue<Observation> observationsErrors = null;
	private CircularFifoQueue<Observation> observationsProcess = null;
	
	private HashMap<String, CircularFifoQueue<String>> prefixes = new HashMap<String, CircularFifoQueue<String>>();
	
	@SuppressWarnings("unused")
	private Dot extendedCoverabilityGraph = null;
	
	static {
		try {
			Properties prop = new Properties();
			InputStream stream = new FileInputStream("./javaOfflinePreProcessor.properties");
			prop.load(stream);
			JAVA_BIN = prop.getProperty("JAVA_BIN");
			OFFLINE_PREPROCESSOR_JAR = prop.getProperty("OFFLINE_PREPROCESSOR_JAR");
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("================================================================================");
		System.out.println("Using this Java interpreter: " + JAVA_BIN);
		System.out.println("Using this region-based-preprocessor: " + OFFLINE_PREPROCESSOR_JAR);
		System.out.println("================================================================================");
		System.out.flush();
	}
	
	public SimpleConformance(File tpnFile) {
		this.tpnFile = tpnFile;
	}
	
	@Override
	public void open(Configuration parameters) throws Exception {
		super.open(parameters);
		loadModel(tpnFile);
	}

	@Override
	public ConformanceResponse ingest(BEvent event) {
		String caseID = event.getTraceName();
		String activityName = event.getEventName();
		
		// calculate conformance
		Pair<State, Integer> returned = miners.replay(caseID, activityName);
		
		// update errors
		if (returned.getRight() > 0) {
			if (observationsErrors.isEmpty() || !observationsErrors.get(observationsErrors.size() - 1).incrementIfWithinTime(MILLISECONDS_BIN)) {
				observationsErrors.offer(new Observation(1));
			}
		} else if (observationsErrors.isEmpty() || !observationsErrors.get(observationsErrors.size() - 1).toUpdateIfWithinTime(MILLISECONDS_BIN)) {
			observationsErrors.offer(new Observation(0));
		}
		
		// update events
		if (observationsProcess.isEmpty() || !observationsProcess.get(observationsProcess.size() - 1).incrementIfWithinTime(MILLISECONDS_BIN)) {
			observationsProcess.offer(new Observation(1));
		}
		
		// update prefixes
		if (!prefixes.containsKey(caseID)) {
			prefixes.put(caseID, new CircularFifoQueue<String>(NUMBER_OF_OBSERVATIONS));
		}
		prefixes.get(caseID).offer(activityName);
		
		return new ConformanceResponse(
				returned.getRight(),
				event,
				returned.getRight() + " - cost of executing " + activityName + " in case " + caseID);
	}
	
	private void loadModel(File tpnFile) throws IOException, InterruptedException {
		File tmpCg = File.createTempFile("coverability", "cg");
		try {
			// calculate graph
			ProcessBuilder pb = new ProcessBuilder(JAVA_BIN, "-jar", OFFLINE_PREPROCESSOR_JAR, tpnFile.getAbsolutePath(), tmpCg.getAbsolutePath());
			System.out.println(JAVA_BIN + " -jar " + OFFLINE_PREPROCESSOR_JAR +" "+ tpnFile.getAbsolutePath() +" "+ tmpCg.getAbsolutePath());
			Process proc = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			while (reader.readLine() != null) { }
			proc.waitFor();
			
			// import the model
			ExtendedCoverabilityGraph ts = new ExtendedCoverabilityGraph("", 1);
			StartStateSet starts = new StartStateSet();
			AcceptStateSet accepts = new AcceptStateSet();
			DirectedGraphElementWeights weights = new DirectedGraphElementWeights();
			GraphLayoutConnection graphLayout = new GraphLayoutConnection(ts);
			
			try {
				Tsml tsml = importTsmlFromStream(tmpCg);
				tsml.unmarshall(ts, starts, accepts, weights, graphLayout);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			extendedCoverabilityGraph = GraphvizConverter.get(ts, weights);
			
			// associate model with process
			miners = new ConformanceTracker(
						ts, weights, starts, accepts,
						statesToStore, maxCasesToStore, errorsToStore, topErrorsToStore);
			
			observationsErrors = new CircularFifoQueue<Observation>(NUMBER_OF_OBSERVATIONS);
			observationsProcess = new CircularFifoQueue<Observation>(NUMBER_OF_OBSERVATIONS);
			prefixes = new HashMap<String, CircularFifoQueue<String>>();
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
		
	private Tsml importTsmlFromStream(File file) throws Exception {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		xpp.setInput(new FileInputStream(file), null);
		int eventType = xpp.getEventType();
		Tsml tsml = new Tsml();
		while (eventType != XmlPullParser.START_TAG) {
			eventType = xpp.next();
		}
		if (xpp.getName().equals(Tsml.TAG)) {
			tsml.importElement(xpp, tsml);
		} else {
			tsml.log(Tsml.TAG, xpp.getLineNumber(), "Expected tsml");
		}
		if (tsml.hasErrors()) {
			return null;
		}
		return tsml;
	}
	
//	@Override
//	public void configure(Collection<MinerParameterValue> collection) {
//		MinerParameterValue referenceModel = collection.iterator().next();
//		try {
//			loadModel((File) referenceModel.getValue());
//		} catch (IOException | InterruptedException e) {
//			e.printStackTrace();
//		}
//	}

//	private List<List<Object>> updateErrors() {
//		List<List<Object>> values = new ArrayList<>();
//		for (Observation obs : observationsErrors) {
//			values.add(Arrays.asList(df.format(obs.getTime()), obs.getObservations()));
//		}
//		return values;
//	}
	
//	private List<List<Object>> updateEvents() {
//		List<List<Object>> values = new ArrayList<>();
//		for (Observation obs : observationsProcess) {
//			values.add(Arrays.asList(df.format(obs.getTime()), obs.getObservations()));
//		}
//		return values;
//	}
	
//	private List<List<Object>> updateCases() {
//		List<List<Object>> values = new ArrayList<>();
//		List<String> handled = new LinkedList<String>(miners.getHandledCases());
//		Collections.sort(handled, new Comparator<String>() {
//			public int compare(String o1, String o2) {
//				ConformanceStatus cs1 = miners.get(o1);
//				ConformanceStatus cs2 = miners.get(o2);
//				if (cs1 == null || cs2 == null) {
//					return 0;
//				}
//				if (cs1.getTotalCost() > cs2.getTotalCost()) {
//					return -1;
//				} else if (cs1.getTotalCost() < cs2.getTotalCost()) {
//					return 1;
//				} else {
//					return cs2.getLastUpdate().compareTo(cs1.getLastUpdate());
//				}
//			}
//		});
//		double maxCost = Double.MIN_VALUE;
//		for (String caseId : handled) {
//			ConformanceStatus cs = miners.get(caseId);
//			maxCost = Math.max(maxCost, cs.getTotalCost());
//		}
//		for (String caseId : handled) {
//			ConformanceStatus cs = miners.get(caseId);
//			if (cs != null) {
//				values.add(Arrays.asList(
//					cs.getTotalCost(),
//					caseId,
//					cs.getLastUpdate(),
//					(cs.traceReachedAcceptState()? "Trace potentially finished" : "")
//				));
//			}
//		}
//		return values;
//	}
	
	public static class ConformanceResponse extends Response {

		private static final long serialVersionUID = -8148713756624004593L;
		private Integer cost;
		private BEvent lastEvent;
		private String message;
		
		public ConformanceResponse(Integer cost, BEvent lastEvent, String message) {
			this.cost = cost;
			this.lastEvent = lastEvent;
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public BEvent getLastEvent() {
			return lastEvent;
		}

		public Integer getCost() {
			return cost;
		}
	}
	
	private class Observation implements Serializable {

		private static final long serialVersionUID = 2020646052865625359L;
		private Date time;
		private Integer obs;
		
		public Observation(Integer obs) {
			this.time = new Date();
			this.obs = obs;
		}
		
//		public Date getTime() {
//			return time;
//		}
		
//		public Integer getObservations() {
//			return obs;
//		}
		
		public boolean toUpdateIfWithinTime(long windowSizeInMilliseconds) {
			return (time.toInstant().plusMillis(windowSizeInMilliseconds).isAfter(new Date().toInstant()));
		}
		
		public boolean incrementIfWithinTime(long windowSizeInMilliseconds) {
			if (toUpdateIfWithinTime(windowSizeInMilliseconds)) {
				obs += 1;
				return true;
			}
			return false;
		}
	}
}