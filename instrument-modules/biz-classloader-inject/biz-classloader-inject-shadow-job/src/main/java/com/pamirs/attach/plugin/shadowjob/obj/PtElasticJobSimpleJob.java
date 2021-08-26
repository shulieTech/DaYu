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
package com.pamirs.attach.plugin.shadowjob.obj;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.pamirs.pradar.internal.PradarInternalService;

/**
 * @author angju
 * @date 2021/3/23 21:44
 */
public class PtElasticJobSimpleJob implements SimpleJob {

    private SimpleJob simpleJob;


    @Override
    public void execute(ShardingContext shardingContext) {
        PradarInternalService.startTrace(null, simpleJob.getClass().getName(), "execute");
        PradarInternalService.middlewareName("elastic-job");
        PradarInternalService.setClusterTest(true);
        simpleJob.execute(shardingContext);
        PradarInternalService.setClusterTest(false);
        PradarInternalService.endTrace(null, 7);
    }

    public void setSimpleJob(SimpleJob simpleJob){
        this.simpleJob = simpleJob;
    }
}
