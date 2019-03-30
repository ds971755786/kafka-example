package io.dongsheng.sink;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HdfsSinkConnector extends SinkConnector {
    public static final String HDFS_URL = "hdfs.url";
    public static final String TOPICS_DIR = "topics.dir";
    public static final String LOGS_DIR = "logs.dir";
    public static final String FLUSH_SIZE = "flush.size";
    public static final String THREAD_POOL_SIZE = "thread.pool.size";
    public static final String MAX_PARTITION = "max.partition";
    public static final String WRITE_CYCLE = "write.cycle";

    private static ConfigDef configDef = new ConfigDef();
    private Map<String, String> configProperties;

    static {
        configDef.define(HDFS_URL, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                "Zookeeper quorum " + "of the hbase cluster");

        configDef.define(TOPICS_DIR, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                "Zookeeper quorum " + "of the hbase cluster");

        configDef.define(LOGS_DIR, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                "Zookeeper quorum " + "of the hbase cluster");

        configDef.define(FLUSH_SIZE, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                "Zookeeper quorum " + "of the hbase cluster");

        configDef.define(THREAD_POOL_SIZE, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                "Zookeeper quorum " + "of the hbase cluster");

        configDef.define(MAX_PARTITION, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                "Zookeeper quorum " + "of the hbase cluster");

        configDef.define(WRITE_CYCLE, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,
                "Zookeeper quorum " + "of the hbase cluster");
    }

    @Override
    public void start(Map<String, String> props) {
        configProperties = props;


    }

    @Override
    public Class<? extends Task> taskClass() {
        return HdfsSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        Map<String, String> taskProps = new HashMap<>();
        taskProps.putAll(configProperties);
        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(taskProps);
        }
        return taskConfigs;
    }

    @Override
    public void stop() {

    }

    @Override
    public ConfigDef config() {
        return configDef;
    }

    @Override
    public String version() {
        return "0.1";
    }
}
