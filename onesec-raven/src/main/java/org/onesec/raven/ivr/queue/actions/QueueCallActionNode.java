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
package org.onesec.raven.ivr.queue.actions;

import java.util.Collection;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.CallQueueRequest;
import org.onesec.raven.ivr.queue.CallQueueRequestSender;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class QueueCallActionNode extends BaseNode implements IvrActionNode, DataSource, CallQueueRequestSender
{
    @NotNull @Parameter(defaultValue="true")
    private Boolean continueConversationOnReadyToCommutate;

    @NotNull @Parameter(defaultValue="true")
    private Boolean continueConversationOnReject;

    @Parameter(defaultValue="10")
    private Integer priority;

    @Parameter()
    private String queueId;

    public IvrAction createAction()
    {
        return new QueueCallAction(this, continueConversationOnReadyToCommutate, continueConversationOnReject
                , priority, queueId);
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pull operation not supported by this datasource");
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    public void sendCallQueueRequest(CallQueueRequest request)
    {
        DataSourceHelper.sendDataToConsumers(this, request, new DataContextImpl());
    }

    public Boolean getContinueConversationOnReadyToCommutate() {
        return continueConversationOnReadyToCommutate;
    }

    public void setContinueConversationOnReadyToCommutate(Boolean continueConversationOnReadyToCommutate) {
        this.continueConversationOnReadyToCommutate = continueConversationOnReadyToCommutate;
    }

    public Boolean getContinueConversationOnReject() {
        return continueConversationOnReject;
    }

    public void setContinueConversationOnReject(Boolean continueConversationOnReject) {
        this.continueConversationOnReject = continueConversationOnReject;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }
}
