/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.agent.core.config;

import com.google.gson.reflect.TypeToken;
import com.shulie.instrument.simulator.agent.api.ExternalAPI;
import com.shulie.instrument.simulator.agent.api.model.CommandPacket;
import com.shulie.instrument.simulator.agent.api.model.HeartRequest;
import com.shulie.instrument.simulator.agent.api.model.Result;
import com.shulie.instrument.simulator.agent.core.gson.SimulatorGsonFactory;
import com.shulie.instrument.simulator.agent.core.util.ConfigUtils;
import com.shulie.instrument.simulator.agent.core.util.DownloadUtils;
import com.shulie.instrument.simulator.agent.core.util.HttpUtils;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.kafka.util.MessageSwitchUtil;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/23 7:21 下午
 */
public class ExternalAPIImpl implements ExternalAPI {
    private final static Logger logger = LoggerFactory.getLogger(ExternalAPIImpl.class);

    private final AgentConfig agentConfig;
    private final AtomicBoolean isWarnAlready;

    private final static String COMMAND_URL = "api/agent/application/node/probe/operate";

    /**
     * 心跳接口
     */
    private final static String HEART_URL = "api/agent/heartbeat";
    private final static String REPORT_URL = "api/agent/application/node/probe/operateResult";

    MessageSendService messageSendService;

    public ExternalAPIImpl(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
        isWarnAlready = new AtomicBoolean(false);
    }

    @Override
    public void onlineUpgrade(CommandPacket commandPacket) {

    }

    @Override
    public File downloadModule(String agentDownloadUrl, String targetPath) {
        if (StringUtils.isNotBlank(agentDownloadUrl)) {
            StringBuilder builder = new StringBuilder(agentDownloadUrl);
            if (StringUtils.indexOf(agentDownloadUrl, '?') != -1) {
                builder.append("&appName=").append(agentConfig.getAppName()).append("&agentId=").append(
                        agentConfig.getAgentId());
            } else {
                builder.append("?appName=").append(agentConfig.getAppName()).append("&agentId=").append(
                        agentConfig.getAgentId());
            }
            return DownloadUtils.download(builder.toString(), targetPath, agentConfig.getHttpMustHeaders());
        }
        return null;
    }

    @Override
    public void reportCommandResult(long commandId, boolean isSuccess, String errorMsg) {
        String webUrl = agentConfig.getTroWebUrl();
        if (StringUtils.isBlank(webUrl)) {
            logger.warn("AGENT: tro.web.url is not assigned.");
            return;
        }
        String url = joinUrl(webUrl, REPORT_URL);
        Map<String, String> body = new HashMap<String, String>();
        body.put("appName", agentConfig.getAppName());
        body.put("agentId", agentConfig.getAgentId());
        body.put("operateResult", isSuccess ? "1" : "0");
        if (StringUtils.isNotBlank(errorMsg)) {
            body.put("errorMsg", errorMsg);
        }
        HttpUtils.doPost(url, agentConfig.getHttpMustHeaders(), SimulatorGsonFactory.getGson().toJson(body));
    }

    @Override
    public CommandPacket getLatestCommandPacket() {
        //如果是kafka注册，当前不和控制台进行命令交互
        String registerName = System.getProperty("register.name", "zookeeper");
        if (registerName.equals("kafka")) {
            return CommandPacket.NO_ACTION_PACKET;
        }
        String webUrl = agentConfig.getTroWebUrl();
        if (StringUtils.isBlank(webUrl)) {
            logger.warn("AGENT: tro.web.url is not assigned.");
            return CommandPacket.NO_ACTION_PACKET;
        }
        //todo file模式？
        String agentConfigUrl = joinUrl(webUrl, COMMAND_URL);
        if (StringUtils.isBlank(agentConfigUrl)) {
            if (isWarnAlready.compareAndSet(false, true)) {
                logger.warn("AGENT: agent.command.url is not assigned.");
            }
            return CommandPacket.NO_ACTION_PACKET;
        }
        String url = agentConfigUrl;
        if (StringUtils.indexOf(url, '?') != -1) {
            url += "&appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        } else {
            url += "?appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        }

        String resp = ConfigUtils.doConfig(url, agentConfig.getHttpMustHeaders());
        if (StringUtils.isBlank(resp)) {
            logger.warn("AGENT: fetch agent command got a err response. {}", url);
            return CommandPacket.NO_ACTION_PACKET;
        }

        /**
         * 这个地方如果服务端没有最新需要执行的命令，则建议返回空,也可以返回最后一次的命令
         */
        try {
            Result<CommandPacket> response = SimulatorGsonFactory.getGson().fromJson(resp, new TypeToken<Result<CommandPacket>>(){}.getType());
            if (!response.isSuccess()) {
                logger.error("fetch agent command got a fault response. resp={}", resp);
                throw new RuntimeException(response.getError());
            }
            return response.getData();
        } catch (Throwable e) {
            logger.error("AGENT: parse command err. " + resp, e);
            return CommandPacket.NO_ACTION_PACKET;
        }
    }

    @Override
    public List<CommandPacket> sendHeart(final HeartRequest heartRequest) {
        HeartRequestUtil.configHeartRequest(heartRequest, agentConfig);
        String webUrl = agentConfig.getTroWebUrl();
        if (StringUtils.isBlank(webUrl)) {
            logger.warn("AGENT: tro.web.url is not assigned.");
            return null;
        }
        final String agentHeartUrl = joinUrl(webUrl, HEART_URL);

        final AtomicReference<List<CommandPacket>> reference = new AtomicReference<List<CommandPacket>>(new ArrayList<CommandPacket>());

        if (messageSendService == null) {
            messageSendService = new PinpointSendServiceFactory().getKafkaMessageInstance();
        }

        if (!MessageSwitchUtil.isKafkaSdkSwitch()) {
            HttpUtils.HttpResult resp = HttpUtils.doPost(agentHeartUrl, agentConfig.getHttpMustHeaders(),
                    SimulatorGsonFactory.getGson().toJson(heartRequest));

            if (null == resp) {
                logger.warn("AGENT: sendHeart got a err response. {}", agentHeartUrl);
                return reference.get();
            }

            if (StringUtils.isBlank(resp.getResult())) {
                logger.warn("AGENT: sendHeart got response empty . {}", agentHeartUrl);
                return reference.get();
            }
            try {
                Result<List<CommandPacket>> response = SimulatorGsonFactory.getGson().fromJson(resp.getResult(), new TypeToken<Result<List<CommandPacket>>>(){}.getType());
                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getError());
                }
                reference.set(response.getData());
            } catch (Throwable e) {
                logger.error("AGENT: parse command err." + resp, e);
            }

        }

        return reference.get();
    }

    @Override
    public List<String> getAgentProcessList() {
        String url = agentConfig.getUploadAgentProcesslistUrl();
        if (StringUtils.isBlank(url)) {
            return Collections.EMPTY_LIST;
        }

        List<String> processlist = new ArrayList<String>();
        final List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for (VirtualMachineDescriptor descriptor : list) {
            processlist.add(descriptor.displayName());
        }
        return processlist;
    }

    private String joinUrl(String troWebUrl, String endpoint) {
        return troWebUrl.endsWith("/") ? troWebUrl + endpoint : troWebUrl + "/" + endpoint;
    }
}
