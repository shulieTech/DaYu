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
package com.pamirs.attach.plugin.log4j.interceptor.v1;

import com.pamirs.attach.plugin.log4j.destroy.Log4jDestroy;
import com.pamirs.attach.plugin.log4j.interceptor.v1.creator.ShadowAppenderCreatorFacadeV1;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.message.ConcurrentWeakHashMap;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: vernon
 * @Date: 2020/12/9 11:22
 * @Description:
 */
@Destroyable(Log4jDestroy.class)
public class AppenderV1RegisterInterceptor extends AroundInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AppenderV1RegisterInterceptor.class);

    private ConcurrentWeakHashMap cache = new ConcurrentWeakHashMap();

    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;

    public AppenderV1RegisterInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public void doAfter(Advice advice) {
        if (!isBusinessLogOpen) {
            return;
        }

        Object[] args = advice.getParameterArray();
        if (args == null || args.length != 1) {
            return;
        }
        Appender appender = (Appender)args[0];
        if (cache.get(appender) != null) {
            return;
        }

        Object target = advice.getTarget();
        if (!(target instanceof Category)) {
            return;
        }
        if (appender.getName().startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
            return;
        }
        Category category = (Category)target;

        Appender ptAppender = ShadowAppenderCreatorFacadeV1.createShadowAppenderCreator(appender, bizShadowLogPath);
        if (ptAppender != null) {
            category.addAppender(ptAppender);
            cache.put(appender, ptAppender);
        }
    }

    @Override
    protected void clean() {
        cache.clear();
    }
}
