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
package com.pamirs.attach.plugin.es.shadowserver.transport;

import com.pamirs.attach.plugin.es.shadowserver.rest.definition.TransportClientDefinition;
import com.pamirs.attach.plugin.es.shadowserver.transport.definition.PreBuiltTransportClientDefinition;
import com.pamirs.attach.plugin.es.shadowserver.transport.definition.TransportClient2XDefinition;
import com.pamirs.pradar.exception.PressureMeasureError;
import org.elasticsearch.client.transport.TransportClient;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/04/12 4:27 下午
 */
public class TransportClientDefinitionStrategy {

    private static final List<Matcher> registryMatchers = new ArrayList<Matcher>();

    static {
        registryMatchers.add(new Matcher() {
            @Override
            public TransportClientDefinition definition () {
                return new TransportClient2XDefinition();
            }

            @Override
            public boolean support (TransportClient transportClient){
                return Modifier.isAbstract(transportClient.getClass().getModifiers());
            }
        });
        registryMatchers.add(new Matcher() {
            @Override
            public TransportClientDefinition definition () {
                return new PreBuiltTransportClientDefinition();
            }

            @Override
            public boolean support (TransportClient transportClient){
                return "org.elasticsearch.transport.client.PreBuiltTransportClient".
                    equals(transportClient.getClass().getName());
            }
        });
    }

    public static TransportClientDefinition match(TransportClient transportClient) {
        for (Matcher registryMatcher : registryMatchers) {
            if (registryMatcher.support(transportClient)) {
                return registryMatcher.definition();
            }
        }
        throw new PressureMeasureError("未支持的TransportClient版本！:" + transportClient.getClass().getName());
    }

    public static void release() {
        registryMatchers.clear();
    }

    interface Matcher {

        boolean support(TransportClient transportClient);

        TransportClientDefinition definition();
    }
}
