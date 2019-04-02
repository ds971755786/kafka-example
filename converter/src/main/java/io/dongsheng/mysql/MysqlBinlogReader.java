package io.dongsheng.mysql;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.github.shyiko.mysql.binlog.event.EventType.*;

public class MysqlBinlogReader {
    //private static final Logger log = LoggerFactory.getLogger(MysqlBinlogReader.class);
    public static boolean isIt(Event event) {
        EventType eventType = event.getHeader().getEventType();
        if (eventType == EXT_WRITE_ROWS || eventType == EXT_UPDATE_ROWS || eventType == EXT_DELETE_ROWS || eventType == TABLE_MAP) {
            return true;
        }
        return false;
    }

    public static void print(Event event) {
        EventData data = event.getData();
        if (data instanceof WriteRowsEventData) {
            WriteRowsEventData write = (WriteRowsEventData) data;
            System.out.println(string(write.getRows()));
        } else if (data instanceof UpdateRowsEventData) {
            UpdateRowsEventData update = (UpdateRowsEventData) data;
            System.out.println(update(update.getRows()));
        } else if (data instanceof DeleteRowsEventData) {
            DeleteRowsEventData delete = (DeleteRowsEventData) data;
            System.out.println(string(delete.getRows()));
        }
    }

    public static String update(List<Map.Entry<Serializable[], Serializable[]>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        if (rows != null) {
            for (Map.Entry<Serializable[], Serializable[]> i : rows) {
                System.out.println(listSerializable(i.getKey()));
                System.out.println(listSerializable(i.getValue()));
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public static String listSerializable(Serializable[] rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        if (rows != null) {
            for (Object i : rows) {
                Object tmp;
                if (i instanceof byte[]) {
                    tmp = new String((byte[])i);
                } else {
                    tmp = i;
                }
                if (first) {
                    sb.append(tmp);
                    first = false;
                } else {
                    sb.append(", ").append(tmp);
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public static String deser(Object s) {
        if (s instanceof byte[]) {
            return new String((byte[])s);
        } else {
            return s.toString();
        }
    }

    public static String string(List<Serializable[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        if (rows != null) {
            for (Serializable[] i : rows) {
                System.out.println(listSerializable(i));
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public static void main(String[] args) {
        BinaryLogClient client = new BinaryLogClient("10.19.140.200", 29746, "root", "root");
        client.setBinlogPosition(8927);
        EventDeserializer eventDeserializer = new EventDeserializer();
        eventDeserializer.setCompatibilityMode(
                EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
                EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
        );
        client.setEventDeserializer(eventDeserializer);
        client.registerEventListener(new BinaryLogClient.EventListener() {
            @Override
            public void onEvent(Event event) {
                if (isIt(event)) {
                    //System.out.println(((EventHeaderV4)event.getHeader()).getNextPosition());
                    System.out.println(event.toString());
                    print(event);
                }
            }
        });
        try {
            client.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
}
