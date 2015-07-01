/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.ivr.conference.impl;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = InvisibleNode.class)
public class ConferenceParticipantNode extends BaseNode {
    public final static String JOIN_TIME_ATTR = "joinTime";
    public final static String DISCONNECT_TIME_ATTR = "disconnectTime";
    
    @Parameter
    private String joinTime;
    @Parameter
    private String disconnectTime;

    public String getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(String joinTime) {
        this.joinTime = joinTime;
    }

    public String getDisconnectTime() {
        return disconnectTime;
    }

    public void setDisconnectTime(String disconnectTime) {
        this.disconnectTime = disconnectTime;
    }
}
