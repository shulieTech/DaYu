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
package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.attach.plugin.shadowjob.adapter.LtsAdapter;
import com.pamirs.attach.plugin.shadowjob.destory.JobDestroy;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.internal.adapter.JobAdapter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author angju
 * @date 2020/7/20 22:57
 */
@Destroyable(JobDestroy.class)
public class LtsInitAdapterInterceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) {
        if (null == GlobalConfig.getInstance().getJobAdaptor(JobAdapter.SHADOW_LTS)) {
            LtsAdapter ltsAdapter = new LtsAdapter();
            GlobalConfig.getInstance().addJobAdaptor(ltsAdapter.getJobName(), ltsAdapter);
        }
    }
}
