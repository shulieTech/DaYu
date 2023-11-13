/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.agent.core.config;

import com.shulie.instrument.simulator.agent.core.util.AddressUtils;
import com.shulie.instrument.simulator.agent.core.util.PidUtils;
import com.shulie.instrument.simulator.agent.core.util.PropertyPlaceholderHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

/**
 * agent 配置
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/17 8:09 下午
 */
public class CoreConfig {

    private final static String CONFIG_PATH_NAME = "config";
    private final static String AGENT_PATH_NAME = "agent";
    private final static String PROVIDER_PATH_NAME = "provider";
    private final static String LOG_PATH_NAME = "simulator.log.path";
    private final static String LOG_LEVEL_NAME = "simulator.log.level";
    private final static String MULTI_APP_SWITCH = "simulator.multiapp.switch.on";
    private final static String DEFAULT_LOG_LEVEL = "info";

    private static final String RESULT_FILE_PATH = System.getProperties().getProperty("user.home")
        + File.separator + "%s" + File.separator + ".simulator.token";
    /**
     * 存放所有的 agent 配置
     */
    private final Map<String, String> configs = new HashMap<String, String>();

    /**
     * agent 配置文件读取的配置
     */
    private final Map<String, String> agentFileConfigs = new HashMap<String, String>();

    /**
     * agent home 路径
     */
    private final String agentHome;

    /**
     * config 文件路径
     */
    private final String configFilePath;

    /**
     * spi 目录路径
     */
    private final String providerFilePath;

    /**
     * simulator 目录路径
     */
    private final String simulatorHome;

    /**
     * simulator 启动jar 路径
     */
    private final String simulatorJarPath;

    /**
     * log 配置文件路径
     */
    private final String logConfigFilePath;

    /**
     * attach的进程 id
     */
    private long attachId = -1L;

    /**
     * attach 的进程名称
     */
    private String attachName;

    private ScheduledExecutorService service;

    public CoreConfig(String agentHome) {
        //暂时无动态参数，不开启
        //        initFetchConfigTask();
        this.agentHome = agentHome;
        this.configFilePath = agentHome + File.separator + CONFIG_PATH_NAME;
        this.providerFilePath = agentHome + File.separator + PROVIDER_PATH_NAME;
        this.simulatorHome = agentHome + File.separator + AGENT_PATH_NAME;
        this.simulatorJarPath = this.simulatorHome + File.separator + "simulator" + File.separator
            + "instrument-simulator-agent.jar";
        this.logConfigFilePath = this.configFilePath + File.separator + "simulator-agent-logback.xml";
        File configFile = new File(configFilePath, "agent.properties");
        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        InputStream configIn = null;
        try {
            if (!configFile.exists() || !configFile.canRead()) {
                configIn = CoreConfig.class.getClassLoader().getResourceAsStream("agent.properties");
            } else {
                configIn = new FileInputStream(configFile);
            }

            Enumeration enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String)enumeration.nextElement();
                configs.put(name, properties.getProperty(name));
            }
            properties.clear();
            properties.load(configIn);
            enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String)enumeration.nextElement();
                agentFileConfigs.put(name, properties.getProperty(name));
            }
            configs.putAll(agentFileConfigs);
        } catch (Throwable e) {
            throw new RuntimeException("Agent: read agent.properties file err:" + configFile.getAbsolutePath(), e);
        } finally {
            if (configIn != null) {
                try {
                    configIn.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    public boolean isMultiAppSwitch() {
        String value = configs.get(MULTI_APP_SWITCH);
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取日志级别
     *
     * @return 日志级别
     */
    public String getLogLevel() {
        String level = configs.get(LOG_LEVEL_NAME);
        if (StringUtils.isBlank(level)) {
            return DEFAULT_LOG_LEVEL;
        }
        return StringUtils.trim(level);
    }

    /**
     * 获取日志路径,日志路径如果不包含应用名称，则自动加上应用名称
     *
     * @return 日志路径
     */
    public String getLogPath() {
        String path = configs.get(LOG_PATH_NAME);
        if (StringUtils.isNotBlank(path)) {
            String cpath = path;
            if (!StringUtils.endsWith(cpath, "/")) {
                cpath += "/";
            }
            String appName = getAppName();
            /**
             * 这样判断是防止有路径包含了应用名称的字母但是不是应用名为目录
             */
            if (StringUtils.isNotBlank(appName) && StringUtils.indexOf(cpath, "/" + appName + "/") == -1) {
                cpath += appName;
                return isMultiAppSwitch() ? cpath + '/' + AddressUtils.getLocalAddress() + '/' + PidUtils.getPid()
                    : cpath;
            }
            return isMultiAppSwitch() ? path + '/' + AddressUtils.getLocalAddress() + '/' + PidUtils.getPid() : path;
        }
        String value = System.getProperty("user.home") + File.separator + "pradarlogs" + File.separator + getAppName();
        if (isMultiAppSwitch()) {
            value += '/' + PidUtils.getPid();
        }
        return value;
    }

    /**
     * 获取 boolean类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(configs.get(propertyName));
    }

    /**
     * 获取 int 类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public int getIntProperty(String propertyName, int defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        String value = StringUtils.trim(configs.get(propertyName));
        if (NumberUtils.isDigits(value)) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    /**
     * 获取 long 类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public long getLongProperty(String propertyName, long defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        String value = StringUtils.trim(configs.get(propertyName));
        if (NumberUtils.isDigits(value)) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    /**
     * 获取属性配置值
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public String getProperty(String propertyName, String defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        return StringUtils.trim(configs.get(propertyName));
    }

    /**
     * 获取 agent home 目录地址
     *
     * @return agent home
     */
    public String getAgentHome() {
        return agentHome;
    }

    /**
     * 获取配置文件路径
     *
     * @return config file path
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * 获取 spi 目录路径
     *
     * @return spi file path
     */
    public String getProviderFilePath() {
        return providerFilePath;
    }

    /**
     * 获取 agent 目录路径
     *
     * @return agent file path
     */
    public String getSimulatorHome() {
        return simulatorHome;
    }

    /**
     * 获取 agent jar 路径
     *
     * @return agent jar path
     */
    public String getSimulatorJarPath() {
        return simulatorJarPath;
    }

    /**
     * 获取 zk 地址
     *
     * @return
     */
    public String getZkServers() {
        return getProperty("simulator.zk.servers", "localhost:2181");
    }

    /**
     * 获取zk 注册路径
     *
     * @return
     */
    public String getRegisterPath() {
        return getProperty("simulator.client.zk.path", "/config/log/pradar/client");
    }

    /**
     * 获取 zk 连接超时时间
     *
     * @return
     */
    public int getZkConnectionTimeout() {
        String connectionTimeout = getProperty("simulator.zk.connection.timeout.ms", "30000");
        if (NumberUtils.isDigits(connectionTimeout)) {
            return Integer.parseInt(connectionTimeout);
        }
        return 60000;
    }

    /**
     * 获取 zk session 超时时间
     *
     * @return
     */
    public int getZkSessionTimeout() {
        String sessionTimeout = getProperty("simulator.zk.session.timeout.ms", "60000");
        if (NumberUtils.isDigits(sessionTimeout)) {
            return Integer.parseInt(sessionTimeout);
        }
        return 60000;
    }

    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        String value = getPropertyInAll("simulator.app.name");
        if (StringUtils.isBlank(value)) {
            value = getPropertyInAll("pradar.project.name");
        }
        return value != null ? value : "default";
    }

    private String getPropertyInAll(String key) {
        String value = System.getProperty(key);
        if (StringUtils.isBlank(value)) {
            value = getProperty(key, null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv(key);
        }
        return value;
    }

    /**
     * 获取 agentId
     *
     * @return 获取 agentId
     */
    public String getAgentId() {
        String agentId = internalGetAgentId();
        if (StringUtils.isBlank(agentId)) {
            return AddressUtils.getLocalAddress() + "-" + PidUtils.getPid();
        } else {
            Properties properties = new Properties();
            properties.setProperty("pid", String.valueOf(PidUtils.getPid()));
            properties.setProperty("hostname", AddressUtils.getHostName());
            properties.setProperty("ip", AddressUtils.getLocalAddress());
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            return propertyPlaceholderHelper.replacePlaceholders(agentId, properties);
        }
    }

    private String internalGetAgentId() {
        String value = System.getProperty("simulator.agentId");
        if (StringUtils.isBlank(value)) {
            value = System.getProperty("pradar.agentId");
        }
        if (StringUtils.isBlank(value)) {
            value = getProperty("simulator.agentId", null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv("simulator.agentId");
        }
        return value;
    }

    public String getTenantAppKey() {
        // 兼容老版本，如果有user.app.key，则优先使用user.app.key
        String value = getProperty("user.app.key");
        if (StringUtils.isBlank(value)) {
            value = getProperty("tenant.app.key");
        }
        return value;
    }

    /**
     * 获取配置信息 优先级：启动参数 > 配置文件 > 环境变量
     *
     * @param key 配置key
     * @return value
     */
    private String getProperty(String key) {
        String value = System.getProperty(key, null);
        if (StringUtils.isBlank(value)) {
            value = getProperty(key, null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv(key);
        }
        return value;
    }

    public String getTroWebUrl() {
        return getProperty("tro.web.url");
    }

    public String getUserId() {
        return getProperty("pradar.user.id");
    }

    /**
     * 获取当前环境
     *
     * @return 当前环境
     */
    public String getEnvCode() {
        return getProperty("pradar.env.code");
    }

    public String getAgentExpand() {
        return getProperty("pradar.agent.expand");
    }

    public String getTenantCode(){
        return getProperty("shulie.agent.tenant.code");
    }

    public String getAgentManagerUrl(){
        return getProperty("shulie.agent.manager.url");
    }

    public String getNacosTimeout(){
        return getProperty("nacos.timeout");
    }

    public String getNacosServerAddr(){
        return getProperty("nacos.serverAddr");
    }

    public String getClusterName(){
        return getProperty("cluster.name");
    }

    public String getShadowPreparationEnable(){
        return getProperty("shadow.preparation.enabled");
    }

    public String getKafkaSdkSwitch() {
        return getProperty("kafka.sdk.switch");
    }

    public String getPinpointCollectorAddress() {
        return getProperty("pradar.data.pusher.pinpoint.collector.address");
    }

    public String getPradarTraceFileSize(){
        return getProperty("pradar.trace.max.file.size");
    }

    public String getPradarMonitorFileSize(){
        return getProperty("pradar.monitor.max.file.size");
    }

    /**
     * 获取发起http请求中必须包含的head
     *
     * @return map集合
     */
    public Map<String, String> getHttpMustHeaders() {
        Map<String, String> headerMap = new HashMap<String, String>();
        // 新探针兼容老版本的控制台，所以userAppKey和tenantAppKey都传
        headerMap.put("userAppKey", getTenantAppKey());
        headerMap.put("tenantAppKey", getTenantAppKey());
        headerMap.put("userId", getUserId());
        headerMap.put("envCode", getEnvCode());
        headerMap.put("agentExpand", getAgentExpand());
        return headerMap;
    }

    /**
     * 获取 agent结果文件路径
     *
     * @return
     */
    public String getAgentResultFilePath() {
        return String.format(RESULT_FILE_PATH, getAppName());
    }

    /**
     * 获取 log 配置文件路径
     *
     * @return
     */
    public String getLogConfigFilePath() {
        return logConfigFilePath;
    }

    public InputStream getLogConfigFile() {
        File file = new File(getLogConfigFilePath());
        if (!file.exists()) {
            return CoreConfig.class.getClassLoader().getResourceAsStream("simulator-agent-logback.xml");
        } else {
            try {
                return new FileInputStream(getLogConfigFilePath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setAttachId(long attachId) {
        this.attachId = attachId;
    }

    public void setAttachName(String attachName) {
        this.attachName = attachName;
    }

    public long getAttachId() {
        return this.attachId;
    }

    public String getAttachName() {
        return this.attachName;
    }

    public Map<String, String> getAgentFileConfigs() {
        return agentFileConfigs;
    }
}
