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
package com.pamirs.attach.plugin.rabbitmqv2.consumer.model;

import com.pamirs.pradar.exception.PradarException;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/29 6:51 下午
 */
public class AdminAccessInfo {

    private final String host;

    private final int port;

    private final String username;

    private final String password;

    private final String virtualHost;

    public AdminAccessInfo(String host, int port, String username, String password, String virtualHost) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;
    }

    public String credentialsEncode() {
        BASE64Encoder encoder = new BASE64Encoder();
        String authString = username + ":" + password;
        return "Basic " + encoder.encode(authString.getBytes(Charset.forName("UTF-8")));
    }

    public String getVirtualHostEncode() {
        try {
            return URLEncoder.encode(virtualHost, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public static AdminAccessInfo solveByConnection(Connection connection) {
        String username;
        String password;
        String host;
        int port;
        Object object = Reflect.on(connection).getSilence("credentialsProvider");
        if (object != null) {//低版本
            CredentialsProvider credentialsProvider = (CredentialsProvider)object;
            username = credentialsProvider.getUsername();
            password = credentialsProvider.getPassword();
        } else {
            username = Reflect.on(connection).getSilence("username");
            password = Reflect.on(connection).getSilence("password");
            if (username == null || password == null) {
                throw new PradarException("未支持的rabbitmq版本！无法获取rabbit连接用户名密码");
            }
        }
        InetAddress inetAddress = connection.getAddress();
        String virtualHost = Reflect.on(connection).get("_virtualHost");
        host = inetAddress.getHostAddress();
        port = Integer.parseInt("1" + connection.getPort());
        return new AdminAccessInfo(host, port, username, password, virtualHost);
    }
}
