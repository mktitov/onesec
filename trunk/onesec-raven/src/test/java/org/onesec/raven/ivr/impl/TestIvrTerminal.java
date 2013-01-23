/*
 *  Copyright 2011 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import java.util.Collection;
import org.onesec.raven.ivr.Codec;
import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.IvrMediaTerminal;
import org.onesec.raven.ivr.RtpStreamManager;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.sched.ExecutorService;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestIvrTerminal extends BaseNode implements IvrMediaTerminal {

//    private final 

    public String getAddress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public RtpStreamManager getRtpStreamManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ExecutorService getExecutor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public IvrConversationScenario getConversationScenario() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Codec getCodec() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getRtpPacketSize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Integer getRtpMaxSendAheadPacketsCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getEnableIncomingRtp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getEnableIncomingCalls() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getObjectName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getObjectDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeAttribute> generateAttributes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
