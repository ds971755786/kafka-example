package io.dongsheng.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

import static io.dongsheng.sink.HdfsSinkConnector.*;

public class HdfsSinkTask extends SinkTask {
    /**
     *        "connector.class": "io.confluent.connect.hdfs.HdfsSinkConnector",
     *         "topics.dir": "/connector/topics",
     *         "flush.size": "16",
     *         "tasks.max": "1",
     *         "topics": "hdfs-sink-stress",
     *         "hdfs.url": "hdfs://etyb-namenode2.spider:8020",
     *         "logs.dir": "/connector/logs",
     *
     *         Configuration configuration = new Configuration();
     * FileSystem hdfs = FileSystem.get( new URI( "hdfs://localhost:54310" ), configuration );
     * hdfs.close();
     * @return
     */
    private static final Logger log = LoggerFactory.getLogger(HdfsSinkTask.class);
    private FileSystem hdfs;
    private int numTopics;
    private ConcurrentHashMap<String, List<ConcurrentLinkedQueue<SinkRecord>>> topicQueueMap = new ConcurrentHashMap<>();
    private String[] topics;
    private int flushSize = 1024;
    private String topicsDir;
    private int maxPartition = 16;
    private String hdfsUrl;

    private ScheduledExecutorService scheduledExecutorService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String version() {
        return "0.1";
    }

    public void initTopicQueueMap(int maxPartition) {
        for (String topic : topics) {
            List<ConcurrentLinkedQueue<SinkRecord>> l = topicQueueMap.get(topic);
            for (int i = 0; i < maxPartition; i++) {
                l.add(new ConcurrentLinkedQueue());
            }
        }
    }

    @Override
    public void start(Map<String, String> props) {
        //

        Configuration configuration = new Configuration();
        this.hdfsUrl = props.get(HDFS_URL);
        if (this.hdfsUrl == null) {
            throw new ConfigException("missing hdfs uri");
        }
        this.topicsDir = props.get(TOPICS_DIR);
        if (this.topicsDir == null) {
            throw new ConfigException("missing topics dir");
        }
        String t = props.get("topics");
        if (t == null) {
            throw new ConfigException("missing topics");
        }
        //topics should be immutable
        this.topics = t.split(",");
        for (String topic : this.topics) {
            this.topicQueueMap.put(topic, Collections.synchronizedList(new ArrayList<>()));
        }

        String maxPartition = props.get(MAX_PARTITION);
        if (maxPartition == null) {
            initTopicQueueMap(this.maxPartition);
        } else {
            initTopicQueueMap(Integer.decode(maxPartition));
        }

        /*
        String tp = props.get(THREAD_POOL_SIZE);
        if (tp == null) {
            this.scheduledExecutorService = Executors.newScheduledThreadPool(4);
        } else {
            this.scheduledExecutorService = Executors.newScheduledThreadPool(Integer.decode(tp));
        }
        */

        /*
        String writeCycle = props.get(WRITE_CYCLE);
        if (writeCycle == null) {
            this.scheduledExecutorService.scheduleAtFixedRate(()->{}, 0, 1000, TimeUnit.MILLISECONDS);
        } else {
            this.scheduledExecutorService.scheduleAtFixedRate(()->{}, 0, Integer.decode(writeCycle), TimeUnit.MILLISECONDS);
        }
        */

        String flushSizeString = props.get(FLUSH_SIZE);
        if (flushSizeString != null) {
            this.flushSize = Integer.decode(flushSizeString);
        }

        try {
            this.hdfs = FileSystem.get( new URI( hdfsUrl ), configuration );
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectException("failed to connect to hdfs");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new ConfigException("bad hdfs uri");
        }



    }

    public final class Write implements Runnable{
        @Override
        public void run() {
            for (Map.Entry<String, List<ConcurrentLinkedQueue<SinkRecord>>> entry : topicQueueMap.entrySet()) {
                String topic = entry.getKey();

            }
        }
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        sameTopic(records);

        if (records != null) {
            for (SinkRecord sinkRecord : records) {
                String topic = sinkRecord.topic();
                List<ConcurrentLinkedQueue<SinkRecord>> partitionList = topicQueueMap.get(topic);
                int partition = sinkRecord.kafkaPartition();
                if (partition >= partitionList.size()) {
                    throw new ConfigException("larger than max partition");
                }
                ConcurrentLinkedQueue<SinkRecord> recordQueue = partitionList.get(partition);
                recordQueue.add(sinkRecord);
            }
        }

        for (Map.Entry<String, List<ConcurrentLinkedQueue<SinkRecord>>> entry : topicQueueMap.entrySet()) {
            String topic = entry.getKey();
            List<ConcurrentLinkedQueue<SinkRecord>> partitionList = topicQueueMap.get(topic);
            //see if each partition record reach
            int partitionNum = 0;
            for (ConcurrentLinkedQueue<SinkRecord> onePartition : partitionList) {
                if (onePartition.size() > flushSize) {
                    List<SinkRecord> list = new ArrayList<>();
                    //there is a large probability that the put method will not be called multi-threaded
                    double offsetStart = -1;
                    double offsetEnd = -1;

                    for (int i = 0; i < flushSize; i++) {
                        SinkRecord oneRecord = onePartition.poll();
                        if (oneRecord == null) {
                            log.error("get null from ConcurrentLinkedQueue");
                            break;
                        }
                        list.add(oneRecord);
                        if (offsetStart == -1) {
                            offsetStart = oneRecord.kafkaOffset();
                        }
                        offsetEnd = oneRecord.kafkaOffset();
                    }

                    if (offsetStart == -1 && offsetEnd == -1) {
                        log.error("offsetStart or offsetEnd is -1");
                    } else {
                        try {
                            writeToHdfs(topic, partitionNum, offsetStart, offsetEnd, objectMapper.writeValueAsBytes(list));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            log.error("objectmapper error");
                        }
                    }

                }

                partitionNum++;
            }
        }

    }

    public void writeToHdfs(String topic, int partition, double offsetStart, double offsetEnd, byte[] data) {
        String dir = this.hdfsUrl + this.topicsDir + "/" + topic + "/partition" + partition + "/";
        Path dirPath = new Path(dir);
        try {
            if (!this.hdfs.exists(dirPath)) {
                boolean createdDir = this.hdfs.mkdirs(dirPath, new FsPermission((short)777));
                log.info("create dir ? {}", createdDir);
            }
            Path file = new Path(dir + topic + "-" + offsetStart + "-" + offsetEnd + ".json");
            OutputStream os = hdfs.create(file);
            os.write(data);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("error writing {} {} offset {} to {}", topic, partition, offsetStart, offsetEnd);
        }
    }

    @Override
    public void stop() {
        try {
            hdfs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean sameTopic(Collection<SinkRecord> records) {
        if (records == null || records.size() == 0)
            return true;
        SinkRecord first = null;

        Iterator<SinkRecord> iter = records.iterator();
        if (iter.hasNext()) {
            if (first == null) {
                first = iter.next();
            } else {
                SinkRecord cur = iter.next();
                if (first.topic() != cur.topic()) {
                    return false;
                }
            }
        }
        log.info("they all have the same topic");
        return true;
    }


    public FileSystem getHdfs() {
        return hdfs;
    }

    public void setHdfs(FileSystem hdfs) {
        this.hdfs = hdfs;
    }

    public int getNumTopics() {
        return numTopics;
    }

    public void setNumTopics(int numTopics) {
        this.numTopics = numTopics;
    }

    public ConcurrentHashMap<String, List<ConcurrentLinkedQueue<SinkRecord>>> getTopicQueueMap() {
        return topicQueueMap;
    }

    public void setTopicQueueMap(ConcurrentHashMap<String, List<ConcurrentLinkedQueue<SinkRecord>>> topicQueueMap) {
        this.topicQueueMap = topicQueueMap;
    }

    public String[] getTopics() {
        return topics;
    }

    public void setTopics(String[] topics) {
        this.topics = topics;
    }

    public int getFlushSize() {
        return flushSize;
    }

    public void setFlushSize(int flushSize) {
        this.flushSize = flushSize;
    }

    public String getTopicsDir() {
        return topicsDir;
    }

    public void setTopicsDir(String topicsDir) {
        this.topicsDir = topicsDir;
    }

    public int getMaxPartition() {
        return maxPartition;
    }

    public void setMaxPartition(int maxPartition) {
        this.maxPartition = maxPartition;
    }

    public String getHdfsUrl() {
        return hdfsUrl;
    }

    public void setHdfsUrl(String hdfsUrl) {
        this.hdfsUrl = hdfsUrl;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
