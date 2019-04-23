package io.dongsheng.sink;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevolvingDoor {
    private Metrics[] first;
    private Metrics[] second;
    private Metrics[] cur;
    private int size;
    private int toWrite;
    private ExecutorService es;

    public RevolvingDoor(int size) {
        this.size = size;
        this.toWrite = 0;

        this.first = new Metrics[this.size];
        this.second = new Metrics[this.size];
        this.cur = first;
        es = Executors.newFixedThreadPool(1);
    }

    public void add(Metrics metrics) {

    }







}
