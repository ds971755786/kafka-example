package io.dongsheng.sink;

public class Metrics {
    private int numRecords;
    private long timeInNs;
    private Type type;

    public Metrics(){}

    public Metrics(int numRecords, long timeInNs, Type type) {
        this.numRecords = numRecords;
        this.timeInNs = timeInNs;
        this.type = type;
    }

    public static enum Type {
        HDFS, FS, WHOLE
    }

    public int getNumRecords() {
        return numRecords;
    }

    public void setNumRecords(int numRecords) {
        this.numRecords = numRecords;
    }

    public long getTimeInNs() {
        return timeInNs;
    }

    public void setTimeInNs(long timeInNs) {
        this.timeInNs = timeInNs;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
