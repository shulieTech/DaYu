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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/24 2:37 下午
 */
@SuppressWarnings({"rawtypes","unchecked"})
@ListenerBehavior(isFilterClusterTest = true)
public class GetAllInterceptor extends IterableKeyInterceptor {

    @Override
    public Object getResult0(Advice advice) {
        Object returnObj = advice.getReturnObj();
        if (!Pradar.isClusterTest()) {
            return returnObj;
        }
        if (!(returnObj instanceof Map)) {
            return returnObj;
        }
        Map returnMap = (Map)returnObj;
        if (returnMap.size() == 0) {
            return returnObj;
        }
        Map resultMap = new HashMap();
        Set<Map.Entry> entries = returnMap.entrySet();
        if (Pradar.isClusterTest()) {
            for (Map.Entry entry : entries) {
                if (entry.getKey() instanceof ClusterTestCacheWrapperKey) {
                    resultMap.put(((ClusterTestCacheWrapperKey)entry.getKey()).getKey(), entry.getValue());
                }
            }
        }
        return resultMap;
    }

}
