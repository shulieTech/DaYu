package com.pamirs.attach.plugin.jdk.http.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.pamirs.pradar.pressurement.mock.MockStrategy;
import com.pamirs.pradar.script.ScriptEvaluator;
import com.pamirs.pradar.script.ScriptManager;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author jiangjibo
 * @date 2022/3/3 2:16 下午
 * @description:
 */
public class HttpURLConnectionGetInputStreamInterceptor extends HttpURLConnectionInterceptor {

    /**
     * 一次请求getInputStream()可能会被调用多次，确保只打印一条trace
     */
    private static ThreadLocal<Integer> traceFlag = new ThreadLocal<Integer>();

    private static ExecutionStrategy fixJsonStrategy = new JsonMockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
            MatchConfig config = (MatchConfig) params;
            String ret = config.getScriptContent();
            Pradar.mockResponse(ret);
            ProcessController.returnImmediately(new ByteArrayInputStream(ret.getBytes()));
            return null;
        }
    };

    private static ExecutionStrategy mockStrategy = new MockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
            MatchConfig config = (MatchConfig) params;
            String scriptContent = config.getScriptContent();
            ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
            Object result = evaluator.evaluate(scriptContent, config.getArgs());

            Pradar.mockResponse(result);

            if (result.getClass().isAssignableFrom(InputStream.class)) {
                ProcessController.returnImmediately(result);
            }
            ProcessController.returnImmediately(new ByteArrayInputStream(GsonFactory.getGson().toJson(result).getBytes()));
            return null;
        }
    };

    @Override
    public void beforeFirst(Advice advice) throws ProcessControlException {
        if (!ClusterTestUtils.enableMock()) {
            return;
        }
        Object target = advice.getTarget();
        final HttpURLConnection request = (HttpURLConnection) target;
        final URL url = request.getURL();
        String fullPath = getService(url.getProtocol(),
                url.getHost(),
                url.getPort(),
                url.getPath());
        String whiteList = request.getRequestProperty(PradarService.PRADAR_WHITE_LIST_CHECK);

        MatchConfig config = ClusterTestUtils.httpClusterTest(fullPath);
        ExecutionStrategy strategy = config.getStrategy();

        config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, whiteList);
        // 白名单需要的信息
        config.addArgs("url", fullPath);
        // mock转发需要信息
        config.addArgs("request", request);
        config.addArgs("method", "url");
        config.addArgs("isInterface", Boolean.FALSE);

        if (strategy instanceof JsonMockStrategy) {
            strategy = fixJsonStrategy;
        }
        if (strategy instanceof MockStrategy) {
            strategy = mockStrategy;
        }
        int hashCode = System.identityHashCode(advice.getTarget());

        // put/post mock请求直接打印trace
        String method = request.getRequestMethod();
        boolean hasBodyRequest = "post".equalsIgnoreCase(method) || "put".equalsIgnoreCase(method);
        // 之前是否记录过trace
        boolean hasTraceBefore = traceFlag.get() != null && traceFlag.get() == hashCode;

        if (hasBodyRequest && !hasTraceBefore) {
            traceFlag.set(hashCode);
            Pradar.startClientInvoke(request.getURL().getPath(), method);
        }
        try {
            strategy.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
        } finally {
            if (hasBodyRequest && !hasTraceBefore) {
                Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, getPluginType());
            }
        }
    }

}
