package com.taobao.metamorphosis.server.utils;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import com.googlecode.aviator.AviatorEvaluator;
import com.taobao.diamond.common.Constants;
import com.taobao.metamorphosis.server.exception.MetamorphosisServerStartupException;
import com.taobao.metamorphosis.utils.DiamondUtils;
import com.taobao.metamorphosis.utils.MetaMBeanServer;
import com.taobao.metamorphosis.utils.ZkUtils.ZKConfig;


/**
 * 服务器端配置
 * 
 * @author boyan
 * @Date 2011-4-21
 * @author wuhua
 */
public class MetaConfig implements Serializable, MetaConfigMBean {
    static final long serialVersionUID = -1L;
    private int brokerId = 0;
    private String dataPath = System.getProperty("user.home") + File.separator + "meta";
    private int serverPort = 8123;
    private String hostName;
    private int numPartitions = 1;
    private int unflushThreshold = 1000;
    private int unflushInterval = 10000;
    private int maxSegmentSize = 1 * 1024 * 1024 * 1024;
    private int maxTransferSize = 1024 * 1024;
    // slave编号,大于等于0表示作为slave启动
    private int slaveId = -1;
    // 作为slave启动时向master订阅消息的group,如果没配置则默认为meta-slave-group
    private String slaveGroup = "meta-slave-group";
    // slave数据同步的最大延时,单位毫秒
    private long slaveMaxDelayInMills = 500;

    private List<String> topics = new ArrayList<String>();

    // private Map<String/* topic */, Integer> topicPartitions = new
    // HashMap<String, Integer>();

    private int getProcessThreadCount = 10 * Runtime.getRuntime().availableProcessors();

    private int putProcessThreadCount = 10 * Runtime.getRuntime().availableProcessors();

    private ZKConfig zkConfig;

    private String diamondZKDataId = DiamondUtils.DEFAULT_ZK_DATAID;
    private String diamondZKGroup = Constants.DEFAULT_GROUP;

    // 文件删除策略:"策略名称,设定值列表"，默认为保存7天
    private String deletePolicy = "delete,168";

    private final Map<String/* topic */, TopicConfig> topicConfigMap = new CopyOnWriteMap<String, TopicConfig>();

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * 需要统计的topic列表，在此列表才会做实时统计，支持通配符*
     */
    private Set<String> statTopicSet = new TreeSet<String>();

    private long lastModified = -1;

    private String path;

    // 事务相关配置
    // 最大保存的checkpoint数目，超过将淘汰最老的
    private int maxCheckpoints = 3;

    // 自动checkpoint间隔，默认1小时
    private long checkpointInterval = 60 * 60 * 1000L;

    // 最大事务超时时间个数，默认3万个
    private int maxTxTimeoutTimerCapacity = 30000;
    // 事务日志刷盘设置，0表示让操作系统决定，1表示每次commit都刷盘，2表示每隔一秒刷盘一次
    private int flushTxLogAtCommit = 1;

    // 事务最大超时时间，默认一分钟
    private int maxTxTimeoutInSeconds = 60;

    // 日志存储目录，默认使用dataPath
    private String dataLogPath = this.dataPath;

    /**
     * 全局的删除crontab表达式，每天早上6点和晚上6点执行
     */
    private String deleteWhen = "0 0 6,18 * * ?";

    /**
     * quartz使用的线程池大小
     */
    private int quartzThreadCount = 5;


    public int getQuartzThreadCount() {
        return this.quartzThreadCount;
    }


    public void setQuartzThreadCount(final int quartzThreadCount) {
        this.quartzThreadCount = quartzThreadCount;
    }


    public int getMaxTxTimeoutTimerCapacity() {
        return this.maxTxTimeoutTimerCapacity;
    }


    public String getDeleteWhen() {
        return this.deleteWhen;
    }


    public void setDeleteWhen(final String deleteWhen) {
        this.deleteWhen = deleteWhen;
    }


    public void setMaxTxTimeoutTimerCapacity(final int maxTxTimeoutTimerCapacity) {
        this.maxTxTimeoutTimerCapacity = maxTxTimeoutTimerCapacity;
    }


    public int getMaxTxTimeoutInSeconds() {
        return this.maxTxTimeoutInSeconds;
    }


    public void setMaxTxTimeoutInSeconds(final int maxTxTimeoutInSeconds) {
        this.maxTxTimeoutInSeconds = maxTxTimeoutInSeconds;
    }


    public int getFlushTxLogAtCommit() {
        return this.flushTxLogAtCommit;
    }


    public void setFlushTxLogAtCommit(final int flushTxLogAtCommit) {
        this.flushTxLogAtCommit = flushTxLogAtCommit;
    }


    public int getMaxCheckpoints() {
        return this.maxCheckpoints;
    }


    public long getCheckpointInterval() {
        return this.checkpointInterval;
    }


    public void setCheckpointInterval(final long checkpointInterval) {
        this.checkpointInterval = checkpointInterval;
    }


    public void setMaxCheckpoints(final int maxCheckpoints) {
        this.maxCheckpoints = maxCheckpoints;
    }


    public long getLastModified() {
        return this.lastModified;
    }


    public void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }


    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }


    public String getDiamondZKDataId() {
        return this.diamondZKDataId;
    }


    public void setDiamondZKDataId(final String diamondZKDataId) {
        this.diamondZKDataId = diamondZKDataId;
    }


    public void setSlaveGroup(final String slaveGroup) {
        this.slaveGroup = slaveGroup;
    }


    public String getDiamondZKGroup() {
        return this.diamondZKGroup;
    }


    public void setDiamondZKGroup(final String diamondZKGroup) {
        this.diamondZKGroup = diamondZKGroup;
    }


    public void loadRootConfig(final String path) {
        try {
            this.path = path;
            final File file = new File(path);
            if (!file.exists()) {
                throw new MetamorphosisServerStartupException("File " + path + " is not exists");
            }
            final Ini conf = new Ini(file);
            this.lastModified = file.lastModified();
            this.populateSystemConf(conf);
            this.populateZookeeperConfig(conf);
        }
        catch (final IOException e) {
            throw new MetamorphosisServerStartupException("Parse configuration failed,path=" + path, e);
        }

    }


    public void loadTopicConfig(final String path) {
        try {
            this.path = path;
            final File file = new File(path);
            if (!file.exists()) {
                throw new MetamorphosisServerStartupException("File " + path + " is not exists");
            }
            final Ini conf = new Ini(file);
            this.lastModified = file.lastModified();
            this.populateTopicsConfig(conf);
        }
        catch (final IOException e) {
            throw new MetamorphosisServerStartupException("Parse configuration failed,path=" + path, e);
        }
    }


    public String getDeletePolicy() {
        return this.deletePolicy;
    }


    public void setDeletePolicy(final String deletePolicy) {
        this.deletePolicy = deletePolicy;
    }


    public ZKConfig getZkConfig() {
        return this.zkConfig;
    }


    public List<String> getTopics() {
        return this.topics;
    }


    public void setTopics(final List<String> topics) {
        this.topics = topics;
    }


    public void setZkConfig(final ZKConfig zkConfig) {
        this.zkConfig = zkConfig;
    }


    public int getNumPartitions() {
        return this.numPartitions;
    }


    public void setNumPartitions(final int numPartitions) {
        this.numPartitions = numPartitions;
    }


    public int getBrokerId() {
        return this.brokerId;
    }


    public void setBrokerId(final int brokerId) {
        this.brokerId = brokerId;
    }


    public String getHostName() {
        return this.hostName;
    }


    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }


    private void populateTopicsConfig(final Ini conf) {
        final Set<String> set = conf.keySet();
        final Set<String> newStatTopics = new TreeSet<String>();
        final List<String> newTopics = new ArrayList<String>();
        boolean numPartitionsChanged = false;
        for (final String name : set) {
            // Is it a topic section?
            if (name != null && name.startsWith("topic=")) {
                final Section section = conf.get(name);
                final String topic = name.substring("topic=".length()).trim();

                final TopicConfig topicConfig = new TopicConfig(topic, this);

                if (StringUtils.isNotBlank(section.get("numPartitions"))) {
                    topicConfig.setNumPartitions(this.getInt(section, "numPartitions"));
                }

                final TopicConfig tmpTC = this.topicConfigMap.get(topic);
                if (tmpTC != null) {
                    numPartitionsChanged =
                            numPartitionsChanged || (topicConfig.getNumPartitions() > tmpTC.getNumPartitions());
                }

                boolean stat = false;
                if (StringUtils.isNotBlank(section.get("stat"))) {
                    stat = Boolean.valueOf(section.get("stat"));
                    if (stat) {
                        newStatTopics.add(topic);
                    }
                }
                if (StringUtils.isNotBlank(section.get("deletePolicy"))) {
                    topicConfig.setDeletePolicy(section.get("deletePolicy"));
                }

                if (StringUtils.isNotBlank(section.get("deleteWhen"))) {
                    topicConfig.setDeleteWhen(section.get("deleteWhen"));
                }

                if (StringUtils.isNotBlank(section.get("dataPath"))) {
                    topicConfig.setDataPath(section.get("dataPath"));
                }

                if (StringUtils.isNotBlank(section.get("unflushInterval"))) {
                    topicConfig.setUnflushInterval(this.getInt(section, "unflushInterval"));
                }

                if (StringUtils.isNotBlank(section.get("unflushThreshold"))) {
                    topicConfig.setUnflushThreshold(this.getInt(section, "unflushThreshold"));
                }

                // this.topicPartitions.put(topic, numPartitions);
                this.topicConfigMap.put(topic, topicConfig);
                newTopics.add(topic);
            }
        }
        Collections.sort(newTopics);
        // fire property change event
        if (!newStatTopics.equals(this.statTopicSet)) {
            this.statTopicSet = newStatTopics;
            this.propertyChangeSupport.firePropertyChange("statTopicSet", null, null);
        }
        if (!newTopics.equals(this.topics) || numPartitionsChanged) {
            this.topics = newTopics;
            this.propertyChangeSupport.firePropertyChange("topics", null, null);
        }

        this.propertyChangeSupport.firePropertyChange("unflushInterval", null, null);
    }


    private void populateZookeeperConfig(final Ini conf) {
        final Section zkConf = conf.get("zookeeper");
        if (StringUtils.isNotBlank(zkConf.get("diamondZKDataId"))) {
            this.diamondZKDataId = zkConf.get("diamondZKDataId");
        }
        if (StringUtils.isNotBlank(zkConf.get("diamondZKGroup"))) {
            this.diamondZKGroup = zkConf.get("diamondZKGroup");
        }
        if (!StringUtils.isBlank(zkConf.get("zk.zkConnect"))) {
            this.newZkConfigIfNull();
            this.zkConfig.zkConnect = zkConf.get("zk.zkConnect");
        }
        if (!StringUtils.isBlank(zkConf.get("zk.zkSessionTimeoutMs"))) {
            this.newZkConfigIfNull();
            this.zkConfig.zkSessionTimeoutMs = this.getInt(zkConf, "zk.zkSessionTimeoutMs");
        }
        if (!StringUtils.isBlank(zkConf.get("zk.zkConnectionTimeoutMs"))) {
            this.newZkConfigIfNull();
            this.zkConfig.zkConnectionTimeoutMs = this.getInt(zkConf, "zk.zkConnectionTimeoutMs");
        }
        if (!StringUtils.isBlank(zkConf.get("zk.zkSyncTimeMs"))) {
            this.newZkConfigIfNull();
            this.zkConfig.zkSyncTimeMs = this.getInt(zkConf, "zk.zkSyncTimeMs");
        }
        if (!StringUtils.isBlank(zkConf.get("zk.zkEnable"))) {
            this.newZkConfigIfNull();
            this.zkConfig.zkEnable = this.getBoolean(zkConf, "zk.zkEnable");
        }
        if (!StringUtils.isBlank(zkConf.get("zk.zkRoot"))) {
            this.newZkConfigIfNull();
            this.zkConfig.zkRoot = zkConf.get("zk.zkRoot");
        }
    }


    private int getInt(final Section section, final String key, final int defaultValue) {
        final String value = section.get(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        else {
            final Long rt = (Long) AviatorEvaluator.execute(value);
            return rt.intValue();
        }
    }


    private int getInt(final Section section, final String key) {
        final String value = section.get(key);
        if (StringUtils.isBlank(value)) {
            throw new NullPointerException("Blank value for " + key);
        }
        else {
            final Long rt = (Long) AviatorEvaluator.execute(value);
            return rt.intValue();
        }
    }


    private boolean getBoolean(final Section section, final String key) {
        final String value = section.get(key);
        if (StringUtils.isBlank(value)) {
            throw new NullPointerException("Blank value for " + key);
        }
        else {
            final Boolean rt = (Boolean) AviatorEvaluator.execute(value);
            return rt;
        }
    }


    private long getLong(final Section section, final String key) {
        final String value = section.get(key);
        if (StringUtils.isBlank(value)) {
            throw new NullPointerException("Blank value for " + key);
        }
        else {
            final Long rt = (Long) AviatorEvaluator.execute(value);
            return rt.longValue();
        }
    }


    private void populateSystemConf(final Ini conf) {
        final Section sysConf = conf.get("system");

        this.brokerId = this.getInt(sysConf, "brokerId");
        this.serverPort = this.getInt(sysConf, "serverPort", 8123);
        if (!StringUtils.isBlank(sysConf.get("hostName"))) {
            this.hostName = sysConf.get("hostName");
        }
        this.numPartitions = this.getInt(sysConf, "numPartitions");
        if (!StringUtils.isBlank(sysConf.get("getProcessThreadCount"))) {
            this.getProcessThreadCount = this.getInt(sysConf, "getProcessThreadCount");
        }
        if (!StringUtils.isBlank(sysConf.get("putProcessThreadCount"))) {
            this.putProcessThreadCount = this.getInt(sysConf, "putProcessThreadCount");
        }
        if (!StringUtils.isBlank(sysConf.get("quartzThreadCount"))) {
            this.quartzThreadCount = this.getInt(sysConf, "quartzThreadCount");
        }
    }


    /**
     * Reload topics configuration
     */
    @Override
    public void reload() {
        final File file = new File(MetaConfig.this.path);
        if (file.lastModified() != this.lastModified) {
            this.lastModified = file.lastModified();
            try {
                log.info("Reloading topics......");
                final Ini conf = new Ini(file);
                MetaConfig.this.populateTopicsConfig(conf);
                log.info("Reload topics successfully");
            }
            catch (final Exception e) {
                log.error("Reload config failed", e);
            }
        }
    }

    static final Log log = LogFactory.getLog(MetaConfig.class);


    public Set<String> getStatTopicSet() {
        return this.statTopicSet;
    }


    private void newZkConfigIfNull() {
        if (this.zkConfig == null) {
            this.zkConfig = new ZKConfig();
        }
    }


    public MetaConfig() {
        super();
        MetaMBeanServer.registMBean(this, null);
    }


    public int getGetProcessThreadCount() {
        return this.getProcessThreadCount;
    }


    public void setGetProcessThreadCount(final int getProcessThreadCount) {
        this.getProcessThreadCount = getProcessThreadCount;
    }


    public int getPutProcessThreadCount() {
        return this.putProcessThreadCount;
    }


    public void setPutProcessThreadCount(final int putProcessThreadCount) {
        this.putProcessThreadCount = putProcessThreadCount;
    }


    public int getServerPort() {
        return this.serverPort;
    }


    public void setUnflushInterval(final int unflushInterval) {
        this.unflushInterval = unflushInterval;
    }


    public int getMaxTransferSize() {
        return this.maxTransferSize;
    }


    public void setMaxTransferSize(final int maxTransferSize) {
        this.maxTransferSize = maxTransferSize;
    }


    public void setMaxSegmentSize(final int maxSegmentSize) {
        this.maxSegmentSize = maxSegmentSize;
    }


    public int getUnflushInterval() {
        return this.unflushInterval;
    }


    public int getMaxSegmentSize() {
        return this.maxSegmentSize;
    }


    public void setUnflushThreshold(final int storeFlushThreshold) {
        this.unflushThreshold = storeFlushThreshold;
    }


    public int getUnflushThreshold() {
        return this.unflushThreshold;
    }


    @Override
    public String toString() {
        return "MetaConfig [brokerId=" + this.brokerId + ", dataPath=" + this.dataPath + ", deletePolicy="
                + this.deletePolicy + ", diamondZKDataId=" + this.diamondZKDataId + ", diamondZKGroup="
                + this.diamondZKGroup + ", getProcessThreadCount=" + this.getProcessThreadCount + ", hostName="
                + this.hostName + ", maxSegmentSize=" + this.maxSegmentSize + ", maxTransferSize="
                + this.maxTransferSize + ", numPartitions=" + this.numPartitions + ", putProcessThreadCount="
                + this.putProcessThreadCount + ", serverPort=" + this.serverPort + ", slaveGroup="
                + this.slaveGroup + ", slaveId=" + this.slaveId + ", statTopicSet=" + this.statTopicSet
                + ", topicDeletePolicy=" + ", topics=" + this.topics + ", unflushInterval=" + this.unflushInterval
                + ", unflushThreshold=" + this.unflushThreshold + ", zkConfig=" + this.zkConfig + "]";
    }


    public void setServerPort(final int serverPort) {
        this.serverPort = serverPort;
    }


    /**
     * 校验配置是否正确
     */
    public void verify() {
        if (this.getTopics().isEmpty()) {
            throw new MetamorphosisServerStartupException("Empty topics list");
        }
    }


    public void setDataPath(final String dataPath) {
        final String oldDataPath = this.dataPath;
        this.dataPath = dataPath;
        // 如果dataLogPath没有改变过，那么也需要将dataLogPath指向新的dataPath
        if (oldDataPath.equals(this.dataLogPath)) {
            this.dataLogPath = this.dataPath;
        }
    }


    public String getDataPath() {
        return this.dataPath;
    }


    public String getDataLogPath() {
        return this.dataLogPath;
    }


    public void setDataLogPath(final String dataLogPath) {
        this.dataLogPath = dataLogPath;
    }


    public int getSlaveId() {
        return this.slaveId;
    }


    public boolean isSlave() {
        return this.slaveId >= 0;
    }


    /** just for test */
    public void setSlaveId(final int slaveId) {
        this.slaveId = slaveId;
    }


    public String getSlaveGroup() {
        return this.slaveGroup;
    }

    private final Map<String/* topic */, Set<Integer/* partition */>> closedPartitionMap =
            new CopyOnWriteMap<String, Set<Integer>>();


    @Override
    public void closePartitions(final String topic, final int start, final int end) {
        if (StringUtils.isBlank(topic) || !this.topics.contains(topic)) {
            log.warn("topic=[" + topic + "]为空或未发布");
            return;
        }
        if (start < 0 || start > end) {
            log.warn("起始或结束的分区号非法,start=" + start + ",end=" + end);
            return;
        }

        for (int i = start; i <= end; i++) {
            this.closePartition(topic, i);
        }

    }


    private void closePartition(final String topic, final int partition) {
        Set<Integer> closedPartitions = this.closedPartitionMap.get(topic);
        if (closedPartitions == null) {
            closedPartitions = new HashSet<Integer>();
            this.closedPartitionMap.put(topic, closedPartitions);
        }
        if (closedPartitions.add(partition)) {
            log.info("close partition=" + partition + ",topic=" + topic);
        }
        else {
            log.info("partition=" + partition + " closed yet,topic=" + topic);
        }

    }


    public boolean isClosedPartition(final String topic, final int partition) {
        final Set<Integer> closedPartitions = this.closedPartitionMap.get(topic);
        return closedPartitions == null ? false : closedPartitions.contains(partition);
    }


    @Override
    public void openPartitions(final String topic) {
        final Set<Integer> partitions = this.closedPartitionMap.remove(topic);
        if (partitions == null || partitions.isEmpty()) {
            log.info("topic[" + topic + "] has no closed partitions");
        }
        else {
            log.info("open partitions " + partitions + ",topic=" + topic);
        }
    }


    public TopicConfig getTopicConfig(final String topic) {
        final TopicConfig topicConfig = this.topicConfigMap.get(topic);
        return topicConfig != null ? topicConfig : new TopicConfig(topic, this);
    }


    public Map<String, TopicConfig> getTopicConfigMap() {
        return this.topicConfigMap;
    }


    public long getSlaveMaxDelayInMills() {
        return this.slaveMaxDelayInMills;
    }


    public void setSlaveMaxDelayInMills(final long slaveMaxDelayInMills) {
        this.slaveMaxDelayInMills = slaveMaxDelayInMills;
    }
}
