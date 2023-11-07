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
package com.shulie.instrument.simulator.core.util.matcher.structure;

import com.google.common.collect.HashBasedTable;
import com.shulie.instrument.simulator.core.ignore.IgnoredTypesPredicateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * 类结构工厂类
 * <p>
 * 根据构造方式的不同，返回的实现方式也不一样。但无论哪一种实现方式都尽可能符合接口约定。
 * </p>
 */
public class ClassStructureFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClassStructureFactory.class);

    /**
     * 在探针启动完成后禁止缓存
     */
    private static boolean enableCacheClassStructure = true;

    /**
     * 最近获取ClassStructure时间
     */
    private static long latestAccessTime = -1;

    private static HashBasedTable<Object, Integer, ClassStructure> classStructureCache = HashBasedTable.create(8192, 1);

    static {
        Thread thread = new Thread("[SIMULATOR_ClassStructure_Cache]") {
            @Override
            public void run() {
                while (enableCacheClassStructure) {
                    // 5分钟后清空缓存，时间太短可能导致一些sync增强点增强后，应用延迟加载时缓存不生效
                    if (latestAccessTime > 0 && System.currentTimeMillis() - latestAccessTime > 5 * 60 * 1000) {
                        logger.info("[SIMULATOR] clear and forbidden ClassStructure Cache.");
                        enableCacheClassStructure = false;
                        classStructureCache.clear();
                        IgnoredTypesPredicateImpl.clearIgnoredTypesCache();
                        break;
                    }
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        logger.error("ClassStructure_Cache Thread is interrupted.");
                    }
                }
            }
        };
        thread.start();
    }

    /**
     * 通过Class类来构造类结构
     *
     * @param clazz 目标Class类
     * @return JDK实现的类结构
     */
    public static ClassStructure createClassStructure(final Class<?> clazz) {
        latestAccessTime = System.currentTimeMillis();
        if (!enableCacheClassStructure) {
            return new JdkClassStructure(clazz);
        }
        ClassLoader loader = clazz.getClassLoader();
        int hashCode = loader == null ? 0 : loader.hashCode();
        ClassStructure classStructure = classStructureCache.get(clazz, hashCode);
        if (classStructure == null) {
            classStructure = new JdkClassStructure(clazz);
            classStructureCache.put(clazz, hashCode, classStructure);
        }
        return classStructure;
    }

    /**
     * 通过Class类字节流来构造类结构
     *
     * @param classInputStream Class类字节流
     * @param loader           即将装载Class的ClassLoader
     * @return ASM实现的类结构
     */
    public static ClassStructure createClassStructure(final InputStream classInputStream, final ClassLoader loader) {
        latestAccessTime = System.currentTimeMillis();
        try {
            if (!enableCacheClassStructure) {
                return new AsmClassStructure(classInputStream, loader);
            }
            int hashCode = loader == null ? 0 : loader.hashCode();
            ClassStructure classStructure = classStructureCache.get(classInputStream, hashCode);
            if (classStructure == null) {
                classStructure = new AsmClassStructure(classInputStream, loader);
                classStructureCache.put(classInputStream, hashCode, classStructure);
            }
            return classStructure;
        } catch (IOException cause) {
            logger.warn("SIMULATOR: create class structure failed by using ASM, return null. loader={};", loader, cause);
            return null;
        }
    }

    /**
     * 通过Class类字节数组来构造类结构
     *
     * @param classByteArray Class类字节数组
     * @param loader         即将装载Class的ClassLoader
     * @return ASM实现的类结构
     */
    public static ClassStructure createClassStructure(final byte[] classByteArray, final ClassLoader loader) {
        latestAccessTime = System.currentTimeMillis();
        if (!enableCacheClassStructure) {
            return new AsmClassStructure(classByteArray, loader);
        }
        int hashCode = loader == null ? 0 : loader.hashCode();
        ClassStructure classStructure = classStructureCache.get(classByteArray, hashCode);
        if (classStructure == null) {
            classStructure = new AsmClassStructure(classByteArray, loader);
            classStructureCache.put(classByteArray, hashCode, classStructure);
        }
        return classStructure;
    }

}
