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
package com.pamirs.attach.plugin.shadowjob.common.quartz;

import com.pamirs.attach.plugin.shadowjob.common.quartz.impl.Quartz1JobHandler;
import com.pamirs.attach.plugin.shadowjob.common.quartz.impl.Quartz2JobHandler;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 7:39 下午
 */
public final class QuartzJobHandlerProvider {

    public static QuartzJobHandler getHandler() {
        try {
            Class.forName("org.quartz.JobKey");
            return new Quartz2JobHandler();
        } catch (Throwable e) {
            return new Quartz1JobHandler();
        }
    }
}
