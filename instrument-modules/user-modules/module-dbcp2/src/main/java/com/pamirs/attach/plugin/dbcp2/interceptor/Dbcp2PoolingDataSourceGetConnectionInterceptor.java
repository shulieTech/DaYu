package com.pamirs.attach.plugin.dbcp2.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.HashMap;
import java.util.Map;

public class Dbcp2PoolingDataSourceGetConnectionInterceptor extends TraceInterceptorAdaptor {

    private static Map<Integer, String> jdbcUrlCache = new HashMap<Integer, String>();

    @Override
    public String getPluginName() {
        return "dbcp2";
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_DB;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        if (Pradar.getInvokeContext() == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        Object target = advice.getTarget();
        int hashCode = System.identityHashCode(target);
        String url = jdbcUrlCache.get(hashCode);
        if (url == null) {
            if (ReflectionUtils.existsField(target, "_pool")) {
                url = ReflectionUtils.getFieldValues(target,"_pool", "factory", "_connFactory", "_connectUri");
            } else {
                url = ReflectionUtils.getFieldValues(target,"pool", "factory", "connectionFactory", "connectionString");
            }
            jdbcUrlCache.put(hashCode, url);
        }
        record.setService(url);
        record.setMethod("PoolingDataSource#" + advice.getBehaviorName());
        record.setRequest(advice.getParameterArray());
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        if (Pradar.getInvokeContext() == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if (Pradar.getInvokeContext() == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getThrowable());
        return record;
    }

}
