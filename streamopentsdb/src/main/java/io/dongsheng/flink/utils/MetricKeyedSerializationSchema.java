package io.dongsheng.flink.utils;

import enn.monitor.streaming.common.proto.Metric;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.apache.log4j.Logger;

public class MetricKeyedSerializationSchema implements KeyedSerializationSchema<Metric> {
    private static Logger logger = Logger.getLogger(MetricKeyedSerializationSchema.class);
    @Override
    public byte[] serializeKey(Metric element) {
        return element.getMetric().getBytes();
    }

    @Override
    public byte[] serializeValue(Metric element) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsBytes(element);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        return new byte[0];
    }

    @Override
    public String getTargetTopic(Metric element) {
        return null;
    }
}
