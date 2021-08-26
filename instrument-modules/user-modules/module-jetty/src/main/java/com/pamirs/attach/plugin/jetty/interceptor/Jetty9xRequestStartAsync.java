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


import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.jetty.Jetty9xAsyncListener;
import com.pamirs.pradar.Pradar;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/25 2:02 下午
 */
public class Jetty9xRequestStartAsync implements JettyRequestStartAsync {
    private final AsyncContext asyncContext;
    private final ServletRequest servletRequest;
    private final ServletResponse servletResponse;
    private final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer;

    public Jetty9xRequestStartAsync(final Object asyncContext, final ServletRequest servletRequest, final ServletResponse servletResponse, final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer) {
        this.asyncContext = (AsyncContext) asyncContext;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.requestTracer = requestTracer;
    }

    @Override
    public void startAsync() {
        final AsyncListener asyncListener = new Jetty9xAsyncListener(asyncContext, Pradar.popInvokeContextMap(), (HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestTracer);
        asyncContext.addListener(asyncListener);
    }
}
