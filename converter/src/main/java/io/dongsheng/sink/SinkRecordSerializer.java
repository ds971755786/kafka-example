package io.dongsheng.sink;

import org.apache.kafka.connect.sink.SinkRecord;

public interface SinkRecordSerializer {
    byte[] serialize(SinkRecord record);
}
