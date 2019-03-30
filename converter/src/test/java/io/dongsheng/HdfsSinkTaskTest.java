package io.dongsheng;

import io.dongsheng.sink.HdfsSinkTask;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.dongsheng.sink.HdfsSinkConnector.HDFS_URL;
import static io.dongsheng.sink.HdfsSinkConnector.TOPICS_DIR;

public class HdfsSinkTaskTest {
    /**
     *     public static final String HDFS_URL = "hdfs.url";
     *     public static final String TOPICS_DIR = "topics.dir";
     *     public static final String LOGS_DIR = "logs.dir";
     *     public static final String FLUSH_SIZE = "flush.size";
     *     public static final String THREAD_POOL_SIZE = "thread.pool.size";
     *     public static final String MAX_PARTITION = "max.partition";
     *     public static final String WRITE_CYCLE = "write.cycle";
     */
    @Test
    public void testWriteToHdfs() throws IOException {
        HdfsSinkTask hdfsSinkTask = new HdfsSinkTask();

        Map<String, String> props = new HashMap<>();
        props.put(HDFS_URL, "hdfs://10.19.140.200:31467");
        props.put(TOPICS_DIR, "/connector/topics");
        props.put("topics", "test");

        hdfsSinkTask.start(props);

        hdfsSinkTask.writeToHdfs("test", 0, 0, 1024, new ObjectMapper().writeValueAsBytes(props));


    }
}
