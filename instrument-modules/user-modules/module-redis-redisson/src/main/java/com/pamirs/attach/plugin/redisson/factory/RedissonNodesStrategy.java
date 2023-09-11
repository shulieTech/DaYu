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
package com.pamirs.attach.plugin.redisson.factory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.pamirs.attach.plugin.common.datasource.redisserver.RedisServerNodesStrategy;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.redisson.RedissonConstants;
import com.pamirs.attach.plugin.redisson.utils.RedissonUtils;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.MasterSlaveServersConfig;
import org.redisson.config.ReplicatedServersConfig;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: vernon
 * @Date: 2020/11/26 14:32
 * @Description:
 */
public class RedissonNodesStrategy implements RedisServerNodesStrategy {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public List<String> match(Object client) {
        if (client == null) {
            return null;
        }

        Config config = null;
        if (client instanceof RedissonClient) {
            RedissonClient redissonClient = (RedissonClient) client;
            config = redissonClient.getConfig();

        } else if (client instanceof RedissonRxClient) {
            RedissonRxClient redissonRxClient = (RedissonRxClient) client;
            config = redissonRxClient.getConfig();

        } else if (client instanceof RedissonReactiveClient) {
            RedissonReactiveClient redissonReactiveClient = (RedissonReactiveClient) client;
            config = redissonReactiveClient.getConfig();
        }

        List<String> addrs = getAddress(config);

        return addrs;

    }

    public static List getAddress(Config config) {

        SingleServerConfig singleServerConfig = null;
        try {
            singleServerConfig = ReflectionUtils.get(config,RedissonConstants.DYNAMIC_FIELD_SINGLE_SERVER_CONFIG);
        } catch (ReflectException e) {
        }
        ClusterServersConfig clusterServersConfig = null;
        try {
            clusterServersConfig = ReflectionUtils.get(config,RedissonConstants.DYNAMIC_FIELD_CLUSTER_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        SentinelServersConfig sentinelServersConfig = null;
        try {
            sentinelServersConfig = ReflectionUtils.get(config,RedissonConstants.DYNAMIC_FIELD_SENTINEL_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        ReplicatedServersConfig replicatedServersConfig = null;
        try {
            replicatedServersConfig = ReflectionUtils.get(config,RedissonConstants.DYNAMIC_FIELD_REPLICATED_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        MasterSlaveServersConfig masterSlaveServersConfig = null;
        try {
            masterSlaveServersConfig = ReflectionUtils.get(config,RedissonConstants.DYNAMIC_FIELD_MASTER_SLAVE_SERVERS_CONFIG);
        } catch (ReflectException e) {
        }
        if (singleServerConfig != null) {
            //在这里返回的address在不同版本可能返回String，可能返回URI在这里做处理
            Object address=ReflectionUtils.get(singleServerConfig,"address");
            String addressConvert=null;
            if(address instanceof URI){
                URI uriConvert=(URI)address;
                addressConvert=RedissonUtils.addPre(new StringBuilder().append(uriConvert.getHost()).append(":").append(uriConvert.getPort()).toString());
            }else if(address instanceof String){
                addressConvert=(String)address;
            }
            return RedissonUtils.removePre(addressConvert);
        } else if (clusterServersConfig != null) {
            return RedissonUtils.removePre(clusterServersConfig.getNodeAddresses());
        } else if (sentinelServersConfig != null) {
            List<String> result = RedissonUtils.removePre(sentinelServersConfig.getSentinelAddresses());
            result.add(sentinelServersConfig.getMasterName());
            return result;
        } else if (replicatedServersConfig != null) {
            return RedissonUtils.removePre(replicatedServersConfig.getNodeAddresses());
        } else if (masterSlaveServersConfig != null) {
            String master = masterSlaveServersConfig.getMasterAddress();
            Set<String> slave = masterSlaveServersConfig.getSlaveAddresses();
            List<String> result = new ArrayList(RedissonUtils.removePre(slave));
            result.addAll(RedissonUtils.removePre(master));
            return result;

        }
        return null;
    }
}
