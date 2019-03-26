package io.dongsheng.transforms;

import io.dongsheng.utils.SchemaUtil;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.*;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.HashMap;
import java.util.Map;

public class Concatenate <R extends ConnectRecord<R>> implements Transformation<R> {
    private static final String FIELD_CONFIG = "fields";

    private static final String CONCATENATE_AS = "concatenate.as";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(FIELD_CONFIG, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, ConfigDef.Importance.MEDIUM, "Field name to extract.")
            .define(CONCATENATE_AS, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, ConfigDef.Importance.MEDIUM, "Field name to extract.");

    private String concatenateAs;
    private String[] fields;

    @Override
    public R apply(R record) {
        final Schema schema = record.valueSchema();
        if (schema == null) {
            Object o = record.value();
            if (o == null) {
                return null;
            }
            if (o instanceof Map) {
                final Map<String, Object> value = (Map<String, Object>) o;
                final Map<String, Object> updatedValue = new HashMap<>();
                StringBuilder sb = new StringBuilder();

                for(String key : value.keySet()) {
                    Object v = value.get(key);
                    boolean isIt = false;
                    for (String f : fields) {
                        if (f.equals(key)) {
                            isIt = true;
                            break;
                        }
                    }
                    if (isIt) {
                        sb.append(v);
                    }
                    updatedValue.put(key, v);

                }
                updatedValue.put(concatenateAs, sb.toString());
                return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), record.valueSchema(), updatedValue, record.timestamp());
            } else {
                throw new DataException("Only Map objects supported in absence of schema, found: " + o.getClass().getName());
            }
        } else {
            Object o = record.value();
            if (o instanceof Struct) {
                Struct value = (Struct) o;
                Schema updatedSchema = makeUpdatedSchema(value.schema());

                final Struct updatedValue = new Struct(updatedSchema);

                StringBuilder sb = new StringBuilder();
                for (Field field : value.schema().fields()) {
                    boolean isIt = false;
                    for (String f : fields) {
                        f.equals(field.name());
                        isIt = true;
                        break;
                    }
                    if (isIt) {
                        sb.append(value.get(field));
                    }
                    updatedValue.put(field.name(), value.get(field));
                }
                updatedValue.put(concatenateAs, sb.toString());
                return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), updatedSchema, updatedValue, record.timestamp());
            } else {
                throw new DataException("Only Struct objects supported, found: " + o.getClass().getName());
            }

        }
    }

    private Schema makeUpdatedSchema(Schema schema) {
        final SchemaBuilder builder = SchemaUtil.copySchemaBasics(schema, SchemaBuilder.struct());
        for (Field field : schema.fields()) {
            builder.field(field.name(), field.schema());
        }
        builder.field(concatenateAs, Schema.OPTIONAL_STRING_SCHEMA );
        return builder.build();
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> props) {
        final AbstractConfig config = new AbstractConfig(CONFIG_DEF, props);
        fields = config.getString(FIELD_CONFIG).split(",");
        concatenateAs = config.getString(CONCATENATE_AS);
    }
}
