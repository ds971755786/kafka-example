package io.dongsheng.sink;

public class HdfsConf {
    public static final String CORE_SITE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>\n" +
            "<configuration>\n" +
            "    <property>\n" +
            "        <name>fs.defaultFS</name>\n" +
            "        <value>hdfs://enncloud-hadoop</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>hadoop.tmp.dir</name>\n" +
            "        <value>file:/mnt/tmp1/tmp,file:/mnt/tmp2/tmp,file:/mnt/tmp3/tmp,file:/mnt/tmp4/tmp</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>ha.zookeeper.quorum</name>\n" +
            "        <value>a-zookeeper1:2181,a-zookeeper2:2181,a-zookeeper3:2181</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>ha.zookeeper.parent-znode </name>\n" +
            "        <value>%s-hadoop-ha</value>\n" +
            "    </property>\n" +
            "</configuration>";

    public static final String HDFS_SITE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>\n" +
            "<configuration>\n" +
            "    <property>\n" +
            "        <name>dfs.nameservices</name>\n" +
            "        <value>enncloud-hadoop</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.ha.namenodes.enncloud-hadoop</name>\n" +
            "        <value>nn1,nn2</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.ha.namenode.id</name>\n" +
            "        <value>nn1</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.namenode.rpc-address.enncloud-hadoop.nn1</name>\n" +
            "        <value>%s-namenode1:8020</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.namenode.rpc-address.enncloud-hadoop.nn2</name>\n" +
            "        <value>%s-namenode2:8020</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.namenode.http-address.enncloud-hadoop.nn1</name>\n" +
            "        <value>%s-namenode1:50070</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.namenode.http-address.enncloud-hadoop.nn2</name>\n" +
            "        <value>%s-namenode2:50070</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.namenode.shared.edits.dir</name>\n" +
            "        <value>qjournal://%s-journalnode1:8485;%s-journalnode2:8485;%s-journalnode3:8485/enncloud-hadoop</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.client.failover.proxy.provider.enncloud-hadoop</name>\n" +
            "        <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.ha.fencing.methods</name>\n" +
            "        <value>shell(/bin/true)</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.journalnode.edits.dir</name>\n" +
            "        <value>/hdfs/journal</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.ha.automatic-failover.enabled</name>\n" +
            "        <value>true</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.namenode.name.dir</name>\n" +
            "        <value>/hdfs/dfs/name</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.datanode.data.dir</name>\n" +
            "        <value>file:/mnt/data1/data,file:/mnt/data2/data,file:/mnt/data3/data,file:/mnt/data4/data</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.webhdfs.enabled</name>\n" +
            "        <value>true</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.namenode.datanode.registration.ip-hostname-check</name>\n" +
            "        <value>true</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.datanode.failed.volumes.tolerated</name>\n" +
            "        <value>1</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.replication</name>\n" +
            "        <value>3</value>\n" +
            "    </property>\n" +
            "    <property>\n" +
            "        <name>dfs.blocksize</name>\n" +
            "        <value>134217728</value>\n" +
            "    </property>\n" +
            "\n" +
            "</configuration>";


}
