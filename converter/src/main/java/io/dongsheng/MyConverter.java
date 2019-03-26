package io.dongsheng;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.storage.Converter;

import java.util.Map;

public class MyConverter implements Converter {
    public void configure(Map<String, ?> map, boolean b) {

    }

    public byte[] fromConnectData(String s, Schema schema, Object o) {
        throw new RuntimeException("you moron!");
    }

    public SchemaAndValue toConnectData(String s, byte[] bytes) {
        throw new RuntimeException("you moron!");
    }
}
