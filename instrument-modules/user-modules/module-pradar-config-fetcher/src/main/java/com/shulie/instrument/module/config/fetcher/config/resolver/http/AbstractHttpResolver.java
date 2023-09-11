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
package com.shulie.instrument.module.config.fetcher.config.resolver.http;

import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.SimulatorDynamicConfig;
import com.shulie.instrument.module.config.fetcher.config.AbstractConfig;
import com.shulie.instrument.module.config.fetcher.config.impl.ApplicationConfig;
import com.shulie.instrument.module.config.fetcher.config.resolver.ConfigResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author shiyajian
 * @since 2020-08-12
 */
public abstract class AbstractHttpResolver<T extends AbstractConfig<T>> implements ConfigResolver<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpResolver.class);

    private final String name;

    private int period = 60;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    private ScheduledFuture future;
    private ScheduledExecutorService service;

    public AbstractHttpResolver(String name, int period, TimeUnit timeUnit) {
        this.name = name;
        if (period > 0) {
            this.period = period;
        }
        if (timeUnit != null) {
            this.timeUnit = timeUnit;
        }
        this.service = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Pradar-Fetch-Config-Service");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        LOGGER.error("Thread {} caught a unknow exception with UncaughtExceptionHandler", t.getName(), e);
                    }
                });
                return t;
            }
        });
    }

    @Override
    public void resolve(T refreshConfig) {
        // scheduled load
        future = service.scheduleAtFixedRate(getRunnableTask(refreshConfig), 20, this.period, timeUnit);
    }

    @Override
    public void triggerFetch(T refreshConfig) {
        refreshConfig.refresh(fetch());
    }

    @Override
    public void destroy() {
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        }
        service.shutdownNow();
    }

    private Runnable getRunnableTask(final T refreshConfig) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    //  全局静默模式下/请求控制台超时等失败时, 不从控制台拉取应用配置
                    if (refreshConfig instanceof ApplicationConfig && (SimulatorDynamicConfig.isAbortPollingAppConfig() || Pradar.isSilenceDegraded())) {
                        return;
                    }
                    refreshConfig.refresh(fetch());
                } catch (Throwable e) {
                    LOGGER.error("定时获取配置异常:", e);
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.AgentError)
                            .setErrorCode("agent-0005")
                            .setMessage("定时获取配置异常")
                            .setDetail(e.getMessage())
                            .report();
                }
            }
        };
    }

}
