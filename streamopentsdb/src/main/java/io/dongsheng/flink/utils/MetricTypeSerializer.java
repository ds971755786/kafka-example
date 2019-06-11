package io.dongsheng.flink.utils;

import enn.monitor.streaming.common.proto.Metric;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.base.MapSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;
import java.util.HashMap;

public class MetricTypeSerializer extends TypeSerializer<Metric> {

    private MapSerializer<String, String> mapSerializer = new MapSerializer<>(new StringSerializer(), new StringSerializer());
    private StringSerializer stringSerializer = new StringSerializer();

    @Override
    public boolean isImmutableType() {
        return false;
    }

    @Override
    public TypeSerializer<Metric> duplicate() {
        return this;
    }

    @Override
    public Metric createInstance() {
        return new Metric();
    }

    @Override
    public Metric copy(Metric from) {
        Metric metric = new Metric();
        metric.setMetric(from.getMetric());
        metric.setTimestamp(from.getTimestamp());
        metric.setValue(from.getValue());
        HashMap<String, String> map = new HashMap(from.getTags());
        metric.setTags(map);
        return metric;
    }

    @Override
    public Metric copy(Metric from, Metric reuse) {
        return copy(from);
    }

    @Override
    public int getLength() {
        return -1;
    }

    @Override
    public void serialize(Metric record, DataOutputView target) throws IOException {
        /**
         * 		final int size = map.size();
         * 		target.writeInt(size);
         *
         * 		for (Map.Entry<K, V> entry : map.entrySet()) {
         * 			keySerializer.serialize(entry.getKey(), target);
         *
         * 			if (entry.getValue() == null) {
         * 				target.writeBoolean(true);
         *                        } else {
         * 				target.writeBoolean(false);
         * 				valueSerializer.serialize(entry.getValue(), target);
         *            }* 		}
         */
        stringSerializer.serialize(record.getMetric(), target);
        target.writeDouble(record.getValue());
        target.writeLong(record.getTimestamp());
        mapSerializer.serialize(record.getTags(), target);
    }

    @Override
    public Metric deserialize(DataInputView source) throws IOException {
        Metric metric = new Metric();
        metric.setMetric(stringSerializer.deserialize(source));
        metric.setValue(source.readDouble());
        metric.setTimestamp(source.readLong());
        metric.setTags(mapSerializer.deserialize(source));
        return metric;
    }

    @Override
    public Metric deserialize(Metric reuse, DataInputView source) throws IOException {
        return deserialize(source);
    }

    @Override
    public void copy(DataInputView source, DataOutputView target) throws IOException {
        stringSerializer.copy(source, target);
        target.writeDouble(source.readDouble());
        target.writeLong(source.readLong());
        mapSerializer.copy(source, target);
    }

    @Override
    public boolean equals(Object obj) {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public TypeSerializerSnapshot<Metric> snapshotConfiguration() {
        return null;
    }
}
