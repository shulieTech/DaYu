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
package com.pamirs.attach.plugin.caffeine.interceptor;

import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 4:31 下午
 */
@ListenerBehavior(isFilterClusterTest = true)
public class FirstKeyInterceptor extends AbstractChangeCacheKeyTraceInterceptor {
    @Override
    protected int getKeyIndex(Object[] parameterArray) {
        return 0;
    }
}
