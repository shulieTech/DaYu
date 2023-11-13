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
package com.pamirs.pradar;

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.Converter;
import com.pamirs.attach.plugin.dynamic.ResourceManager;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.json.ResultSerializer;
import com.shulie.instrument.simulator.api.util.StringUtil;
import io.shulie.takin.pinpoint.thrift.dto.TStressTestTraceData;
import io.shulie.takin.pinpoint.thrift.dto.TStressTestTracePayloadData;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * @Auther: vernon
 * @Date: 2021/1/18 10:28
 * @Description:
 */

/**
 * traceId|startTime|agentId|invokeId|invokeType|appName|cost|middlewareName|serviceName|methodName|resultCode
 * |request|response|flags|callbackMsg|#samplingInterval|@attributes|@localAttributes
 */
public abstract class TraceEncoder {
    public abstract void encode(BaseContext ctx, PradarAppender appender) throws IOException;
}

/**
 * Pradar RPC 日志的输出
 */
class TraceInvokeContextEncoder extends TraceEncoder {

    private int DEFAULT_BUFFER_SIZE = 256;
    private StringBuilder buffer = new StringBuilder(DEFAULT_BUFFER_SIZE);

    @Override
    public void encode(BaseContext base, PradarAppender eea) throws IOException {
        AbstractContext ctx;
        if (base instanceof AbstractContext) {
            ctx = (AbstractContext) base;
        } else {
            return;
        }
        attachment(ctx);
        //Pradar TODO
        StringBuilder buffer = this.buffer;
        buffer.delete(0, buffer.length());
        buffer.append(ctx.getTraceId() == null ? "" : ctx.getTraceId()).append('|')
                .append(ctx.getStartTime()).append('|');
        // 新版本兼容老版本的控制台和大数据
        if (StringUtils.isNotBlank(Pradar.PRADAR_ENV_CODE)) {
            buffer.append(StringUtils.isBlank(Pradar.PRADAR_TENANT_KEY) ? "" : Pradar.PRADAR_TENANT_KEY).append('|')
                    .append(StringUtils.isBlank(Pradar.PRADAR_ENV_CODE) ? "" : Pradar.PRADAR_ENV_CODE).append('|')
                    .append(StringUtils.isBlank(Pradar.PRADAR_USER_ID) ? "" : Pradar.PRADAR_USER_ID).append('|');
        }
        buffer.append(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO).append('|')
                .append(ctx.getInvokeId() == null ? "" : ctx.getInvokeId()).append('|')
                .append(ctx.getInvokeType()).append('|')
                .append(PradarCoreUtils.makeLogSafe(AppNameUtils.appName())).append('|')
                .append(ctx.getLogTime() - ctx.getStartTime()).append('|')
                .append(PradarCoreUtils.makeLogSafe(ctx.getMiddlewareName() == null ? "" : ctx.getMiddlewareName())).append(
                        '|')
                .append(PradarCoreUtils.makeLogSafe(ctx.getServiceName() == null ? "" : ctx.getServiceName())).append('|')
                .append(PradarCoreUtils.makeLogSafe(ctx.getMethodName() == null ? "" : ctx.getMethodName())).append('|')
                .append(ctx.getResultCode() == null ? "" : ctx.getResultCode()).append('|')
                .append(PradarCoreUtils.makeLogSafe(
                        ResultSerializer.serializeRequest(ctx.getRequest() == null ? "" : ctx.getRequest(),
                                Pradar.getPluginRequestSize()))).append('|')
                .append(PradarCoreUtils.makeLogSafe(
                        ResultSerializer.serializeRequest(ctx.getMockResponse() != null ? ctx.getMockResponse() : ctx.getResponse() != null ? ctx.getResponse() : "",
                                Pradar.getPluginRequestSize()))).append('|')
                .append(TraceCoreUtils.combineString(ctx.isClusterTest(), ctx.isDebug(),
                        "0".equals(ctx.invokeId),
                        TraceCoreUtils.isServer(ctx)))
                .append("|")
                .append(PradarCoreUtils.makeLogSafe(ctx.getCallBackMsg() == null ? "" : ctx.getCallBackMsg()));
        int samplingInterval;
        if (ctx.isClusterTest()) {
            samplingInterval = PradarSwitcher.getClusterTestSamplingInterval();
        } else {
            samplingInterval = PradarSwitcher.getSamplingInterval();
        }
        buffer.append("|#").append(samplingInterval);
        buffer.append("|@").append(TraceCoreUtils.attributes(ctx.traceAppName, ctx.traceServiceName, ctx.traceMethod))
                .append("|@")
                .append(TraceCoreUtils.localAttributes(
                        ctx.upAppName, ctx.remoteIp, ctx.getPort(), ctx.requestSize, ctx.responseSize, ctx.mockResponse != null))
                .append("|")
                .append(ctx.ext == null ? "" : ctx.ext);
        ctx.logContextData(buffer);
        buffer.append(PradarCoreUtils.NEWLINE);
        eea.append(buffer.toString());
        ctx.destroy();
    }

    void attachment(AbstractContext ctx) {
        if (ctx.isClusterTest) {
            return;
        }
        try {
            if (ctx.ext != null) {
                Object t = ctx.ext;
                if (Attachment.class.isAssignableFrom(t.getClass())) {
                    Attachment attachment = (Attachment) t;
                    Object key = Converter.TemplateConverter
                            .ofClass(
                                    attachment.getExt().getClass()
                            ).getKey();
                    ctx.ext = key + "@##" + GsonFactory.getGson().toJson(attachment);
                }
                return;
            }

            String middleware = PradarCoreUtils.makeLogSafe(ctx.getMiddlewareName()
                    == null
                    ? "" : ctx.getMiddlewareName());
            if (StringUtil.isEmpty(middleware)) {
                return;
            }

            Object t = ResourceManager.get(ctx.index, middleware);
            if (t == null) {
                String serviceName = PradarCoreUtils.makeLogSafe(
                        ctx.getServiceName() == null ? "" : ctx.getServiceName());

                t = ResourceManager.get(serviceName, middleware);

                if (t == null) {
                    String methodName = PradarCoreUtils.makeLogSafe(
                            ctx.getMethodName() == null ? "" : ctx.getMethodName());

                    t = ResourceManager.get(serviceName + methodName, middleware);
                    if (t == null) {
                        return;
                    }
                }
            }
            ctx.ext = t;
            if (ctx.ext instanceof Attachment) {
                Attachment attachment = (Attachment) ctx.ext;
                Object key = Converter.TemplateConverter
                        .ofClass(
                                attachment.getExt().getClass()
                        ).getKey();
                ctx.ext = key + "@##" + GsonFactory.getGson().toJson(ctx.ext);
            }
        } catch (Throwable t) {

        }

    }
}

/**
 * 业务跟踪日志的输出
 */
class TraceTraceEncoder extends TraceEncoder {

    private static final int DEFAULT_BUFFER_SIZE = 256;

    /**
     * 需要做换行和分隔符过滤
     */
    static final int REQUIRED_LINE_FEED_ESCAPE = 1;

    private final char entryDelimiter;
    private StringBuilder buffer = new StringBuilder(DEFAULT_BUFFER_SIZE);

    TraceTraceEncoder(char entryDelimiter) {
        this.entryDelimiter = entryDelimiter;
    }

    @Override
    public void encode(BaseContext ctx, PradarAppender eea) throws IOException {
        final char entryDelimiter = this.entryDelimiter;
        StringBuilder buffer = this.buffer;
        buffer.delete(0, buffer.length());
        buffer.append(ctx.getTraceId()).append(entryDelimiter)// traceId
                .append(ctx.getTraceAppName()).append(entryDelimiter)
                .append(ctx.getUpAppName()).append(entryDelimiter)
                .append(ctx.getLogTime()).append(entryDelimiter)
                .append(ctx.getInvokeId()).append(entryDelimiter)// rpcId
                .append(ctx.getServiceName()).append(entryDelimiter)// bizKey
                .append(ctx.getMethodName()).append(entryDelimiter)// queryKey
                .append(ctx.getLogType()).append(entryDelimiter)// clusterTest
                .append(ctx.traceName).append(entryDelimiter)// bizType and bizValue
                .append(ctx.isClusterTest() ? '1' : '0').append(entryDelimiter);// bizType and bizValue

        // logContent
        if (ctx.getInvokeType() == REQUIRED_LINE_FEED_ESCAPE) {
            PradarCoreUtils.appendLog(ctx.callBackMsg, buffer, '\0');
        } else {
            buffer.append(ctx.callBackMsg);
        }
        buffer.append(PradarCoreUtils.NEWLINE);
        eea.append(buffer.toString());
    }
}

class TraceCollectorInvokeEncoder extends TraceEncoder {
    private int DEFAULT_BUFFER_SIZE = 256;
    private StringBuilder buffer = new StringBuilder(DEFAULT_BUFFER_SIZE);

    @Override
    public void encode(BaseContext base, PradarAppender appender) throws IOException {
        AbstractContext ctx;
        if (base instanceof AbstractContext) {
            ctx = (AbstractContext) base;
        } else {
            return;
        }
        attachment(ctx);

        StringBuilder buffer = this.buffer;
        buffer.delete(0, buffer.length());

        TStressTestTraceData traceData = new TStressTestTraceData();
        traceData.setTraceId(ctx.getTraceId() == null ? "" : ctx.getTraceId());
        traceData.setTimestamp(ctx.getStartTime());
        traceData.setAgentId(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO);
        traceData.setInvokeId(ctx.getInvokeId() == null ? "" : ctx.getInvokeId());
        traceData.setInvokeType((byte) ctx.getInvokeType());
        traceData.setAppName(PradarCoreUtils.makeLogSafe(AppNameUtils.appName()));
        traceData.setCost((int) (ctx.getLogTime() - ctx.getStartTime()));
        traceData.setMiddlewareName(PradarCoreUtils.makeLogSafe(ctx.getMiddlewareName() == null ? "" : ctx.getMiddlewareName()));
        traceData.setServiceName(ctx.getServiceName() == null ? "" : ctx.getServiceName());
        traceData.setMethodName(PradarCoreUtils.makeLogSafe(ctx.getMethodName() == null ? "" : ctx.getMethodName()));
        traceData.setResultCode(ctx.getResultCode() == null ? "" : ctx.getResultCode());
        traceData.setPressureTest(ctx.isClusterTest());
        traceData.setDebugTest(ctx.isDebug());
        traceData.setEntrance("0".equals(ctx.invokeId));
        traceData.setServer(TraceCoreUtils.isServer(ctx));
        traceData.setUpAppName(ctx.upAppName);
        traceData.setRemoteIp(ctx.remoteIp);
        try {
            traceData.setPort(StringUtils.isBlank(ctx.getPort()) ? 0 : Integer.parseInt(ctx.getPort()));
        } catch (Exception e) {
            traceData.setPort(0);
        }
        if (ctx.attributes != null && ctx.attributes.containsKey(PradarService.PRADAR_TRACE_NODE_KEY)) {
            traceData.setEntranceId(ctx.attributes.get(PradarService.PRADAR_TRACE_NODE_KEY));
        }

        TStressTestTracePayloadData tracePayloadData = new TStressTestTracePayloadData();
        tracePayloadData.setTraceId(traceData.getTraceId());
        tracePayloadData.setTimestamp(traceData.getTimestamp());
        tracePayloadData.setAgentId(traceData.getAgentId());
        tracePayloadData.setInvokeId(traceData.getInvokeId());
        tracePayloadData.setInvokeType(traceData.getInvokeType());
        tracePayloadData.setAppName(traceData.getAppName());
        tracePayloadData.setMiddlewareName(traceData.getMiddlewareName());
        tracePayloadData.setServiceName(traceData.getServiceName());
        tracePayloadData.setMethodName(traceData.getMethodName());
        tracePayloadData.setPressureTest(traceData.isPressureTest());
        tracePayloadData.setEntrance(traceData.isEntrance());
        tracePayloadData.setServer(traceData.isServer());
        tracePayloadData.setRequest(PradarCoreUtils.makeLogSafe(
                ResultSerializer.serializeRequest(ctx.getRequest() == null ? "" : ctx.getRequest(),
                        Pradar.getPluginRequestSize())));
        tracePayloadData.setResponse(PradarCoreUtils.makeLogSafe(
                ResultSerializer.serializeRequest(ctx.getResponse() == null ? "" : ctx.getResponse(),
                        Pradar.getPluginRequestSize())));
        tracePayloadData.setCallbackMsg(PradarCoreUtils.makeLogSafe(ctx.getCallBackMsg() == null ? "" : ctx.getCallBackMsg()));
        tracePayloadData.setRequestSize((int) ctx.requestSize);
        tracePayloadData.setResponseSize((int) ctx.responseSize);
        tracePayloadData.setExt(ctx.ext == null ? "" : ctx.ext + "");

        ctx.logContextData(buffer);
        tracePayloadData.setRpcContent(buffer.toString());
        ctx.destroy();

        appender.appendObject(traceData);
        appender.appendObject(tracePayloadData);
    }

    void attachment(AbstractContext ctx) {
        if (ctx.isClusterTest) {
            return;
        }
        try {
            if (ctx.ext != null) {
                Object t = ctx.ext;
                if (Attachment.class.isAssignableFrom(t.getClass())) {
                    Attachment attachment = (Attachment) t;
                    Object key = Converter.TemplateConverter
                            .ofClass(
                                    attachment.getExt().getClass()
                            ).getKey();
                    ctx.ext = key + "@##" + GsonFactory.getGson().toJson(attachment);
                }
                return;
            }

            String middleware = PradarCoreUtils.makeLogSafe(ctx.getMiddlewareName()
                    == null
                    ? "" : ctx.getMiddlewareName());
            if (StringUtil.isEmpty(middleware)) {
                return;
            }

            Object t = ResourceManager.get(ctx.index, middleware);
            if (t == null) {
                String serviceName = PradarCoreUtils.makeLogSafe(
                        ctx.getServiceName() == null ? "" : ctx.getServiceName());

                t = ResourceManager.get(serviceName, middleware);

                if (t == null) {
                    String methodName = PradarCoreUtils.makeLogSafe(
                            ctx.getMethodName() == null ? "" : ctx.getMethodName());

                    t = ResourceManager.get(serviceName + methodName, middleware);
                    if (t == null) {
                        return;
                    }
                }
            }
            ctx.ext = t;
            if (ctx.ext instanceof Attachment) {
                Attachment attachment = (Attachment) ctx.ext;
                Object key = Converter.TemplateConverter
                        .ofClass(
                                attachment.getExt().getClass()
                        ).getKey();
                ctx.ext = key + "@##" + GsonFactory.getGson().toJson(ctx.ext);
            }
        } catch (Throwable t) {

        }

    }
}