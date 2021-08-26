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
package com.pamirs.attach.plugin.neo4j.operation;

import com.pamirs.attach.plugin.neo4j.config.Neo4JSessionExt;
import org.neo4j.ogm.session.event.Event;

/**
 * @ClassName: NotifyListenersOperation
 * @author: wangjian
 * @Date: 2020/8/1 00:12
 * @Description:
 */
public class NotifyListenersOperation extends AbstractOperation implements Operation {

    @Override
    public Object invoke(Neo4JSessionExt neo4jSession, Object[] args) {
        check(neo4jSession);
        neo4jSession.notifyListeners((Event) args[0]);
        return null;
    }
}
