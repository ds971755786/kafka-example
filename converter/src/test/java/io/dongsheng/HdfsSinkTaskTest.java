package io.dongsheng;

import org.codehaus.jackson.type.TypeReference;
import io.dongsheng.sink.HdfsSinkTask;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.sink.SinkRecord;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;

import static io.dongsheng.sink.HdfsConf.CORE_SITE_XML;
import static io.dongsheng.sink.HdfsConf.HDFS_SITE_XML;
import static io.dongsheng.sink.HdfsSinkConnector.HDFS_PREFIX;
import static io.dongsheng.sink.HdfsSinkConnector.TOPICS_DIR;

public class HdfsSinkTaskTest {
    @Test
    public void testWriteToHdfs() throws IOException {
        HdfsSinkTask hdfsSinkTask = new HdfsSinkTask();

        Map<String, String> props = new HashMap<>();
        props.put(TOPICS_DIR, "/connector/topics");
        props.put("topics", "test");

        hdfsSinkTask.start(props);

        hdfsSinkTask.writeToHdfs("test", 1, 1025, 2047, new ObjectMapper().writeValueAsBytes(props));
    }

    @Test
    public void testInit() {
        HdfsSinkTask hdfsSinkTask = new HdfsSinkTask();

        Map<String, String> props = new HashMap<>();
        props.put(TOPICS_DIR, "/connector/topics");
        props.put("topics", "test1,test2,test3");
        props.put(HDFS_PREFIX, "bdwt");

        hdfsSinkTask.start(props);

        hdfsSinkTask.initTempFileMeta(4);
        hdfsSinkTask.setFlushSize(8);
        hdfsSinkTask.setMaxPartition(4);
        hdfsSinkTask.setUnitTest(true);

        hdfsSinkTask.write(get(23, 3, 0));
    }

    //public SinkRecord(String topic, int partition, Schema keySchema, Object key, Schema valueSchema, Object value, long kafkaOffset,
    //                      Long timestamp, TimestampType timestampType)
    public List<SinkRecord> get(int l, int partitions, int start) {
        Random rand = new Random();
        String[] s = {"test1", "test2", "test3"};
        List<SinkRecord> records = new ArrayList<>(l);
        for (int i = start; i < l + start; i++) {
            records.add(new SinkRecord("test1", 0, null, i, null,
                    s[rand.nextInt(3)], i, System.currentTimeMillis(), TimestampType.LOG_APPEND_TIME));
            records.add(new SinkRecord("test1", 1, null, i, null,
                    s[rand.nextInt(3)], i, System.currentTimeMillis(), TimestampType.LOG_APPEND_TIME));
            records.add(new SinkRecord("test1", 2, null, i, null,
                    s[rand.nextInt(3)], i, System.currentTimeMillis(), TimestampType.LOG_APPEND_TIME));
            records.add(new SinkRecord("test1", 3, null, i, null,
                    s[rand.nextInt(3)], i, System.currentTimeMillis(), TimestampType.LOG_APPEND_TIME));
        }
        return records;
    }


    @Test
    public void testConf() {
        String coreSite = String.format(CORE_SITE_XML, "etyb");
        String hdfsSite = String.format(HDFS_SITE_XML, "etyb", "etyb", "etyb", "etyb", "etyb", "etyb", "etyb");
        System.out.println(coreSite);
        System.out.println(hdfsSite);
    }

    @Test
    public void testAppendFileNotExist() {
        String path = "test/resources/test.txt";
        try (FileOutputStream out = new FileOutputStream(path, false)) {
            out.write('a');
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDirExists() {
        Assert.assertEquals(true, Files.exists(FileSystems.getDefault().getPath("/tmp")));
    }

    @Test
    public void testArrayHashCode() {

    }

    @Test
    public void test() throws IOException {
        String path = "/home/dongsheng/work/java_projects/kafka-example/kafka-connect/good";
        File dir = new File(path);

        int notJson = 0;
        int missingRecords = 0;
        int not1024 = 0;
        int previous = 0;
        Set<String> l = new TreeSet<>();

        ObjectMapper mapper = new ObjectMapper();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File f : files) {
                previous = 0;
                byte[] data = Files.readAllBytes(FileSystems.getDefault().getPath(f.getAbsolutePath()));
                try {
                    List<Map<String, Object>> list = mapper.readValue(data, new My());
                    int c = 0;
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, Object> m = list.get(i);
                        c++;
                        int key = (int) m.get("offset");

                        //if (previous != 0) {
                            if (key - previous != 1 && i != 0) {
                                missingRecords++;
                                System.out.printf("%s %d %d\n", f.getName(), previous, key);
                            }
                            previous = key;
                        //
                    }
                    if (c != 1024) not1024++;
                } catch (IOException e) {
                    notJson++;
                    l.add(f.getName());
                }

            }
        }

        //Assert.assertEquals(0, notJson);
        System.out.println(l.size());
        for (String a : l) {
            System.out.println(a);
        }
        Assert.assertEquals(0, missingRecords);
        Assert.assertEquals(0, not1024);
    }

    public class My extends TypeReference<List<Map<String, Object>>> {
    }

}