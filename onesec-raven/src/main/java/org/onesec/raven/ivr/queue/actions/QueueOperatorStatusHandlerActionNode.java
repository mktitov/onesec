/*
 * Copyright 2012 Mikhail Titov.
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

import javax.script.Bindings;
import org.onesec.raven.ivr.IvrAction;
import org.onesec.raven.ivr.IvrActionNode;
import org.onesec.raven.ivr.impl.AudioFileNode;
import org.onesec.raven.ivr.impl.IvrConversationScenarioNode;
import org.onesec.raven.ivr.queue.impl.CallsQueuesNode;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ResourceReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=IvrConversationScenarioNode.class)
public class QueueOperatorStatusHandlerActionNode extends BaseNode implements IvrActionNode {
    
    @NotNull @Parameter(defaultValue="IVR/sounds/hello", valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private AudioFileNode helloAudio;
    
    @NotNull @Parameter(defaultValue="IVR/sounds/status/current_status", valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private AudioFileNode currentStatusAudio;
    
    @NotNull @Parameter(defaultValue="IVR/sounds/status/status_available", valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private AudioFileNode availableStatusAudio;
    
    @NotNull @Parameter(defaultValue="IVR/sounds/status/status_unavailable", valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private AudioFileNode unavailableStatusAudio;
    
    @NotNull @Parameter(defaultValue="IVR/sounds/status/press_1_to_change_status", valueHandlerType=ResourceReferenceValueHandlerFactory.TYPE)
    private AudioFileNode pressOneToChangeStatusAudio;
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private CallsQueuesNode callsQueues;
    
    private BindingSupportImpl bindingSupport;

    public IvrAction createAction() {
        return new QueueOperatorStatusHandlerAction(this);
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

    public BindingSupportImpl getBindingSupport() {
        return bindingSupport;
    }

    public AudioFileNode getAvailableStatusAudio() {
        return availableStatusAudio;
    }

    public void setAvailableStatusAudio(AudioFileNode availableStatusAudio) {
        this.availableStatusAudio = availableStatusAudio;
    }

    public CallsQueuesNode getCallsQueues() {
        return callsQueues;
    }

    public void setCallsQueues(CallsQueuesNode callsQueues) {
        this.callsQueues = callsQueues;
    }

    public AudioFileNode getCurrentStatusAudio() {
        return currentStatusAudio;
    }

    public void setCurrentStatusAudio(AudioFileNode currentStatusAudio) {
        this.currentStatusAudio = currentStatusAudio;
    }

    public AudioFileNode getHelloAudio() {
        return helloAudio;
    }

    public void setHelloAudio(AudioFileNode helloAudio) {
        this.helloAudio = helloAudio;
    }

    public AudioFileNode getPressOneToChangeStatusAudio() {
        return pressOneToChangeStatusAudio;
    }

    public void setPressOneToChangeStatusAudio(AudioFileNode pressOneToChangeStatusAudio) {
        this.pressOneToChangeStatusAudio = pressOneToChangeStatusAudio;
    }

    public AudioFileNode getUnavailableStatusAudio() {
        return unavailableStatusAudio;
    }

    public void setUnavailableStatusAudio(AudioFileNode unavailableStatusAudio) {
        this.unavailableStatusAudio = unavailableStatusAudio;
    }
}