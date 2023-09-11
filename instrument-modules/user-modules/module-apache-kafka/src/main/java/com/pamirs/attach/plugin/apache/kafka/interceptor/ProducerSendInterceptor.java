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
package com.pamirs.attach.plugin.apache.kafka.interceptor;

import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProvider;
import com.pamirs.attach.plugin.apache.kafka.header.ProducerConfigProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.ProducerConfigProvider;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.lang.reflect.Field;

/**
 * send方法增强类
 *
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.kafka.interceptor
 * @Date 2019-08-05 19:25
 */
@Destroyable(KafkaDestroy.class)
@ListenerBehavior(isFilterBusinessData = true)
public class ProducerSendInterceptor extends TraceInterceptorAdaptor {
    private Field topicField;
    private Field producerConfigField;

//    private void initTopicField(Object target) {
//        if (topicField != null) {
//            return;
//        }
//        try {
//            topicField = target.getClass().getDeclaredField(KafkaConstants.REFLECT_FIELD_TOPIC);
//            topicField.setAccessible(true);
//        } catch (Throwable e) {
//        }
//    }

    private void initProducerConfigField(Object target) {
        if (producerConfigField != null) {
            return;
        }
        try {
            producerConfigField = target.getClass().getDeclaredField(KafkaConstants.REFLECT_FIELD_PRODUCER_CONFIG);
            producerConfigField.setAccessible(true);
        } catch (Throwable e) {
        }
    }

//    private void setTopic(Object producerRecord, String topic) {
//        if (topicField != null) {
//            try {
//                topicField.set(producerRecord, topic);
//            } catch (Throwable e) {
//                try {
//                    Reflect.on(producerRecord).set(KafkaConstants.REFLECT_FIELD_TOPIC, topic);
//                } catch (ReflectException ex) {
//                }
//            }
//        } else {
//            try {
//                Reflect.on(producerRecord).set(KafkaConstants.REFLECT_FIELD_TOPIC, topic);
//            } catch (ReflectException ex) {
//            }
//        }
//    }

    @Override
    public String getPluginName() {
        return KafkaConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return KafkaConstants.PLUGIN_TYPE;
    }

    private String getValue(ProducerConfig producerConfig, String key) {
        ProducerConfigProcessor processor = ProducerConfigProvider.getProducerConfigProcessor(producerConfig);
        return processor.getValue(producerConfig, key);
    }

    private String getRemoteAddress(Object remoteAddressFieldAccessor) {
        initProducerConfigField(remoteAddressFieldAccessor);
        try {
            ProducerConfig producerConfig = ReflectionUtils.getField(producerConfigField, remoteAddressFieldAccessor);
            String value = getValue(producerConfig, KafkaConstants.KEY_BOOTSTRAP_SERVERS);
            if (value == null) {
                value = getValue(producerConfig, KafkaConstants.KEY_ZOOKEEPER_CONNECT);
            }
            if (value == null) {
                return KafkaConstants.UNKNOWN;
            }
            return value;
        } catch (Throwable e) {
            return KafkaConstants.UNKNOWN;
        }
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        final ProducerRecord producerRecord = (ProducerRecord)args[0];
        if (PradarSwitcher.isKafkaMessageHeadersEnabled() && !isHeadReadOnly(producerRecord)) {
            return new ContextTransfer() {
                @Override
                public void transfer(String key, String value) {
                    HeaderProcessor headerProcessor = HeaderProvider.getHeaderProcessor(producerRecord);
                    headerProcessor.setHeader(producerRecord, key, value);
                }
            };
        }
        return null;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        String remoteAddress = getRemoteAddress(target);
        final ProducerRecord producerRecord = (ProducerRecord)args[0];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(producerRecord.topic());
        spanRecord.setMethod("MQSend");
        spanRecord.setRemoteIp(remoteAddress);
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object result = advice.getReturnObj();
        final ProducerRecord producerRecord = (ProducerRecord)args[0];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(producerRecord);
        spanRecord.setResponse(result);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Throwable throwable = advice.getThrowable();
        final ProducerRecord producerRecord = (ProducerRecord)args[0];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(producerRecord);
        spanRecord.setResponse(throwable);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }

    private static Field readOnlyField = null;

    private boolean isHeadReadOnly(ProducerRecord producerRecord) {
        Headers headers = producerRecord.headers();
        if (headers instanceof RecordHeaders) {
            if (readOnlyField == null) {
                readOnlyField = ReflectionUtils.findField(headers.getClass(), "isReadOnly");
            }
            return ReflectionUtils.getField(readOnlyField, headers);
        }
        return false;
    }
}
