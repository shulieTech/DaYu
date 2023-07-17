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
package com.pamirs.attach.plugin.apache.kafka;

import com.pamirs.attach.plugin.apache.kafka.header.HeaderProvider;
import com.pamirs.attach.plugin.apache.kafka.header.ProducerConfigProvider;
import com.pamirs.attach.plugin.apache.kafka.interceptor.*;
import com.pamirs.attach.plugin.apache.kafka.listener.KafkaShadowPreCheckEventListener;
import com.pamirs.pradar.interceptor.Interceptors;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @since 2019-08-05 19:40
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = KafkaConstants.MODULE_NAME, version = "1.0.0", author = "tangyuhan@shulie.io",
        description = "apache kafka 消息中间件")
public class KafkaPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {

        ignoredTypesBuilder.ignoreClass("org.apache.kafka.");

        /**
         *
         */
        addProducerInterceptor();
        if (simulatorConfig.getBooleanProperty("kafka.use.other.plugin", false)) {
            return true;
        }
//        final ShadowConsumerDisableListenerImpl shadowConsumerDisableListener = new ShadowConsumerDisableListenerImpl();
//        EventRouter.router().addListener(shadowConsumerDisableListener);
//        return addConsumerRegisterInterceptor();

        addListener();
        return addConsumerTraceInterceptor();
    }

    private boolean addProducerInterceptor() {
        this.enhanceTemplate.enhance(this, "org.apache.kafka.clients.producer.KafkaProducer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod sendMethod = target.getDeclaredMethod("send",
                        "org.apache.kafka.clients.producer.ProducerRecord", "org.apache.kafka.clients.producer.Callback");
                sendMethod.addInterceptor(Listeners.of(ProducerSendInterceptor.class));
            }
        });

        this.enhanceTemplate.enhance(this, "kafka.javaapi.producer.Producer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod sendMsgMethod = target.getDeclaredMethod("send", "kafka.producer.KeyedMessage");
                InstrumentMethod sendListMethod = target.getDeclaredMethod("send", "java.util.List");

                sendMsgMethod.addInterceptor(Listeners.of(OriginProducerSendInterceptor.class));
                sendListMethod.addInterceptor(Listeners.of(OriginProducerSendInterceptor.class));

            }
        });
        return true;
    }

    private boolean addConsumerTraceInterceptor() {

        enhanceConsumerRecordEntryPoint("org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter");
        enhanceConsumerRecordEntryPoint("org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter");
        enhanceBatchMessagingMessage("org.springframework.kafka.listener.adapter.BatchMessagingMessageListenerAdapter");

        enhanceSetMessageListener("org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer");

        this.enhanceTemplate.enhance(this, "org.apache.kafka.clients.consumer.KafkaConsumer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod constructor = target.getConstructors();
                constructor.addInterceptor(
                        Listeners.of(ConsumerConstructorInterceptor.class, "KafkaConsumerConstructorScope",
                                ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod pollMethod = target.getDeclaredMethods("poll");
                pollMethod.addInterceptor(Listeners.of(ConsumerPollInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
                        Interceptors.SCOPE_CALLBACK));
                pollMethod.addInterceptor(
                        Listeners.of(ConsumerTraceInterceptor.class, "kafkaTraceScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

                InstrumentMethod acquireMethod = target.getDeclaredMethods("acquire");
                acquireMethod.addInterceptor(Listeners.of(ConsumerAcquireMethodInterceptor.class, "kafkaAcquire", ExecutionPolicy.BOUNDARY,
                        Interceptors.SCOPE_CALLBACK));
            }
        });

        this.enhanceTemplate.enhance(this, "org.springframework.kafka.core.DefaultKafkaConsumerFactory", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("createRawConsumer", "java.util.Map");
                instrumentMethod.addInterceptor(Listeners.of(CreateRawConsumerInterceptor.class));
            }
        });
        return true;
    }

    private boolean addConsumerRegisterInterceptor() {
        enhanceConsumerRecordEntryPoint("org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter");
        enhanceConsumerRecordEntryPoint("org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter");
        enhanceBatchMessagingMessage("org.springframework.kafka.listener.adapter.BatchMessagingMessageListenerAdapter");

        enhanceSetMessageListener("org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer");

        this.enhanceTemplate.enhance(this, "org.apache.kafka.clients.consumer.KafkaConsumer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod constructor = target.getConstructors();
                constructor.addInterceptor(
                        Listeners.of(ConsumerConstructorInterceptor.class, "KafkaConsumerConstructorScope",
                                ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                // Version 2.2.0+ is supported.
                InstrumentMethod pollMethod = target.getDeclaredMethod("poll", "org.apache.kafka.common.utils.Timer",
                        "boolean");
//                pollMethod.addInterceptor(Listeners.of(ConsumerPollInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
//                        Interceptors.SCOPE_CALLBACK));
                pollMethod.addInterceptor(
                        Listeners.of(ConsumerTraceInterceptor.class, "kafkaTraceScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

                // Version 2.0.0+ is supported.
                InstrumentMethod pollMethod1 = target.getDeclaredMethod("poll", "long", "boolean");
//                pollMethod1.addInterceptor(
//                        Listeners.of(ConsumerPollInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
//                                Interceptors.SCOPE_CALLBACK));
                pollMethod1.addInterceptor(
                        Listeners.of(ConsumerTraceInterceptor.class, "kafkaTraceScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

                // Version 2.0.0-
                InstrumentMethod pollMethod2 = target.getDeclaredMethod("poll", "long");
//                pollMethod2.addInterceptor(
//                        Listeners.of(ConsumerPollInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
//                                Interceptors.SCOPE_CALLBACK));
                pollMethod2.addInterceptor(
                        Listeners.of(ConsumerTraceInterceptor.class, "kafkaTraceScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

                //以下提交方法必须都要增强
                target.getDeclaredMethod("commitAsync", "org.apache.kafka.clients.consumer.OffsetCommitCallback")
                        .addInterceptor(
                                Listeners.of(ConsumerCommitAsyncInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
                                        Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("commitAsync", "java.util.Map",
                        "org.apache.kafka.clients.consumer.OffsetCommitCallback")
                        .addInterceptor(
                                Listeners.of(ConsumerCommitAsyncInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
                                        Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("commitSync", "java.util.Map")
                        .addInterceptor(Listeners.of(ConsumerCommitSyncInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("commitSync", "java.time.Duration")
                        .addInterceptor(Listeners.of(ConsumerCommitSyncInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("commitSync", "java.util.Map", "java.time.Duration")
                        .addInterceptor(Listeners.of(ConsumerCommitSyncInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethods("close", "position", "assignment", "subscription", "subscribe", "assign",
                        "unsubscribe", "seek", "seekToBeginning", "seekToEnd", "position", "committed", "metrics",
                        "partitionsFor",
                        "listTopics", "paused", "pause", "resume", "offsetsForTimes", "beginningOffsets", "endOffsets",
                        "close", "wakeup").addInterceptor(
                        Listeners.of(ConsumerOtherMethodInterceptor.class, "kafkaScope", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

            }
        });

        // 排除kafka 消费拦截器可能导致的ClassNotFoundException
        this.enhanceTemplate.enhance(this, "org.apache.kafka.common.config.AbstractConfig", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("getConfiguredInstances", "java.lang.String", "java.lang.Class");
                method.addInterceptor(Listeners.of(AbstractConfigGetInstanceInterceptor.class));
            }
        });

        this.enhanceTemplate.enhance(this, "org.springframework.kafka.core.DefaultKafkaConsumerFactory", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("createRawConsumer", "java.util.Map");
                instrumentMethod.addInterceptor(Listeners.of(CreateRawConsumerInterceptor.class));
            }
        });

        return true;
    }

    public void enhanceConsumerRecordEntryPoint(String className) {
        this.enhanceTemplate.enhance(this, className, new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod declaredMethod = target.getDeclaredMethod("onMessage",
                        "org.apache.kafka.clients.consumer.ConsumerRecord", "*", "*");
                declaredMethod.addInterceptor(
                        Listeners.of(ConsumerRecordEntryPointInterceptor.class, "KAFKA_SCOPE", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));
                /**
                 * spring-kafka 1.3.9
                 */
                InstrumentMethod declareMethod2 = target.getDeclaredMethod("onMessage",
                        "org.apache.kafka.clients.consumer.ConsumerRecord", "*");
                declareMethod2.addInterceptor(
                        Listeners.of(ConsumerRecordEntryPointInterceptor2.class, "KAFKA_SCOPE", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));

               /* InstrumentMethod onMessage = target.getDeclaredMethod("onMessage", "org.apache.kafka.clients.consumer
               .ConsumerRecord", "org.springframework.kafka.support.Acknowledgment");
                onMessage.addInterceptor(
                        Listeners.of(ConsumerRecordEntryPointInterceptor.class, "KAFKA_SCOPE", ExecutionPolicy.BOUNDARY,
                        Interceptors.SCOPE_CALLBACK));*/
            }
        });
    }

    public void enhanceBatchMessagingMessage(String className) {
        this.enhanceTemplate.enhance(this, className, new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod onMessageMethod1 = target.getDeclaredMethod("onMessage",
                        "org.apache.kafka.clients.consumer.ConsumerRecords", "*", "*");
                onMessageMethod1.addInterceptor(Listeners.of(ConsumerMultiRecordEntryPointInterceptor.class));

                InstrumentMethod onMessageMethod2 = target.getDeclaredMethod("onMessage", "java.util.List", "*", "*");
                onMessageMethod2.addInterceptor(Listeners.of(ConsumerMultiRecordEntryPointInterceptor.class));
            }
        });
    }

    private void enhanceSetMessageListener(String className) {
        this.enhanceTemplate.enhance(this, className, new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
//                InstrumentMethod startMethod = target.getDeclaredMethods("invokeListener");
//
//                startMethod.addInterceptor(Listeners.of(KafkaListenerContainerInterceptor.class));

                //高版本
                target.getDeclaredMethods("pollAndInvoke").addInterceptor(Listeners.of(
                        SpringKafkaPollAndInvokeInterceptor.class));
                //低版本
                target.getDeclaredMethods("processSeeks").addInterceptor(Listeners.of(
                        SpringKafkaProcessSeeksInterceptor.class));
            }
        });
    }

    private void addListener() {
        EventRouter.router()
                .addListener(new KafkaShadowPreCheckEventListener());
    }

    @Override
    public void onFrozen() throws Throwable {
        HeaderProvider.clear();
        ProducerConfigProvider.clear();
    }
}
