package io.dongsheng.flink.utils;

import enn.monitor.streaming.common.proto.Metric;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;

public  class MetricResultTypeQueryable implements ResultTypeQueryable<Metric> {
    private MetricTypeSerializer metricTypeSerializer = new MetricTypeSerializer();

    @Override
    public TypeInformation<Metric> getProducedType() {
        return new TypeInformation<Metric>() {
            @Override
            public boolean isBasicType() {
                return false;
            }

            @Override
            public boolean isTupleType() {
                return false;
            }

            @Override
            public int getArity() {
                return 3;
            }

            @Override
            public int getTotalFields() {
                return 4;
            }

            @Override
            public Class<Metric> getTypeClass() {
                return Metric.class;
            }

            @Override
            public boolean isKeyType() {
                return false;
            }

            @Override
            public TypeSerializer<Metric> createSerializer(ExecutionConfig config) {
                return metricTypeSerializer;
            }

            @Override
            public String toString() {
                return null;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public boolean canEqual(Object obj) {
                return false;
            }
        };
    }
}