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
package com.pamirs.attach.plugin.spring.cloud.gateway.tracer;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 3:05 下午
 */
public class ServerHttpRequestTracer extends RequestTracer<ServerHttpRequest, ServerHttpResponse> {

    @Override
    public String getHeader(ServerHttpRequest request, String key) {
        return request.getHeaders().getFirst(key);
    }

    @Override
    public String getRemoteAddr(ServerHttpRequest request) {
        return request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public String getRemotePort(ServerHttpRequest request) {
        return String.valueOf(request.getRemoteAddress().getPort());
    }

    @Override
    public String getRequestURI(ServerHttpRequest request) {
        return request.getPath().value();
    }

    @Override
    public String getMethod(ServerHttpRequest request) {
        return request.getMethod().name();
    }

    @Override
    public void setAttribute(ServerHttpRequest request, String key, Object value) {
        HttpHeaders headers = request.getHeaders();
        try {
            headers.add(key, value.toString());
        }catch (UnsupportedOperationException exception){
            //readOnlyHttpHeaders
            HttpHeaders httpHeaders = new HttpHeaders();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                httpHeaders.put(entry.getKey(),entry.getValue());
            }
            httpHeaders.add(key,value.toString());
            ReflectionUtils.setField(getHeaders(request), request, httpHeaders);
        }
    }

    @Override
    public Object getAttribute(ServerHttpRequest request, String key) {
        return request.getHeaders().getFirst(key);
    }

    @Override
    public long getContentLength(ServerHttpRequest request) {
        return 0;
    }

    @Override
    public String getParams(ServerHttpRequest request) {
        return null;
    }

    @Override
    public String getResponse(ServerHttpResponse response) {
        return null;
    }

    @Override
    public String getStatusCode(ServerHttpResponse response, Throwable throwable) {
        if (throwable != null) {
            return "500";
        }
        if (response == null) {
            return "200";
        }
        int code = response.getStatusCode() == null ? 200 : response.getStatusCode().value();
        return String.valueOf(code);
    }

    @Override
    public void setResponseHeader(ServerHttpResponse httpResponseInfo, String key, Object value) {
        if (httpResponseInfo == null) {
            return;
        }
        HttpHeaders headers = httpResponseInfo.getHeaders();
        try {
            headers.add(key, String.valueOf(value));
        }catch (UnsupportedOperationException exception){
            //readOnlyHttpHeaders
            HttpHeaders httpHeaders = new HttpHeaders();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                httpHeaders.put(entry.getKey(),entry.getValue());
            }
            httpHeaders.add(key,String.valueOf(value));
            ReflectionUtils.setField(getHeaders(httpResponseInfo),httpResponseInfo, httpHeaders );
        }
    }




    private static final Map<Class, Field> headersMap = new ConcurrentHashMap<Class, Field>();


    private Field getHeaders(Object target) {
        Field field = headersMap.get(target.getClass());
        if(field != null){
            return field;
        }
        field = ReflectionUtils.findField(target.getClass(), "headers");
        headersMap.put(target.getClass(),field);
        return field;
    }

}
