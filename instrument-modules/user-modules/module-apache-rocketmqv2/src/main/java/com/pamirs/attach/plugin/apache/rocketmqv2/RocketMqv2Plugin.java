/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.apache.rocketmqv2;

import com.pamirs.attach.plugin.apache.rocketmqv2.consumer.RocketmqShadowConsumerExecute;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.messaging.consumer.ConsumerManager;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/29 15:46
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "apache-rocketmqv2", version = "1.0.0", author = "wanglinglong@shulie.io", description = "apache-rocketmq新版插件")
public class RocketMqv2Plugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(RocketMqv2Plugin.class);

    @Override
    public boolean onActive() throws Throwable {
        ConsumerRegister consumerRegister = new ConsumerRegister().consumerExecute(RocketmqShadowConsumerExecute.class);
        ConsumerManager.register(consumerRegister, "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer#start");
        return true;
    }
}
