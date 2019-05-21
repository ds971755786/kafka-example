package io.dongsheng.flink.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import enn.monitor.framework.metrics.kubelet.proto.ContainerInfo;
import enn.monitor.streaming.common.proto.Metric;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestMetricsConvertor {
    @Test
    public void test() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileReader fileReader = new FileReader("src/main/resources/metrics.jsons");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String oneLine = bufferedReader.readLine();
        List<Metric> metrics = new ArrayList<>();
        while (oneLine != null) {
            ContainerInfo info = gson.fromJson(oneLine, ContainerInfo.class);
            metrics.addAll(MetricsConvertor.convertor(info, "test-token"));
            oneLine = bufferedReader.readLine();
        }

        FileOutputStream ou = new FileOutputStream("src/main/resources/metrics.json");
        ou.write(gson.toJson(metrics).getBytes());
        ou.close();
    }
}
