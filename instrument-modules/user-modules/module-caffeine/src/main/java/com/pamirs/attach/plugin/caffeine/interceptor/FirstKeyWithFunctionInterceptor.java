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

import java.util.function.Function;

import com.pamirs.attach.plugin.caffeine.utils.WrapFunction;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/30 8:13 下午
 */
@ListenerBehavior(isFilterClusterTest = true)
public class FirstKeyWithFunctionInterceptor extends AbstractChangeCacheKeyAndLambdaInterceptor {

    @Override
    protected Object wrapLambda(Object lambda) {
        return (lambda instanceof Function) ? new WrapFunction((Function)lambda) : lambda;
    }

    @Override
    protected int getLambdaIndex(Object[] args) {
        return args.length - 1;
    }

    @Override

    protected int getKeyIndex(Object[] parameterArray) {
        return 0;
    }
}
