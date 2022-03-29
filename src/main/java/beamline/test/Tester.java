package beamline.test;

import java.io.File;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import beamline.events.BEvent;
import beamline.miners.simpleconformance.SimpleConformance;
import beamline.miners.simpleconformance.SimpleConformance.ConformanceResponse;
import beamline.sources.StringTestSource;

public class Tester {
	public static void main(String...args) throws Exception {
		SimpleConformance conformance = new SimpleConformance(new File("C:\\Users\\andbur\\Desktop\\model.tpn"));

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env
//			.addSource(new XesLogSource("C:\\Users\\andbur\\Desktop\\log.xes.gz"))
			.addSource(new StringTestSource("ACEDB"))
			.keyBy(BEvent::getProcessName)
			.flatMap(conformance)
			.addSink(new SinkFunction<ConformanceResponse>(){
				private static final long serialVersionUID = 1L;

				public void invoke(ConformanceResponse value, Context context) throws Exception {
					System.out.println(value.message);
				};
			});
		env.execute();
		
		System.out.println("done");
	}
}
