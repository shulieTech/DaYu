package com.pamirs.attach.plugin.apache.hbase.interceptor;

import com.pamirs.attach.plugin.apache.hbase.interceptor.shadowserver.HbaseMediatorConnection;
import com.pamirs.attach.plugin.apache.hbase.utils.ShadowConnectionHolder;
import com.pamirs.attach.plugin.apache.hbase.utils.ShadowConnectionHolder.Supplier;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.security.User;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/04/14 4:37 PM
 * 这个切点是对Hbase影子集群的补充，针对获取一次连接多次使用的场景，没有走ConnectionFactory.createConnection()
 * 因为这个切点版本兼容起来比较麻烦，并且有可能业务不使用hbase底层的connection，
 * 所以影子集群功能我们希望还是尽可能走ConnectionShadowInterceptor
 * 这个需要对ConnectionShadowInterceptor的影子connection做兼容
 * see com.pamirs.attach.plugin.apache.hbase.interceptor.ConnectionShadowInterceptor
 */
@ListenerBehavior(isFilterClusterTest = true)
public class HConnectionShadowReplaceInterceptor extends CutoffInterceptorAdaptor {

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(HConnectionShadowReplaceInterceptor.class);

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (advice.getTarget() instanceof HbaseMediatorConnection) {
            return CutOffResult.passed();
        }
        if (ShadowConnectionHolder.isPtConnection((ClusterConnection)advice.getTarget())) {
            return CutOffResult.passed();
        }
        Object target = advice.getTarget();
        if (!(target instanceof ClusterConnection)) {
            return CutOffResult.passed();
        }
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }
        if (!GlobalConfig.getInstance().isShadowHbaseServer()) {
            return CutOffResult.passed();
        }
        ClusterConnection ptClusterConnection = ShadowConnectionHolder.computeIfAbsent((ClusterConnection)target,
            new Supplier() {
                @Override
                public ClusterConnection get(ClusterConnection busClusterConnection) {
                    Configuration busConfiguration = busClusterConnection.getConfiguration();
                    Configuration ptConfiguration = ShadowConnectionHolder.matching(busConfiguration);
                    if (ptConfiguration != null) {
                        try {
                            Connection prefConnection = ConnectionFactory.createConnection(ptConfiguration,
                                (ExecutorService)ReflectionUtils.get(busClusterConnection,"batchPool"),
                                (User)ReflectionUtils.get(busClusterConnection,"user"));
                            return (ClusterConnection)prefConnection;
                        } catch (IOException e) {
                            throw new RuntimeException("[hbase] create shadow connection fail!", e);
                        }
                    }
                    return null;
                }
            });
        if (ptClusterConnection != null) {
            return CutOffResult.cutoff(process(ptClusterConnection, advice.getBehaviorName(), advice.getParameterArray()));
        } else {
            Configuration busConfiguration = ((ClusterConnection)target).getConfiguration();

            String quorum = busConfiguration.get(HConstants.ZOOKEEPER_QUORUM);
            String port = busConfiguration.get(HConstants.ZOOKEEPER_CLIENT_PORT);
            String znode = busConfiguration.get(HConstants.ZOOKEEPER_ZNODE_PARENT);

            throw new PressureMeasureError(
                "[hbase]hbase未配置影子库, HConnectionShadowReplaceInterceptor business config quorums:  " + quorum
                    + " +, port: " + port + ", znode:" + znode + " ---------- ");
        }
    }

    private Object process(ClusterConnection ptClusterConnection, String behaviorName, Object[] args) throws Exception {
        if ("isMasterRunning".equals(behaviorName)) {
            return ptClusterConnection.isMasterRunning();
        } else if ("isTableAvailable".equals(behaviorName)) {
            if (args.length == 2) {
                if (args[0] instanceof byte[] && args[1] instanceof byte[][]) {
                    return ptClusterConnection.isTableAvailable((byte[])args[0], (byte[][])args[1]);
                } else if (args[0] instanceof TableName && args[1] instanceof byte[][]) {
                    return ptClusterConnection.isTableAvailable((TableName)args[0], (byte[][])args[1]);
                }
            } else if (args.length == 1) {
                if (args[0] instanceof byte[]) {
                    return ptClusterConnection.isTableAvailable((byte[])args[0]);
                } else if (args[0] instanceof TableName) {
                    return ptClusterConnection.isTableAvailable((TableName)args[0]);
                }
            }
        } else if ("locateRegion".equals(behaviorName)) {
            if (args.length == 2) {
                if (args[0] instanceof byte[] && args[1] instanceof byte[]) {
                    return ptClusterConnection.locateRegion((byte[])args[0], (byte[])args[1]);
                } else if (args[0] instanceof TableName && args[1] instanceof byte[]) {
                    return ptClusterConnection.locateRegion((TableName)args[0], (byte[])args[1]);
                }
            } else if (args.length == 1) {
                if (args[0] instanceof byte[]) {
                    return ptClusterConnection.locateRegion((byte[])args[0]);
                }
            } else if (args.length == 4) {
                ptClusterConnection.locateRegion((TableName)args[0], (byte[])args[1], (Boolean)args[2], (Boolean)args[3]);
            } else if (args.length == 5) {
                ptClusterConnection.locateRegion((TableName)args[0], (byte[])args[1], (Boolean)args[2], (Boolean)args[3],
                    (Integer)args[4]);
            }
        } else if ("clearRegionCache".equals(behaviorName)) {
            if (args.length == 1) {
                if (args[0] instanceof byte[]) {
                    ptClusterConnection.clearRegionCache((byte[])args[0]);
                } else if (args[0] instanceof TableName) {
                    ptClusterConnection.clearRegionCache((TableName)args[0]);
                }
            }
        } else if ("cacheLocation".equals(behaviorName)) {
            ptClusterConnection.cacheLocation((TableName)args[0], (RegionLocations)args[1]);
            return null;
        } else if ("deleteCachedRegionLocation".equals(behaviorName)) {
            ptClusterConnection.deleteCachedRegionLocation((HRegionLocation)args[0]);
            return null;
        } else if ("relocateRegion".equals(behaviorName)) {
            if (args.length == 3) {
                return ptClusterConnection.relocateRegion((TableName)args[0], (byte[])args[1], (Integer)args[2]);
            } else if (args[0] instanceof TableName) {
                return ptClusterConnection.relocateRegion((TableName)args[0], (byte[])args[1]);
            } else if (args[0] instanceof byte[]) {
                return ptClusterConnection.relocateRegion((byte[])args[0], (byte[])args[1]);
            }
        } else if ("updateCachedLocations".equals(behaviorName)) {
            if (args.length == 5) {
                ptClusterConnection.updateCachedLocations((TableName)args[0], (byte[])args[1], (byte[])args[2], args[3],
                    (ServerName)args[4]);
                return null;
            } else if (args[0] instanceof TableName) {
                ptClusterConnection.updateCachedLocations((TableName)args[0], (byte[])args[1], args[2],
                    (HRegionLocation)args[3]);
                return null;
            } else if (args[0] instanceof byte[]) {
                ptClusterConnection.updateCachedLocations((byte[])args[0], (byte[])args[1], args[2],
                    (HRegionLocation)args[3]);
                return null;
            }
        } else if ("locateRegions".equals(behaviorName)) {
            if (args.length == 3) {
                if (args[0] instanceof TableName) {
                    return ptClusterConnection.locateRegions((TableName)args[0], (Boolean)args[1], (Boolean)args[2]);
                } else {
                    return ptClusterConnection.locateRegions((byte[])args[0], (Boolean)args[1], (Boolean)args[2]);
                }
            } else if (args[0] instanceof TableName) {
                return ptClusterConnection.locateRegions((TableName)args[0]);
            } else if (args[0] instanceof byte[]) {
                return ptClusterConnection.locateRegions((byte[])args[0]);
            }
        } else if ("getMaster".equals(behaviorName)) {
            return ptClusterConnection.getMaster();
        } else if ("getAdmin".equals(behaviorName)) {
            return ptClusterConnection.getAdmin();
        } else if ("getClient".equals(behaviorName)) {
            return ptClusterConnection.getClient((ServerName)args[0]);
        } else if ("getRegionLocation".equals(behaviorName)) {
            if (args[0] instanceof TableName) {
                return ptClusterConnection.getRegionLocation((TableName)args[0], (byte[])args[1], (Boolean)args[2]);
            } else if (args[0] instanceof byte[]) {
                return ptClusterConnection.getRegionLocation((byte[])args[0], (byte[])args[1], (Boolean)args[2]);
            }
        } else if ("clearCaches".equals(behaviorName)) {
            ptClusterConnection.clearCaches((ServerName)args[0]);
            return null;
        } else if ("getKeepAliveMasterService".equals(behaviorName)) {
            return ptClusterConnection.getKeepAliveMasterService();
        } else if ("isDeadServer".equals(behaviorName)) {
            return ptClusterConnection.isDeadServer((ServerName)args[0]);
        } else if ("getNonceGenerator".equals(behaviorName)) {
            return ptClusterConnection.getNonceGenerator();
        } else if ("getAsyncProcess".equals(behaviorName)) {
            return ptClusterConnection.getAsyncProcess();
        } else if ("getNewRpcRetryingCallerFactory".equals(behaviorName)) {
            return ptClusterConnection.getNewRpcRetryingCallerFactory((Configuration)args[0]);
        } else if ("getRpcRetryingCallerFactory".equals(behaviorName)) {
            return ptClusterConnection.getRpcRetryingCallerFactory();
        } else if ("getRpcControllerFactory".equals(behaviorName)) {
            return ptClusterConnection.getRpcControllerFactory();
        } else if ("getConnectionConfiguration".equals(behaviorName)) {
            return ptClusterConnection.getConnectionConfiguration();
        } else if ("isManaged".equals(behaviorName)) {
            return ptClusterConnection.isManaged();
        } else if ("getStatisticsTracker".equals(behaviorName)) {
            return ptClusterConnection.getStatisticsTracker();
        } else if ("getBackoffPolicy".equals(behaviorName)) {
            return ptClusterConnection.getBackoffPolicy();
        } else if ("getConnectionMetrics".equals(behaviorName)) {
            return ptClusterConnection.getConnectionMetrics();
        } else if ("hasCellBlockSupport".equals(behaviorName)) {
            return ptClusterConnection.hasCellBlockSupport();
        } else if ("getConfiguration".equals(behaviorName)) {
            return ptClusterConnection.getConfiguration();
        } else if ("getTable".equals(behaviorName)) {
            if (args.length == 1) {
                if (args[0] instanceof String) {
                    return ptClusterConnection.getTable((String)args[0]);
                } else if (args[0] instanceof byte[]) {
                    return ptClusterConnection.getTable((byte[])args[0]);
                } else if (args[0] instanceof TableName) {
                    return ptClusterConnection.getTable((TableName)args[0]);
                }
            } else {
                if (args[0] instanceof String) {
                    return ptClusterConnection.getTable((String)args[0], (ExecutorService)args[1]);
                } else if (args[0] instanceof byte[]) {
                    return ptClusterConnection.getTable((byte[])args[0], (ExecutorService)args[1]);
                } else if (args[0] instanceof TableName) {
                    return ptClusterConnection.getTable((TableName)args[0], (ExecutorService)args[1]);
                }
            }
        } else if ("getRegionLocator".equals(behaviorName)) {
            return ptClusterConnection.getRegionLocator((TableName)args[0]);
        } else if ("isTableEnabled".equals(behaviorName)) {
            if (args[0] instanceof TableName) {
                return ptClusterConnection.isTableEnabled((TableName)args[0]);
            } else if (args[0] instanceof byte[]) {
                return ptClusterConnection.isTableEnabled((byte[])args[0]);
            }
        } else if ("isTableDisabled".equals(behaviorName)) {
            if (args[0] instanceof TableName) {
                return ptClusterConnection.isTableDisabled((TableName)args[0]);
            } else if (args[0] instanceof byte[]) {
                return ptClusterConnection.isTableDisabled((byte[])args[0]);
            }
        } else if ("listTables".equals(behaviorName)) {
            return ptClusterConnection.listTables();
        } else if ("getTableNames".equals(behaviorName)) {
            return ptClusterConnection.getTableNames();
        } else if ("listTableNames".equals(behaviorName)) {
            return ptClusterConnection.listTableNames();
        } else if ("getHTableDescriptor".equals(behaviorName)) {
            if (args[0] instanceof TableName) {
                return ptClusterConnection.getHTableDescriptor((TableName)args[0]);
            } else if (args[0] instanceof byte[]) {
                return ptClusterConnection.getHTableDescriptor((byte[])args[0]);
            }
        } else if ("processBatch".equals(behaviorName)) {
            if (args[1] instanceof TableName) {
                ptClusterConnection.processBatch((List<? extends Row>)args[0], (TableName)args[1], (ExecutorService)args[2],
                    (Object[])args[3]);
                return null;
            } else if (args[1] instanceof byte[]) {
                ptClusterConnection.processBatch((List<? extends Row>)args[0], (byte[])args[1], (ExecutorService)args[2],
                    (Object[])args[3]);
                return null;
            }
        } else if ("processBatchCallback".equals(behaviorName)) {
            if (args[1] instanceof TableName) {
                ptClusterConnection.processBatchCallback((List<? extends Row>)args[0], (TableName)args[1],
                    (ExecutorService)args[2], (Object[])args[3], (Batch.Callback<? extends Object>)args[4]);
                return null;
            } else if (args[1] instanceof byte[]) {
                ptClusterConnection.processBatchCallback((List<? extends Row>)args[0], (byte[])args[1],
                    (ExecutorService)args[2], (Object[])args[3], (Batch.Callback<? extends Object>)args[4]);
                return null;
            }
        } else if ("setRegionCachePrefetch".equals(behaviorName)) {
            if (args[0] instanceof TableName) {
                ptClusterConnection.setRegionCachePrefetch((TableName)args[0], (Boolean)args[1]);
                return null;
            } else if (args[0] instanceof byte[]) {
                ptClusterConnection.setRegionCachePrefetch((byte[])args[0], (Boolean)args[1]);
                return null;
            }
        } else if ("getRegionCachePrefetch".equals(behaviorName)) {
            if (args[0] instanceof TableName) {
                return ptClusterConnection.getRegionCachePrefetch((TableName)args[0]);
            } else if (args[0] instanceof byte[]) {
                return ptClusterConnection.getRegionCachePrefetch((byte[])args[0]);
            }
        } else if ("getCurrentNrHRS".equals(behaviorName)) {
            return ptClusterConnection.getCurrentNrHRS();
        } else if ("getHTableDescriptorsByTableName".equals(behaviorName)) {
            return ptClusterConnection.getHTableDescriptorsByTableName((List<TableName>)args[0]);
        } else if ("getHTableDescriptors".equals(behaviorName)) {
            return ptClusterConnection.getHTableDescriptors((List<String>)args[0]);
        } else if ("isClosed".equals(behaviorName)) {
            return ptClusterConnection.isClosed();
        } else if ("getBufferedMutator".equals(behaviorName)) {
            if (args[0] instanceof TableName) {
                return ptClusterConnection.getBufferedMutator((TableName)args[0]);
            } else if (args[0] instanceof BufferedMutatorParams) {
                return ptClusterConnection.getBufferedMutator((BufferedMutatorParams)args[0]);
            }
        } else if ("close".equals(behaviorName)) {
            ptClusterConnection.close();
            return null;
        }
        throw throwError(behaviorName, args);
    }

    private RuntimeException throwError(String behaviorName, Object[] args) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object arg : args) {
            if (!first) {
                sb.append(",");
            }
            sb.append(arg.getClass().getName());
            first = false;
        }
        return new RuntimeException(
            "[hbase] shadow connection method invoke process error! behaviorName : " + behaviorName + " args : " + sb);
    }
}
