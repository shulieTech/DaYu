/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesConfigurer;

public class AdditionalLibraryIgnoredTypesConfigurer implements IgnoredTypesConfigurer {

  // only used by tests (to bypass the ignores check)
  public void configure(IgnoredTypesBuilder builder) {
    builder
        .ignoreClass("com.beust.jcommander.")
        .ignoreClass("com.fasterxml.classmate.")
        .ignoreClass("com.github.mustachejava.")
        .ignoreClass("com.jayway.jsonpath.")
        .ignoreClass("com.lightbend.lagom.")
        .ignoreClass("javax.el.")
        .ignoreClass("org.apache.lucene.")
        .ignoreClass("org.apache.tartarus.")
        .ignoreClass("org.json.simple.")
        .ignoreClass("org.yaml.snakeyaml.")
        .allowClass("org.apache.lucene.util.bkd.BKDWriter$OneDimensionBKDWriter$$Lambda$");

    builder.ignoreClass("net.sf.cglib.").allowClass("net.sf.cglib.core.internal.LoadingCache$2");

    builder
        .ignoreClass("org.springframework.aop.")
        .ignoreClass("org.springframework.cache.")
        .ignoreClass("org.springframework.dao.")
        .ignoreClass("org.springframework.ejb.")
        .ignoreClass("org.springframework.expression.")
        .ignoreClass("org.springframework.format.")
        .ignoreClass("org.springframework.jca.")
        .ignoreClass("org.springframework.jdbc.")
        .ignoreClass("org.springframework.jmx.")
        .ignoreClass("org.springframework.jndi.")
        .ignoreClass("org.springframework.lang.")
        .ignoreClass("org.springframework.messaging.")
        .ignoreClass("org.springframework.objenesis.")
        .ignoreClass("org.springframework.orm.")
        .ignoreClass("org.springframework.scripting.")
        .ignoreClass("org.springframework.stereotype.")
        .ignoreClass("org.springframework.transaction.")
        .ignoreClass("org.springframework.ui.")
        .ignoreClass("org.springframework.validation.");

    builder
        .ignoreClass("org.springframework.remoting.")
        .allowClass("org.springframework.remoting.rmi.RmiBasedExporter")
        .allowClass("org.springframework.remoting.rmi.RmiClientInterceptor")
        .allowClass("org.springframework.remoting.rmi.RmiProxyFactoryBean");

    builder
        .ignoreClass("org.springframework.data.")
        .allowClass("org.springframework.data.repository.core.support.RepositoryFactorySupport")
        .allowClass("org.springframework.data.convert.ClassGeneratingEntityInstantiator$")
        .allowClass("org.springframework.data.jpa.repository.config.InspectionClassLoader")
        .allowClass(
            "org.springframework.data.jpa.repository.query.QueryParameterSetter$NamedOrIndexedQueryParameterSetter$$Lambda$");

    builder
        .ignoreClass("org.springframework.amqp.")
        .allowClass("org.springframework.amqp.rabbit.connection.")
        .allowClass("org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer");

    builder
        .ignoreClass("org.springframework.beans.")
        .allowClass("org.springframework.beans.factory.support.DisposableBeanAdapter")
        .allowClass("org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader$");

    builder
        .ignoreClass("org.springframework.boot.")
        .allowClass("org.springframework.boot.context.web.")
        .allowClass("org.springframework.boot.logging.logback.")
        .allowClass("org.springframework.boot.web.filter.")
        .allowClass("org.springframework.boot.web.servlet.")
        .allowClass("org.springframework.boot.autoconfigure.BackgroundPreinitializer$")
        .allowClass(
            "org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration$$Lambda$")
        .allowClass("org.springframework.boot.autoconfigure.condition.OnClassCondition$")
        .allowClass(
            "org.springframework.boot.autoconfigure.web.ResourceProperties$Cache$Cachecontrol$$Lambda$")
        .allowClass(
            "org.springframework.boot.autoconfigure.web.WebProperties$Resources$Cache$Cachecontrol$$Lambda$")
        .allowClass("org.springframework.boot.web.embedded.netty.NettyWebServer$")
        .allowClass("org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedContext$$Lambda$")
        .allowClass(
            "org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer$")
        .allowClass(
            "org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedWebappClassLoader")
        .allowClass("org.springframework.boot.context.embedded.EmbeddedWebApplicationContext")
        .allowClass(
            "org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext")
        // spring boot 2 classes
        .allowClass(
            "org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext")
        .allowClass(
            "org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext")
        .allowClass("org.springframework.boot.web.embedded.tomcat.TomcatWebServer$")
        .allowClass("org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader")
        .allowClass("org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean$")
        .allowClass("org.springframework.boot.StartupInfoLogger$")
        .allowClass("org.springframework.boot.SpringApplicationShutdownHook");

    builder
        .ignoreClass("org.springframework.cglib.")
        // This class contains nested Callable instance that we'd happily not touch, but
        // unfortunately our field injection code is not flexible enough to realize that, so instead
        // we instrument this Callable to make tests happy.
        .allowClass("org.springframework.cglib.core.internal.LoadingCache$");

    builder
        .ignoreClass("org.springframework.context.")
        // More runnables to deal with
        .allowClass("org.springframework.context.support.AbstractApplicationContext$")
        .allowClass("org.springframework.context.support.ContextTypeMatchClassLoader")
        .allowClass("org.springframework.context.support.DefaultLifecycleProcessor$$Lambda$")
        // Allow instrumenting ApplicationContext implementations - to inject beans
        .allowClass("org.springframework.context.annotation.AnnotationConfigApplicationContext")
        .allowClass("org.springframework.context.support.AbstractApplicationContext")
        .allowClass("org.springframework.context.support.GenericApplicationContext");

    builder
        .ignoreClass("org.springframework.core.")
        .allowClass("org.springframework.core.task.")
        .allowClass("org.springframework.core.DecoratingClassLoader")
        .allowClass("org.springframework.core.OverridingClassLoader")
        .allowClass("org.springframework.core.ReactiveAdapterRegistry$EmptyCompletableFuture");

    builder
        .ignoreClass("org.springframework.instrument.")
        .allowClass("org.springframework.instrument.classloading.SimpleThrowawayClassLoader")
        .allowClass("org.springframework.instrument.classloading.ShadowingClassLoader");

    builder
        .ignoreClass("org.springframework.http.")
        .allowClass("org.springframework.http.client.reactive.AbstractClientHttpRequest$$Lambda$")
        .allowClass("org.springframework.http.client.reactive.ReactorClientHttpConnector$$Lambda$")
        .allowClass("org.springframework.http.codec.multipart.FileStorage$TempFileStorage$$Lambda$")
        // There are some Mono implementation that get instrumented
        .allowClass("org.springframework.http.server.reactive.");

    builder
        .ignoreClass("org.springframework.jms.")
        .allowClass("org.springframework.jms.listener.")
        .allowClass(
            "org.springframework.jms.config.JmsListenerEndpointRegistry$AggregatingCallback");

    builder
        .ignoreClass("org.springframework.util.")
        .allowClass("org.springframework.util.concurrent.");

    builder
        .ignoreClass("org.springframework.web.")
        .allowClass("org.springframework.web.servlet.")
        .allowClass("org.springframework.web.filter.")
        .allowClass("org.springframework.web.multipart.")
        .allowClass("org.springframework.web.reactive.")
        .allowClass("org.springframework.web.context.request.async.")
        .allowClass(
            "org.springframework.web.context.support.AbstractRefreshableWebApplicationContext")
        .allowClass("org.springframework.web.context.support.GenericWebApplicationContext")
        .allowClass("org.springframework.web.context.support.XmlWebApplicationContext");

    // xml-apis, xerces, xalan, but not xml web-services
    builder
        .ignoreClass("javax.xml.")
        .allowClass("javax.xml.ws.")
        .ignoreClass("org.apache.bcel.")
        .ignoreClass("org.apache.html.")
        .ignoreClass("org.apache.regexp.")
        .ignoreClass("org.apache.wml.")
        .ignoreClass("org.apache.xalan.")
        .ignoreClass("org.apache.xerces.")
        .ignoreClass("org.apache.xml.")
        .ignoreClass("org.apache.xpath.")
        .ignoreClass("org.xml.");

    builder
        .ignoreClass("ch.qos.logback.")
        // We instrument this Runnable
        .allowClass("ch.qos.logback.core.AsyncAppenderBase$Worker")
        // Allow instrumenting loggers & events
        .allowClass("ch.qos.logback.classic.Logger")
        .allowClass("ch.qos.logback.classic.spi.LoggingEvent")
        .allowClass("ch.qos.logback.classic.spi.LoggingEventVO");

    builder
        .ignoreClass("com.couchbase.client.deps.")
        // Couchbase library includes some packaged dependencies, unfortunately some of them are
        // instrumented by executors instrumentation
        .allowClass("com.couchbase.client.deps.io.netty.")
        .allowClass("com.couchbase.client.deps.org.LatencyUtils.")
        .allowClass("com.couchbase.client.deps.com.lmax.disruptor.");

    builder
        .ignoreClass("com.google.cloud.")
        .ignoreClass("com.google.instrumentation.")
        .ignoreClass("com.google.j2objc.")
        .ignoreClass("com.google.gson.")
        .ignoreClass("com.google.logging.")
        .ignoreClass("com.google.longrunning.")
        .ignoreClass("com.google.protobuf.")
        .ignoreClass("com.google.rpc.")
        .ignoreClass("com.google.thirdparty.")
        .ignoreClass("com.google.type.");

    builder
        .ignoreClass("com.google.common.")
        .allowClass("com.google.common.util.concurrent.")
        .allowClass("com.google.common.base.internal.Finalizer")
        .allowClass("com.google.common.base.Java8Usage$$Lambda$");

    builder
        .ignoreClass("com.google.inject.")
        // We instrument Runnable there
        .allowClass("com.google.inject.internal.AbstractBindingProcessor$")
        .allowClass("com.google.inject.internal.BytecodeGen$")
        .allowClass("com.google.inject.internal.cglib.core.internal.$LoadingCache$");

    builder.ignoreClass("com.google.api.").allowClass("com.google.api.client.http.HttpRequest");

    builder
        .ignoreClass("org.h2.")
        .allowClass("org.h2.Driver")
        .allowClass("org.h2.jdbc.")
        .allowClass("org.h2.jdbcx.")
        // Some runnables that get instrumented
        .allowClass("org.h2.util.Task")
        .allowClass("org.h2.store.FileLock")
        .allowClass("org.h2.engine.DatabaseCloser")
        .allowClass("org.h2.engine.OnExitDatabaseCloser");

    builder
        .ignoreClass("com.carrotsearch.hppc.")
        .allowClass("com.carrotsearch.hppc.HashOrderMixing$");

    builder
        .ignoreClass("com.fasterxml.jackson.")
        .allowClass("com.fasterxml.jackson.module.afterburner.util.MyClassLoader")
        .allowClass("com.fasterxml.jackson.databind.util.internal.PrivateMaxEntriesMap$AddTask");

    // kotlin, note we do not ignore kotlinx because we instrument coroutines code
    builder.ignoreClass("kotlin.").allowClass("kotlin.coroutines.jvm.internal.DebugProbesKt");
  }

  @Override
  public int order() {
    return 0;
  }
}
