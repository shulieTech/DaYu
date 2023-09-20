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
package com.shulie.instrument.simulator.module.stack.trace;

import com.shulie.instrument.simulator.api.CommandResponse;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.annotation.Command;
import com.shulie.instrument.simulator.api.filter.ClassDescriptor;
import com.shulie.instrument.simulator.api.filter.ExtFilter;
import com.shulie.instrument.simulator.api.filter.Filter;
import com.shulie.instrument.simulator.api.filter.MethodDescriptor;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.listener.ext.BuildingForListeners;
import com.shulie.instrument.simulator.api.listener.ext.EventWatchBuilder;
import com.shulie.instrument.simulator.api.listener.ext.EventWatcher;
import com.shulie.instrument.simulator.api.resource.LoadedClassDataSource;
import com.shulie.instrument.simulator.api.resource.ModuleEventWatcher;
import com.shulie.instrument.simulator.api.util.ParameterUtils;
import com.shulie.instrument.simulator.api.util.StringUtil;
import com.shulie.instrument.simulator.module.model.trace2.TraceView;
import com.shulie.instrument.simulator.module.stack.trace.util.ConcurrentHashSet;
import com.shulie.instrument.simulator.module.stack.trace.util.PatternMatchUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.shulie.instrument.simulator.api.listener.ext.PatternType.WILDCARD;

/**
 * trace命令
 * <p>测试用模块</p>
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "stack-trace", version = "1.0.0", author = "xiaobin@shulie.io", description = "方法追踪模块")
public class TraceModule extends ModuleLifecycleAdapter implements ExtensionModule {
    private final Logger logger = LoggerFactory.getLogger(TraceModule.class);

    @Resource
    private ModuleEventWatcher moduleEventWatcher;
    @Resource
    private LoadedClassDataSource loadedClassDataSource;

    private Set<Class<?>> getProxyInterfaceImplClasses(final Class<?> clazz) {
        if (!Proxy.isProxyClass(clazz)) {
            return new HashSet<Class<?>>(Arrays.asList(clazz));
        }
        return loadedClassDataSource.find(new Filter() {
            @Override
            public boolean doClassNameFilter(String javaClassName) {
                return true;
            }

            @Override
            public boolean doClassFilter(ClassDescriptor classDescriptor) {
                String[] interfaces = classDescriptor.getInterfaceTypeJavaClassNameArray();
                return indexOf(interfaces, clazz.getName(), 0) >= 0;
            }

            @Override
            public List<BuildingForListeners> doMethodFilter(MethodDescriptor methodDescriptor) {
                return Collections.EMPTY_LIST;
            }

            @Override
            public List<BuildingForListeners> getAllListeners() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public Set<String> getAllListeningTypes() {
                return Collections.EMPTY_SET;
            }
        });
    }

    @Command(value = "info", description = "方法追踪")
    public CommandResponse trace(final Map<String, String> param) {
        final String classPatternStr = param.get("classPatterns");
        if (StringUtil.isEmpty(classPatternStr)) {
            return CommandResponse.failure("classPatterns must not be empty.");
        }
        final String[] classPatterns = classPatternStr.split(",");
        /**
         * 如果 wait 和 count 都没有填写，则默认统计20条
         */
        final int wait = ParameterUtils.getInt(param, "wait", 5000);

        /**
         * 最多层数
         */
        final int level = ParameterUtils.getInt(param, "level", 0);

        /**
         * 条数限定
         */
        final int limits = ParameterUtils.getInt(param, "limits", 100);

        Set<EventWatcher> childrenWatchers = new ConcurrentHashSet<EventWatcher>();
        List<EventWatcher> watchers = new ArrayList<EventWatcher>();
        try {
            if (wait > 10 * 60 * 1000) {
                return CommandResponse.failure("wait 最大等待时间不能超过10分钟");
            }

            if (limits > 5000) {
                return CommandResponse.failure("limits 最大不能超过5000");
            }

            /**
             * 多少毫秒以下停止
             */
            final int stopInMills = ParameterUtils.getInt(param, "stop", -1);

            Map<String, Queue<TraceView>> traceViews = new ConcurrentHashMap<String, Queue<TraceView>>();
            Set<String> traceMethods = new ConcurrentHashSet<String>();
            Set<Class<?>> classes = findClasses(classPatterns);
            Set<Class<?>> instrumentClasses = new HashSet<Class<?>>();
            boolean foundInterface = false;
            boolean foundEnum = false;
            boolean foundAnnotation = false;
            for (Class clazz : classes) {
                if (clazz.isInterface()) {
                    Set<Class<?>> implClasses = findImplClasses(clazz);
                    instrumentClasses.addAll(implClasses);
                } else if (!clazz.isEnum() && !clazz.isAnnotation()) {
                    instrumentClasses.addAll(getProxyInterfaceImplClasses(clazz));
                } else if (clazz.isEnum()) {
                    foundEnum = true;
                } else if (clazz.isAnnotation()) {
                    foundAnnotation = true;
                }
            }

            if (instrumentClasses.isEmpty()) {
                String errorMsg = "can't found class:" + classPatternStr;
                if (foundInterface) {
                    errorMsg = "can't found impl class with interface:" + classPatternStr;
                } else if (foundEnum) {
                    errorMsg = "can't trace class because of it is a enum:" + classPatternStr;
                } else if (foundAnnotation) {
                    errorMsg = "can't trace class because of it is a annotation:" + classPatternStr;
                }

                return CommandResponse.failure(errorMsg);
            }

            final CountDownLatch latch = new CountDownLatch(1);
            for (Class clazz : instrumentClasses) {
                EventWatcher watcher = new EventWatchBuilder(moduleEventWatcher)
                        .onClass(clazz.getName()).includeSubClasses()
                        .onAnyBehavior()
                        .withInvoke().withCall()
                        .onListener(Listeners.of(TraceListener.class,
                                new Object[]{
                                        classPatterns,
                                        latch,
                                        traceViews,
                                        traceMethods,
                                        childrenWatchers,
                                        level,
                                        limits,
                                        stopInMills,
                                        wait
                                }))
                        .onClass().onWatch();
                watchers.add(watcher);
            }

            if (wait > 0) {
                latch.await(wait, TimeUnit.MILLISECONDS);
            } else if (limits > 0) {
                latch.await();
            }
            return CommandResponse.success(traceViews);
        } catch (Throwable e) {
            logger.error("SIMULATOR: trace module err! class={},limits={}, wait={}", limits, wait, e);
            return CommandResponse.failure(e);
        } finally {
            for (EventWatcher watcher : watchers) {
                try {
                    watcher.onUnWatched();
                } catch (Throwable e) {
                    logger.error("SIMULATOR: trace module unwatched failed! class={},limits={}, wait={}", limits, wait, e);
                }
            }
            for (EventWatcher eventWatcher : childrenWatchers) {
                try {
                    eventWatcher.onUnWatched();
                } catch (Throwable e) {
                    logger.error("SIMULATOR: trace module unwatched failed! class={},limits={}, wait={}", limits, wait, e);
                }
            }
        }

    }

    private Set<Class<?>> findImplClasses(final Class clazz) {
        return loadedClassDataSource.find(new ExtFilter() {
            @Override
            public boolean isIncludeSubClasses() {
                return false;
            }

            @Override
            public boolean isIncludeBootstrap() {
                return true;
            }

            @Override
            public boolean doClassNameFilter(String javaClassName) {
                return true;
            }

            @Override
            public boolean doClassFilter(ClassDescriptor classDescriptor) {
                String[] interfaces = classDescriptor.getInterfaceTypeJavaClassNameArray();
                return indexOf(interfaces, clazz.getName(), 0) >= 0;
            }

            @Override
            public List<BuildingForListeners> doMethodFilter(MethodDescriptor methodDescriptor) {
                return Collections.EMPTY_LIST;
            }

            @Override
            public List<BuildingForListeners> getAllListeners() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public Set<String> getAllListeningTypes() {
                return Collections.EMPTY_SET;
            }
        });
    }

    private Set<Class<?>> findClasses(final String[] classPattern) {
        return loadedClassDataSource.find(new ExtFilter() {
            @Override
            public boolean isIncludeSubClasses() {
                return false;
            }

            @Override
            public boolean isIncludeBootstrap() {
                return true;
            }

            @Override
            public boolean doClassNameFilter(String javaClassName) {
                return PatternMatchUtils.patternMatching(javaClassName, classPattern, WILDCARD);
            }

            @Override
            public boolean doClassFilter(ClassDescriptor classDescriptor) {
                return PatternMatchUtils.patternMatching(classDescriptor.getClassName(), classPattern, WILDCARD);
            }

            @Override
            public List<BuildingForListeners> doMethodFilter(MethodDescriptor methodDescriptor) {
                return Collections.EMPTY_LIST;
            }

            @Override
            public List<BuildingForListeners> getAllListeners() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public Set<String> getAllListeningTypes() {
                return Collections.EMPTY_SET;
            }
        });
    }

    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return -1;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else if (array.getClass().getComponentType().isInstance(objectToFind)) {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

}
