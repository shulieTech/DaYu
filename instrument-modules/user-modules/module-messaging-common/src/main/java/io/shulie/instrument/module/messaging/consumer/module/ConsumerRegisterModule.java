package io.shulie.instrument.module.messaging.consumer.module;

import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ConsumerRegisterModule {
    private final Map<String, SyncObject> syncObjectMap = new ConcurrentHashMap<String, SyncObject>();
    private final Map<SyncObjectData,ShadowConsumerExecute> syncObjectDataMap = new ConcurrentHashMap<>();
    private ConsumerRegister consumerRegister;
    private ConsumerIsolationRegister isolationRegister;

    private boolean isEnhanced;

    private String name;

    public ConsumerIsolationRegister getIsolationRegister() {
        return isolationRegister;
    }

    public ConsumerRegisterModule(ConsumerRegister consumerRegister) {
        this.consumerRegister = consumerRegister;
        name = consumerRegister.getName();
    }

    public ConsumerRegisterModule(ConsumerRegister consumerRegister, ConsumerIsolationRegister isolationRegister) {
        this(consumerRegister);
        this.isolationRegister = isolationRegister;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnhanced() {
        return isEnhanced;
    }

    public void setEnhanced(boolean enhanced) {
        isEnhanced = enhanced;
    }

    public Map<String, SyncObject> getSyncObjectMap() {
        return syncObjectMap;
    }

    public Map<SyncObjectData, ShadowConsumerExecute> getSyncObjectDataMap() {
        return syncObjectDataMap;
    }

    public ConsumerRegister getConsumerRegister() {
        return consumerRegister;
    }

    public void setConsumerRegister(ConsumerRegister consumerRegister) {
        this.consumerRegister = consumerRegister;
    }
}
