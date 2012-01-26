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
package org.onesec.raven.ivr.queue.impl;

import org.onesec.raven.ivr.IvrConversationScenario;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.queue.CallQueueRequestWrapper;
import org.onesec.raven.ivr.queue.CallsQueue;
import org.onesec.raven.ivr.queue.CallsQueueOperator;
import org.onesec.raven.ivr.queue.CallsQueueOperatorRef;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=CallsQueuePrioritySelectorNode.class)
public class CallsQueueOperatorRefNode extends BaseNode implements CallsQueueOperatorRef
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsQueueOperator operator;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private IvrConversationScenario conversationScenario;

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private AudioFileNode greeting;

    @Parameter
    private String phoneNumbers;
    
    private int sortIndex;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        sortIndex = 0;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }

    public void setOperator(CallsQueueOperator operator) {
        this.operator = operator;
    }

    public CallsQueueOperator getOperator() {
        return operator;
    }

    public IvrConversationScenario getConversationScenario() {
        return conversationScenario;
    }

    public void setConversationScenario(IvrConversationScenario conversationScenario) {
        this.conversationScenario = conversationScenario;
    }

    public AudioFileNode getGreeting() {
        return greeting;
    }

    public void setGreeting(AudioFileNode greeting) {
        this.greeting = greeting;
    }

    public String getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(String phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public boolean processRequest(CallsQueue queue, CallQueueRequestWrapper request) {
        try {
            return operator.processRequest(
                    queue, request, conversationScenario, greeting, phoneNumbers);
        } catch (Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(
                        String.format("Error occurred while processing request by operator (%s)"
                            , operator.getPath())
                        , e);
            return false;
        }
    }
}
