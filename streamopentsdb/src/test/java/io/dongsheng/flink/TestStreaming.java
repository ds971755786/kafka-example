package io.dongsheng.flink;

import com.google.gson.Gson;
import enn.monitor.framework.metrics.kubelet.proto.ContainerInfo;
import enn.monitor.streaming.common.proto.Metric;
import io.dongsheng.flink.utils.MetricsConvertor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TestStreaming {
    private static Logger logger = LoggerFactory.getLogger(TestStreaming.class);
    @Test
    public void test() {
        Gson gson = new Gson();
        String a = "{\"timestamp\":\"2019-05-22T22:25:03.576758722-04:00\",\"machine_name\":\"ennew-ceph-144\",\"hostIp\":\"10.19.137.144\",\"container_Name\":\"/system.slice/rhel-dmesg.service\",\"container_stats\":{\"timestamp\":\"2019-05-22T22:25:03.576747007-04:00\",\"cpu\":{\"usage\":{\"total\":0,\"user\":0,\"system\":0},\"cfs\":{\"periods\":0,\"throttled_periods\":0,\"throttled_time\":0},\"schedstat\":{\"run_time\":0,\"runqueue_time\":0,\"run_periods\":0},\"load_average\":0},\"diskio\":{},\"memory\":{\"usage\":0,\"max_usage\":0,\"cache\":0,\"rss\":0,\"swap\":0,\"mapped_file\":0,\"working_set\":0,\"failcnt\":0,\"container_data\":{\"pgfault\":0,\"pgmajfault\":0},\"hierarchical_data\":{\"pgfault\":0,\"pgmajfault\":0}},\"network\":{\"name\":\"\",\"rx_bytes\":0,\"rx_packets\":0,\"rx_errors\":0,\"rx_dropped\":0,\"tx_bytes\":0,\"tx_packets\":0,\"tx_errors\":0,\"tx_dropped\":0,\"tcp\":{\"Established\":0,\"SynSent\":0,\"SynRecv\":0,\"FinWait1\":0,\"FinWait2\":0,\"TimeWait\":0,\"Close\":0,\"CloseWait\":0,\"LastAck\":0,\"Listen\":0,\"Closing\":0},\"tcp6\":{\"Established\":0,\"SynSent\":0,\"SynRecv\":0,\"FinWait1\":0,\"FinWait2\":0,\"TimeWait\":0,\"Close\":0,\"CloseWait\":0,\"LastAck\":0,\"Listen\":0,\"Closing\":0},\"udp\":{\"Listen\":0,\"Dropped\":0,\"RxQueued\":0,\"TxQueued\":0},\"udp6\":{\"Listen\":0,\"Dropped\":0,\"RxQueued\":0,\"TxQueued\":0}},\"task_stats\":{\"nr_sleeping\":0,\"nr_running\":0,\"nr_stopped\":0,\"nr_uninterruptible\":0,\"nr_io_wait\":0},\"processes\":{\"process_count\":0,\"fd_count\":0},\"diskquota\":{\"numofquotadisk\":0,\"capacity\":0,\"curusesize\":0,\"curquotasize\":0,\"avaliablesize\":\"\",\"diskstatus\":[]},\"clustername\":\"shanghai\"}}";
        ContainerInfo info = gson.fromJson(a, ContainerInfo.class);
        List<Metric> list = MetricsConvertor.convertor(info, "test-token");
        //System.out.println(gson.toJson(list));
        list.stream().forEach(m->System.out.println(gson.toJson(m)));
    }
}
