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
package org.onesec.raven.ivr.queue.actions;

import java.util.Collection;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.onesec.raven.ivr.IvrEndpointConversation;
import org.onesec.raven.ivr.IvrEndpointConversationState;
import org.onesec.raven.ivr.actions.PlayAudioActionNode;
import org.onesec.raven.ivr.actions.SayNumberActionNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.QueuedCallStatus;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conv.BindingScope;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.AttributeReferenceValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class, importChildTypesFromParent=true)
public class SayTimeLeftForAnswerActionNode extends BaseNode {
    
    public final static String NODE1_NAME = "say operator will answer";
    public final static String NODE2_NAME = "say number";
    public final static String NODE3_NAME = "say minutes";
    public final static String MINUTES_LEFT_ATTR = "minutesLeft";
    public final static String QUEUE_AVG_CALL_DURATION_ATTR = "queueAvgCallDuration";
    public final static String QUEUE_OPERATORS_COUNT_ATTR = "queueOperatorsCount";
    public final static String QUEUE_NUMBER_IN_QUEUE = "queueNumberInQueue";
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean recreateChildNodes;
    
    @NotNull @Parameter(defaultValue="60")
    private Integer minRepeatInterval;
    
    @NotNull @Parameter(defaultValue="60")
    private Integer minCallDuration;
    
    @Parameter(valueHandlerType=AttributeReferenceValueHandlerFactory.TYPE, 
               defaultValue="./@"+QUEUE_AVG_CALL_DURATION_ATTR)
    private Integer avgCallDuration;
    
    @Parameter(valueHandlerType=AttributeReferenceValueHandlerFactory.TYPE, 
               defaultValue="./@"+QUEUE_OPERATORS_COUNT_ATTR)
    private Integer operatorsCount;
    
    @Parameter(valueHandlerType=AttributeReferenceValueHandlerFactory.TYPE, 
               defaultValue="./@"+QUEUE_NUMBER_IN_QUEUE)
    private Integer numberInQueue;
    
    
    private BindingSupportImpl bindingSupport;

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveChildrens() {
        //get conversation bindings
        ConversationScenarioState convState = getConversationState();
        if (convState==null)
            return null;
        Bindings bindings = convState.getBindings();
        //initializing
        bindingSupport.enableScriptExecution();
        try {
            String lastInformTimeKey = getLastInformTimeKey();
            String lastMinutesLeftKey = getLastMinutesLeftKey();
            if (!bindings.containsKey(lastInformTimeKey)) {
                convState.setBinding(lastInformTimeKey, System.currentTimeMillis()-
                                     minRepeatInterval*2*1000, BindingScope.POINT);
                convState.setBinding(lastMinutesLeftKey, Integer.MAX_VALUE, BindingScope.POINT);
            }
            //calculating
            Long lastInformTime = (Long) bindings.get(lastInformTimeKey);
            if (lastInformTime+minCallDuration>System.currentTimeMillis())
                return null;
            QueuedCallStatus callStatus = (QueuedCallStatus) bindings.get(
                    QueueCallAction.QUEUED_CALL_STATUS_BINDING);
            Integer _avgCallDuration = avgCallDuration;
            if (_avgCallDuration==null || _avgCallDuration<=0)
                _avgCallDuration = minCallDuration;
            Integer minutesLeft = numberInQueue * _avgCallDuration / operatorsCount / 60;
            Integer lastMinutesLeft = (Integer) bindings.get(lastMinutesLeftKey);
            if (minutesLeft >= lastMinutesLeft || minutesLeft <= 0)
                return null;
            bindings.put(lastMinutesLeftKey, minutesLeft);
            bindings.put(lastInformTimeKey, System.currentTimeMillis());
            return super.getEffectiveChildrens();
        } finally {
            bindingSupport.reset();
        }
    }

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (recreateChildNodes)
            createChildNodes();
    }
    
    private void createChildNodes() {
        recreateChildNodes = false;
    }
    
    private void createAudioNode(String name, String resourcePath) throws Exception {
        if (getChildren(name)==null) {
            PlayAudioActionNode node = new PlayAudioActionNode();
            node.setName(name);
            addAndSaveChildren(node);
            NodeAttribute attr = node.getNodeAttribute(PlayAudioActionNode.AUDIO_FILE_ATTR);
            attr.setValueHandlerType(ResourceReferenceValueHandlerFactory.TYPE);
            attr.setValue(resourcePath);
            attr.save();
        }
    }
    
    private void createSayNumberNode() throws Exception {
        if (getChildren(NODE2_NAME)==null) {
            SayNumberActionNode node = new SayNumberActionNode();
            node.setName(NODE2_NAME);
            addAndSaveChildren(node);
            NodeAttribute attr = node.getNodeAttribute(SayNumberActionNode.NUMBER_ATTR);
            attr.setValueHandlerType(AttributeReferenceValueHandlerFactory.TYPE);
            attr.setValue("../@"+MINUTES_LEFT_ATTR);
            attr.save();
            node.start();
        }
    }
    
    private ConversationScenarioState getConversationState() {
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        return (ConversationScenarioState) bindings.get(IvrEndpointConversation.CONVERSATION_STATE_BINDING);
    }
    
    private QueuedCallStatus getQueuedCallStatus() {
        ConversationScenarioState state = getConversationState();
        return (QueuedCallStatus) 
               (state==null? null : state.getBindings().get(QueueCallAction.QUEUED_CALL_STATUS_BINDING));
    }
    
    private String getLastInformTimeKey() {
        return RavenUtils.generateKey("lastInformTime", this);
    }
    
    private String getLastMinutesLeftKey() {
        return RavenUtils.generateKey("lastMinutesLeft", this);
    }
    
    @Parameter(readOnly=true)
    public Integer getMinutesLeft() {
        return 0;
    }
    
    @Parameter(readOnly=true)
    public Integer getQueueAvgCallDuration() {
        return null;
    }

    @Parameter(readOnly=true)
    public Integer getQueueOperatorsCount() {
        return null;
    }
    
    @Parameter(readOnly=true)
    public Integer getQueueNumberInQueue() {
        QueuedCallStatus callStatus = getQueuedCallStatus();
        return callStatus==null? null : callStatus.getSerialNumber();
    }

    public Boolean getRecreateChildNodes() {
        return recreateChildNodes;
    }

    public void setRecreateChildNodes(Boolean recreateChildNodes) {
        this.recreateChildNodes = recreateChildNodes;
    }

    public Integer getMinRepeatInterval() {
        return minRepeatInterval;
    }

    public void setMinRepeatInterval(Integer minRepeatInterval) {
        this.minRepeatInterval = minRepeatInterval;
    }

    public Integer getMinCallDuration() {
        return minCallDuration;
    }

    public void setMinCallDuration(Integer minCallDuration) {
        this.minCallDuration = minCallDuration;
    }

    public Integer getAvgCallDuration() {
        return avgCallDuration;
    }

    public void setAvgCallDuration(Integer avgCallDuration) {
        this.avgCallDuration = avgCallDuration;
    }

    public Integer getOperatorsCount() {
        return operatorsCount;
    }

    public void setOperatorsCount(Integer operatorsCount) {
        this.operatorsCount = operatorsCount;
    }

    public Integer getNumberInQueue() {
        return numberInQueue;
    }

    public void setNumberInQueue(Integer numberInQueue) {
        this.numberInQueue = numberInQueue;
    }
    
}
