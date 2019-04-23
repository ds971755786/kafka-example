package io.dongsheng.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.HashMap;
import java.util.Map;

public class SinkRecordJsonSerializer implements SinkRecordSerializer {
    private ObjectMapper mapper = new ObjectMapper();
    @Override
    public byte[] serialize(SinkRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("offset", record.kafkaOffset());
        map.put("key", record.key());
        map.put("value", record.value());
        map.put("timestampType", record.timestampType());
        map.put("header", record.headers());
        try {
            return mapper.writeValueAsBytes(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
