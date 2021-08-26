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
package com.pamirs.attach.plugin.jetty.interceptor;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/25 2:02 下午
 */
public interface JettyRequestStartAsync {

    /**
     * 开启异步
     */
    void startAsync();

}
