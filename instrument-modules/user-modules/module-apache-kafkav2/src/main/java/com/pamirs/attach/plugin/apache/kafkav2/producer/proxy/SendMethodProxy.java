package com.pamirs.attach.plugin.apache.kafkav2.producer.proxy;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Pradar;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class SendMethodProxy implements ShadowMethodProxy {
    private static final Logger logger = LoggerFactory.getLogger(SendMethodProxy.class);

    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) throws Exception {
        if (args != null && args.length > 0) {
            KafkaProducer kafkaProducer = (KafkaProducer) shadowTarget;
            ProducerRecord bizRecord = (ProducerRecord) args[0];
            String topic = bizRecord.topic();
            topic = Pradar.isClusterTestPrefix(topic) ? topic : Pradar.addClusterTestPrefix(topic);
            ProducerRecord shadowProducerRecord = new ProducerRecord(
                    topic,
                    null,
                    null,
                    bizRecord.key(),
                    bizRecord.value());
            // apache-kafka 0.10.1.1
            boolean hasHeaders = ReflectionUtils.existsField(bizRecord.getClass(), "headers");
            if (hasHeaders) {
                ReflectionUtils.set(shadowProducerRecord, "headers", bizRecord.headers());
            }
            if (args.length == 1) {
                return kafkaProducer.send(shadowProducerRecord);
            } else if (args.length == 2) {
                return kafkaProducer.send(shadowProducerRecord, (Callback) args[1]);
            }
        }
        throw new RuntimeException("not support apache-kafka version!");
    }
}
