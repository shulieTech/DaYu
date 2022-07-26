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
package com.pamirs.attach.plugin.apache.kafka.interceptor;

import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerHolder;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.apache.kafka.clients.consumer.Consumer;

import java.lang.reflect.Proxy;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/12 4:08 下午
 * 高版本支持spring-kafka
 */
@Destroyable(KafkaDestroy.class)
public class SpringKafkaPollAndInvokeInterceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) throws Throwable {
        try {
            Object consumer = Reflect.on(advice.getTarget()).get(KafkaConstants.REFLECT_FIELD_CONSUMER);
            if (Proxy.class.isInstance(consumer)) {
                return;
            }
            if (consumer instanceof Consumer) {
                if (consumer.getClass().getName().equals("brave.kafka.clients.TracingConsumer")) {
                    consumer = Reflect.on(consumer).get("delegate");
                }
                ConsumerHolder.addWorkWithSpring((Consumer<?, ?>) consumer);
            }
        } catch (ReflectException ignore) {
        }
    }
}
