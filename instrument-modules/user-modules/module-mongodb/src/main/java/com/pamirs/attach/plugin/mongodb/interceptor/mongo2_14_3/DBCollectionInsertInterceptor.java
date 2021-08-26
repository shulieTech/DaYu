/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.util.List;

import com.mongodb.DBCollection;
import com.mongodb.DBEncoder;
import com.mongodb.InsertOptions;
import com.mongodb.WriteConcern;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:18 下午
 */
public class DBCollectionInsertInterceptor extends AbstractDBCollectionInterceptor {

    @Override
    protected boolean check(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null) {
            return false;
        }
        if (args.length == 2) {
            return args[0] instanceof List
                && args[1] instanceof WriteConcern;
        }
        if (args.length == 3) {
            return args[0] instanceof List
                && args[1] instanceof WriteConcern
                && (args[2] == null || args[2] instanceof DBEncoder);
        }
        return false;
    }

    @Override
    protected boolean isRead() {
        return false;
    }

    @Override
    protected Object cutoffShadow(DBCollection ptDbCollection, Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args.length == 2) {
            return ptDbCollection.insert((List)args[0], (InsertOptions)args[1]);
        }
        if (args.length == 3) {
            return ptDbCollection.insert((List)args[0], (WriteConcern)args[1], (DBEncoder)args[2]);
        }
        throw new RuntimeException("this should never happened!");
    }

}
