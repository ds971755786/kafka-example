package io.dongsheng.flink;

import com.google.gson.Gson;
import enn.monitor.framework.metrics.kubelet.proto.ContainerInfo;
import enn.monitor.streaming.common.proto.Metric;
import io.dongsheng.flink.utils.MetricKeyedSerializationSchema;
import io.dongsheng.flink.utils.MetricResultTypeQueryable;
import io.dongsheng.flink.utils.MetricsConvertor;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Collector;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class Streaming {
    private static Logger logger = Logger.getLogger(Streaming.class);
    static Set<String> aggregateKeys;
    static {
        aggregateKeys = new HashSet<>();
        aggregateKeys.add("cpu_total");
        aggregateKeys.add("memory_usage");
        //aggregateKeys.add("memory_working_set");
        //aggregateKeys.add("memory_cache");
        //aggregateKeys.add("memory_rss");
        //aggregateKeys.add("memory_swap");
        //aggregateKeys.add("diskio_bytes_Total");
        aggregateKeys.add("diskio_bytes_Read");
        aggregateKeys.add("diskio_bytes_Write");
        aggregateKeys.add("network_rcv_bytes");
        aggregateKeys.add("network_snd_bytes");
    }

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // the host and the sourceTopic to connect to
        final String brokers;
        final String sourceTopic;
        final String sinkTopic;
        final String jobName;
        try {
            final ParameterTool params = ParameterTool.fromArgs(args);
            brokers = params.get("brokers");
            sourceTopic = params.get("sourceTopic");
            sinkTopic = params.get("sinkTopic");
            jobName = params.has("jobName") ? params.get("jobName") : "opentsdb streaming";
        } catch (Exception e) {
            System.err.println("No sourceTopic specified. Please run 'Streaming " +
                    "--brokers <brokers> --sourceTopic <sourceTopic> --sinkTopic <sinkTopic>', where brokers are kafka brokers ");
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", brokers);

        //properties.setProperty("zookeeper.connect", "localhost:2181");
        properties.setProperty("group.id", "test");
        //Gson gson = new Gson();
        //ObjectMapper mapper = new ObjectMapper();
        /**
         * DataStream<Tuple2<String, String>> stream = env
         *                 .addSource(new FlinkKafkaConsumer<>(sourceTopic,
         *                         new TypeInformationKeyValueSerializationSchema<>(String.class, String.class, new ExecutionConfig()), properties));
         */

        DataStream<String> stream = env
                .addSource(new FlinkKafkaConsumer<>(sourceTopic,
                        new SimpleStringSchema(), properties));

        DataStream<Metric> metrics = stream.flatMap(new MetricsFlatMapFunction());


        metrics.filter(m->{return m.getInstance() != null && aggregateKeys.contains(m.getMetric());})
                .addSink(new FlinkKafkaProducer<>(brokers, sinkTopic, new MetricKeyedSerializationSchema()));

        //metrics.addSink(new FlinkKafkaProducer<byte[]>(brokers, sinkTopic, b->b));

        env.execute(jobName);
        return;
    }

    public static class MyFlatMap implements FlatMapFunction<String, byte[]> {
        @Override
        public void flatMap(String value, Collector<byte[]> collector) throws Exception {
            Gson gson = new Gson();
            logger.info(value);
            ContainerInfo info = gson.fromJson(value, ContainerInfo.class);
            List<Metric> list = MetricsConvertor.convertor(info, "test-token");
            list.stream().filter(m->m.getInstance() != null && aggregateKeys.contains(m.getMetric())).forEach(m-> {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    collector.collect(mapper.writeValueAsBytes(m));
                } catch (JsonProcessingException e) {
                    logger.error(e.getMessage());
                }
            });
        }
    }

    public static class MetricsFlatMapFunction extends MetricResultTypeQueryable implements ResultTypeQueryable<Metric>, FlatMapFunction<String, Metric> {
        @Override
        public void flatMap(String value, Collector<Metric> collector) throws Exception {
            Gson gson = new Gson();
            ContainerInfo info = gson.fromJson(value, ContainerInfo.class);
            List<Metric> list = MetricsConvertor.convertor(info, "test-token");
            list.stream().forEach(m->collector.collect(m));
        }
    }
}
