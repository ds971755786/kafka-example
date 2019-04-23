package io.dongsheng.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.*;
import java.util.concurrent.*;

import static io.dongsheng.sink.HdfsConf.CORE_SITE_XML;
import static io.dongsheng.sink.HdfsConf.HDFS_SITE_XML;
import static io.dongsheng.sink.HdfsSinkConnector.*;
import static java.nio.file.StandardOpenOption.*;

public class HdfsSinkTask extends SinkTask {
    /**
     */
    private static final Logger log = LoggerFactory.getLogger(HdfsSinkTask.class);
    private FileSystem hdfs;
    private int numTopics;
    private ConcurrentHashMap<String, List<ConcurrentLinkedQueue<SinkRecord>>> topicQueueMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<TempFileMeta>> topicPartitionTempFileMeta = new ConcurrentHashMap<>();
    private String[] topics;
    private int flushSize = 1024;
    private String topicsDir;
    private int maxPartition = 16;
    private String hdfsPrefix;
    private String fileExtension = ".json";
    private String tempDir = "/tmp";
    private String metricsFile = "/tmp/metrics";
    private static final byte COMMA = ',';
    private static final byte LEFT_BRACKET = '[';
    private static final byte RIGHT_BRACKET = ']';
    private static final byte NEW_LINE = '\n';
    private boolean unitTest = false;

    private ScheduledExecutorService scheduledExecutorService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private SinkRecordSerializer sinkRecordSerializer = new SinkRecordJsonSerializer();
    private ConcurrentLinkedQueue<Metrics> metricsQueue = new ConcurrentLinkedQueue<>();

    public HdfsSinkTask() {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        this.scheduledExecutorService.scheduleAtFixedRate(()->{
            int size = this.metricsQueue.size();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                for (int i = 0; i < size; i++) {
                    Metrics m = this.metricsQueue.poll();
                    if (m == null) {
                        break;
                    }
                    outputStream.write(this.objectMapper.writeValueAsBytes(m));
                    outputStream.write(NEW_LINE);
                }
                byte[] data = outputStream.toByteArray();
                if (Files.exists(FileSystems.getDefault().getPath(this.metricsFile))) {
                    Files.write(FileSystems.getDefault().getPath(this.metricsFile), data, APPEND);
                } else {
                    Files.write(FileSystems.getDefault().getPath(this.metricsFile), data, CREATE);
                }
            } catch (IOException e) {
                log.error("write metrics to file error : {}", e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public String getFileNamePrefix(String topic, Integer partition) {
        return topic + '-' + "partition" + partition.toString();
    }

    public String getFileName(String prefix, Integer fileNum, Integer recordCount) {
        Integer start = fileNum * this.flushSize;
        Integer end = start + recordCount - 1;
        return prefix + '-' + start + '-' + end + this.fileExtension;
    }

    public void initTempFileMeta(int maxPartition) {
        for (String topic : this.topics) {
            List<TempFileMeta> partitionsTempFileMeta = new ArrayList<>(maxPartition);
            String topicDir = getTopicDir(topic);
            if (Files.exists(FileSystems.getDefault().getPath(topicDir))) {
                for (int i = 0; i < maxPartition; i++) {
                    String tempDir = getTempDir(topic, i);
                    if (Files.exists(FileSystems.getDefault().getPath(tempDir))) {
                        String metaFileName = tempDir + TempFileMeta.metaFileName(getFileNamePrefix(topic, i));
                        if (Files.exists(FileSystems.getDefault().getPath(metaFileName))) {
                            try {
                                TempFileMeta tempFileMeta = this.objectMapper.readValue(Files.readAllBytes(FileSystems.getDefault().getPath(metaFileName)), TempFileMeta.class);
                                partitionsTempFileMeta.add(tempFileMeta);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            partitionsTempFileMeta.add(new TempFileMeta(getFileNamePrefix(topic, i), 0, 0));
                        }
                    } else {
                        partitionsTempFileMeta.add(new TempFileMeta(getFileNamePrefix(topic, i), 0, 0));
                        try {
                            Files.createDirectories(FileSystems.getDefault().getPath(tempDir));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                try {
                    Files.createDirectories(FileSystems.getDefault().getPath(topicDir));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < maxPartition; i++) {
                    partitionsTempFileMeta.add(new TempFileMeta(getFileNamePrefix(topic, i), 0, 0));
                    try {
                        String tempDir = getTempDir(topic, i);
                        Files.createDirectories(FileSystems.getDefault().getPath(tempDir));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            this.topicPartitionTempFileMeta.put(topic, partitionsTempFileMeta);
        }
    }

    public ConcurrentHashMap<String, List<TempFileMeta>> getTopicPartitionTempFileMeta() {
        return this.topicPartitionTempFileMeta;
    }

    /**
     *
     * @param path
     * @param tempFileMeta
     * @return true if append
     */
    public boolean appendOrNew(java.nio.file.Path path, TempFileMeta tempFileMeta) {
        if (Files.exists(path)) {
            if (tempFileMeta.getRecordCount() == 0) {
                this.log.error("temp file exists but recordCount in memory is 0");
            }
            return true;
        } else {
            if (tempFileMeta.getRecordCount() > 0) {
                this.log.error("temp file not there but recordCount in memory is not 0");
            }
            return false;
        }
    }

    public String toHdfsPath(String p) {
        return "file://" + p;
    }

    public void write(Collection<SinkRecord> records) {
        HashMap<String, List<List<SinkRecord>>> topicPartitionListRecordsMap = new HashMap<>(this.topics.length);

        for (SinkRecord oneRecord : records) {
            String topic = oneRecord.topic();
            List<List<SinkRecord>> partitions = topicPartitionListRecordsMap.get(topic);
            if (partitions == null) {
                partitions = new ArrayList<>(this.maxPartition);
                for (int i = 0; i < this.maxPartition; i++) {
                    partitions.add(new ArrayList<>());
                }
                topicPartitionListRecordsMap.put(topic, partitions);
            }
            List<SinkRecord> recordsOfOnePartition = partitions.get(oneRecord.kafkaPartition());
            recordsOfOnePartition.add(oneRecord);
        }
        for (Map.Entry<String, List<List<SinkRecord>>> entry : topicPartitionListRecordsMap.entrySet()) {
            for (int i = 0; i < maxPartition; i++) {
                writeToTemp(entry.getKey(), i, this.topicPartitionTempFileMeta.get(entry.getKey()).get(i), entry.getValue().get(i));
            }
        }
    }

    public void writeToTemp(String topic, int partition, TempFileMeta tempFileMeta, List<SinkRecord> sinkRecords) {
        long s = System.nanoTime();
        if (sinkRecords == null || sinkRecords.isEmpty()) {
            return;
        }
        // if current temp file has flush size elements, use new one
        if (tempFileMeta.getRecordCount() == this.flushSize) {
            tempFileMeta.incFileNum();
            tempFileMeta.setRecordCount(0);
        }

        String fileName = getFileName(tempFileMeta.getFileNamePrefix(), tempFileMeta.getFileNum(), this.flushSize);
        String tempDir = getTempDir(topic, partition);
        String tempPath = tempDir + fileName;
        java.nio.file.Path path = java.nio.file.FileSystems.getDefault().getPath(tempPath);
        if (appendOrNew(path, tempFileMeta)) {
            int recordCount = tempFileMeta.getRecordCount();
            try {
                // what happens if we append a file which not exists
                FileOutputStream out = new FileOutputStream(tempPath, true);

                // record count reach not flush size yet
                if (this.flushSize > sinkRecords.size() + recordCount) {
                    for (SinkRecord record : sinkRecords) {
                        out.write(this.sinkRecordSerializer.serialize(record));
                        out.write(COMMA);
                    }
                    updateFileMeta(tempFileMeta, tempFileMeta.getFileNum(), tempFileMeta.getRecordCount() + sinkRecords.size(), tempDir);
                } else {
                    int startIndex = 0;
                    for (; recordCount + startIndex < this.flushSize; startIndex++) {
                        out.write(this.sinkRecordSerializer.serialize(sinkRecords.get(startIndex)));
                        if (recordCount + startIndex != this.flushSize - 1) {
                            out.write(COMMA);
                        }
                    }
                    out.write(RIGHT_BRACKET);
                    out.close();


                    //update meta file after write temp data file
                    tempFileMeta.setRecordCount(recordCount + startIndex);
                    java.nio.file.Path metaPath = java.nio.file.FileSystems.getDefault().getPath(tempDir + tempFileMeta.getMetaFileName());
                    Files.write(metaPath, this.objectMapper.writeValueAsBytes(tempFileMeta), TRUNCATE_EXISTING);
                    updateFileMeta(tempFileMeta, tempFileMeta.getFileNum() + 1, 0, tempDir);
                    copyToHdfs(tempPath, topic, partition, fileName);

                    // startIndex is the num of record which has been put in file
                    while (sinkRecords.size() - startIndex >= this.flushSize) {
                        writeOneTempFile(topic, partition, tempDir, tempFileMeta, sinkRecords, startIndex, this.flushSize);
                        startIndex += this.flushSize;
                    }

                    //write tail
                    writePartTempFile(tempDir, tempFileMeta, sinkRecords, startIndex, sinkRecords.size() - startIndex);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (this.flushSize > sinkRecords.size() + tempFileMeta.getRecordCount()) {
                writePartTempFile(tempDir, tempFileMeta, sinkRecords, 0, sinkRecords.size());
            } else {
                int startIndex = 0;
                while (sinkRecords.size() - startIndex >= this.flushSize ) {
                    writeOneTempFile(topic, partition, tempDir, tempFileMeta, sinkRecords, startIndex, this.flushSize);
                    startIndex += this.flushSize;
                }

                //write tail
                writePartTempFile(tempDir, tempFileMeta, sinkRecords, startIndex, sinkRecords.size() - startIndex);
            }

        }
        long t = System.nanoTime();
        metricsQueue.offer(new Metrics(sinkRecords.size(), t - s, Metrics.Type.WHOLE));
    }

    public void copyToHdfs(String tempPath, String topic, int partition, String fileName) throws IOException {
        if (!this.unitTest) {
            long s = System.nanoTime();
            hdfs.copyFromLocalFile(false, true, new Path(toHdfsPath(tempPath)), new Path(getDir(topic, partition) + fileName));
            long t = System.nanoTime();
            metricsQueue.offer(new Metrics(this.flushSize, t - s, Metrics.Type.HDFS));
        }
    }

    public void writeRecoveryFile(String tempDir, TempFileMeta tempFileMeta, List<SinkRecord> sinkRecords, int startIndex, int count) {
        List<Long> offsets = new ArrayList<>(count);
        for( int i = 0; i < count; i++) {
            offsets.add(sinkRecords.get(startIndex + i).kafkaOffset());
        }
        TempFileRecovery tempFileRecovery = new TempFileRecovery(tempFileMeta.getFileNamePrefix(), tempFileMeta.getFileNum(), offsets);
        try {
            byte[] data = objectMapper.writeValueAsBytes(tempFileRecovery);
            java.nio.file.Path recoveryPath = java.nio.file.FileSystems.getDefault().getPath(tempDir + tempFileRecovery.getFileName());
            Files.write(recoveryPath, data, CREATE);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            log.error("failed to write temp file recovery");
            e.printStackTrace();
        }
    }

    //from beginning
    public void writePartTempFile(String tempDir, TempFileMeta tempFileMeta, List<SinkRecord> sinkRecords, int startIndex, int count) {
        long s = System.nanoTime();
        try {
            String pathString = tempDir + getFileName(tempFileMeta.getFileNamePrefix(), tempFileMeta.getFileNum(), this.flushSize);
            java.nio.file.Path path = java.nio.file.FileSystems.getDefault().getPath(pathString);
            Files.createFile(path);
            FileOutputStream out = new FileOutputStream(pathString, false);
            out.write(LEFT_BRACKET);
            for (int i = 0; i < count; i++) {
                out.write(this.sinkRecordSerializer.serialize(sinkRecords.get(startIndex + i)));
                out.write(COMMA);
            }
            out.close();

            updateFileMeta(tempFileMeta, tempFileMeta.getFileNum(), tempFileMeta.getRecordCount() + count, tempDir);
        } catch (IOException e) {
            writeRecoveryFile(tempDir, tempFileMeta, sinkRecords, startIndex, count);
            e.printStackTrace();
        }
        long t = System.nanoTime();
        metricsQueue.offer(new Metrics(count, t - s, Metrics.Type.FS));
    }

    public void writeOneTempFile(String topic, int partition, String tempDir, TempFileMeta tempFileMeta, List<SinkRecord> sinkRecords, int startIndex, int count) {
        long s = System.nanoTime();
        java.nio.file.Path metaPath = java.nio.file.FileSystems.getDefault().getPath(tempDir + tempFileMeta.getMetaFileName());
        try {
            Files.write(metaPath, this.objectMapper.writeValueAsBytes(tempFileMeta), CREATE);

            String fileName = getFileName(tempFileMeta.getFileNamePrefix(), tempFileMeta.getFileNum(), this.flushSize);
            String tempPath = tempDir + fileName;
            java.nio.file.Path path = java.nio.file.FileSystems.getDefault().getPath(tempPath);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                this.log.error("file already exists");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(LEFT_BRACKET);
            for (int i = 0; i < count; i++) {
                outputStream.write(this.sinkRecordSerializer.serialize(sinkRecords.get(startIndex + i)));
                if (i != count - 1) {
                    outputStream.write(COMMA);
                }
            }
            outputStream.write(RIGHT_BRACKET);
            //write temp file
            Files.write(path, outputStream.toByteArray(), CREATE);

            updateFileMeta(tempFileMeta, tempFileMeta.getFileNum() + 1, 0, tempDir);
            copyToHdfs(tempPath, topic, partition, fileName);
        } catch (IOException e) {
            writeRecoveryFile(tempDir, tempFileMeta, sinkRecords, startIndex, count);
            e.printStackTrace();
        }
        long t = System.nanoTime();
        metricsQueue.offer(new Metrics(count, t - s, Metrics.Type.FS));
    }

    //both in memory and in file
    public void updateFileMeta(TempFileMeta tempFileMeta, int fileNum, int recordCount, String tempDir) {
        tempFileMeta.setFileNum(fileNum);
        tempFileMeta.setRecordCount(recordCount);
        java.nio.file.Path metaPath = java.nio.file.FileSystems.getDefault().getPath(tempDir + tempFileMeta.getMetaFileName());
        try {
            Files.write(metaPath, this.objectMapper.writeValueAsBytes(tempFileMeta), CREATE);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String version() {
        return "0.1";
    }

    public void initTopicQueueMap(int maxPartition) {
        for (String topic : this.topics) {
            List<ConcurrentLinkedQueue<SinkRecord>> l = this.topicQueueMap.get(topic);
            for (int i = 0; i < maxPartition; i++) {
                l.add(new ConcurrentLinkedQueue());
            }
        }
    }

    @Override
    public void start(Map<String, String> props) {
        Configuration configuration = new Configuration();
        this.hdfsPrefix = props.get(HDFS_PREFIX);
        if (this.hdfsPrefix == null) {
            throw new ConfigException("missing hdfs prifix");
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

        String ext = props.get(FILE_EXTENSION);
        if (ext != null) {
            this.fileExtension = ext;
        }
        String tempDataDir = props.get(TEMP_DATA_DIRECTORY);
        if (tempDataDir != null) {
            this.tempDir = tempDataDir;
        }

        try {
            String coreSite = String.format(CORE_SITE_XML, this.hdfsPrefix);
            String hdfsSite = String.format(HDFS_SITE_XML, this.hdfsPrefix, this.hdfsPrefix, this.hdfsPrefix, this.hdfsPrefix, this.hdfsPrefix, this.hdfsPrefix, this.hdfsPrefix);
            configuration.addResource(new ByteArrayInputStream(coreSite.getBytes()));
            configuration.addResource(new ByteArrayInputStream(hdfsSite.getBytes()));
            this.hdfs = FileSystem.get(configuration );
            System.out.println("hdfs uri is " + FileSystem.getDefaultUri(configuration));
            initTempFileMeta(this.maxPartition);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectException("failed to connect to hdfs");
        }
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);


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
        write(records);
    }

    public void put1(Collection<SinkRecord> records) {
        sameTopic(records);

        if (records != null) {
            for (SinkRecord sinkRecord : records) {
                String topic = sinkRecord.topic();
                List<ConcurrentLinkedQueue<SinkRecord>> partitionList = this.topicQueueMap.get(topic);
                int partition = sinkRecord.kafkaPartition();
                if (partition >= partitionList.size()) {
                    throw new ConfigException("larger than max partition");
                }
                ConcurrentLinkedQueue<SinkRecord> recordQueue = partitionList.get(partition);
                recordQueue.add(sinkRecord);
            }
        }

        for (Map.Entry<String, List<ConcurrentLinkedQueue<SinkRecord>>> entry : this.topicQueueMap.entrySet()) {
            String topic = entry.getKey();
            List<ConcurrentLinkedQueue<SinkRecord>> partitionList = this.topicQueueMap.get(topic);
            //see if each partition record reach
            int partitionNum = 0;
            for (ConcurrentLinkedQueue<SinkRecord> onePartition : partitionList) {
                if (onePartition.size() > this.flushSize) {
                    List<SinkRecord> list = new ArrayList<>();
                    //there is a large probability that the put method will not be called multi-threaded
                    double offsetStart = -1;
                    double offsetEnd = -1;

                    for (int i = 0; i < this.flushSize; i++) {
                        SinkRecord oneRecord = onePartition.poll();
                        if (oneRecord == null) {
                            this.log.error("get null from ConcurrentLinkedQueue");
                            break;
                        }
                        list.add(oneRecord);
                        if (offsetStart == -1) {
                            offsetStart = oneRecord.kafkaOffset();
                        }
                        offsetEnd = oneRecord.kafkaOffset();
                    }

                    if (offsetStart == -1 && offsetEnd == -1) {
                        this.log.error("offsetStart or offsetEnd is -1");
                    } else {
                        byte[] data = toBytes(list);
                        if (data != null) {
                            writeToHdfs(topic, partitionNum, offsetStart, offsetEnd, data);
                        }
                    }

                }

                partitionNum++;
            }
        }
    }

    public byte[] toBytes(List<SinkRecord> list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (SinkRecord sinkRecord : list) {
            sb.append(sinkRecord.toString());
            sb.append("\n");
        }
        return sb.toString().getBytes();
    }

    public String getTopicDir(String topic) {
        return this.tempDir + '/' + topic;
    }

    public String getTempDir(String topic, int partition) {
        return this.tempDir + '/' + topic + "/partition" + partition + "/";
    }

    public String getDir(String topic, int partition) {
        return this.topicsDir + "/" + topic + "/partition" + partition + "/";
    }

    public void writeToHdfs(String topic, int partition, double offsetStart, double offsetEnd, byte[] data) {
        String dir = getDir(topic, partition);
        Path dirPath = new Path(dir);
        try {
            if (!this.hdfs.exists(dirPath)) {
                boolean createdDir = this.hdfs.mkdirs(dirPath, new FsPermission((short)777));
                log.info("create dir ? {}", createdDir);
            }
            Path file = new Path(dir + topic + "-" + offsetStart + "-" + offsetEnd + ".json");
            if (this.hdfs.exists(file)) {
                log.error("file {} already there", file.getName());
            }

            OutputStream os = this.hdfs.create(file);
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
            this.hdfs.close();
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
                    this.log.info("they have different topics");
                    return false;
                }
            }
        }
        //log.info("they all have the same topic");
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

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public void setUnitTest(boolean unitTest) {
        this.unitTest = unitTest;
    }

    public void setHdfsPrefix(String hdfsPrefix) {
        this.hdfsPrefix = hdfsPrefix;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }
}
