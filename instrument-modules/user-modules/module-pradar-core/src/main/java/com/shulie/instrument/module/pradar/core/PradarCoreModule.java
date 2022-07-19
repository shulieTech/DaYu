/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.module.pradar.core;

import javax.annotation.Resource;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.common.ClassUtils;
import com.pamirs.pradar.debug.DebugHelper;
import com.pamirs.pradar.internal.GlobalConfigService;
import com.pamirs.pradar.internal.PradarInternalService;
import com.pamirs.pradar.pressurement.agent.shared.exit.ArbiterHttpExit;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.SqlMetadataParser;
import com.pamirs.pradar.upload.uploader.AgentOnlineUploader;
import com.pamirs.pradar.utils.MonitorCollector;
import com.shulie.instrument.module.pradar.core.handler.DefaultExceptionHandler;
import com.shulie.instrument.module.pradar.core.handler.DefaultExecutionTagSupplier;
import com.shulie.instrument.module.pradar.core.handler.EmptyExecutionTagSupplier;
import com.shulie.instrument.module.pradar.core.handler.OnlySilenceExecutionTagSupplier;
import com.shulie.instrument.module.pradar.core.service.DefaultGlobalConfigService;
import com.shulie.instrument.module.pradar.core.service.DefaultPradarInternalService;
import com.shulie.instrument.module.pradar.core.service.DefaultPradarService;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.resource.ModuleCommandInvoker;
import com.shulie.instrument.simulator.message.Messager;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于公共依赖的模块
 * 需要在module.config文件中指定要导出的包列表, 给其他的模块依赖
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/1 12:22 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "pradar-core", version = "1.0.0", author = "xiaobin@shulie.io",
        description = "pradar core 模式，提供链路追踪 trace 埋点以及压测标等服务")
public class PradarCoreModule extends ModuleLifecycleAdapter implements ExtensionModule {
    private final static Logger logger = LoggerFactory.getLogger(PradarCoreModule.class);

    private MonitorCollector monitorCollector;

    @Resource
    private ModuleCommandInvoker moduleCommandInvoker;

    @Override
    public boolean onActive() throws Throwable {
        //将simulator home路径和plugin相关的配置全部导入到system property中
        String home = simulatorConfig.getSimulatorHome();
        if (home != null) {
            System.setProperty("simulator.home", home);
        }
        Integer requestSize = simulatorConfig.getIntProperty("plugin.request.size");
        if (requestSize != null) {
            System.setProperty("plugin.request.size", String.valueOf(requestSize));
        }

        Integer responseSize = simulatorConfig.getIntProperty("plugin.response.size");
        if (responseSize != null) {
            System.setProperty("plugin.response.size", String.valueOf(responseSize));
        }

        Boolean requestOn = simulatorConfig.getBooleanProperty("plugin.request.on");
        if (requestOn != null) {
            System.setProperty("plugin.request.on", String.valueOf(requestOn));
        }

        Boolean responseOn = simulatorConfig.getBooleanProperty("plugin.response.on");
        if (responseOn != null) {
            System.setProperty("plugin.response.on", String.valueOf(responseOn));
        }

        PradarService.registerPradarService(new DefaultPradarService());
        PradarInternalService.registerService(new DefaultPradarInternalService());
        DebugHelper.registerModuleCommandInvoker(moduleCommandInvoker);
        GlobalConfigService.registerService(new DefaultGlobalConfigService());

        /**
         * 注册自定义的异常处理器
         */
        Messager.registerExceptionHandler(new DefaultExceptionHandler());
        /**
         * 注册自定义的执行 tag 提供者
         */
        String key = System.getProperty("simulator.execution.type");
        if ("empty".equals(key)) {
            Messager.registerExecutionTagSupplier(new EmptyExecutionTagSupplier());
            logger.info("use execution type : {}", key);
        } else if ("silence".equals(key)) {
            Messager.registerExecutionTagSupplier(new OnlySilenceExecutionTagSupplier());
            logger.info("use execution type : {}", key);
        } else {
            Messager.registerExecutionTagSupplier(new DefaultExecutionTagSupplier());
        }

        monitorCollector = MonitorCollector.getInstance();
        monitorCollector.start();
        return true;
    }

    @Override
    public void onFrozen() throws Throwable {
        EventRouter.router().shutdown();
        AgentOnlineUploader.getInstance().shutdown();
        if (monitorCollector != null) {
            monitorCollector.stop();
        }
        Pradar.shutdown();
    }

    @Override
    public void onUnload() throws Throwable {
        PradarService.registerPradarService(null);
        PradarInternalService.registerService(null);
        GlobalConfigService.registerService(null);
        GlobalConfig.getInstance().release();
        PradarSwitcher.destroy();
        ArbiterHttpExit.release();
        SqlParser.release();
        ClassUtils.release();
        PradarSpringUtil.release();
        SqlMetadataParser.clear();
    }
}
