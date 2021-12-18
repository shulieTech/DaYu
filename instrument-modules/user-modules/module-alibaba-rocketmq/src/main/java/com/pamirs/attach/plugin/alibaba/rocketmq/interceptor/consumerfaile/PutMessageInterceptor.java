/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor.consumerfaile;

import com.alibaba.rocketmq.common.message.MessageExt;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author angju
 * @date 2021/12/17 18:36
 */
public class PutMessageInterceptor extends AroundInterceptor {
    private Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");

    @Override
    public void doBefore(Advice advice) {
        List<MessageExt> msgs = (List<MessageExt>) advice.getParameterArray()[0];
        if (msgs.size() > 0){
            String topic = msgs.get(0).getTopic();
            int queueId = msgs.get(0).getQueueId();
            logger.error("执行PutMessageInterceptor before, topic is " + topic + " queueId is " + queueId
                    );
        }

    }

    @Override
    public void doAfter(Advice advice) {
        List<MessageExt> msgs = (List<MessageExt>) advice.getParameterArray()[0];
        if (msgs.size() > 0){
            String topic = msgs.get(0).getTopic();
            int queueId = msgs.get(0).getQueueId();
            logger.error("执行PutMessageInterceptor after, topic is " + topic + " queueId is " + queueId
            );
        }
    }

    @Override
    public void doException(Advice advice) {
        List<MessageExt> msgs = (List<MessageExt>) advice.getParameterArray()[0];
        if (msgs.size() > 0){
            String topic = msgs.get(0).getTopic();
            int queueId = msgs.get(0).getQueueId();
            logger.error("执行PutMessageInterceptor before, topic is " + topic + " queueId is " + queueId
             + " error is " + advice.getThrowable().getMessage());
        }
    }
}
